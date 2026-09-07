# DicoEvent Versi 2

Proyek Akhir kelas **Belajar Fundamental Back-End dengan Python** (Dicoding, kode 788).

Dibangun dengan Python 3.10, Django 4.2, Django REST Framework, PostgreSQL,
Minio SDK, Redis, Celery, dan Loguru.

## Kriteria yang diimplementasikan

### 1. Pengelolaan Berkas Media (Minio SDK)
- Kredensial Minio (`MINIO_ENDPOINT_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`,
  `MINIO_BUCKET`) dibaca melalui environment variable.
- Validasi ukuran maksimal berkas **500 kB** dan validasi MIME type
  (hanya berkas gambar yang diizinkan).
- Nama berkas disimpan pada tabel `event_posters` dengan relasi ForeignKey
  ke model `Event`.
- Endpoint `GET /api/events/posters/<filename>/` menyajikan kembali berkas
  gambar langsung dari Minio.

### 2. Caching RESTful API (Redis)
- Kredensial Redis (`REDIS_HOST`, `REDIS_PORT`, `REDIS_DB`) dibaca melalui
  environment variable.
- Caching diterapkan pada endpoint detail event (`/api/events/<id>/`) dan
  daftar event (`/api/events/`).
- Masa berlaku cache (TTL) **1 jam** (3600 detik).
- Cache otomatis di-invalidate ketika ada penambahan, pembaruan, atau
  penghapusan event.
- Header response `X-Data-Source` menandai data dilayani dari `database`
  atau `cache`.

### 3. Asynchronous Task (Celery)
- Broker Celery (`CELERY_BROKER_URL`, `CELERY_RESULT_BACKEND`) dibaca melalui
  environment variable.
- Email reminder dikirim kepada pengguna yang telah memesan tiket tepat
  **H-2 jam** (120 menit) sebelum event dimulai, lewat task
  `api.tasks.send_event_reminder_email` dengan parameter `eta`.

### 4. Custom Logging (Loguru)
- Format log memuat timestamp, log level, lokasi (modul:fungsi:baris), dan pesan.
- Log level INFO dicatat pada `logs/application.log`.
- Log level ERROR dicatat pada `logs/error.log`.
- File rotation aktif setiap **1 hari** (`rotation="1 day"`).

## Menjalankan Proyek

**1. Virtual environment dan dependensi**

```bash
pipenv install
pipenv shell
```

**2. Konfigurasi environment**

Salin `.env.example` menjadi `.env`, lalu sesuaikan kredensial:

| Kelompok | Variabel |
|---|---|
| PostgreSQL | `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD`, `DATABASE_HOST`, `DATABASE_PORT` |
| Minio | `MINIO_ENDPOINT_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_DB` |
| Celery | `CELERY_BROKER_URL`, `CELERY_RESULT_BACKEND` |
| Email | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_USE_TLS`, `MAIL_FROM` |

**3. Migrasi database**

```bash
python manage.py migrate
```

**4. Worker Celery** (terminal terpisah)

```bash
celery -A dicoevent worker -l info
```

**5. Server Django**

```bash
python manage.py runserver 8000
```

Superuser untuk pengujian Postman:

```bash
python manage.py createsuperuser --username Aras --email aras@example.com
```

## Catatan

Folder `_spec/` berisi berkas bantu verifikasi (collection Postman, skrip probe
kriteria, reset database). Folder ini bukan bagian dari kode aplikasi dan tidak
perlu diikutkan saat pengumpulan submission.
