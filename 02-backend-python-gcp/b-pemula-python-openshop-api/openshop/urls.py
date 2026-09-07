from django.http import JsonResponse
from django.urls import path, include

urlpatterns = [
    path('', include('products.urls')),
]


def not_found(request, exception=None):
    """Balas JSON untuk URL yang tidak dikenal (bukan halaman HTML Django)."""
    return JsonResponse({'detail': 'Not found.'}, status=404)


def server_error(request):
    return JsonResponse({'detail': 'Internal server error.'}, status=500)


handler404 = 'openshop.urls.not_found'
handler500 = 'openshop.urls.server_error'
