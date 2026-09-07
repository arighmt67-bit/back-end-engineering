#!/usr/bin/env bash
#
# Provisioning server produksi Forum API di Ubuntu 24.04 LTS.
#
# Dijalankan SEKALI pada server yang baru dibuat:
#   bash setup-server.sh <DOMAIN> <EMAIL>
#
# Contoh:
#   bash setup-server.sh aconk-forumapi.duckdns.org arirahmatromadhon@gmail.com
#
# Skrip memasang Node.js 22, PostgreSQL, NGINX, PM2, Certbot, dan fail2ban,
# lalu menyiapkan basis data serta konfigurasi NGINX beserta sertifikat HTTPS.

set -euo pipefail

DOMAIN="${1:-}"
EMAIL="${2:-}"

if [[ -z "$DOMAIN" || -z "$EMAIL" ]]; then
  echo "Penggunaan: bash setup-server.sh <DOMAIN> <EMAIL>"
  exit 1
fi

REPO_URL="https://github.com/arighmt67-bit/forum-api.git"
APP_DIR="$HOME/forum-api"
DB_NAME="forumapi"
DB_USER="forumapi"
DB_PASS="$(openssl rand -hex 16)"
ACCESS_TOKEN_KEY="$(openssl rand -hex 32)"
REFRESH_TOKEN_KEY="$(openssl rand -hex 32)"

echo "==> [1/9] Memperbarui paket sistem"
sudo apt-get update -y
sudo apt-get upgrade -y

echo "==> [2/9] Memasang Node.js 22"
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs
node -v

echo "==> [3/9] Memasang PostgreSQL, NGINX, Certbot, fail2ban"
sudo apt-get install -y postgresql postgresql-contrib nginx certbot python3-certbot-nginx fail2ban git

echo "==> [4/9] Menyiapkan basis data"
sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';" || true
sudo -u postgres psql -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;" || true

echo "==> [5/9] Mengambil kode aplikasi"
if [[ -d "$APP_DIR/.git" ]]; then
  cd "$APP_DIR" && git fetch origin master && git reset --hard origin/master
else
  git clone "$REPO_URL" "$APP_DIR"
  cd "$APP_DIR"
fi

echo "==> [6/9] Menulis berkas environment"
cat > "$APP_DIR/.env" <<ENVFILE
NODE_ENV=production
HOST=0.0.0.0
PORT=5000

PGHOST=localhost
PGUSER=$DB_USER
PGDATABASE=$DB_NAME
PGPASSWORD=$DB_PASS
PGPORT=5432

ACCESS_TOKEN_KEY=$ACCESS_TOKEN_KEY
REFRESH_TOKEN_KEY=$REFRESH_TOKEN_KEY
ACCESS_TOKEN_AGE=3000
ENVFILE
chmod 600 "$APP_DIR/.env"

echo "==> [7/9] Memasang dependensi dan menjalankan migrasi"
cd "$APP_DIR"
npm ci
npm run migrate up

echo "==> [8/9] Menjalankan aplikasi dengan PM2"
sudo npm install -g pm2
pm2 delete forum-api 2>/dev/null || true
pm2 start src/app.js --name forum-api
pm2 save
sudo env PATH=$PATH:/usr/bin pm2 startup systemd -u "$USER" --hp "$HOME"

echo "==> [9/9] Mengonfigurasi NGINX dan HTTPS"
# Konfigurasi sementara berbasis HTTP agar Certbot dapat menjalankan
# HTTP-01 challenge. Certbot kemudian menambahkan blok HTTPS sendiri.
sudo tee /etc/nginx/sites-available/forumapi > /dev/null <<NGINXCONF
limit_req_zone \$binary_remote_addr zone=threads_limit:10m rate=90r/m;
limit_req_status 429;

server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN;

    location /threads {
        limit_req zone=threads_limit burst=90 nodelay;
        proxy_pass http://127.0.0.1:5000;
        proxy_http_version 1.1;
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_http_version 1.1;
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINXCONF

sudo ln -sf /etc/nginx/sites-available/forumapi /etc/nginx/sites-enabled/forumapi
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

sudo certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" --redirect

echo ""
echo "======================================================"
echo " SELESAI"
echo "======================================================"
echo " URL aplikasi : https://$DOMAIN"
echo ""
echo " Uji cepat:"
echo "   curl -i https://$DOMAIN/threads/xxx"
echo "   (balasan 404 menandakan aplikasi dan HTTPS berjalan)"
echo ""
echo " Kredensial basis data tersimpan di $APP_DIR/.env"
echo "======================================================"
