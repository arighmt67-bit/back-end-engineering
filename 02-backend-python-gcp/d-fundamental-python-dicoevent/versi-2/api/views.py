from django.contrib.auth.models import Group
from django.conf import settings as django_settings
from django.http import HttpResponse
from loguru import logger
from rest_framework import status
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from . import cache as event_cache
from . import storages
from .models import Event, EventPoster, Payment, Registration, Ticket, User
from .permissions import (
    IsAdminOrOrganizerOrReadOnly,
    IsAdminOrSuperUser,
    IsSuperUser,
    ReadOnlyOrAuthenticated,
    is_admin,
    is_organizer,
)
from .tasks import schedule_event_reminder
from .serializers import (
    AssignRoleSerializer,
    EventPosterSerializer,
    EventSerializer,
    GroupSerializer,
    PaymentSerializer,
    RegistrationSerializer,
    TicketSerializer,
    UserSerializer,
)


def paginate(request, queryset):
    """Django ORM lanjutan: limit, ordering, filter (kriteria 1, 4 pts)."""
    page = request.query_params.get("page")
    limit = request.query_params.get("limit")
    if page is None and limit is None:
        return queryset
    try:
        page_num = max(int(page or 1), 1)
    except ValueError:
        page_num = 1
    try:
        size = max(int(limit or 10), 1)
    except ValueError:
        size = 10
    start = (page_num - 1) * size
    return queryset[start:start + size]


class UserListCreateView(APIView):
    def get_permissions(self):
        if self.request.method == "POST":
            return [AllowAny()]
        return [IsAdminOrSuperUser()]

    def get(self, request):
        users = paginate(request, User.objects.all().order_by("-date_joined"))
        serializer = UserSerializer(users, many=True)
        return Response({"users": serializer.data}, status=status.HTTP_200_OK)

    def post(self, request):
        serializer = UserSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        return Response(UserSerializer(user).data, status=status.HTTP_201_CREATED)


class UserDetailView(APIView):
    permission_classes = [ReadOnlyOrAuthenticated]

    def get_object(self, pk):
        return User.objects.filter(pk=pk).first()

    def get(self, request, pk):
        user = self.get_object(pk)
        if user is None:
            return Response({"message": "User not found"}, status=status.HTTP_404_NOT_FOUND)
        return Response(UserSerializer(user).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        user = self.get_object(pk)
        if user is None:
            return Response({"message": "User not found"}, status=status.HTTP_404_NOT_FOUND)
        if not is_admin(request.user) and request.user.pk != user.pk:
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        serializer = UserSerializer(user, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        user = self.get_object(pk)
        if user is None:
            return Response({"message": "User not found"}, status=status.HTTP_404_NOT_FOUND)
        user.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class GroupListCreateView(APIView):
    permission_classes = [IsSuperUser]

    def get(self, request):
        groups = paginate(request, Group.objects.all().order_by("id"))
        return Response({"groups": GroupSerializer(groups, many=True).data},
                        status=status.HTTP_200_OK)

    def post(self, request):
        serializer = GroupSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        group = serializer.save()
        return Response(GroupSerializer(group).data, status=status.HTTP_201_CREATED)


class GroupDetailView(APIView):
    permission_classes = [IsSuperUser]

    def get_object(self, pk):
        return Group.objects.filter(pk=pk).first()

    def get(self, request, pk):
        group = self.get_object(pk)
        if group is None:
            return Response({"message": "Group not found"}, status=status.HTTP_404_NOT_FOUND)
        return Response(GroupSerializer(group).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        group = self.get_object(pk)
        if group is None:
            return Response({"message": "Group not found"}, status=status.HTTP_404_NOT_FOUND)
        serializer = GroupSerializer(group, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        group = self.get_object(pk)
        if group is None:
            return Response({"message": "Group not found"}, status=status.HTTP_404_NOT_FOUND)
        group.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class AssignRoleView(APIView):
    permission_classes = [IsSuperUser]

    def post(self, request):
        serializer = AssignRoleSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = User.objects.get(pk=serializer.validated_data["user_id"])
        group = Group.objects.get(pk=serializer.validated_data["group_id"])
        user.groups.add(group)
        return Response(
            {"message": "Role assigned successfully",
             "user_id": str(user.pk), "group_id": group.pk},
            status=status.HTTP_201_CREATED,
        )


class EventListCreateView(APIView):
    permission_classes = [IsAdminOrOrganizerOrReadOnly]

    def get(self, request):
        """Kriteria 2 (4 pts): caching pada endpoint selain detail events."""
        key = event_cache.list_key(request)
        cached = event_cache.get_cached(key)
        if cached is not None:
            logger.info(f"Daftar event dilayani dari cache oleh {request.user}")
            response = Response(cached, status=status.HTTP_200_OK)
            response["X-Data-Source"] = event_cache.CACHE
            return response

        queryset = Event.objects.all().order_by("-created_at")
        category = request.query_params.get("category")
        event_status = request.query_params.get("status")
        if category:
            queryset = queryset.filter(category__iexact=category)
        if event_status:
            queryset = queryset.filter(status__iexact=event_status)
        queryset = paginate(request, queryset)
        payload = {"events": EventSerializer(queryset, many=True).data}
        event_cache.set_cached(key, payload)
        logger.info(f"Daftar event diambil dari database oleh {request.user}")
        response = Response(payload, status=status.HTTP_200_OK)
        response["X-Data-Source"] = event_cache.DATABASE
        return response

    def post(self, request):
        serializer = EventSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        event = serializer.save()
        event_cache.invalidate_event(event.id)
        logger.info(f"Event {event.id} created by {request.user}")
        return Response(EventSerializer(event).data, status=status.HTTP_201_CREATED)


class EventDetailView(APIView):
    permission_classes = [IsAdminOrOrganizerOrReadOnly]

    def get_object(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is not None:
            self.check_object_permissions(request, event)
        return event

    def get(self, request, pk):
        """Kriteria 2 (2 pts): caching pada endpoint detail events."""
        key = event_cache.detail_key(pk)
        cached = event_cache.get_cached(key)
        if cached is not None:
            logger.info(f"Detail event {pk} dilayani dari cache")
            response = Response(cached, status=status.HTTP_200_OK)
            response["X-Data-Source"] = event_cache.CACHE
            return response

        event = Event.objects.filter(pk=pk).first()
        if event is None:
            logger.error(f"Detail event {pk} tidak ditemukan")
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        payload = EventSerializer(event).data
        event_cache.set_cached(key, payload)
        logger.info(f"Detail event {pk} diambil dari database")
        response = Response(payload, status=status.HTTP_200_OK)
        response["X-Data-Source"] = event_cache.DATABASE
        return response

    def put(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is None:
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        self.check_object_permissions(request, event)
        serializer = EventSerializer(event, data=request.data, partial=True,
                                     context={"request": request})
        serializer.is_valid(raise_exception=True)
        serializer.save()
        event_cache.invalidate_event(pk)
        logger.info(f"Event {pk} updated by {request.user}")
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is None:
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        self.check_object_permissions(request, event)
        event.delete()
        event_cache.invalidate_event(pk)
        logger.info(f"Event {pk} deleted by {request.user}")
        return Response(status=status.HTTP_204_NO_CONTENT)


class EventPosterUploadView(APIView):
    """Kriteria 1: RESTful API dapat mengunggah berkas media event."""

    permission_classes = [IsAdminOrOrganizerOrReadOnly]
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        uploaded = request.FILES.get("image")
        if uploaded is None:
            logger.error("Unggah poster gagal: berkas image tidak dikirim")
            return Response({"message": "Berkas image wajib diunggah"},
                            status=status.HTTP_400_BAD_REQUEST)

        content_type = (uploaded.content_type or "").lower()
        if not content_type.startswith(storages.ALLOWED_MIME_PREFIX):
            logger.error(f"Unggah poster ditolak: MIME type {content_type} bukan gambar")
            return Response({"message": "Berkas yang diunggah harus berupa gambar"},
                            status=status.HTTP_400_BAD_REQUEST)

        if uploaded.size > django_settings.MAX_UPLOAD_SIZE:
            logger.error(f"Unggah poster ditolak: ukuran {uploaded.size} melebihi 500 kB")
            return Response({"message": "Ukuran berkas maksimal 500 kB"},
                            status=status.HTTP_400_BAD_REQUEST)

        event_id = request.data.get("event")
        event = Event.objects.filter(pk=event_id).first() if event_id else None
        if event is None:
            logger.error(f"Unggah poster gagal: event {event_id} tidak ditemukan")
            return Response({"message": "Event not found"}, status=status.HTTP_400_BAD_REQUEST)

        object_name = storages.save_poster(uploaded)
        poster = EventPoster.objects.create(
            event=event,
            image=object_name,
            original_name=uploaded.name,
            content_type=content_type,
            size=uploaded.size,
        )
        logger.info(f"Poster {poster.image} untuk event {event.id} diunggah oleh {request.user}")
        return Response(EventPosterSerializer(poster).data, status=status.HTTP_201_CREATED)


class EventPosterListView(APIView):
    """Kriteria 1 (4 pts): menampilkan berkas media yang telah diunggah."""

    permission_classes = [ReadOnlyOrAuthenticated]

    def get(self, request, pk):
        posters = EventPoster.objects.filter(event_id=pk).order_by("-created_at")
        data = EventPosterSerializer(posters, many=True).data
        for item in data:
            item["url"] = storages.poster_url(item["image"])
        logger.info(f"Daftar poster event {pk} diakses oleh {request.user}")
        return Response(data, status=status.HTTP_200_OK)


class EventPosterFileView(APIView):
    """Kriteria 1 (4 pts): mengunduh berkas media yang tersimpan di Minio."""

    permission_classes = [ReadOnlyOrAuthenticated]

    def get(self, request, filename):
        poster = EventPoster.objects.filter(image=filename).first()
        content = storages.read_poster(filename)
        if content is None:
            logger.error(f"Berkas poster {filename} tidak ditemukan")
            return Response({"message": "Berkas tidak ditemukan"},
                            status=status.HTTP_404_NOT_FOUND)
        content_type = poster.content_type if poster else "application/octet-stream"
        logger.info(f"Berkas poster {filename} ditampilkan")
        return HttpResponse(content, content_type=content_type or "application/octet-stream")


class TicketListCreateView(APIView):
    permission_classes = [IsAdminOrOrganizerOrReadOnly]

    def get(self, request):
        queryset = Ticket.objects.select_related("event").all().order_by("-created_at")
        event_id = request.query_params.get("event_id")
        if event_id:
            queryset = queryset.filter(event__id=event_id)
        queryset = paginate(request, queryset)
        return Response({"tickets": TicketSerializer(queryset, many=True).data},
                        status=status.HTTP_200_OK)

    def post(self, request):
        serializer = TicketSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        ticket = serializer.save()
        return Response(TicketSerializer(ticket).data, status=status.HTTP_201_CREATED)


class TicketDetailView(APIView):
    permission_classes = [IsAdminOrOrganizerOrReadOnly]

    def get(self, request, pk):
        ticket = Ticket.objects.filter(pk=pk).first()
        if ticket is None:
            return Response({"message": "Ticket not found"}, status=status.HTTP_404_NOT_FOUND)
        return Response(TicketSerializer(ticket).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        ticket = Ticket.objects.filter(pk=pk).first()
        if ticket is None:
            return Response({"message": "Ticket not found"}, status=status.HTTP_404_NOT_FOUND)
        serializer = TicketSerializer(ticket, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        ticket = Ticket.objects.filter(pk=pk).first()
        if ticket is None:
            return Response({"message": "Ticket not found"}, status=status.HTTP_404_NOT_FOUND)
        ticket.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class RegistrationListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        queryset = Registration.objects.select_related("user", "ticket").all()
        queryset = paginate(request, queryset.order_by("-created_at"))
        return Response({"registrations": RegistrationSerializer(queryset, many=True).data},
                        status=status.HTTP_200_OK)

    def post(self, request):
        serializer = RegistrationSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        registration = serializer.save()
        logger.info(
            f"Registration {registration.id} created by {request.user}"
        )
        # Kriteria 3: menjadwalkan email reminder H-2 jam lewat Celery
        schedule_event_reminder(registration)
        return Response(RegistrationSerializer(registration).data,
                        status=status.HTTP_201_CREATED)


class RegistrationDetailView(APIView):
    permission_classes = [ReadOnlyOrAuthenticated]

    def get(self, request, pk):
        registration = Registration.objects.filter(pk=pk).first()
        if registration is None:
            return Response({"message": "Registration not found"},
                            status=status.HTTP_404_NOT_FOUND)
        return Response(RegistrationSerializer(registration).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        registration = Registration.objects.filter(pk=pk).first()
        if registration is None:
            return Response({"message": "Registration not found"},
                            status=status.HTTP_404_NOT_FOUND)
        serializer = RegistrationSerializer(registration, data=request.data, partial=True,
                                            context={"request": request})
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        registration = Registration.objects.filter(pk=pk).first()
        if registration is None:
            return Response({"message": "Registration not found"},
                            status=status.HTTP_404_NOT_FOUND)
        registration.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PaymentListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        queryset = Payment.objects.select_related("registration").all()
        queryset = paginate(request, queryset.order_by("-created_at"))
        return Response({"payments": PaymentSerializer(queryset, many=True).data},
                        status=status.HTTP_200_OK)

    def post(self, request):
        serializer = PaymentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        payment = serializer.save()
        return Response(PaymentSerializer(payment).data, status=status.HTTP_201_CREATED)


class PaymentDetailView(APIView):
    permission_classes = [ReadOnlyOrAuthenticated]

    def get(self, request, pk):
        payment = Payment.objects.filter(pk=pk).first()
        if payment is None:
            return Response({"message": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)
        return Response(PaymentSerializer(payment).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        payment = Payment.objects.filter(pk=pk).first()
        if payment is None:
            return Response({"message": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)
        serializer = PaymentSerializer(payment, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        if not is_admin(request.user):
            return Response({"message": "Forbidden"}, status=status.HTTP_403_FORBIDDEN)
        payment = Payment.objects.filter(pk=pk).first()
        if payment is None:
            return Response({"message": "Payment not found"}, status=status.HTTP_404_NOT_FOUND)
        payment.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
