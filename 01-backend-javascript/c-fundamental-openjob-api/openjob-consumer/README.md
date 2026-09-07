# OpenJob Consumer

Program consumer RabbitMQ untuk OpenJob RESTful API versi 2. Berjalan sebagai
project mandiri dan terpisah dari `openjob-api`; keduanya berkomunikasi melalui
message broker RabbitMQ.

## Cara Kerja

1. `openjob-api` menerbitkan pesan ke antrean `application:new` setiap kali kandidat membuat lamaran. Payload pesan hanya berisi `application_id`.
2. Consumer ini menerima pesan tersebut secara asynchronous.
3. Consumer melakukan query ke PostgreSQL untuk mencari pemilik lowongan (job owner) beserta data pelamar.
4. Email notifikasi dikirim menggunakan Nodemailer kepada pemilik lowongan, bukan kepada pelamar.

Seluruh data pada email diambil dari database dan tidak ada yang ditulis langsung
di dalam kode.

## Isi Email

| Data | Sumber |
|---|---|
| Nama pelamar | `users.name` |
| Email pelamar | `users.email` |
| Tanggal lamaran | `applications.created_at` |
| Judul lowongan | `jobs.title` |
| Nama perusahaan | `companies.name` |

## Menjalankan

```bash
npm install
cp .env.example .env    # sesuaikan kredensial
npm run start
```

Pastikan PostgreSQL dan RabbitMQ sudah berjalan, serta migrasi database sudah
dijalankan dari project `openjob-api`.

## Environment Variables

| Variabel | Keterangan |
|---|---|
| `PGUSER`, `PGPASSWORD`, `PGDATABASE`, `PGHOST`, `PGPORT` | Koneksi PostgreSQL |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Koneksi RabbitMQ |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD` | Kredensial SMTP Nodemailer |

`MAIL_PASSWORD` wajib berupa App Password 16 karakter dari akun Google yang telah
mengaktifkan verifikasi dua langkah.

## Struktur

```
openjob-consumer/
├── package.json
├── .env.example
└── src/
    ├── consumer.js              # titik masuk, konsumsi antrean
    ├── config/
    │   ├── database.js          # koneksi PostgreSQL
    │   └── rabbitmq.js          # URL broker dan nama antrean
    └── services/
        └── mailSender.js        # pengiriman email Nodemailer
```
