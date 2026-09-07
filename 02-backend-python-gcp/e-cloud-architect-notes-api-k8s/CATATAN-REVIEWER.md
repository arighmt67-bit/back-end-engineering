# Catatan untuk Reviewer - Proyek Notes API

Halo Tim Reviewer Dicoding,

Berikut adalah informasi detail untuk pengujian dan audit submission Proyek Notes API (Menjadi Google Cloud Architect):

---

## 1. Informasi Utama

| Parameter | Nilai |
|---|---|
| **Project ID** | `project-bbbe31b4-c727-4c5a-96a` |
| **External Endpoint (GKE Service)** | `http://34.50.98.82` (Port 80) |
| **GCP Region / Zone** | `asia-southeast2` (Jakarta) / `asia-southeast2-a` |
| **GKE Cluster Name** | `notes-api-cluster` |
| **Artifact Registry Repo** | `notes-api-repo` |
| **Image URI** | `asia-southeast2-docker.pkg.dev/project-bbbe31b4-c727-4c5a-96a/notes-api-repo/notes-api:v1` |

---

## 2. Pemenuhan Kriteria Utama

1. **Kriteria 1: Simpan Docker Image di Artifact Registry**
   - Repository Artifact Registry: `notes-api-repo` di region `asia-southeast2`.
   - Docker image di-build dan di-push dengan tag: `asia-southeast2-docker.pkg.dev/project-bbbe31b4-c727-4c5a-96a/notes-api-repo/notes-api:v1` (arsitektur `linux/amd64`).

2. **Kriteria 2: Deploy ke Google Kubernetes Engine**
   - GKE Cluster `notes-api-cluster` aktif berjalan.
   - Workload `notes-api-deployment` berjalan 1/1 Pod dalam kondisi `Running`.
   - Service `notes-api-service` bertipe `LoadBalancer` aktif dengan Public External IP `34.50.98.82`.
   - Saat membuka root endpoint `http://34.50.98.82/` mengembalikan JSON `{"statusCode":404,"error":"Not Found","message":"Not Found"}` (sesuai tips di dokumen instruksi).
   - CRUD API berjalan normal pada endpoint `http://34.50.98.82/notes`.

3. **Kriteria 3: Berikan Hak Akses ke Auditor Eksternal**
   - Hak akses telah diberikan kepada `group:reviewer_googlecloud@dicoding.com`.

---

## 3. Penerapan Saran Penilaian (Target Bintang 5)

Proyek ini telah menerapkan 3 saran penilaian untuk efisiensi biaya, arsitektur, dan keamanan:

1. **Konfigurasi GKE cluster yang efektif dan efisien:**
   - **Zone:** `asia-southeast2-a` (Jakarta)
   - **Jumlah Node:** 1 node
   - **Machine Type:** `e2-micro` (hemat credit GCP)
2. **Port Mapping Berbeda dari Port Container:**
   - Container port berjalan pada port `5000`.
   - Service Kubernetes diekspos ke publik menggunakan port `80` (External HTTP baku):
     - `port: 80` (Service) -> `targetPort: 5000` (Pod Container).
3. **Principle of Least Privilege:**
   - Auditor eksternal tidak diberikan hak akses berlebih (`Owner` / `Editor`).
   - Auditor diberikan role minimal yang sesuai kebutuhan audit:
     - `roles/artifactregistry.reader` (membaca Docker image)
     - `roles/container.viewer` (melihat cluster dan workload GKE)
     - `roles/browser` (navigasi resource di GCP Console)

---

## 4. Cara Pengujian Front-End

1. Buka browser (disarankan Firefox / browser non-chromium untuk menghindari auto-redirect ke HTTPS pada protokol HTTP):
   `http://notesapp-v1.dicodingacademy.com/`
2. Klik tombol **Change URL**.
3. Masukkan endpoint:
   `http://34.50.98.82`
4. Tekan simpan/hubungkan. Aplikasi Notes App akan otomatis terhubung ke Back-End Notes API di GKE.

Terima kasih atas waktu dan review yang diberikan!
