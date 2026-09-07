# OpenJob RESTful API V2

Submission Proyek Akhir kelas Belajar Fundamental Back-End dengan JavaScript.
Versi kedua ini menambahkan tiga kemampuan baru di atas OpenJob V1: penyimpanan
berkas, caching, dan message broker.

## Ketentuan Berkas PDF

- Berkas diunggah melalui `POST /documents` dengan field bernama `document`.
- Hanya menerima MIME type `application/pdf`.
- Ukuran maksimal 5 MB.
- Berkas tersimpan pada folder `uploads/`, sedangkan metadatanya pada tabel `documents`.
- Berkas dapat dilihat kembali melalui `GET /documents/:id`.

## Ketentuan Caching

Cache disimpan pada Redis dengan masa berlaku 1 jam (3600 detik). Setiap respons
menyertakan header `X-Data-Source` berisi `cache` atau `database`.

Endpoint yang menggunakan cache:

| Endpoint | Kunci cache |
|---|---|
| `GET /companies/:id` | `company:<id>` |
| `GET /users/:id` | `user:<id>` |
| `GET /applications/:id` | `application:<id>` |
| `GET /applications/user/:userId` | `applications:user:<userId>` |
| `GET /applications/job/:jobId` | `applications:job:<jobId>` |
| `GET /bookmarks` | `bookmarks:<userId>` |

Cache dibatalkan secara otomatis ketika data diperbarui atau dihapus.

## Ketentuan Message Broker

Ketika kandidat melamar pekerjaan, aplikasi mengirim pesan ke antrean RabbitMQ
bernama `application:new`. Consumer terpisah membaca antrean tersebut, mengambil
detail lamaran dari basis data, lalu mengirim email notifikasi kepada **pemilik
lowongan** menggunakan Nodemailer. Email memuat nama pelamar, email pelamar, dan
tanggal lamaran.

## Menjalankan Proyek

1. Salin `.env.example` menjadi `.env`, lalu sesuaikan isinya.
2. Pasang dependensi.

   ```
   npm install
   ```

3. Jalankan migrasi basis data.

   ```
   npm run migrate up
   ```

4. Pastikan PostgreSQL, Redis, dan RabbitMQ telah berjalan.
5. Jalankan HTTP server.

   ```
   npm run start
   ```

6. Jalankan consumer pada terminal terpisah.

   ```
   npm run consumer
   ```

## Struktur Proyek

```
src/
  config/       konfigurasi database, redis, dan rabbitmq
  exceptions/   kelas error khusus
  middlewares/  autentikasi, validasi, cache, unggah berkas, penanganan error
  routes/       definisi seluruh endpoint
  services/     layanan cache, producer, dan pengirim email
  utils/        pembungkus async dan pengelola token
  consumer.js   consumer RabbitMQ
  server.js     titik masuk aplikasi
migrations/     berkas migrasi basis data
uploads/        lokasi penyimpanan berkas PDF
```
