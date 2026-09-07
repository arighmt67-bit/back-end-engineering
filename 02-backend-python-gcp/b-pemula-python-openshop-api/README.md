# OpenShop RESTful API

RESTful API untuk platform e-commerce, dibangun dengan **Python 3.10**, **Django 4.2 LTS**,
dan **Django REST Framework**.

## Setup

```bash
pipenv install
pipenv shell
python manage.py migrate
python manage.py runserver
```

Server berjalan di `http://127.0.0.1:8000`.

## Menjalankan pengujian

```bash
python manage.py test
```

20 pengujian mencakup seluruh butir checklist (simpan, tampilkan, ubah, hapus, cari).

## Daftar Endpoint

| Method | URL | Keterangan | Status sukses |
|--------|-----|------------|---------------|
| POST | `/products/` | Menyimpan produk baru | `201 Created` |
| GET | `/products/` | Menampilkan seluruh produk | `200 OK` |
| GET | `/products/?name=<kata>` | Mencari produk berdasarkan nama (case-insensitive) | `200 OK` |
| GET | `/products/?location=<kata>` | Mencari produk berdasarkan lokasi (case-insensitive) | `200 OK` |
| GET | `/products/{id}/` | Menampilkan detail produk + tautan HATEOAS | `200 OK` |
| PUT | `/products/{id}/` | Mengubah data produk (penggantian utuh, seluruh field wajib) | `200 OK` |
| PATCH | `/products/{id}/` | Mengubah sebagian data produk | `200 OK` |
| DELETE | `/products/{id}/` | Menghapus produk (soft delete, `is_delete = true`) | `204 No Content` |

Seluruh endpoint juga dapat diakses **tanpa** trailing slash (mis. `POST /products`).

## Atribut Produk

| Field | Tipe | Keterangan |
|-------|------|------------|
| `id` | UUID | Dibuat otomatis, tidak dapat diubah |
| `name` | string | Wajib |
| `sku` | string | Wajib |
| `description` | string | Wajib |
| `shop` | string | Wajib |
| `location` | string | Wajib |
| `price` | integer | Wajib, tidak boleh negatif |
| `discount` | integer | Opsional, default `0` |
| `category` | string | Wajib |
| `stock` | integer | Wajib, tidak boleh negatif |
| `is_available` | boolean | Opsional, default `true` |
| `is_delete` | boolean | Opsional, default `false` (penanda soft delete) |
| `picture` | string | Opsional |

## Contoh Penggunaan

**Menyimpan produk**

```bash
curl -X POST http://127.0.0.1:8000/products/ \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kaos Polos Hitam",
    "sku": "KP-001",
    "description": "Kaos katun combed 30s",
    "shop": "Toko Aconk",
    "location": "Jakarta",
    "price": 75000,
    "discount": 5,
    "category": "Fashion",
    "stock": 20
  }'
```

Respons `201 Created`:

```json
{
  "id": "c4689747-eeb5-427e-a67b-463f095101e7",
  "name": "Kaos Polos Hitam",
  "sku": "KP-001",
  "description": "Kaos katun combed 30s",
  "shop": "Toko Aconk",
  "location": "Jakarta",
  "price": 75000,
  "discount": 5,
  "category": "Fashion",
  "stock": 20,
  "is_available": true,
  "is_delete": false,
  "picture": "",
  "_links": [
    { "rel": "self",   "href": "http://127.0.0.1:8000/products/c4689747-.../", "action": "GET",    "types": ["application/json"] },
    { "rel": "update", "href": "http://127.0.0.1:8000/products/c4689747-.../", "action": "PUT",    "types": ["application/json"] },
    { "rel": "delete", "href": "http://127.0.0.1:8000/products/c4689747-.../", "action": "DELETE", "types": ["application/json"] }
  ]
}
```

**Menampilkan seluruh produk** — respons dibungkus dalam objek `products`:

```json
{ "products": [ { "id": "...", "name": "Kaos Polos Hitam", "...": "..." } ] }
```

**Mencari produk**

```bash
curl "http://127.0.0.1:8000/products/?name=kaos"
curl "http://127.0.0.1:8000/products/?location=jakarta"
```

**Menghapus produk**

```bash
curl -X DELETE http://127.0.0.1:8000/products/{id}/
```

Penghapusan bersifat *soft delete*: `is_delete` diubah menjadi `true`, sehingga produk
tidak lagi muncul pada daftar maupun pencarian, tetapi tetap dapat diakses lewat endpoint detail.

## Penanganan Error

Seluruh respons error dikembalikan dalam format JSON.

| Kondisi | Status | Respons |
|---------|--------|---------|
| Data tidak valid / field wajib kosong | `400` | `{"name": ["This field is required."]}` |
| Harga atau stok negatif | `400` | `{"price": ["Price must be a non-negative number."]}` |
| Produk tidak ditemukan / id bukan UUID | `404` | `{"detail": "Not found."}` |
| URL tidak dikenal | `404` | `{"detail": "Not found."}` |
