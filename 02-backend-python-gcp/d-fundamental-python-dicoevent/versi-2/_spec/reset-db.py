"""Reset data domain + Group + user non-superuser sebelum menjalankan Newman."""
import os
import sys

import django

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "dicoevent.settings")
django.setup()

from django.contrib.auth.models import Group  # noqa: E402
from django.core.cache import cache  # noqa: E402

from api.models import Event, EventPoster, Payment, Registration, Ticket, User  # noqa: E402

EventPoster.objects.all().delete()
Payment.objects.all().delete()
Registration.objects.all().delete()
Ticket.objects.all().delete()
Event.objects.all().delete()
Group.objects.all().delete()
User.objects.exclude(username="Aras").delete()

su = User.objects.filter(username="Aras").first()
if su is None:
    su = User.objects.create_superuser(
        username="Aras", email="aras@example.com", password="1234qwer!@#$"
    )
else:
    su.set_password("1234qwer!@#$")
    su.is_superuser = True
    su.is_staff = True
    su.save()

cache.clear()
print("RESET OK | superuser:", su.username, "| users:", User.objects.count(),
      "| events:", Event.objects.count(), "| groups:", Group.objects.count())
