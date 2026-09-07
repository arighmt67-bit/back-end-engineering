"""Kriteria 3: konfigurasi Celery untuk asynchronous task.

Broker dibaca dari environment variable CELERY_BROKER_URL (kriteria 3, 4 pts).
"""

import os

from celery import Celery

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "dicoevent.settings")

app = Celery("dicoevent")
app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
