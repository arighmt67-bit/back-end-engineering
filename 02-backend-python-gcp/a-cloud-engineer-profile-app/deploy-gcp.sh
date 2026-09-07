#!/bin/bash
# Deploy Aplikasi Profile ke GCP (GCS + Compute Engine) - sekali jalan, output ringkas.
# Dipakai setelah `gcloud auth login` berhasil.
set -uo pipefail

APP_DIR="$HOME/projects/gcp-profile-app"
BUCKET="${BUCKET:-}"
VM="instance-profile-app"
# Default = GCP Always Free tier (biaya $0 selamanya):
#   - 1x e2-micro di us-west1 / us-central1 / us-east1
#   - 5 GB Standard Storage di region US
# Override kalau mau Jakarta (BERBAYAR ~$7/bln): ZONE=asia-southeast2-a REGION=asia-southeast2 ./deploy-gcp.sh
ZONE="${ZONE:-us-central1-a}"
REGION="${REGION:-us-central1}"
LOG="/tmp/gcp-deploy-detail.log"
: > "$LOG"

r() { echo "  $*"; }
run() { "$@" >>"$LOG" 2>&1; }

echo "=== TAHAP 0: cek akun & project ==="
ACC=$(gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null | head -1)
[ -z "$ACC" ] && { echo "GAGAL: belum login. Jalankan: gcloud auth login"; exit 1; }
r "akun    : $ACC"

PROJ=$(gcloud config get-value project 2>/dev/null)
if [ -z "$PROJ" ] || [ "$PROJ" = "(unset)" ]; then
  PROJ=$(gcloud projects list --format="value(projectId)" --limit=1 2>/dev/null)
  [ -z "$PROJ" ] && { echo "GAGAL: tidak ada project GCP. Buat dulu di console.cloud.google.com"; exit 1; }
  run gcloud config set project "$PROJ"
fi
r "project : $PROJ"

# billing wajib untuk Compute Engine
BILL=$(gcloud beta billing projects describe "$PROJ" --format="value(billingEnabled)" 2>/dev/null)
r "billing : ${BILL:-tidak diketahui}"
if [ "$BILL" = "False" ]; then
  echo "GAGAL: billing belum aktif di project $PROJ. Aktifkan dulu (free trial \$300 cukup)."
  exit 1
fi

[ -z "$BUCKET" ] && BUCKET="${PROJ}-profile-web"
r "bucket  : $BUCKET"

echo "=== TAHAP 1: aktifkan API yang dibutuhkan ==="
run gcloud services enable compute.googleapis.com storage.googleapis.com
r "compute + storage API aktif"

echo "=== TAHAP 2: bucket GCS + upload asset ==="
if gcloud storage buckets describe "gs://$BUCKET" >/dev/null 2>&1; then
  r "bucket sudah ada, dipakai ulang"
else
  run gcloud storage buckets create "gs://$BUCKET" --location="$REGION" --uniform-bucket-level-access \
    || { echo "GAGAL: tidak bisa membuat bucket (nama bentrok?). Cek $LOG"; exit 1; }
  r "bucket dibuat di $REGION"
fi

run gcloud storage cp "$APP_DIR/assets/profile.jpg" "gs://$BUCKET/profile.jpg"
run gcloud storage cp "$APP_DIR/assets/banner.jpg"  "gs://$BUCKET/banner.jpg"
r "profile.jpg + banner.jpg terunggah"

run gcloud storage buckets add-iam-policy-binding "gs://$BUCKET" \
  --member=allUsers --role=roles/storage.objectViewer
r "bucket dijadikan public (allUsers: objectViewer)"

echo "=== TAHAP 3: siapkan HTML dengan URL bucket yang benar ==="
mkdir -p /tmp/gcp-web
sed "s|arirahmat-profile-bucket|$BUCKET|g" "$APP_DIR/index.html" > /tmp/gcp-web/index.html
cp "$APP_DIR/style.css" /tmp/gcp-web/style.css
r "URL asset -> storage.googleapis.com/$BUCKET/..."

# startup script: nginx + file langsung tertanam, jadi VM siap tanpa SSH
cat > /tmp/gcp-web/startup.sh <<STARTUP
#!/bin/bash
apt-get update -y
apt-get install -y nginx
rm -rf /var/www/html/*
cat > /var/www/html/index.html <<'HTMLEOF'
$(cat /tmp/gcp-web/index.html)
HTMLEOF
cat > /var/www/html/style.css <<'CSSEOF'
$(cat /tmp/gcp-web/style.css)
CSSEOF
chown -R www-data:www-data /var/www/html
chmod -R 755 /var/www/html
systemctl enable nginx
systemctl restart nginx
STARTUP
r "startup-script disiapkan (nginx auto-install)"

echo "=== TAHAP 4: firewall HTTP ==="
if gcloud compute firewall-rules describe allow-http-profile >/dev/null 2>&1; then
  r "aturan firewall sudah ada"
else
  run gcloud compute firewall-rules create allow-http-profile \
    --allow=tcp:80 --target-tags=http-server --description="Allow HTTP for profile app"
  r "port 80 dibuka untuk tag http-server"
fi

echo "=== TAHAP 5: VM Compute Engine ==="
if gcloud compute instances describe "$VM" --zone="$ZONE" >/dev/null 2>&1; then
  r "VM sudah ada, dipakai ulang"
else
  run gcloud compute instances create "$VM" \
    --zone="$ZONE" --machine-type=e2-micro \
    --image-family=debian-12 --image-project=debian-cloud \
    --tags=http-server \
    --metadata-from-file=startup-script=/tmp/gcp-web/startup.sh \
    || { echo "GAGAL: tidak bisa membuat VM. Cek $LOG"; exit 1; }
  r "VM $VM dibuat (e2-micro, $ZONE)"
fi

IP=$(gcloud compute instances describe "$VM" --zone="$ZONE" \
     --format="value(networkInterfaces[0].accessConfigs[0].natIP)" 2>/dev/null)
r "External IP: $IP"

echo "=== TAHAP 6: tunggu nginx siap ==="
OK=""
for i in $(seq 1 30); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "http://$IP" 2>/dev/null)
  if [ "$CODE" = "200" ]; then OK="ya"; r "web hidup setelah ~$((i*10)) detik (HTTP 200)"; break; fi
  sleep 10
done
[ -z "$OK" ] && r "PERINGATAN: belum 200 setelah 5 menit (boot masih jalan?)"

echo "$IP" > "$APP_DIR/url.txt"
echo
echo "RINGKASAN"
echo "  BUCKET=$BUCKET"
echo "  IP=$IP"
echo "  URL=http://$IP"
echo "  FOTO=https://storage.googleapis.com/$BUCKET/profile.jpg"
echo "  STATUS_HTTP=${CODE:-belum}"
