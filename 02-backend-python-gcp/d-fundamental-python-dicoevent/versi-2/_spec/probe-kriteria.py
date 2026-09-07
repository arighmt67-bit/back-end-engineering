"""Probe butir kriteria yang tidak terjangkau Postman.

Membuktikan: objek benar-benar ada di bucket Minio, TTL cache = 1 jam,
header X-Data-Source, endpoint penyajian berkas, dan berkas log loguru.
"""
import json
import os
import sys
import urllib.request
import urllib.error

BASE = "http://localhost:8010/api"
PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, PROJ)


def req(method, path, token=None, data=None):
    url = BASE + path
    body = json.dumps(data).encode() if data is not None else None
    r = urllib.request.Request(url, data=body, method=method)
    if body:
        r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r) as resp:
            raw = resp.read()
            return resp.status, dict(resp.headers), raw
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read()


hasil = []


def cek(nama, kondisi, detail=""):
    hasil.append((nama, bool(kondisi), detail))


# --- login superuser ---
st, _, raw = req("POST", "/login/", data={"username": "Aras", "password": "1234qwer!@#$"})
tok = json.loads(raw)["access"]
cek("login superuser", st == 200, f"status={st}")

# --- ambil satu event ---
st, hdr, raw = req("GET", "/events/", token=tok)
events = json.loads(raw)["events"]
cek("GET /events/ 200", st == 200, f"status={st} n={len(events)}")
cek("header X-Data-Source ada", "X-Data-Source" in hdr, hdr.get("X-Data-Source"))

if not events:
    st, _, raw = req("POST", "/events/", token=tok, data={
        "name": "Probe Event", "description": "probe", "location": "Online",
        "start_time": "2027-01-01 10:00", "end_time": "2027-01-01 12:00",
        "status": "scheduled", "quota": 10, "category": "Seminar"})
    events = [json.loads(raw)]
eid = events[0]["id"]

# --- cache detail: miss lalu hit ---
st1, h1, _ = req("GET", f"/events/{eid}/", token=tok)
st2, h2, _ = req("GET", f"/events/{eid}/", token=tok)
cek("detail cache miss -> database", h1.get("X-Data-Source") == "database", h1.get("X-Data-Source"))
cek("detail cache hit -> cache", h2.get("X-Data-Source") == "cache", h2.get("X-Data-Source"))

# --- TTL cache harus 3600 detik (1 jam) ---
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "dicoevent.settings")
import django  # noqa: E402

django.setup()
from django.core.cache import cache  # noqa: E402
from django.conf import settings  # noqa: E402
from api import cache as event_cache  # noqa: E402
from api.models import EventPoster  # noqa: E402

client = cache._cache.get_client()
ttl = client.ttl(f":1:{event_cache.detail_key(eid)}")
cek("TTL cache detail = 1 jam", 3500 <= ttl <= 3600, f"ttl={ttl} detik")
cek("backend cache = Redis",
    "RedisCache" in settings.CACHES["default"]["BACKEND"],
    settings.CACHES["default"]["BACKEND"])

# --- invalidasi setelah update ---
req("PUT", f"/events/{eid}/", token=tok, data={"quota": 77})
st3, h3, _ = req("GET", f"/events/{eid}/", token=tok)
cek("cache invalidate setelah update", h3.get("X-Data-Source") == "database", h3.get("X-Data-Source"))

# --- objek benar-benar ada di bucket Minio ---
from api import storages  # noqa: E402

poster = EventPoster.objects.order_by("-created_at").first()
cek("nama berkas tersimpan di tabel event_posters", poster is not None,
    poster.image if poster else "tidak ada baris")
if poster:
    mc = storages.get_client()
    try:
        stat = mc.stat_object(settings.MINIO_BUCKET, poster.image)
        cek("objek ada di bucket Minio (SDK)", True,
            f"{settings.MINIO_BUCKET}/{poster.image} size={stat.size} type={stat.content_type}")
    except Exception as exc:  # noqa: BLE001
        cek("objek ada di bucket Minio (SDK)", False, str(exc))

    isi = storages.read_poster(poster.image)
    cek("berkas dapat dibaca kembali dari Minio", isi is not None and len(isi) > 0,
        f"{len(isi) if isi else 0} byte")

    st, hdr, raw = req("GET", f"/events/posters/{poster.image}/", token=tok)
    cek("endpoint menampilkan berkas media", st == 200 and len(raw) > 0,
        f"status={st} content_type={hdr.get('Content-Type')} bytes={len(raw)}")

# --- berkas log loguru ---
logdir = os.path.join(PROJ, "logs")
app_log = os.path.join(logdir, "application.log")
err_log = os.path.join(logdir, "error.log")
cek("application.log ada & berisi INFO", os.path.exists(app_log)
    and "| INFO" in open(app_log).read(), app_log)
cek("error.log ada", os.path.exists(err_log), err_log)
if os.path.exists(app_log):
    baris = [b for b in open(app_log).read().splitlines() if "| INFO" in b]
    contoh = baris[-1] if baris else ""
    import re
    pola = r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \| INFO\s+\| [\w\.]+:\w+:\d+ - "
    cek("format log: timestamp | level | lokasi | pesan", bool(re.match(pola, contoh)), contoh[:120])

print("\n%-52s %s" % ("BUTIR", "HASIL"))
print("-" * 100)
gagal = 0
for nama, ok, detail in hasil:
    print("%-52s %-5s %s" % (nama, "OK" if ok else "GAGAL", detail))
    gagal += 0 if ok else 1
print("-" * 100)
print(f"TOTAL {len(hasil)} butir | GAGAL {gagal}")
sys.exit(1 if gagal else 0)
