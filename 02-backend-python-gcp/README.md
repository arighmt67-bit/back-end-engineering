# Python Back-End & Google Cloud Platform

Kumpulan proyek back-end Python dan arsitektur cloud dari Dicoding
**Back-End Developer** & **Cloud Engineer / Cloud Architect** learning path.

[![CI](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml/badge.svg)](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml)
[![Python](https://img.shields.io/badge/Python-3.10%2B-3776AB?logo=python&logoColor=white)](https://www.python.org)
[![Django](https://img.shields.io/badge/Django-4.2%20LTS-092E20?logo=django&logoColor=white)](https://www.djangoproject.com)
[![DRF](https://img.shields.io/badge/Django%20REST-3.x-A30000?logo=django&logoColor=white)](https://www.django-rest-framework.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)

---

## Ringkasan Proyek

| # | Proyek | Fokus Teknis | Stack Utama |
| --- | --- | --- | --- |
| **d** | **DicoEvent API** | REST API manajemen event dengan autentikasi, RBAC, caching, dan *async task* | Django REST Framework, PostgreSQL, JWT, Redis, Celery, MinIO, Loguru |
| **b** | **OpenShop API** | REST API e-commerce dengan pencarian, HATEOAS, dan 20 unit test | Django REST Framework, SQLite |
| **f** | **ETL Pipeline** | Pipeline *extract–transform–load* terjadwal dengan pengujian otomatis | pandas, SQLAlchemy, PostgreSQL, pytest, cron |
| **e** | **Notes API on Kubernetes** | Deployment API ke Google Kubernetes Engine | Node.js, Docker, Kubernetes, GKE |
| **c** | **Money Tracker** | Aplikasi *full-stack* dengan pemisahan backend/frontend | Back-end API + client |
| **a** | **Cloud Profile App** | Provisioning VM dengan *startup script* otomatis | Compute Engine, Bash |

---

## `d-fundamental-python-dicoevent` — DicoEvent API

Proyek back-end paling lengkap pada direktori ini. Dikerjakan dalam dua versi
yang saling melanjutkan.

### Versi 1 — Fondasi REST API

| Aspek | Implementasi |
| --- | --- |
| Framework | Django 4.2 LTS + Django REST Framework |
| Basis data | PostgreSQL, kredensial via *environment variable* |
| Model & relasi | `Event`, `Ticket`, `Order` beserta relasi antar tabel (ERD tersedia) |
| Autentikasi | JWT — *login* dan *refresh token* |
| Otorisasi | *Role-Based Access Control* (admin / user) per *endpoint* |

### Versi 2 — Kapabilitas Produksi

| Kriteria | Implementasi |
| --- | --- |
| **Pengelolaan berkas media** | MinIO SDK — validasi ukuran maks. 500 kB dan MIME type, tabel `event_posters`, *endpoint* penyajian berkas |
| **Caching** | Redis — TTL 1 jam pada daftar & detail event, invalidasi otomatis saat data berubah, header `X-Data-Source` untuk menandai *cache hit/miss* |
| **Asynchronous task** | Celery — *email reminder* terjadwal H-2 jam sebelum event dimulai, dijalankan di luar *request cycle* |
| **Custom logging** | Loguru — pemisahan `application.log` (INFO) dan `error.log` (ERROR) dengan *rotation* harian |

**Konsep yang dilatih:** pemisahan *concern* antara request sinkron dan
pekerjaan asinkron, strategi *cache invalidation*, kontrol akses berbasis
peran, serta *observability* melalui log terstruktur.

---

## `b-pemula-python-openshop-api` — OpenShop RESTful API

REST API katalog produk dengan Django REST Framework.

| Aspek | Implementasi |
| --- | --- |
| CRUD | `POST`, `GET`, `PUT`, `PATCH`, `DELETE` pada resource `/products/` |
| Pencarian | *Query parameter* `?name=` dan `?location=`, *case-insensitive* |
| HATEOAS | Tautan relasi disertakan pada respons detail produk |
| Penanganan galat | *Custom exception handler* dengan respons terstruktur |
| Pengujian | **20 unit test** mencakup seluruh butir kriteria |

```bash
pipenv install && pipenv shell
python manage.py migrate
python manage.py test        # 20 tests
python manage.py runserver
```

---

## `f-fundamental-pemrosesan-data-etl` — ETL Pipeline

Pipeline *extract–transform–load* terjadwal, disusun modular agar setiap tahap
dapat diuji secara terpisah.

| Tahap | Berkas | Fungsi |
| --- | --- | --- |
| **Extract** | `utils/extract.py` | Pengambilan data dari sumber web (requests + BeautifulSoup) |
| **Transform** | `utils/transform.py` | Pembersihan dan normalisasi data dengan pandas |
| **Load** | `utils/load.py` | Penulisan ke PostgreSQL via SQLAlchemy, CSV, dan Google Sheets API |

Pengujian otomatis tersedia pada `tests/` menggunakan **pytest** dan
**pytest-cov**, dengan berkas uji terpisah untuk masing-masing tahap.
Penjadwalan dilakukan melalui `python-crontab`.

```bash
pip install -r requirements.txt
pytest --cov=utils           # menjalankan pengujian beserta laporan cakupan
python main.py               # menjalankan pipeline
```

---

## `e-cloud-architect-notes-api-k8s` — Notes API on Kubernetes

Deployment REST API ke **Google Kubernetes Engine** — kontainerisasi dengan
Docker, manifest Kubernetes pada direktori `k8s/`, dan konfigurasi *service*
untuk eksposur trafik.

---

## Catatan Keamanan

Berkas `.env` berisi kredensial asli **tidak pernah** diikutkan ke repositori.
Setiap proyek menyediakan `.env.example` sebagai acuan variabel yang
dibutuhkan. *Pipeline* CI menjalankan pemindaian *secret* pada setiap
*pull request*.
