"""Pengujian OpenShop RESTful API.

Skenario disusun mengikuti koleksi pengujian resmi
"[743] OpenShop API Test With Soft Delete", sehingga setiap butir checklist
review mandiri terbukti lewat eksekusi, bukan asumsi.
"""
import json

from django.test import TestCase

from .models import Product


# Data mengikuti environment resmi (With Soft Delete)
PRODUCT = {
    "name": "Kelas Belajar Python",
    "sku": "DCD01",
    "description": "This is a sample description of the product.",
    "shop": "Dicoding",
    "location": "Bandung",
    "price": 1500000,
    "discount": 0,
    "category": "Course",
    "stock": 1000,
    "is_available": True,
    "is_delete": False,
    "picture": "https://www.shutterstock.com/image-vector/sample-red-square-grunge-stamp-260nw-338250266.jpg",
}

UPDATE = {
    "name": "Kelas Belajar Python Dasar",
    "sku": "DCD02",
    "description": "This is a updated sample description of the product.",
    "shop": "Dicoding Academy",
    "location": "Indonesia",
    "price": 1200000,
    "discount": 300000,
    "category": "Bootcamp",
    "stock": 500,
    "is_available": True,
    "is_delete": False,
    "picture": PRODUCT["picture"],
}

NOT_FOUND_ID = "e7640250-b51e-45f0-bd2f-f91c2c6db46a"


class ProductAPITest(TestCase):

    def post_product(self, **overrides):
        return self.client.post(
            "/products/", data=json.dumps({**PRODUCT, **overrides}),
            content_type="application/json",
        )

    def buat_produk(self, **overrides):
        return self.post_product(**overrides).json()["id"]

    # ------------------------------------------------------------------
    # 1. RESTful API dapat menyimpan data produk
    # ------------------------------------------------------------------
    def test_menyimpan_data_produk(self):
        response = self.post_product()

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response["Content-Type"], "application/json")
        body = response.json()
        self.assertIn("id", body)
        self.assertNotEqual(body["id"], "")
        self.assertEqual(Product.objects.count(), 1)

    def test_menyimpan_data_produk_tanpa_trailing_slash(self):
        response = self.client.post(
            "/products", data=json.dumps(PRODUCT),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 201)

    def test_menyimpan_data_tidak_lengkap_ditolak_400(self):
        """Body hanya 5 field (sesuai request 'Adding Product with Invalid Data')."""
        payload = {k: PRODUCT[k] for k in
                   ("name", "sku", "description", "shop", "location")}

        response = self.client.post(
            "/products/", data=json.dumps(payload),
            content_type="application/json",
        )
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response["Content-Type"], "application/json")
        self.assertIsInstance(response.json(), dict)

    # ------------------------------------------------------------------
    # 2. RESTful API dapat menampilkan data produk
    # ------------------------------------------------------------------
    def test_menampilkan_seluruh_data_produk(self):
        self.post_product()
        self.post_product(name="Celana Chino", location="Jakarta")

        response = self.client.get("/products/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response["Content-Type"], "application/json")
        body = response.json()
        self.assertIn("products", body)
        self.assertIsInstance(body["products"], list)
        self.assertEqual(len(body["products"]), 2)

    def test_menampilkan_detail_produk_lengkap(self):
        product_id = self.buat_produk()

        response = self.client.get(f"/products/{product_id}/")

        self.assertEqual(response.status_code, 200)
        body = response.json()
        for field in ("id", "name", "shop", "price", "sku", "description",
                      "location", "discount", "category", "stock",
                      "is_available", "picture", "_links"):
            self.assertIn(field, body)

        self.assertEqual(body["id"], product_id)
        self.assertEqual(body["name"], PRODUCT["name"])
        self.assertEqual(body["sku"], PRODUCT["sku"])
        self.assertEqual(body["description"], PRODUCT["description"])
        self.assertEqual(body["shop"], PRODUCT["shop"])
        self.assertEqual(body["location"], PRODUCT["location"])
        self.assertEqual(body["price"], PRODUCT["price"])
        self.assertEqual(body["discount"], PRODUCT["discount"])
        self.assertEqual(body["category"], PRODUCT["category"])
        self.assertEqual(body["stock"], PRODUCT["stock"])
        self.assertEqual(body["is_available"], PRODUCT["is_available"])

    def test_links_hateoas_berbentuk_array(self):
        """Kontrak: pm.expect(responseJson._links).to.be.an('array')."""
        product_id = self.buat_produk()

        body = self.client.get(f"/products/{product_id}/").json()

        self.assertIsInstance(body["_links"], list)
        self.assertTrue(all(isinstance(item, dict) for item in body["_links"]))
        self.assertEqual(
            {item["rel"] for item in body["_links"]},
            {"self", "update", "delete"},
        )

    def test_detail_produk_tidak_ada_balas_404_json(self):
        response = self.client.get(f"/products/{NOT_FOUND_ID}/")

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
        self.assertEqual(response.json()["detail"], "Not found.")

    def test_id_bukan_uuid_balas_404_json(self):
        response = self.client.get("/products/bukan-uuid/")

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.json()["detail"], "Not found.")

    # ------------------------------------------------------------------
    # 3. RESTful API dapat mengubah data produk
    # ------------------------------------------------------------------
    def test_mengubah_data_produk(self):
        product_id = self.buat_produk()

        response = self.client.put(
            f"/products/{product_id}/", data=json.dumps(UPDATE),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response["Content-Type"], "application/json")

        # verifikasi lewat GET ulang, seperti pm.sendRequest pada koleksi
        body = self.client.get(f"/products/{product_id}/").json()
        self.assertEqual(body["id"], product_id)
        self.assertEqual(body["name"], UPDATE["name"])
        self.assertEqual(body["description"], UPDATE["description"])
        self.assertEqual(body["price"], UPDATE["price"])

    def test_mengubah_dengan_data_tidak_lengkap_ditolak_400(self):
        """PUT = penggantian utuh; body 6 field harus ditolak."""
        product_id = self.buat_produk()
        payload = {k: UPDATE[k] for k in
                   ("name", "sku", "description", "shop", "location", "price")}

        response = self.client.put(
            f"/products/{product_id}/", data=json.dumps(payload),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 400)
        self.assertEqual(response["Content-Type"], "application/json")

    def test_mengubah_produk_tidak_ada_balas_404(self):
        payload = {k: UPDATE[k] for k in
                   ("name", "sku", "description", "shop", "location", "price")}

        response = self.client.put(
            f"/products/{NOT_FOUND_ID}/", data=json.dumps(payload),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
        self.assertEqual(response.json()["detail"], "Not found.")

    def test_patch_mengubah_sebagian_field(self):
        product_id = self.buat_produk()

        response = self.client.patch(
            f"/products/{product_id}/", data=json.dumps({"stock": 3}),
            content_type="application/json",
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["stock"], 3)
        self.assertEqual(response.json()["sku"], PRODUCT["sku"])

    # ------------------------------------------------------------------
    # 4. RESTful API dapat menghapus data produk (soft delete)
    # ------------------------------------------------------------------
    def test_menghapus_data_produk(self):
        product_id = self.buat_produk()

        response = self.client.delete(f"/products/{product_id}/")

        self.assertEqual(response.status_code, 204)

        # kontrak soft delete: produk tetap ada dan ditandai terhapus
        detail = self.client.get(f"/products/{product_id}/")
        self.assertEqual(detail.status_code, 200)
        self.assertIs(detail.json()["is_delete"], True)

    def test_produk_terhapus_tidak_muncul_di_daftar(self):
        product_id = self.buat_produk()
        self.post_product(name="Celana Chino")

        self.client.delete(f"/products/{product_id}/")

        daftar = self.client.get("/products/").json()["products"]
        self.assertEqual(len(daftar), 1)
        self.assertNotIn(product_id, [p["id"] for p in daftar])

    def test_menghapus_produk_tidak_ada_balas_404(self):
        response = self.client.delete(f"/products/{NOT_FOUND_ID}/")

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
        self.assertEqual(response.json()["detail"], "Not found.")

    # ------------------------------------------------------------------
    # 5. RESTful API dapat mencari data produk
    # ------------------------------------------------------------------
    def test_mencari_produk_berdasarkan_nama(self):
        self.post_product()
        self.post_product(name="Celana Chino")

        response = self.client.get("/products?name=Python")

        self.assertEqual(response.status_code, 200)
        produk = response.json()["products"]
        self.assertEqual(len(produk), 1)
        for item in produk:
            self.assertTrue(item["id"])
            self.assertIn("python", item["name"].lower())

    def test_mencari_produk_berdasarkan_nama_case_insensitive(self):
        self.post_product(name="Baju Koko")

        produk = self.client.get("/products?name=baju").json()["products"]

        self.assertEqual(len(produk), 1)
        self.assertIn("baju", produk[0]["name"].lower())

    def test_mencari_produk_berdasarkan_lokasi(self):
        self.post_product(location="Indonesia")
        self.post_product(name="Celana Chino", location="Bandung")

        produk = self.client.get("/products?location=Indonesia").json()["products"]

        self.assertEqual(len(produk), 1)
        self.assertIn("indonesia", produk[0]["location"].lower())

    def test_mencari_produk_berdasarkan_lokasi_case_insensitive(self):
        self.post_product(location="Jakarta")

        produk = self.client.get("/products?location=jakarta").json()["products"]

        self.assertEqual(len(produk), 1)
        self.assertIn("jakarta", produk[0]["location"].lower())

    def test_pencarian_tanpa_hasil_balas_daftar_kosong(self):
        self.post_product()

        response = self.client.get("/products?name=tidakada")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["products"], [])

    def test_produk_terhapus_tidak_muncul_di_pencarian(self):
        product_id = self.buat_produk()
        self.client.delete(f"/products/{product_id}/")

        produk = self.client.get("/products?name=Python").json()["products"]

        self.assertEqual(produk, [])

    # ------------------------------------------------------------------
    # Validasi tambahan
    # ------------------------------------------------------------------
    def test_harga_negatif_ditolak(self):
        self.assertEqual(self.post_product(price=-1).status_code, 400)

    def test_stok_negatif_ditolak(self):
        self.assertEqual(self.post_product(stock=-1).status_code, 400)

    def test_url_tidak_dikenal_balas_json(self):
        response = self.client.get("/")

        self.assertEqual(response.status_code, 404)
        self.assertEqual(response["Content-Type"], "application/json")
