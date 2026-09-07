# Catatan untuk Reviewer — Forum API (Proyek CI/CD dan Security)

> **Status: submission SUDAH DINILAI dan LULUS.**
> Seluruh resource AWS (EC2, Elastic IP, EBS) telah dihapus untuk menghindari
> tagihan. Karena itu URL produksi di bawah **sudah tidak aktif lagi**, dan
> workflow *Continuous Deployment* sengaja dinonaktifkan.
> Dokumen ini dipertahankan sebagai catatan arsitektur dan bukti pengerjaan.

## URL Produksi (tidak aktif — server sudah dihapus)
**https://aconk-forumapi.duckdns.org**

Ketika server masih berjalan, membuka URL di atas lewat browser menampilkan health check:

```json
{ "status": "success", "message": "Forum API is running" }
```

---

## Perbaikan atas Catatan Review Sebelumnya

> **Catatan reviewer:** *"Penerapan Limit Access pada resource /threads yang telah di-deploy masih belum
> terimplementasi. Hal tersebut dapat terlihat ketika menjalankan collections pada Postman tanpa delay
> tidak menampilkan error limit access apapun."*

**Terima kasih atas koreksinya — temuan tersebut benar dan sudah saya perbaiki.**

### Akar masalah

Direktif `limit_req` sebenarnya sudah terpasang, namun ditulis dengan parameter `burst` yang terlalu
longgar:

```nginx
limit_req zone=threads_limit burst=90 nodelay;   # konfigurasi LAMA (keliru)
```

Parameter `burst=90 nodelay` mengizinkan **90 permintaan menumpuk sekaligus** tanpa penundaan.
Postman collection Forum API V2 hanya berisi **42 request menuju resource `/threads`** — masih di bawah
ambang burst tersebut. Akibatnya, meskipun collection dijalankan tanpa delay, jatah burst tidak pernah
habis sehingga **error limit access tidak pernah muncul**, persis seperti yang reviewer laporkan.

### Perbaikan

Parameter `burst` dihapus sepenuhnya, mengikuti konfigurasi pada modul kelas, sehingga pembatasan
berlaku seketika:

```nginx
limit_req_zone $binary_remote_addr zone=threads_limit:10m rate=90r/m;
limit_req_status 429;

location /threads {
    limit_req zone=threads_limit;      # konfigurasi BARU — tanpa burst
    proxy_pass http://127.0.0.1:5000;
    ...
}
```

Dengan `rate=90r/m`, server hanya melayani satu permintaan setiap ±0,67 detik. Permintaan yang datang
lebih cepat dari itu langsung dibalas **HTTP 429 Too Many Requests**.

Perubahan ini sudah diterapkan pada berkas `nginx.conf` di root proyek **dan** pada server produksi
(`/etc/nginx/sites-available/forumapi`), lalu diverifikasi dengan `nginx -t` dan `systemctl reload nginx`.

### Bukti verifikasi

**1. Menjalankan collection TANPA delay — limit access muncul**

```
$ newman run "Forum API V2 Test.postman_collection.json" -e env-prod.json

AssertionError  should response with status code 200
                expected response to have status code 200 but got 429
                inside "Threads / Get Thread"
...
```

Respons mentah yang diterima:

```html
<html>
<head><title>429 Too Many Requests</title></head>
<body>
<center><h1>429 Too Many Requests</h1></center>
<hr><center>nginx/1.24.0 (Ubuntu)</center>
</body>
</html>
```

**2. Menjalankan collection DENGAN delay — seluruh pengujian lulus**

```
$ newman run "Forum API V2 Test.postman_collection.json" -e env-prod.json --delay-request 750

                         executed    failed
      iterations                1         0
        requests               92         0
    test-scripts               80         0
      assertions              118         0

total run duration: 1m 27.5s
```

Kedua hasil di atas membuktikan pembatasan akses aktif pada `/threads` sekaligus memastikan seluruh
fungsionalitas API tetap berjalan normal pada laju permintaan yang wajar.

**3. Inbound traffic langsung ke aplikasi sudah ditutup**

Port aplikasi (5000) tidak dapat diakses dari luar; satu-satunya jalur masuk adalah melalui reverse
proxy Nginx pada port 80/443.

```
$ curl -m 6 http://3.105.126.203:5000/
(tidak ada respons — koneksi ditolak security group)
```

---

## Ringkasan Pemenuhan Kriteria

| # | Kriteria | Implementasi |
|---|----------|--------------|
| 1 | Continuous Integration | GitHub Actions menjalankan seluruh automation test pada setiap *pull request* ke branch `master`. |
| 2 | Continuous Deployment | GitHub Actions melakukan deploy otomatis ke server EC2 pada setiap *push* ke branch `master`. |
| 3 | Limit Access (Rate Limit) | Nginx membatasi resource `/threads` beserta seluruh path di dalamnya sebanyak **90 request per menit** per alamat IP; kelebihannya dibalas **HTTP 429**. Berkas konfigurasi terlampir sebagai `nginx.conf` di root proyek. |
| 4 | HTTPS | Sertifikat Let's Encrypt terpasang pada Nginx. Akses `http://` otomatis dialihkan (301) ke `https://`. |

## Informasi Tambahan

- **Repository**: https://github.com/arighmt67-bit/forum-api (publik)
- **Automation test**: 50 file, 186 test, coverage 100% (statements, branch, functions, lines)
- **Branch protection**: branch `master` dilindungi — perubahan wajib melalui pull request yang lulus pengecekan CI
- **Arsitektur**: Clean Architecture (Domains, Applications, Infrastructures, Interfaces, Commons)
- **Stack**: Node.js + Express, PostgreSQL, JWT, bcrypt, node-pg-migrate

Terima kasih atas waktu dan masukannya.
