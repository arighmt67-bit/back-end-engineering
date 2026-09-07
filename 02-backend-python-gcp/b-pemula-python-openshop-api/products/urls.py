from django.urls import path
from .views import ProductListView, ProductDetailView

urlpatterns = [
    # Terima dengan maupun tanpa trailing slash.
    # Tanpa ini, POST /products (tanpa slash) memicu RuntimeError 500 karena
    # APPEND_SLASH tidak dapat melakukan redirect pada request ber-body.
    path('products/', ProductListView.as_view(), name='product-list'),
    path('products', ProductListView.as_view(), name='product-list-noslash'),
    path('products/<str:pk>/', ProductDetailView.as_view(), name='product-detail'),
    path('products/<str:pk>', ProductDetailView.as_view(), name='product-detail-noslash'),
]
