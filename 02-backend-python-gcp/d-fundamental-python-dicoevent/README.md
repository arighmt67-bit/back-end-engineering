# DicoEvent

Kumpulan submission proyek **DicoEvent** dari kelas Dicoding
**Belajar Fundamental Back-End dengan Python** (kode 788).

Repositori ini menyimpan dua versi proyek dalam satu tempat agar riwayat
pengerjaannya tidak terpisah.

| Folder | Proyek | Stack tambahan |
|---|---|---|
| [`versi-1/`](versi-1/) | Proyek Pertama: DicoEvent Versi 1 | Django 4.2 + DRF + PostgreSQL, JWT, RBAC |
| [`versi-2/`](versi-2/) | Proyek Akhir: DicoEvent Versi 2 | ditambah Minio, Redis, Celery, Loguru |

Masing-masing folder berdiri sendiri: punya `Pipfile`, `.env.example`, dan
dependensi terpisah. Jalankan `pipenv install` di dalam folder versi yang ingin
dipakai, bukan di root.

---

## Versi 1 — Proyek Pertama

RESTful API manajemen event dengan autentikasi JWT dan pembatasan akses
berbasis peran (RBAC).

- Model utama: `Event`, `Ticket`, `Order`, beserta relasinya
- Autentikasi JWT (login, refresh token)
- Pembatasan akses berbasis peran (admin / user)
- Database PostgreSQL, kredensial dibaca dari environment variable
- ERD tersedia pada `versi-1/ERD-DicoEvent-versi-1.png`

Detail menjalankan: lihat `versi-1/.env.example` untuk daftar variabel yang
dibutuhkan.

---

## Versi 2 — Proyek Akhir

Melanjutkan Versi 1 dengan empat kriteria tambahan:

1. **Pengelolaan berkas media (Minio SDK)** — validasi ukuran maks. 500 kB dan
   MIME type, tabel `event_posters`, endpoint penyajian berkas.
2. **Caching RESTful API (Redis)** — TTL 1 jam pada daftar & detail event,
   invalidasi otomatis, header `X-Data-Source`.
3. **Asynchronous task (Celery)** — email reminder H-2 jam sebelum event dimulai.
4. **Custom logging (Loguru)** — `application.log` (INFO) dan `error.log`
   (ERROR), rotation 1 hari.

Dokumentasi lengkap beserta panduan menjalankan: [`versi-2/README.md`](versi-2/README.md).

---

## Catatan

- Berkas `.env` berisi kredensial asli **tidak pernah** diikutkan ke repositori.
  Gunakan `.env.example` di tiap folder sebagai acuan.
- Folder `_spec/` berisi berkas bantu verifikasi (collection Postman, skrip
  probe kriteria). Bukan bagian dari kode aplikasi dan tidak perlu diikutkan
  saat pengumpulan submission.
