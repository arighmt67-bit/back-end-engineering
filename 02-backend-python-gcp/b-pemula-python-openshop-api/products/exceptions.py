from rest_framework.views import exception_handler
from rest_framework.exceptions import NotFound
from rest_framework.response import Response
from rest_framework import status


def custom_exception_handler(exc, context):
    # Force all 404s to return the standard DRF "Not found." message
    if isinstance(exc, NotFound):
        return Response({'detail': 'Not found.'}, status=status.HTTP_404_NOT_FOUND,
                        content_type='application/json')
    return exception_handler(exc, context)
