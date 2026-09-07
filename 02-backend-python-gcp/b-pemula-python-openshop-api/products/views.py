from django.core.exceptions import ValidationError as DjangoValidationError
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.exceptions import NotFound
from .models import Product
from .serializers import ProductSerializer


def build_hateoas(request, product_id):
    """Tautan HATEOAS dalam bentuk ARRAY (sesuai kontrak pengujian)."""
    base = request.build_absolute_uri(f'/products/{product_id}/')
    return [
        {"rel": "self", "href": base, "action": "GET",
         "types": ["application/json"]},
        {"rel": "update", "href": base, "action": "PUT",
         "types": ["application/json"]},
        {"rel": "delete", "href": base, "action": "DELETE",
         "types": ["application/json"]},
    ]


def serialize_with_links(request, product):
    data = ProductSerializer(product).data
    data['_links'] = build_hateoas(request, product.id)
    return data


def get_product_or_404(pk):
    """Ambil produk berdasarkan id.

    Id yang bukan UUID valid memicu ValidationError/ValueError dari ORM; keduanya
    diperlakukan sebagai 404 agar respons tetap JSON, bukan halaman error HTML.
    """
    try:
        return Product.objects.get(pk=pk)
    except (Product.DoesNotExist, DjangoValidationError, ValueError, TypeError):
        raise NotFound(detail="Not found.")


class ProductListView(APIView):

    def get(self, request):
        # Produk yang sudah dihapus (soft delete) tidak ikut ditampilkan/dicari
        queryset = Product.objects.filter(is_delete=False)

        name = request.query_params.get('name')
        location = request.query_params.get('location')

        if name:
            queryset = queryset.filter(name__icontains=name)
        if location:
            queryset = queryset.filter(location__icontains=location)

        products = [serialize_with_links(request, p) for p in queryset]
        return Response({'products': products}, status=status.HTTP_200_OK,
                        content_type='application/json')

    def post(self, request):
        serializer = ProductSerializer(data=request.data)
        if serializer.is_valid():
            product = serializer.save()
            data = serialize_with_links(request, product)
            return Response(data, status=status.HTTP_201_CREATED,
                            content_type='application/json')
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST,
                        content_type='application/json')


class ProductDetailView(APIView):

    def get(self, request, pk):
        # Detail menampilkan semua produk, termasuk yang sudah di-soft delete
        product = get_product_or_404(pk)
        data = serialize_with_links(request, product)
        return Response(data, status=status.HTTP_200_OK,
                        content_type='application/json')

    def _update(self, request, pk, partial):
        product = get_product_or_404(pk)
        serializer = ProductSerializer(product, data=request.data,
                                       partial=partial)
        if serializer.is_valid():
            updated = serializer.save()
            data = serialize_with_links(request, updated)
            return Response(data, status=status.HTTP_200_OK,
                            content_type='application/json')
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST,
                        content_type='application/json')

    def put(self, request, pk):
        # PUT = penggantian utuh: seluruh field wajib dikirim, data tidak
        # lengkap ditolak 400.
        return self._update(request, pk, partial=False)

    def patch(self, request, pk):
        # PATCH = perubahan sebagian.
        return self._update(request, pk, partial=True)

    def delete(self, request, pk):
        product = get_product_or_404(pk)
        product.is_delete = True
        product.save()
        return Response(status=status.HTTP_204_NO_CONTENT)
