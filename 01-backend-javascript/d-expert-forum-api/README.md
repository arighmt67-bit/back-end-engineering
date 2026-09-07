# Forum API

RESTful API aplikasi forum diskusi untuk Garuda Game. Dibangun dengan Express.js,
PostgreSQL, dan menerapkan Clean Architecture serta automation testing.

## Fitur

**Kriteria wajib**

- Registrasi pengguna, login, refresh access token, dan logout
- Menambahkan thread (restrict, membutuhkan access token)
- Menambahkan dan menghapus komentar pada thread (soft delete)
- Melihat detail thread beserta seluruh komentarnya (terbuka, tanpa token)

**Kriteria opsional**

- Menambahkan dan menghapus balasan pada komentar thread (soft delete)
- Balasan ditampilkan ter-nested pada setiap item komentar
- Menyukai dan batal menyukai komentar thread (restrict)
- Jumlah suka (`likeCount`) ditampilkan pada setiap item komentar

## Arsitektur

Proyek disusun mengikuti Clean Architecture dengan empat lapisan:

| Lapisan | Isi |
| --- | --- |
| `src/Domains` | Entitas dan kontrak repository (abstract) |
| `src/Applications` | Use case dan kontrak layanan keamanan |
| `src/Infrastructures` | Implementasi konkret: PostgreSQL, bcrypt, JWT, server HTTP |
| `src/Interfaces` | Router dan handler Express |

Ketergantungan antar lapisan diatur melalui container (`instances-container`),
sehingga lapisan dalam tidak pernah bergantung pada lapisan luar.

## Continuous Integration dan Continuous Deployment

| Berkas | Pemicu | Kegunaan |
| --- | --- | --- |
| `.github/workflows/ci.yml` | Pull request ke `master` | Menjalankan ESLint serta unit, integration, dan functional test di atas PostgreSQL service container |
| `.github/workflows/cd.yml` | Push ke `master` | Deployment otomatis ke server produksi melalui SSH |

Secrets yang dibutuhkan proses deployment: `SSH_HOST`, `SSH_USERNAME`,
`SSH_PRIVATE_KEY`, dan `SSH_PORT`.

## Keamanan

- **Limit Access** — resource `/threads` beserta seluruh path di dalamnya
  dibatasi 90 request per menit melalui NGINX, sebagai langkah preventif
  terhadap DDoS Attack. Konfigurasinya tersedia pada `nginx.conf`.
- **HTTPS** — seluruh lalu lintas dialihkan ke TLS dengan sertifikat
  Let's Encrypt agar terhindar dari serangan Man In The Middle.

## Menjalankan Proyek

```bash
npm install
cp .env.example .env      # sesuaikan nilainya
npm run migrate up
npm run start
```

## Pengujian

```bash
npm run lint              # memeriksa gaya penulisan kode
npm run test              # menjalankan seluruh pengujian
npm run test:coverage     # menjalankan pengujian beserta laporan cakupan
```

Pengujian memerlukan basis data terpisah yang dikonfigurasi melalui `.test.env`,
lalu dimigrasikan dengan `npm run migrate:test up`.

## Daftar Endpoint

| Method | Path | Akses |
| --- | --- | --- |
| POST | `/users` | Terbuka |
| POST | `/authentications` | Terbuka |
| PUT | `/authentications` | Terbuka |
| DELETE | `/authentications` | Terbuka |
| POST | `/threads` | Restrict |
| GET | `/threads/{threadId}` | Terbuka |
| POST | `/threads/{threadId}/comments` | Restrict |
| DELETE | `/threads/{threadId}/comments/{commentId}` | Restrict |
| POST | `/threads/{threadId}/comments/{commentId}/replies` | Restrict |
| DELETE | `/threads/{threadId}/comments/{commentId}/replies/{replyId}` | Restrict |
| PUT | `/threads/{threadId}/comments/{commentId}/likes` | Restrict |
