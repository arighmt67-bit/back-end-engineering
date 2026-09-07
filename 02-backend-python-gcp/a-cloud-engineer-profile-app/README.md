# Dicoding Kelas 133: Deploy Web Profile ke GCP

Repository submission untuk kelas **Menjadi Google Cloud Engineer (Dicoding Kelas 133)**: Proyek Deploy Aplikasi Profile ke Google Compute Engine (GCE) dan Google Cloud Storage (GCS).

## Arsitektur & Layanan GCP
- **Compute Service:** Google Compute Engine (VM `profile-vm`, Ubuntu 22.04 LTS, Machine Type `e2-micro`, Zone `asia-southeast2-a`).
- **Web Server:** NGINX 1.18.0 (Pure HTML5 & CSS3, No Client-Side Rendering/JavaScript sesuai kriteria).
- **Storage Service:** Google Cloud Storage Bucket (`gs://web-profil-arirahmatr`, Region `asia-southeast2`, Public Access).
- **Live Endpoint:** `http://34.50.103.97/`

## Struktur Berkas
```text
├── index.html                  # Source code halaman profil
├── style.css                   # Styling responsive modern
├── startup-script.sh           # Otomasi instalasi Nginx & deployment via metadata GCE
├── submission_pkg/             # Berkas paket submission Dicoding
│   ├── google-compute-service.png
│   ├── google-storage-service.png
│   └── url.txt
└── README.md
```

## Verifikasi Deployment
- `GET http://34.50.103.97/` -> HTTP 200 OK (Content-Type: text/html)
- `GET http://34.50.103.97/style.css` -> HTTP 200 OK (Content-Type: text/css)
- `GET https://storage.googleapis.com/web-profil-arirahmatr/profile.jpg` -> HTTP 200 OK (Content-Type: image/jpeg)
