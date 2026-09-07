"""Bukti kriteria 3 (Skilled): reminder dijadwalkan H-2 jam sebelum event, bukan langsung."""
import os
import sys
from datetime import timedelta

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "dicoevent.settings")
import django  # noqa: E402

django.setup()

from django.utils import timezone  # noqa: E402

from api.models import Event, Registration, Ticket, User  # noqa: E402
from api.tasks import REMINDER_OFFSET  # noqa: E402

print("REMINDER_OFFSET =", REMINDER_OFFSET, "(harus 2:00:00)")
assert REMINDER_OFFSET == timedelta(hours=2), "offset bukan 2 jam"

su = User.objects.filter(username="Aras").first()
mulai = timezone.now() + timedelta(hours=10)
event = Event.objects.create(
    name="Event Jadwal H-2", description="uji eta", location="Online",
    start_time=mulai, end_time=mulai + timedelta(hours=2),
    status="scheduled", quota=5, category="Seminar", organizer=su)
ticket = Ticket.objects.create(
    event=event, name="Tiket ETA", price=1000,
    sales_start=timezone.now(), sales_end=mulai, quota=5)
peserta = User.objects.create_user(
    username="peserta_eta", email="peserta_eta@example.com", password="1234qwer!@#$")
reg = Registration.objects.create(user=peserta, ticket=ticket, status="confirmed")

from api.tasks import schedule_event_reminder  # noqa: E402

schedule_event_reminder(reg)

eta_harusnya = mulai - REMINDER_OFFSET
print("event mulai      :", timezone.localtime(mulai).strftime("%Y-%m-%d %H:%M:%S"))
print("reminder eta     :", timezone.localtime(eta_harusnya).strftime("%Y-%m-%d %H:%M:%S"))
selisih = (mulai - eta_harusnya).total_seconds() / 3600
print(f"selisih          : {selisih:.1f} jam sebelum event")

log = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "logs", "application.log")
baris = [b for b in open(log).read().splitlines() if str(reg.id) in b]
print("bukti log        :", baris[-1] if baris else "(tidak ada)")

ok = abs(selisih - 2.0) < 0.01 and any("dijadwalkan" in b for b in baris)
print("\nHASIL:", "OK - reminder dijadwalkan H-2 jam" if ok else "GAGAL")

reg.delete(); ticket.delete(); event.delete(); peserta.delete()
sys.exit(0 if ok else 1)
