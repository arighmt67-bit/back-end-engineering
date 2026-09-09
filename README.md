# Back-End Engineering Showcase & Learning Monorepo

Repositori ini merupakan showcase terpadu dan monorepo portofolio untuk seluruh proyek submission pada **Dicoding Back-End Developer & Cloud AI Learning Path**, dibangun dengan standar arsitektur industri, automated testing ketat, dan kesiapan deploy ke lingkungan cloud enterprise.

---

## 📂 Struktur Monorepo

```
back-end-engineering/
├── 01-backend-javascript/          # Dasar & Praktik Node.js, Hapi.js REST API
├── 02-backend-python-gcp/          # Microservices Python, Flask, Cloud SQL & GCP
└── 03-machine-learning-gcp/        # Deployment Machine Learning (TensorFlow.js) di Google Cloud
    └── asclepius/                  # Proyek Akhir Belajar Penerapan ML dengan Google Cloud (Bintang 5)
        ├── backend/                # Serverless Hapi API + TF.js on Cloud Run
        ├── frontend/               # Web Interface on App Engine Standard F1
        ├── requirements.json       # Metadata evaluasi submission
        └── README.md               # Dokumentasi lengkap arsitektur sistem
```

---

## 🌟 Proyek Unggulan

### 1. Asclepius - Machine Learning Deployment on Google Cloud (Bintang 5)
* **Direktori:** `03-machine-learning-gcp/asclepius/`
* **Tech Stack:** Node.js, Hapi.js, TensorFlow.js (MobileNetV3 Graph Model), Google Cloud Run, Google App Engine, Google Cloud Storage, Google Cloud Firestore Native.
* **Fitur Utama:**
  * **Serverless Backend (Cloud Run):** Auto-scaling 0 hingga 2 instance, hemat biaya, dan responsif.
  * **Decoupled Model Storage (GCS):** Model deep learning di-load secara dinamis dari Cloud Storage bucket.
  * **Database NoSQL (Firestore Native):** Penyimpanan riwayat prediksi dan logging inferensi secara realtime.
  * **Security & Least Privilege:** Role auditor dibatasi khusus (*Viewer/Reader*) tanpa izin modifikasi.
  * **Newman Tested:** 100% lulus 5 skenario uji Postman resmi (201, 400, 413, 200).

---

## 🛠️ Standar & Metodologi
- **Strict Linting & Clean Code**: Pemisahan modul routes, handler, services, dan domain exceptions.
- **Automated API Testing**: Validasi menyeluruh menggunakan Postman Collection via Newman CLI.
- **Zero-Cost Cloud Architecture**: Desain infrastruktur serverless memanfaatkan kuota Always Free Google Cloud.
