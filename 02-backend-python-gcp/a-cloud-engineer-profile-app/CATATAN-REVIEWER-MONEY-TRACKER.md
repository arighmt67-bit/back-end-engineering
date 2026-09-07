# Catatan untuk Reviewer: Proyek Akhir Money Tracker App

Yth. Tim Reviewer Dicoding,
Berikut adalah detail implementasi dan verifikasi untuk **Proyek Akhir: Money Tracker App** pada kelas **Menjadi Google Cloud Engineer (Dicoding Kelas 133)**.

---

### 1. Ringkasan Data Submission (`project.json`)

```json
{
  "project_name": "submission-mgce-arirahmatr",
  "url_fe": "http://frontend.project-bbbe31b4-c727-4c5a-96a.et.r.appspot.com",
  "url_be": "https://backend-dot-project-bbbe31b4-c727-4c5a-96a.et.r.appspot.com",
  "bucket_name": "money-tracker-storage-project-bbbe31b4-c727-4c5a-96a"
}
```

- **Project ID GCP**: `project-bbbe31b4-c727-4c5a-96a`
- **Project Name GCP**: `submission-mgce-arirahmatr`
- **Region Seluruh Layanan**: `asia-southeast2` (Jakarta)

---

### 2. Pemenuhan Kriteria Wajib dan Saran Bintang 5

| Kriteria / Fitur | Spesifikasi Implementasi | Status | Bukti Nyata / Endpoint |
|---|---|---|---|
| **Kriteria 1: Project Name** | Sesuai format `submission-mgce-namapeserta` | ✅ Terpenuhi | `submission-mgce-arirahmatr` |
| **Kriteria 2: Akses Reviewer** | Hak akses reviewer ke `reviewer_googlecloud@dicoding.com` | ✅ Terpenuhi | `roles/viewer`, `roles/storage.admin`, `roles/appengine.appViewer`, `roles/cloudsql.viewer`, `roles/browser`, `roles/artifactregistry.reader` |
| **Kriteria 3: Deploy App** | Full stack deployed dan functional | ✅ Terpenuhi | Frontend, Backend, Cloud SQL, dan Cloud Storage aktif terhubung |
| **Saran 1: Kesesuaian Region & Layanan** | Semua service di `asia-southeast2` (Jakarta) sesuai arsitektur | ⭐ Bintang 5 | GAE (`asia-southeast2`), Cloud SQL (`asia-southeast2-b`), GCS (`asia-southeast2`) |
| **Saran 2: Principle of Least Privilege** | Tidak memberikan `roles/editor` atau `roles/owner` | ⭐ Bintang 5 | Hanya role view spesifik per service (`appViewer`, `cloudsql.viewer`, `browser`) |
| **Saran 3: GCS Lifecycle Management** | Menghapus object lama secara otomatis | ⭐ Bintang 5 | Rule aktif: Hapus otomatis objek berusia > 30 hari (`Delete age 30`) |

---

### 3. Detail Arsitektur dan Layanan yang Digunakan

1. **Google App Engine (Standard Environment)**
   - **Frontend Service (`service: frontend`)**:
     - Runtime: `php83` (CodeIgniter 3.1.10)
     - URL: `http://frontend.project-bbbe31b4-c727-4c5a-96a.et.r.appspot.com` (dan HTTPS: `https://frontend-dot-project-bbbe31b4-c727-4c5a-96a.et.r.appspot.com`)
   - **Backend Service (`service: backend`)**:
     - Runtime: `nodejs22` (Express.js REST API)
     - URL: `https://backend-dot-project-bbbe31b4-c727-4c5a-96a.et.r.appspot.com`

2. **Cloud SQL for MySQL 5.7**
   - **Instance Name**: `money-tracker-db`
   - **Tier**: `db-f1-micro` (Region `asia-southeast2-b`)
   - **Database**: `money_tracker_db`
   - **Public IP**: `34.101.166.60`
   - **Tabel**: `records` (id, name, amount, date, notes, attachment)

3. **Cloud Storage (GCS)**
   - **Bucket Name**: `money-tracker-storage-project-bbbe31b4-c727-4c5a-96a`
   - **Location**: `asia-southeast2` (Jakarta)
   - **Public Read Access**: `allUsers:objectViewer` aktif
   - **Lifecycle Management**: Rule Delete Action dengan kondisi age = 30 hari

---

### 4. Hasil Pengujian Fungsional (Live Probe)

1. **Backend Health Check**:
   - `GET /` -> `HTTP 200 OK` (`Response Success!`)
   - `GET /dashboard` -> `HTTP 200 OK` (`[{"month_records":1,"total_amount":5000000}]`)
   - `GET /getrecords` -> `HTTP 200 OK`
2. **Upload Berkas ke Cloud Storage**:
   - `POST /insertrecord` berhasil mengunggah file attachment ke GCS dan menyimpan metadata ke Cloud SQL.
   - Contoh file attachment live: `https://storage.googleapis.com/money-tracker-storage-project-bbbe31b4-c727-4c5a-96a/20260906-043403-slip_gaji.png` (`HTTP 200 OK`, Content-Type: `image/png`).
3. **Frontend Dashboard**:
   - Menampilkan catatan transaksi secara dinamis dari Cloud SQL melalui Backend API.

Terima kasih atas waktu dan dedikasi tim reviewer!
