# Helpdesk Ticketing API

[![Back-End Monorepo CI](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml/badge.svg)](https://github.com/arighmt67-bit/back-end-engineering/actions/workflows/ci.yml)
![Coverage](https://img.shields.io/badge/coverage-89.9%25%20line-brightgreen)
![Tests](https://img.shields.io/badge/tests-73%20passed-brightgreen)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)

REST API manajemen tiket helpdesk: registrasi & login JWT, CRUD tiket dengan kontrol
kepemilikan, dan endpoint agregasi untuk dashboard. Dibuat sebagai latihan Spring Boot
sekaligus studi kasus perbedaan **401 vs 403** yang benar pada REST API.

---

## Menjalankan

Butuh **JDK 17**. Tidak perlu memasang Maven maupun database — wrapper dan H2 in-memory
sudah disertakan.

```bash
cd 04-backend-java-spring/a-helpdesk-api
./mvnw spring-boot:run
```

Aplikasi berjalan di `http://localhost:8080` dengan profil `dev`.

| Alamat | Keterangan |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI (dokumentasi interaktif) |
| http://localhost:8080/v3/api-docs | Spesifikasi OpenAPI 3 mentah |
| http://localhost:8080/h2-console | Konsol H2 (JDBC URL `jdbc:h2:mem:helpdesk`, user `sa`, password kosong) |
| http://localhost:8080/actuator/health | Health check |

Menjalankan seluruh test beserta gerbang coverage:

```bash
./mvnw clean verify
```

### Profil produksi

Profil `prod` memakai PostgreSQL dan membaca semua nilai sensitif dari environment variable:

```bash
export JWT_SECRET='minimal-32-karakter-untuk-algoritma-HS256'
export DB_URL='jdbc:postgresql://localhost:5432/helpdesk'
export DB_USERNAME='helpdesk'
export DB_PASSWORD='...'
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Event pipeline dan Docker Compose

Perubahan tiket dicatat ke **transactional outbox** dalam transaksi PostgreSQL yang sama.
Relay mengirim kontrak JSON v1 ke Kafka setelah commit; worker menyimpan notification log
dengan unique key `event_id`, sehingga retry/replay tetap idempotent. Cache detail tiket
memakai Redis tetapi gagal-terbuka ke PostgreSQL.

```bash
cd 04-backend-java-spring
export JWT_SECRET="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
docker compose up --build -d
./scripts/smoke-helpdesk-event-pipeline.sh
```

Smoke test memvalidasi create → outbox → Kafka → worker, replay duplikat, fallback saat
Redis mati, dan recovery pending outbox setelah Kafka pulih. Model Compose lima service
sudah tervalidasi lokal; runtime smoke juga menjadi gate CI.

---

## Endpoint

Semua endpoint di bawah `/api/tickets` dan `/api/reports` memerlukan header
`Authorization: Bearer <token>`.

### Authentication — `/api/auth`

| Method | Path | Akses | Keterangan |
|---|---|---|---|
| `POST` | `/api/auth/register` | publik | Daftar akun baru, langsung mengembalikan token. Selalu `ROLE_USER`. |
| `POST` | `/api/auth/login` | publik | Menukar email + password dengan token JWT (berlaku 30 menit). |

### Tickets — `/api/tickets`

| Method | Path | Akses | Keterangan |
|---|---|---|---|
| `POST` | `/api/tickets` | terautentikasi | Buat tiket, status awal `OPEN`. |
| `GET` | `/api/tickets` | terautentikasi | Daftar paginated. `USER` hanya melihat tiket miliknya; `AGENT`/`ADMIN` melihat semua. |
| `GET` | `/api/tickets/{id}` | pemilik atau `AGENT`/`ADMIN` | Detail satu tiket. |
| `PUT` | `/api/tickets/{id}` | pemilik atau `AGENT`/`ADMIN` | Perbarui judul, deskripsi, prioritas. |
| `PATCH` | `/api/tickets/{id}/status` | `AGENT`/`ADMIN` | Ubah status. `RESOLVED`/`CLOSED` otomatis mengisi `resolvedAt`. |
| `DELETE` | `/api/tickets/{id}` | pemilik atau `AGENT`/`ADMIN` | Hapus tiket. |

Query parameter untuk `GET /api/tickets`: `status`, `page` (0), `size` (10, maks 100),
`sortBy` (`createdAt`).

### Reports — `/api/reports` (khusus `AGENT`/`ADMIN`)

| Method | Path | Keterangan |
|---|---|---|
| `GET` | `/api/reports/summary` | Total, tiket terbuka, tiket selesai, rata-rata jam penyelesaian, breakdown status & prioritas. |
| `GET` | `/api/reports/monthly?year=2026` | Jumlah tiket per bulan. |
| `GET` | `/api/reports/by-status` | Jumlah tiket per status. |
| `GET` | `/api/reports/by-priority` | Jumlah tiket per prioritas. |
| `GET` | `/api/reports/workload` | Total tiket dan tiket `OPEN` per pemilik. |

Enum: status `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` · prioritas `LOW`, `MEDIUM`, `HIGH`, `URGENT`.

---

## Contoh pemakaian

**1. Daftar akun**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia123","fullName":"Budi Santoso"}'
```

**2. Login dan simpan token**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"budi@example.com","password":"rahasia123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')
```

Respons login:

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 1800
}
```

**3. Buat tiket**

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Printer lantai 3 macet","description":"Paper jam saat duplex","priority":"HIGH"}'
```

**4. Lihat tiket sendiri**

```bash
curl -H "Authorization: Bearer $TOKEN" 'http://localhost:8080/api/tickets?status=OPEN&size=5'
```

**5. Membuktikan 401 dan 403 berbeda**

```bash
# tanpa token -> 401 Unauthorized
curl -i http://localhost:8080/api/tickets

# token ROLE_USER menuju endpoint khusus AGENT/ADMIN -> 403 Forbidden
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/reports/summary
```

Format error seragam untuk semua kegagalan:

```json
{
  "timestamp": "2026-09-22T13:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Anda tidak punya akses ke resource ini",
  "path": "/api/reports/summary",
  "fieldErrors": null
}
```

---

## Arsitektur

```text
controller/   Lapisan HTTP, validasi payload, anotasi OpenAPI
service/      Aturan bisnis dan transactional outbox write
repository/   Spring Data JPA + revision probe untuk cache freshness
event/        Kontrak v1, Kafka publisher, scheduled outbox relay
cache/        Redis cache-aside dengan PostgreSQL fallback
entity/       User, Ticket, TicketEventOutbox, enum domain
security/     JWT stateless dan authorization
exception/    Format ApiError seragam
```

Autentikasi memakai JWT tanpa sesi (`SessionCreationPolicy.STATELESS`), password
di-hash BCrypt, dan otorisasi berlapis: aturan path di `SecurityConfig` untuk hal kasar,
pemeriksaan kepemilikan di `TicketService` untuk hal halus.

---

## Pengujian

73 test API, seluruhnya hijau (66 unit + 7 integration). Coverage **89,9% baris / 75,0%
cabang**; gerbang minimum tetap 80% / 70%.

```bash
./mvnw clean verify                       # unit + integration + gerbang coverage
open target/site/jacoco/index.html        # laporan rinci
```

| Berkas | Jenis | Fokus |
|---|---|---|
| `JwtServiceTest` | unit | Penerbitan & validasi token, masa berlaku, issuer |
| `JwtAuthenticationFilterTest` | unit | Ekstraksi header, pengisian SecurityContext |
| `AuthServiceTest`, `TicketServiceTest` | unit | Aturan bisnis & kepemilikan |
| `SecurityMatrixMockMvcTest` | MockMvc | Matriks status 401/403/200 pada filter chain |
| `SecurityMatrixHttpIT` | HTTP nyata | Perilaku Tomcat termasuk ERROR dispatch |

### Tiga bug yang dijaga

Proyek ini sempat memiliki tiga cacat yang seluruh unit test-nya tetap hijau. Ketiganya
kini dijaga oleh pemeriksaan yang benar-benar gagal bila bug dikembalikan:

| Bug | Gejala | Penjaga |
|---|---|---|
| `jjwt-impl` dideklarasikan ulang dengan `scope=test` | Test hijau, aplikasi gagal boot (`UnknownClassException: KeysBridge`) | `maven-enforcer-plugin` (`banDuplicatePomDependencyVersions`) + `SecurityMatrixHttpIT` |
| Tidak ada `AuthenticationEntryPoint` | Request anonim dibalas 403, seharusnya 401 | `SecurityMatrixMockMvcTest$Unauthorized` |
| Filter melewati ERROR dispatch | 403 berubah jadi 401 karena `/error` dianggap anonim | `SecurityMatrixHttpIT` |

Bug pertama tidak mungkin ditangkap test Java mana pun — `scope=test` justru membuat
kelasnya tersedia di classpath test — sehingga penjaganya dipasang di tingkat build.

Untuk membuktikan penjaga ini bukan sekadar hiasan, tersedia skrip yang menyuntikkan
ulang ketiga bug dan memastikan build **gagal** pada masing-masing kasus:

```bash
./scripts/verify-regression-guards.sh
```

```text
PASS  entry-point-401     -> build GAGAL seperti yang diharapkan
PASS  error-dispatch-403  -> build GAGAL seperti yang diharapkan
PASS  jjwt-runtime-scope  -> build GAGAL seperti yang diharapkan
```

---

## Catatan teknis

* **`expiration` ditulis sebagai `Duration` (`30m`), bukan angka telanjang.** Nilai seperti
  `1800` rawan ditafsirkan sebagai milidetik oleh library lain sehingga token langsung
  kedaluwarsa. Test `masaBerlakuToken` mengunci selisih `exp - iat` tepat 1800 detik.
* **401 vs 403 dibedakan dengan sengaja.** 401 berarti "ambil token baru", 403 berarti
  "jangan diulang". Menyamakan keduanya membuat klien API melakukan retry yang sia-sia.
* **`spring.jpa.open-in-view=false`.** Mencegah koneksi database tertahan selama rendering
  respons; relasi yang dibutuhkan diambil eksplisit lewat `findByIdWithOwner`.

## Teknologi

Java 17 · Spring Boot 3.5.3 · PostgreSQL · Flyway · Redis · Apache Kafka · Docker Compose ·
Spring Security · JJWT 0.12.6 · JUnit 5 · JaCoCo
