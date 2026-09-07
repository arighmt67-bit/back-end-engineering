from django.contrib import admin
from django.contrib.auth.admin import UserAdmin

from .models import Event, Payment, Registration, Ticket, User

admin.site.register(User, UserAdmin)
admin.site.register(Event)
admin.site.register(Ticket)
admin.site.register(Registration)
admin.site.register(Payment)
