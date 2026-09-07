from django.contrib.auth.models import Group
from rest_framework import status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Event, Payment, Registration, Ticket, User
from .permissions import (
    IsAdminOrOrganizerOrReadOnly,
    IsAdminOrSuperUser,
    IsSuperUser,
    ReadOnlyOrAuthenticated,
    is_admin,
    is_organizer,
)
from .serializers import (
    AssignRoleSerializer,
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
        queryset = Event.objects.all().order_by("-created_at")
        category = request.query_params.get("category")
        event_status = request.query_params.get("status")
        if category:
            queryset = queryset.filter(category__iexact=category)
        if event_status:
            queryset = queryset.filter(status__iexact=event_status)
        queryset = paginate(request, queryset)
        return Response({"events": EventSerializer(queryset, many=True).data},
                        status=status.HTTP_200_OK)

    def post(self, request):
        serializer = EventSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        event = serializer.save()
        return Response(EventSerializer(event).data, status=status.HTTP_201_CREATED)


class EventDetailView(APIView):
    permission_classes = [IsAdminOrOrganizerOrReadOnly]

    def get_object(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is not None:
            self.check_object_permissions(request, event)
        return event

    def get(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is None:
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        return Response(EventSerializer(event).data, status=status.HTTP_200_OK)

    def put(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is None:
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        self.check_object_permissions(request, event)
        serializer = EventSerializer(event, data=request.data, partial=True,
                                     context={"request": request})
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data, status=status.HTTP_200_OK)

    def delete(self, request, pk):
        event = Event.objects.filter(pk=pk).first()
        if event is None:
            return Response({"message": "Event not found"}, status=status.HTTP_404_NOT_FOUND)
        self.check_object_permissions(request, event)
        event.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


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
