# Proyek Back-End yang Saya Kerjakan

[![Back-End Monorepo CI](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml/badge.svg)](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml)

Repo ini berisi proyek-proyek yang saya kerjakan selama belajar back-end engineering. Isinya bukan satu aplikasi besar: sebagian berasal dari submission Dicoding, sebagian berupa lab kecil untuk memahami konsep tertentu, dan sisanya adalah latihan mandiri untuk mencoba hal yang belum dibahas di kelas.

Saya memulai dari REST API sederhana di Node.js, lalu beranjak ke autentikasi, database, caching, message broker, background worker, deployment cloud, dan pengujian otomatis. Proyek Java di repo ini adalah latihan mandiri; bukan submission Dicoding.

## Mulai dari mana?

Kalau hanya ingin melihat beberapa proyek yang paling mewakili proses belajar saya, mulai dari sini:

### 1. [Helpdesk event pipeline](04-backend-java-spring/a-helpdesk-api)

Latihan mandiri terbaru saya untuk memahami apa yang terjadi ketika sebuah API mulai bergantung pada beberapa service. Aplikasi utamanya adalah REST API Spring Boot dengan JWT dan kontrol akses. PostgreSQL menyimpan data utama, Redis digunakan sebagai cache untuk pembacaan tiket, dan Kafka membawa event ke notification worker.

Bagian yang paling banyak saya pelajari bukan CRUD-nya, melainkan cara menghadapi kegagalan: cache dibuat *fail-open*, event pembuatan dan perubahan status tiket dicatat melalui transactional outbox, lalu worker mencegah event yang sama diproses dua kali. Stack lengkapnya dapat dijalankan dengan Docker Compose dan diuji dengan skenario Redis maupun Kafka berhenti sementara.

- Java 17, Spring Boot, PostgreSQL, Redis, Kafka, Flyway
- 73 test pada API dan 11 test pada worker
- CI menjalankan build API dan worker, coverage gate API, validasi Compose, dan smoke test alur event
- Dokumentasi: [Helpdesk Ticketing API](04-backend-java-spring/a-helpdesk-api/README.md)

### 2. [Forum API](01-backend-javascript/d-expert-forum-api)

API forum dengan registrasi, autentikasi JWT, thread, komentar, balasan, dan like. Di proyek ini saya berlatih memisahkan domain, use case, interface, dan infrastructure dengan Clean Architecture.

Struktur tersebut membantu saat menulis test dan mengganti implementasi repository, tetapi juga menunjukkan bahwa semakin banyak lapisan berarti semakin banyak kode yang harus dijaga. Karena itu, saya tidak menganggap pola ini sebagai jawaban untuk semua API; manfaatnya baru terasa ketika aturan bisnis dan ketergantungan mulai bertambah.

- Node.js, Express, PostgreSQL, JWT
- Unit, integration, dan functional test
- Versi terpisah: [repo `forum-api`](https://github.com/arighmt67-bit/forum-api) dengan [CI untuk lint dan test](https://github.com/arighmt67-bit/forum-api/actions/workflows/ci.yml)
- Dokumentasi di monorepo: [Forum API](01-backend-javascript/d-expert-forum-api/README.md)

### 3. [DicoEvent API](02-backend-python-gcp/d-fundamental-python-dicoevent)

REST API pengelolaan event yang dibuat dalam dua tahap. Versi pertama berfokus pada model data, JWT, dan role-based access control. Versi kedua menambahkan Redis untuk cache, Celery untuk pekerjaan asinkron, MinIO untuk berkas, dan Loguru untuk pencatatan aplikasi.

Proyek ini membuat saya lebih memahami bahwa menambahkan layanan bukan sekadar menambah daftar teknologi. Cache membutuhkan strategi invalidasi, pekerjaan latar belakang membutuhkan worker yang dipantau, dan penyimpanan objek membawa aturan validasi serta konfigurasi baru.

- Python, Django REST Framework, PostgreSQL
- Redis, Celery, MinIO, dan log berformat tetap dengan rotasi berkas pada versi kedua
- Dokumentasi: [DicoEvent](02-backend-python-gcp/d-fundamental-python-dicoevent/README.md)

## Proyek lain di repo ini

| Area | Proyek | Yang saya kerjakan |
| --- | --- | --- |
| JavaScript | [Bookshelf API](01-backend-javascript/a-pemula-bookshelf-api) | REST API buku dengan Hapi, validasi input, filter, dan pernah diverifikasi menggunakan koleksi Postman Dicoding. |
| JavaScript | [Node.js labs](01-backend-javascript/b-nodejs-developer-labs) | Latihan pemrograman asinkron, module system, promises, debugging, dan error handling. |
| JavaScript | [OpenJob API & consumer](01-backend-javascript/c-fundamental-openjob-api) | PostgreSQL, Redis cache, upload PDF, RabbitMQ, dan worker pengirim email. |
| Python | [OpenShop API](02-backend-python-gcp/b-pemula-python-openshop-api) | API katalog produk dengan Django REST Framework, filtering, dan test. |
| Data | [ETL pipeline](02-backend-python-gcp/f-fundamental-pemrosesan-data-etl) | Extract, transform, dan load ke CSV, PostgreSQL, atau Google Sheets; setiap tahap diuji dengan pytest. |
| Google Cloud | [Profile app](02-backend-python-gcp/a-cloud-engineer-profile-app) | Deployment aplikasi sederhana ke Compute Engine dengan startup script dan Nginx. |
| Google Cloud | [Money Tracker](02-backend-python-gcp/c-cloud-engineer-money-tracker) | Backend dan frontend terpisah di App Engine, dengan Cloud SQL dan Cloud Storage. |
| Google Cloud | [Notes API on Kubernetes](02-backend-python-gcp/e-cloud-architect-notes-api-k8s) | Container dan manifest Kubernetes untuk deployment ke GKE. |
| ML deployment | [Asclepius](03-machine-learning-gcp/asclepius) | Backend inferensi TensorFlow.js di Cloud Run, model di Cloud Storage, dan riwayat prediksi di Firestore. |
| Rust | [unitconv](05-rust-systems/unitconv) | CLI konversi suhu dan panjang dengan Clap, Serde, riwayat lokal, serta penanganan input tidak valid. |

## Struktur repo

```text
01-backend-javascript/   # REST API, Node.js labs, dan message broker
02-backend-python-gcp/   # Django, ETL, serta latihan deployment GCP
03-machine-learning-gcp/ # deployment model TensorFlow.js
04-backend-java-spring/  # helpdesk API dan notification worker (latihan mandiri)
05-rust-systems/         # CLI unit converter
```

Setiap proyek yang cukup besar memiliki README sendiri. Root README ini hanya menjadi peta agar pembaca tidak perlu menelusuri seluruh direktori untuk menemukan proyek yang relevan.

## Pengujian dan CI

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) berjalan pada pull request dan push ke `main`. Cakupannya sengaja ditulis secara spesifik karena belum semua submission lama memakai cara pengujian yang sama.

| Job | Yang benar-benar diperiksa |
| --- | --- |
| `Node.js Lint & Static Checks` | ESLint pada Forum API dan Bookshelf API. |
| `Python ETL Test Suite` | Test untuk tahap extract, transform, dan load pada proyek ETL. |
| `Java Spring Boot Test & Coverage` | `mvn verify` untuk API dan worker, coverage gate pada API, validasi Compose, serta smoke test termasuk simulasi gangguan Redis dan Kafka. |
| `Rust Systems Check & Test` | `cargo check` dan `cargo test` untuk `unitconv`. |

Branch `main` dilindungi oleh empat check tersebut. Perubahan dikirim melalui pull request, dan force push maupun penghapusan `main` dinonaktifkan.

## Menjalankan proyek

Clone repo, lalu masuk ke direktori proyek yang ingin dicoba:

```bash
git clone https://github.com/arighmt67-bit/back-end-engineering.git
cd back-end-engineering
```

Kebutuhan dan cara menjalankan tiap proyek berbeda, jadi ikuti README di direktori masing-masing. Sebagai contoh, Helpdesk API dapat dijalankan secara lokal dengan JDK 17 dan Maven Wrapper:

```bash
cd 04-backend-java-spring/a-helpdesk-api
./mvnw spring-boot:run
```

Untuk menjalankan API, worker, PostgreSQL, Redis, dan Kafka sebagai satu alur, lihat bagian Docker Compose pada [dokumentasi Helpdesk API](04-backend-java-spring/a-helpdesk-api/README.md#event-pipeline-dan-docker-compose).

## Catatan tentang status proyek

Repo ini adalah catatan belajar, bukan kumpulan layanan produksi yang selalu aktif. Beberapa resource AWS dan Google Cloud yang pernah dipakai untuk submission atau latihan sudah dimatikan setelah verifikasi agar tidak terus menimbulkan biaya. Kode sumber, konfigurasi, dan catatan arsitekturnya tetap disimpan di sini.

Saya juga masih merapikan proyek-proyek lama agar standar dokumentasi dan pengujiannya lebih konsisten. Badge CI di atas menunjukkan kondisi pipeline saat ini, bukan jaminan bahwa setiap deployment cloud lama masih tersedia.

## Tentang saya

Saya **Ari Rahmat Romadhon**. Latar belakang saya Sistem Informasi, dan saat ini saya memperdalam back-end, cloud, DevOps, serta security operations melalui proyek yang bisa saya bongkar dan uji sendiri.

- [GitHub](https://github.com/arighmt67-bit)
- [LinkedIn](https://www.linkedin.com/in/arirahmatr/)
