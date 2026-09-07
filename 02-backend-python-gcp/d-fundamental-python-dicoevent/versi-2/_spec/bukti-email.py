"""Bukti end-to-end kriteria 3: registrasi tiket memicu email reminder via Celery."""
import json
import os
import sys
import time
import urllib.request
import urllib.error
from datetime import datetime, timedelta

BASE = "http://localhost:8010/api"


def req(method, path, token=None, data=None):
    body = json.dumps(data).encode() if data is not None else None
    r = urllib.request.Request(BASE + path, data=body, method=method)
    if body:
        r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


st, raw = req("POST", "/login/", data={"username": "Aras", "password": "1234qwer!@#$"})
tok = json.loads(raw)["access"]
print("login superuser:", st)

stamp = str(int(time.time()))
peserta = f"peserta{stamp}"
st, raw = req("POST", "/users/", data={
    "username": peserta, "email": f"{peserta}@example.com",
    "password": "1234qwer!@#$", "first_name": "Peserta", "last_name": "Uji"})
uid = json.loads(raw)["id"]
print("buat peserta:", st, uid, f"{peserta}@example.com")

# Event mulai 1 jam lagi -> H-2 jam sudah lewat -> reminder dikirim SEGERA
mulai = (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%d %H:%M")
selesai = (datetime.now() + timedelta(hours=3)).strftime("%Y-%m-%d %H:%M")
st, raw = req("POST", "/events/", token=tok, data={
    "name": "Event Uji Reminder", "description": "bukti celery", "location": "Online",
    "start_time": mulai, "end_time": selesai, "status": "scheduled",
    "quota": 50, "category": "Seminar"})
eid = json.loads(raw)["id"]
print("buat event:", st, eid, "mulai", mulai)

st, raw = req("POST", "/tickets/", token=tok, data={
    "event_id": eid, "name": "Tiket Uji", "price": 10000,
    "sales_start": "2026-01-01 00:00", "sales_end": selesai, "quota": 10})
tid = json.loads(raw)["id"]
print("buat tiket:", st, tid)

st, raw = req("POST", "/registrations/", token=tok, data={
    "user_id": uid, "ticket_id": tid, "status": "confirmed"})
print("buat registrasi:", st, raw[:120].decode(errors="replace"))

print("\nmenunggu Celery mengirim email...")
target = f"{peserta}@example.com"
for _ in range(30):
    time.sleep(1)
    if os.path.exists("/tmp/email-terkirim.txt"):
        isi = open("/tmp/email-terkirim.txt", encoding="utf-8", errors="replace").read()
        if target in isi:
            print("\n=== EMAIL DITERIMA SMTP CATCHER ===")
            blok = isi[isi.rfind("=" * 10, 0, isi.find(target)):]
            print(blok[:1200])
            sys.exit(0)
print("GAGAL: email tidak diterima dalam 30 detik")
print(open("/tmp/email-terkirim.txt").read()[-800:] if os.path.exists("/tmp/email-terkirim.txt") else "(berkas kosong)")
sys.exit(1)
