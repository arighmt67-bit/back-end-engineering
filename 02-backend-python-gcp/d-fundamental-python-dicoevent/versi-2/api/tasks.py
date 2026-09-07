"""Kriteria 3: asynchronous task pengiriman email reminder event dengan Celery."""

from datetime import timedelta

from celery import shared_task
from django.conf import settings
from django.core.mail import send_mail
from django.utils import timezone
from loguru import logger

REMINDER_OFFSET = timedelta(hours=2)  # H-2 jam sebelum event (kriteria 3, 3 pts)


@shared_task(name="api.tasks.send_event_reminder_email")
def send_event_reminder_email(registration_id):
    """Mengirim email reminder ke pengguna yang telah memesan tiket."""
    from .models import Registration

    registration = (
        Registration.objects.select_related("user", "ticket__event")
        .filter(pk=registration_id)
        .first()
    )
    if registration is None:
        logger.error(f"Reminder dibatalkan: registration {registration_id} tidak ditemukan")
        return "registration tidak ditemukan"

    user = registration.user
    event = registration.ticket.event
    if not user.email:
        logger.error(f"Reminder dibatalkan: user {user.username} tidak memiliki email")
        return "email pengguna kosong"

    subject = f"Reminder: {event.name} akan segera dimulai"
    message = (
        f"Halo {user.username},\n\n"
        f"Event \"{event.name}\" yang tiketnya Anda pesan akan dimulai pada "
        f"{timezone.localtime(event.start_time).strftime('%d-%m-%Y %H:%M')} "
        f"di {event.location}.\n\n"
        f"Tiket: {registration.ticket.name}\n"
        f"Status registrasi: {registration.get_status_display()}\n\n"
        "Sampai jumpa di acara!\n"
        "Tim DicoEvent"
    )
    send_mail(
        subject,
        message,
        settings.DEFAULT_FROM_EMAIL,
        [user.email],
        fail_silently=False,
    )
    logger.info(f"Email reminder event {event.id} terkirim ke {user.email}")
    return f"reminder terkirim ke {user.email}"


def schedule_event_reminder(registration):
    """Menjadwalkan reminder H-2 jam sebelum event dimulai.

    Bila waktu H-2 jam sudah lewat, reminder dikirim sesegera mungkin.
    """
    event = registration.ticket.event
    run_at = event.start_time - REMINDER_OFFSET
    now = timezone.now()
    try:
        if run_at > now:
            send_event_reminder_email.apply_async(args=[str(registration.id)], eta=run_at)
            logger.info(
                f"Reminder registration {registration.id} dijadwalkan pada "
                f"{timezone.localtime(run_at).isoformat()}"
            )
        else:
            send_event_reminder_email.delay(str(registration.id))
            logger.info(f"Reminder registration {registration.id} dikirim segera (H-2 jam terlewat)")
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Gagal menjadwalkan reminder registration {registration.id}: {exc}")


@shared_task(name="api.tasks.sweep_upcoming_event_reminders")
def sweep_upcoming_event_reminders():
    """Task berkala: mengirim reminder untuk event yang mulai dalam 2 jam ke depan."""
    from .models import Registration

    now = timezone.now()
    window_start = now + REMINDER_OFFSET - timedelta(minutes=5)
    window_end = now + REMINDER_OFFSET + timedelta(minutes=5)
    registrations = Registration.objects.select_related("user", "ticket__event").filter(
        ticket__event__start_time__gte=window_start,
        ticket__event__start_time__lte=window_end,
    )
    total = 0
    for registration in registrations:
        send_event_reminder_email.delay(str(registration.id))
        total += 1
    logger.info(f"Sweep reminder H-2 jam: {total} email dijadwalkan")
    return total
