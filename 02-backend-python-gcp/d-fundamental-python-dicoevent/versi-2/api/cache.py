"""Kriteria 2: caching RESTful API dengan Redis.

Kredensial Redis dibaca dari environment variable REDIS_HOST (kriteria 2, 4 pts).
Cache disimpan selama 1 jam dan di-invalidate setiap ada perubahan data.
"""

import hashlib

from django.core.cache import cache
from loguru import logger

CACHE_TTL = 60 * 60  # 1 jam (kriteria 2, 3 pts)

DATABASE = "database"
CACHE = "cache"

_LIST_VERSION_KEY = "events:list:version"


def _list_version():
    version = cache.get(_LIST_VERSION_KEY)
    if version is None:
        version = 1
        cache.set(_LIST_VERSION_KEY, version, None)
    return version


def list_key(request):
    """Kunci cache daftar event, dibedakan per kombinasi query string."""
    query = request.GET.urlencode()
    digest = hashlib.md5(query.encode()).hexdigest()
    return f"events:list:v{_list_version()}:{digest}"


def detail_key(pk):
    return f"events:detail:{pk}"


def get_cached(key):
    return cache.get(key)


def set_cached(key, value):
    cache.set(key, value, CACHE_TTL)


def invalidate_event(pk=None):
    """Menghapus cache daftar event dan (opsional) cache detail event."""
    try:
        cache.set(_LIST_VERSION_KEY, _list_version() + 1, None)
        if pk is not None:
            cache.delete(detail_key(pk))
        logger.info(f"Cache event di-invalidate (event_id={pk})")
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Gagal invalidate cache event: {exc}")
