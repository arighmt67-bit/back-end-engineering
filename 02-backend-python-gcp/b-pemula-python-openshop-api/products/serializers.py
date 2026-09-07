from rest_framework import serializers
from .models import Product


class ProductSerializer(serializers.ModelSerializer):
    # is_delete is optional on input; defaults to False in the model
    is_delete = serializers.BooleanField(required=False, default=False)

    class Meta:
        model = Product
        fields = [
            'id', 'name', 'sku', 'description', 'shop', 'location',
            'price', 'discount', 'category', 'stock', 'is_available',
            'is_delete', 'picture'
        ]

    def validate_price(self, value):
        if value < 0:
            raise serializers.ValidationError("Price must be a non-negative number.")
        return value

    def validate_stock(self, value):
        if value < 0:
            raise serializers.ValidationError("Stock must be a non-negative number.")
        return value
