from django.urls import path
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView

from . import views

urlpatterns = [
    path("login/", TokenObtainPairView.as_view(), name="login"),
    path("token/", TokenRefreshView.as_view(), name="token_refresh"),
    path("users/", views.UserListCreateView.as_view(), name="user-list"),
    path("users/<uuid:pk>/", views.UserDetailView.as_view(), name="user-detail"),
    path("groups/", views.GroupListCreateView.as_view(), name="group-list"),
    path("groups/<int:pk>/", views.GroupDetailView.as_view(), name="group-detail"),
    path("assign-roles/", views.AssignRoleView.as_view(), name="assign-roles"),
    path("events/", views.EventListCreateView.as_view(), name="event-list"),
    path("events/upload/", views.EventPosterUploadView.as_view(), name="event-poster-upload"),
    path("events/posters/<str:filename>/", views.EventPosterFileView.as_view(),
         name="event-poster-file"),
    path("events/<uuid:pk>/poster/", views.EventPosterListView.as_view(), name="event-poster-list"),
    path("events/<uuid:pk>/", views.EventDetailView.as_view(), name="event-detail"),
    path("tickets/", views.TicketListCreateView.as_view(), name="ticket-list"),
    path("tickets/<uuid:pk>/", views.TicketDetailView.as_view(), name="ticket-detail"),
    path("registrations/", views.RegistrationListCreateView.as_view(), name="registration-list"),
    path("registrations/<uuid:pk>/", views.RegistrationDetailView.as_view(), name="registration-detail"),
    path("payments/", views.PaymentListCreateView.as_view(), name="payment-list"),
    path("payments/<uuid:pk>/", views.PaymentDetailView.as_view(), name="payment-detail"),
]
