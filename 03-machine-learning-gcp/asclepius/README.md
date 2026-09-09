# Asclepius - Machine Learning Deployment on Google Cloud (Dicoding Bintang 5)

Proyek akhir untuk kelas **Belajar Penerapan Machine Learning dengan Google Cloud (Dicoding)** dengan target evaluasi **Bintang 5 (Memenuhi Semua Kriteria Utama & Semua Poin Saran)**.

Aplikasi ini mendeteksi indikasi kanker kulit (*Cancer* vs *Non-cancer*) berbasis model deep learning MobileNetV3 (TensorFlow.js) yang diarsiteki secara *serverless*, *resilient*, dan menerapkan *Zero-Cost Blueprint* di Google Cloud Platform.

---

## 🏛️ Arsitektur Sistem

```
                 +-----------------------------------+
                 |           End User                |
                 +-----------------+-----------------+
                                   |
                  (HTTP GET Web UI)| (HTTP POST /predict)
                                   v
    +--------------------------------+   +------------------------------------+
    |      Google App Engine         |   |          Google Cloud Run          |
    | (Frontend - Standard F1 Node22)|   |   (Backend - Hapi.js + TF.js)      |
    | URL: submissionmlgc-arirahmatr |   | URL: asclepius-backend-...run.app  |
    +--------------------------------+   +---+----------------------------+---+
                                             |                            |
                             (Load Model)    v                            v (Store History)
                       +---------------------------+       +-------------------------------+
                       |    Google Cloud Storage   |       |       Google Cloud Firestore  |
                       | gs://...-arirahmatr-model |       |      (Native Mode, 'predictions')
                       +---------------------------+       +-------------------------------+
```

---

## 🌟 Kriteria Penilaian Bintang 5 Terpenuhi

| Kriteria / Saran | Status | Implementasi Teknis |
|---|:---:|---|
| **Kriteria 1: Project GCP Baru** | ✅ Lolos | Project ID: `submissionmlgc-arirahmatr` |
| **Kriteria 2: Akses Auditor** | ✅ Lolos | Role Least-Privilege diberikan ke `reviewer_googlecloud@dicoding.com` (Viewer di App Engine, Storage, Run, Datastore, Artifact Registry, Network) |
| **Kriteria 3: API Backend** | ✅ Lolos | POST `/predict` multipart/form-data max 1,000,000 bytes. Format respons Cancer / Non-cancer sesuai format baku. |
| **Kriteria 4: Frontend Deploy** | ✅ Lolos | Deployed ke **Google App Engine (Standard F1 Node.js 22)**. |
| **Kriteria 5: GCS Bucket Model** | ✅ Lolos | Model TensorFlow.js (`model.json` + 4 shards) disimpan di bucket `gs://submissionmlgc-arirahmatr-model`. |
| **Kriteria 6: Firestore Database** | ✅ Lolos | Database `(default)` Firestore Native Mode, collection `predictions`. |
| **Kriteria 7: Static IP & Compute Engine** | ✅ Terpenuhi Otomatis | Menggunakan Cloud Run yang otomatis menggugurkan kewajiban Compute Engine & IP statis sesuai panduan resmi Dicoding halaman 6. |
| **Saran 1: Least Privilege IAM** | ✅ Lolos | Hak akses auditor diberikan spesifik per-layanan (*Viewer/Reader only*), bukan *Editor/Owner* proyek. |
| **Saran 2: Cloud Run Deployment** | ✅ Lolos | Backend dideploy ke **Google Cloud Run (Fully Managed)** dengan zero-scaling (skala ke 0 saat idle). |
| **Saran 3: Riwayat Prediksi** | ✅ Lolos | Endpoint GET `/predict/histories` terintegrasi dengan Firestore Native collection `predictions`. |

---

## 🧪 Validasi Pengujian (Newman CLI)

Pengujian otomatis dijalankan menggunakan Postman Collection resmi `Asclepius.postman_collection.json` langsung terhadap endpoint Google Cloud Run:

```bash
newman run Asclepius.postman_collection.json -e asclepius-cloud-env.json
```

**Hasil:** **15 / 15 Assertions Passed (100% Lolos)**:
* `[Mandatory] Prediction Cancer` $\rightarrow$ **201 Created**
* `[Mandatory] Prediction Non-Cancer` $\rightarrow$ **201 Created**
* `[Mandatory] Prediction With Image Size More Than 1000000 byte` $\rightarrow$ **413 Payload Too Large**
* `[Mandatory] Prediction With Bad Request` $\rightarrow$ **400 Bad Request**
* `[Opsional] getHistories` $\rightarrow$ **200 OK**

---

## ⚙️ Berkas `requirements.json`

```json
{
  "backend-service-url": "https://asclepius-backend-952195566496.asia-southeast2.run.app",
  "frontend-service-url": "https://submissionmlgc-arirahmatr.et.r.appspot.com",
  "project-id": "submissionmlgc-arirahmatr",
  "bucket-name": "submissionmlgc-arirahmatr-model"
}
```
