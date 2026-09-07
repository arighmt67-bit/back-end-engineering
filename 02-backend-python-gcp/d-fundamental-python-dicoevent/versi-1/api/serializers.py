from django.contrib.auth.models import Group
from rest_framework import serializers

from .models import Event, Payment, Registration, Ticket, User

DATETIME_INPUT_FORMATS = ["%Y-%m-%d %H:%M", "%Y-%m-%dT%H:%M:%S", "iso-8601"]


class UserSerializer(serializers.ModelSerializer):
    id = serializers.CharField(read_only=True)
    password = serializers.CharField(write_only=True, required=True)

    class Meta:
        model = User
        fields = ["id", "username", "email", "password", "first_name", "last_name"]

    def create(self, validated_data):
        raw = validated_data.pop("password")
        user = User(**validated_data)
        user.set_password(raw)
        user.save()
        return user

    def update(self, instance, validated_data):
        raw = validated_data.pop("password", None)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        if raw:
            instance.set_password(raw)
        instance.save()
        return instance


class GroupSerializer(serializers.ModelSerializer):
    class Meta:
        model = Group
        fields = ["id", "name"]


class AssignRoleSerializer(serializers.Serializer):
    user_id = serializers.CharField()
    group_id = serializers.IntegerField()

    def validate_user_id(self, value):
        if not User.objects.filter(pk=value).exists():
            raise serializers.ValidationError("User not found.")
        return value

    def validate_group_id(self, value):
        if not Group.objects.filter(pk=value).exists():
            raise serializers.ValidationError("Group not found.")
        return value


class EventSerializer(serializers.ModelSerializer):
    id = serializers.CharField(read_only=True)
    quota = serializers.IntegerField()
    start_time = serializers.DateTimeField(input_formats=DATETIME_INPUT_FORMATS)
    end_time = serializers.DateTimeField(input_formats=DATETIME_INPUT_FORMATS)
    organizer_id = serializers.CharField(write_only=True, required=False, allow_null=True)
    organizer = serializers.CharField(source="organizer.id", read_only=True, default=None)

    class Meta:
        model = Event
        fields = [
            "id", "name", "description", "location", "start_time", "end_time",
            "status", "quota", "category", "organizer", "organizer_id",
        ]

    def create(self, validated_data):
        organizer_id = validated_data.pop("organizer_id", None)
        request = self.context.get("request")
        organizer = None
        if organizer_id:
            organizer = User.objects.filter(pk=organizer_id).first()
        if organizer is None and request is not None:
            organizer = request.user
        return Event.objects.create(organizer=organizer, **validated_data)

    def update(self, instance, validated_data):
        organizer_id = validated_data.pop("organizer_id", None)
        if organizer_id:
            organizer = User.objects.filter(pk=organizer_id).first()
            if organizer:
                instance.organizer = organizer
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class TicketSerializer(serializers.ModelSerializer):
    id = serializers.CharField(read_only=True)
    event = serializers.CharField(source="event.id", read_only=True)
    event_id = serializers.CharField(write_only=True)
    price = serializers.FloatField()
    quota = serializers.IntegerField()
    sales_start = serializers.DateTimeField(input_formats=DATETIME_INPUT_FORMATS)
    sales_end = serializers.DateTimeField(input_formats=DATETIME_INPUT_FORMATS)

    class Meta:
        model = Ticket
        fields = ["id", "event", "event_id", "name", "price", "sales_start", "sales_end", "quota"]

    def validate_event_id(self, value):
        if not Event.objects.filter(pk=value).exists():
            raise serializers.ValidationError("Event not found.")
        return value

    def create(self, validated_data):
        event = Event.objects.get(pk=validated_data.pop("event_id"))
        return Ticket.objects.create(event=event, **validated_data)

    def update(self, instance, validated_data):
        event_id = validated_data.pop("event_id", None)
        if event_id:
            instance.event = Event.objects.get(pk=event_id)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class RegistrationSerializer(serializers.ModelSerializer):
    id = serializers.CharField(read_only=True)
    user = serializers.CharField(source="user.id", read_only=True)
    ticket = serializers.CharField(source="ticket.id", read_only=True)
    user_id = serializers.CharField(write_only=True, required=False)
    ticket_id = serializers.CharField(write_only=True)

    class Meta:
        model = Registration
        fields = ["id", "user", "ticket", "user_id", "ticket_id", "status"]
        read_only_fields = ["status"]

    def validate_ticket_id(self, value):
        if not Ticket.objects.filter(pk=value).exists():
            raise serializers.ValidationError("Ticket not found.")
        return value

    def create(self, validated_data):
        ticket = Ticket.objects.get(pk=validated_data.pop("ticket_id"))
        user_id = validated_data.pop("user_id", None)
        request = self.context.get("request")
        user = User.objects.filter(pk=user_id).first() if user_id else None
        if user is None and request is not None:
            user = request.user
        return Registration.objects.create(user=user, ticket=ticket, **validated_data)

    def update(self, instance, validated_data):
        ticket_id = validated_data.pop("ticket_id", None)
        user_id = validated_data.pop("user_id", None)
        if ticket_id:
            instance.ticket = Ticket.objects.get(pk=ticket_id)
        if user_id:
            user = User.objects.filter(pk=user_id).first()
            if user:
                instance.user = user
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance


class PaymentSerializer(serializers.ModelSerializer):
    id = serializers.CharField(read_only=True)
    registration = serializers.CharField(source="registration.id", read_only=True)
    registration_id = serializers.CharField(write_only=True)
    amount_paid = serializers.FloatField()
    # Kembalikan label tampilan pilihan (mis. "Completed"), bukan nilai mentahnya
    payment_status = serializers.CharField(required=False)

    class Meta:
        model = Payment
        fields = [
            "id", "registration", "registration_id",
            "payment_method", "payment_status", "amount_paid",
        ]

    def to_representation(self, instance):
        data = super().to_representation(instance)
        data["payment_status"] = instance.get_payment_status_display()
        return data

    def validate_registration_id(self, value):
        if not Registration.objects.filter(pk=value).exists():
            raise serializers.ValidationError("Registration not found.")
        return value

    def create(self, validated_data):
        registration = Registration.objects.get(pk=validated_data.pop("registration_id"))
        return Payment.objects.create(registration=registration, **validated_data)

    def update(self, instance, validated_data):
        registration_id = validated_data.pop("registration_id", None)
        if registration_id:
            instance.registration = Registration.objects.get(pk=registration_id)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        return instance
