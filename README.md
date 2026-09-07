# Back-End Engineering Portfolio Showcase

Repositori ini merupakan showcase terpadu dan monorepo portofolio untuk seluruh submission proyek pada **Dicoding Back-End Developer Learning Path**, yang mencakup dua spesialisasi utama industri:
1. **Track JavaScript / Node.js Back-End Developer (dengan AWS Cloud)**
2. **Track Python Back-End Developer & Google Cloud Platform (GCP)**

---

## 🧰 Ringkasan Bahasa & Cloud Provider yang Digunakan

* **Bahasa Pemrograman**:
  * **JavaScript (Node.js)**: ES6+, CommonJS, Native Asynchronous / Event-Loop, Hapi.js Framework, Express.js.
  * **Python**: Python 3.11+, Django REST Framework (DRF), ASGI/WSGI.
  * **PHP**: Digunakan pada modul arsitektur cloud App Engine (CodeIgniter MVC frontend).
* **Database & Caching**:
  * **Relational DB**: PostgreSQL, MySQL, Cloud SQL.
  * **Cache & Memory Store**: Redis (Cache-aside pattern, invalidation, TTL).
  * **Message Broker & Asynchronous Tasks**: RabbitMQ (AMQP), Celery, Redis Broker.
* **Cloud & Infrastructure**:
  * **Amazon Web Services (AWS)**: EC2 (Ubuntu 22.04 LTS), Elastic IP, EBS, Security Groups, SSH Key Pair, PM2 Process Manager, automated Continuous Deployment via GitHub Actions (`appleboy/ssh-action`).
  * **Google Cloud Platform (GCP)**:
    * **Compute**: Google Compute Engine (GCE - IaaS Virtual Machine & Startup Script), Google App Engine (GAE - PaaS), Google Kubernetes Engine (GKE - Managed K8s).
    * **Storage**: Google Cloud Storage (GCS - Object Storage, public bucket, signed URL), MinIO S3-compatible storage.
    * **Networking & Resilience**: Cloud Load Balancing, Managed Instance Groups (MIG), Custom Mode VPC, Network Firewall Rules, Cloud Monitoring Dashboards.

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
└── 02-backend-python-gcp/
    ├── a-cloud-engineer-profile-app/       # Belajar Membuat Aplikasi Back-End Pemula dengan Google Cloud
    │   └── (GCE Virtual Machine, GCS Storage, Nginx, Automation Startup Scripts)
    ├── b-pemula-python-openshop-api/       # Belajar Back-End Pemula dengan Python
    │   └── (Django REST Framework, Model Serializers, Automated Testing)
    ├── c-cloud-engineer-money-tracker/     # Menjadi Google Cloud Engineer
    │   ├── money-tracker-backend/          # Node.js 22 di Google App Engine + Cloud SQL + GCS
    │   └── money-tracker-frontend/         # PHP CodeIgniter di GAE terhubung ke Backend API
    ├── d-fundamental-python-dicoevent/     # Belajar Fundamental Back-End dengan Python
    │   ├── versi-1/                        # DRF, JWT Authentication, Custom RBAC Permissions
    │   └── versi-2/                        # Celery Background Worker, Redis Cache, MinIO S3, Loguru
    └── e-cloud-architect-notes-api-k8s/    # Menjadi Google Cloud Architect
        ├── k8s/                            # Manifests K8s Deployment & LoadBalancer Service di GKE
        └── submission-arsitektur-andal/     # Managed Instance Group, VPC Custom, Load Balancer, Monitoring
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

## 👤 Author

* **Nama**: Ari Rahmat Romadhon
* **GitHub**: [@arighmt67-bit](https://github.com/arighmt67-bit)
* **LinkedIn**: [Ari Rahmat Romadhon](https://www.linkedin.com/in/arirahmatr/)
* **Platform**: Dicoding Indonesia - Back-End Developer & Cloud Computing Learning Paths
