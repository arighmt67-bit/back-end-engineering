# Back-End Engineering Portfolio Showcase

[![Back-End Monorepo CI](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml/badge.svg)](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml)

Repositori ini merupakan showcase terpadu dan monorepo portofolio untuk seluruh submission proyek pada **Dicoding Back-End Developer, Cloud Computing & Systems Learning Path**, yang mencakup empat spesialisasi utama industri:
1. **Track JavaScript / Node.js Back-End Developer (dengan AWS Cloud)**
2. **Track Python Back-End Developer & Google Cloud Platform (GCP)**
3. **Track Machine Learning Deployment di Google Cloud Platform (GCP & TensorFlow.js)**
4. **Track Systems Programming & CLI Development dengan Rust**

---

## 🧰 Ringkasan Bahasa & Cloud Provider yang Digunakan

* **Bahasa Pemrograman & Frameworks**:
  * **Rust**: Rust 2021 Edition, CLI Parsing via `clap` (Derive feature), Serialization via `serde` & `serde_json`, Native Memory Safety, Zero-Cost Abstractions.
  * **JavaScript (Node.js)**: ES6+, CommonJS, Native Asynchronous / Event-Loop, Hapi.js Framework, Express.js.
  * **Machine Learning Runtime**: TensorFlow.js (`@tensorflow/tfjs`), MobileNetV3 Graph Model inference.
  * **Python**: Python 3.11+, Django REST Framework (DRF), ASGI/WSGI.
  * **PHP**: Digunakan pada modul arsitektur cloud App Engine (CodeIgniter MVC frontend).
* **Database, Storage & Caching**:
  * **NoSQL Database**: Google Cloud Firestore (Native Mode, root collection `predictions`).
  * **Relational DB**: PostgreSQL, MySQL, Cloud SQL.
  * **Cache & Memory Store**: Redis (Cache-aside pattern, invalidation, TTL).
  * **Message Broker & Asynchronous Tasks**: RabbitMQ (AMQP), Celery, Redis Broker.
  * **Local File Storage & Persistence**: JSON File I/O (`conversion.json`).
* **Cloud & Infrastructure**:
  * **Amazon Web Services (AWS)**: EC2 (Ubuntu 22.04 LTS), Elastic IP, EBS, Security Groups, SSH Key Pair, PM2 Process Manager, automated Continuous Deployment via GitHub Actions (`appleboy/ssh-action`).
  * **Google Cloud Platform (GCP)**:
    * **Serverless & Containers**: Google Cloud Run (Fully Managed, auto-scaling 0-2 instance, zero-cost blueprint), Google App Engine (GAE Standard F1 Node.js 22), Artifact Registry.
    * **Compute**: Google Compute Engine (GCE - IaaS Virtual Machine & Startup Script), Google Kubernetes Engine (GKE - Managed K8s).
    * **Storage**: Google Cloud Storage (GCS - Object Storage bucket untuk ML weights & static assets), MinIO S3-compatible storage.
    * **Networking & Resilience**: Cloud Load Balancing, Managed Instance Groups (MIG), Custom Mode VPC, Network Firewall Rules, Cloud Monitoring Dashboards.
    * **Security & IAM**: Principle of Least Privilege role binding untuk auditor reviewer.

---

## 🗺️ Master Directory Structure

```text
back-end-engineering/
│
├── 01-backend-javascript/
│   ├── a-pemula-bookshelf-api/             # Belajar Back-End Pemula dengan JavaScript
│   │   └── (Hapi.js, REST API CRUD, Postman Automated Tests)
│   ├── b-nodejs-developer-labs/            # Menjadi Node.js Application Developer
│   │   └── (Event Loop, Debugger/Inspector, Streams, Promises, Error Handling)
│   ├── c-fundamental-openjob-api/          # Belajar Fundamental Back-End dengan JavaScript
│   │   ├── openjob-api/                    # REST API, PostgreSQL, Redis Caching, RabbitMQ Producer, Multer
│   │   └── openjob-consumer/               # Asynchronous Consumer, Nodemailer, Worker Service
│   └── d-expert-forum-api/                 # Menjadi Back-End Developer Expert dengan JavaScript
│       └── (Clean Architecture, DDD, TDD 100% Coverage, CI/CD ke AWS EC2, Nginx Rate Limiting)
│
├── 02-backend-python-gcp/
│   ├── a-cloud-engineer-profile-app/       # Belajar Membuat Aplikasi Back-End Pemula dengan Google Cloud
│   │   └── (GCE Virtual Machine, GCS Storage, Nginx, Automation Startup Scripts)
│   ├── b-pemula-python-openshop-api/       # Belajar Back-End Pemula dengan Python
│   │   └── (Django REST Framework, Model Serializers, Automated Testing)
│   ├── c-cloud-engineer-money-tracker/     # Menjadi Google Cloud Engineer
│   │   ├── money-tracker-backend/          # Node.js 22 di Google App Engine + Cloud SQL + GCS
│   │   └── money-tracker-frontend/         # PHP CodeIgniter di GAE terhubung ke Backend API
│   ├── d-fundamental-python-dicoevent/     # Belajar Fundamental Back-End dengan Python
│   │   ├── versi-1/                        # DRF, JWT Authentication, Custom RBAC Permissions
│   │   └── versi-2/                        # Celery Background Worker, Redis Cache, MinIO S3, Loguru
│   └── e-cloud-architect-notes-api-k8s/    # Menjadi Google Cloud Architect
│       ├── k8s/                            # Manifests K8s Deployment & LoadBalancer Service di GKE
│       └── submission-arsitektur-andal/     # Managed Instance Group, VPC Custom, Load Balancer, Monitoring
│
├── 03-machine-learning-gcp/
│   └── asclepius/                          # Belajar Penerapan Machine Learning dengan Google Cloud (Bintang 5)
│       ├── backend/                        # Serverless Hapi.js API + TF.js on Cloud Run
│       ├── frontend/                       # Web UI on App Engine Standard F1 (Node.js 22)
│       ├── requirements.json               # Metadata Evaluasi Submission
│       └── README.md                       # Dokumentasi Lengkap Arsitektur & Newman Test
│
└── 05-rust-systems/
    └── unitconv/                           # Belajar Pemrograman Rust untuk Pemula (Bintang 5)
        ├── Cargo.toml                      # Rust Manifest & Dependencies (clap, serde, serde_json)
        ├── conversion.json                 # Local Persistent History File
        ├── src/main.rs                     # Temperature & Length CLI Engine
        └── README.md                       # Dokumentasi Perintah CLI & Pengujian
```

---

## 📋 Detail Spesifikasi Tiap Modul

### Track 1: Back-End Developer JavaScript

| Sub-Modul | Course Dicoding | Tech Stack | Fitur & Arsitektur Utama |
| :--- | :--- | :--- | :--- |
| **`a-pemula-bookshelf-api`** | Belajar Back-End Pemula dengan JavaScript | Hapi.js, Node.js | Validasi payload JSON, full CRUD buku, query parameters filtering (name, reading, finished), lolos 100% tes otomasi Postman. |
| **`b-nodejs-developer-labs`** | Menjadi Node.js Application Developer | Node.js Core, Inspect CDP, V8 | Diagnostic breakpoint, inspect mode, ESM/CJS module interoperability, custom EventEmitter, dynamic error handling, concurrent Promise resolution. |
| **`c-fundamental-openjob-api`** | Belajar Fundamental Back-End dengan JavaScript | Node.js, Express, PostgreSQL, Redis, RabbitMQ | Otentikasi JWT (access & refresh token), upload berkas PDF ke storage lokal, server-side caching Redis 1 jam dengan header `X-Data-Source`, asynchronous message broker RabbitMQ dengan worker consumer pengirim email. |
| **`d-expert-forum-api`** | Menjadi Back-End Developer Expert dengan JavaScript | Node.js, Hapi/Express, PostgreSQL, Vitest, Nginx, AWS EC2, PM2, GitHub Actions | Implementasi **Clean Architecture** (Entities, Domain, Use Cases, Interfaces, Frameworks/Infrastructures). Dependency Injection container, TDD dengan 100% coverage, automated CI/CD pipeline ke **AWS EC2**, reverse proxy Nginx hardening (Rate Limiting `/threads` 90r/m & HTTPS Let's Encrypt), PM2 zero-downtime reload. |

---

### Track 2: Back-End Developer Python & Google Cloud Platform (GCP)

| Sub-Modul | Course Dicoding | Tech Stack / Cloud Services | Fitur & Arsitektur Utama |
| :--- | :--- | :--- | :--- |
| **`a-cloud-engineer-profile-app`** | Belajar Membuat Aplikasi Back-End Pemula dg GCP | GCP Compute Engine (GCE), Google Cloud Storage (GCS), Nginx | Provisioning VM Linux Ubuntu di region Jakarta (`asia-southeast2`), static asset hosting via GCS public bucket, bootstrap konfigurasi via metadata startup script. |
| **`b-pemula-python-openshop-api`** | Belajar Back-End Pemula dengan Python | Python, Django REST Framework | REST API e-commerce katalog produk, model serializers, filtering, pagination, custom exceptions, unit testing DRF. |
| **`c-cloud-engineer-money-tracker`** | Menjadi Google Cloud Engineer | GCP App Engine (Standard), Cloud SQL, Cloud Storage, Node.js, PHP | Arsitektur multi-service di GCP: backend API Node.js dan frontend PHP berjalan di App Engine, terhubung ke Cloud Storage bucket untuk upload bukti transaksi dan Cloud SQL database. |
| **`d-fundamental-python-dicoevent`** | Belajar Fundamental Back-End dengan Python | Python, Django, Celery, Redis, MinIO S3, Loguru | **Versi 1**: Otentikasi JWT dengan granular Role-Based Access Control (RBAC).<br>**Versi 2**: Asynchronous task queue via Celery & Redis, object storage MinIO (S3 compatible), database caching Redis, structured rotating logging. |
| **`e-cloud-architect-notes-api-k8s`** | Menjadi Google Cloud Architect | GCP GKE, Managed Instance Groups, Cloud Load Balancing, VPC | Desain arsitektur cloud berdaya tahan tinggi (High Availability & Scalability), orkestrasi container Notes API di Google Kubernetes Engine (GKE), konfigurasi auto-scaling MIG, Custom VPC, dan custom monitoring dashboard. |

---

### Track 3: Machine Learning Deployment di Google Cloud (MLGC)

| Sub-Modul | Course Dicoding | Tech Stack / Cloud Services | Fitur & Arsitektur Utama (Bintang 5) |
| :--- | :--- | :--- | :--- |
| **`03-machine-learning-gcp/asclepius`** | Belajar Penerapan Machine Learning dengan Google Cloud | Node.js, Hapi.js, TensorFlow.js (MobileNetV3), Cloud Run, App Engine, GCS, Firestore Native | **Target Evaluasi Bintang 5**:<br>• **Serverless Backend (Cloud Run)**: Auto-scaling 0-2 instance, otomatis memenuhi kriteria Compute Engine & Static IP tanpa biaya sewa IP statis.<br>• **Decoupled Model Storage (GCS)**: Model di-load dinamis dari bucket `gs://submissionmlgc-arirahmatr-model`.<br>• **NoSQL Database (Firestore Native)**: Endpoint GET `/predict/histories` & logging riwayat prediksi ke collection `predictions`.<br>• **Security & Least Privilege**: Hak akses auditor eksternal dibatasi spesifik (*Viewer/Reader only*).<br>• **Newman Tested**: 100% lolos 15/15 assertions Postman resmi Dicoding (201, 400, 413, 200). |

---

### Track 4: Systems Programming & CLI Development (Rust)

| Sub-Modul | Course Dicoding | Tech Stack | Fitur & Arsitektur Utama (Bintang 5) |
| :--- | :--- | :--- | :--- |
| **`05-rust-systems/unitconv`** | Belajar Pemrograman Rust untuk Pemula | Rust 2021, Cargo, Clap v4, Serde, Serde JSON | **Target Evaluasi Bintang 5 (Advance)**:<br>• **Konversi Suhu & Panjang**: Mendukung `celsius`, `fahrenheit`, `kelvin`, `cm`, `inch`, `km`, dan `miles` dengan format angka presisi.<br>• **Subcommand List**: `unitconv list` menampilkan seluruh 7 satuan terkelompok kategori `[suhu]` dan `[panjang]`.<br>• **Ketahanan Data (Persistensi)**: Pencatatan riwayat otomatis ke berkas lokal `conversion.json` & audit riwayat via `unitconv history`.<br>• **Logika & Strict Error Handling**: Menolak satuan tak dikenal tanpa panic dan memblokir anomali konversi lintas kategori (`[panjang] cm → [suhu] celsius`). |

---

## 👤 Author

* **Nama**: Ari Rahmat Romadhon
* **GitHub**: [@arighmt67-bit](https://github.com/arighmt67-bit)
* **LinkedIn**: [Ari Rahmat Romadhon](https://www.linkedin.com/in/arirahmatr/)
* **Platform**: Dicoding Indonesia - Back-End Developer, Cloud Computing & Systems Learning Paths
