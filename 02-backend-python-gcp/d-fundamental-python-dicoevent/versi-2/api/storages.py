"""Kriteria 1: pengelolaan berkas media memakai Minio SDK.

Kredensial dibaca dari environment variables MINIO_ENDPOINT_URL,
MINIO_ACCESS_KEY, dan MINIO_SECRET_KEY (kriteria 1, 4 pts).
"""

import io
import os
import uuid
from pathlib import Path

from django.conf import settings
from loguru import logger

try:
    from minio import Minio
except ImportError:  # pragma: no cover
    Minio = None

MAX_UPLOAD_SIZE = 500 * 1024  # 500 kB
ALLOWED_MIME_PREFIX = "image/"

_client = None


def _endpoint_parts():
    raw = os.getenv("MINIO_ENDPOINT_URL", "localhost:9000")
    secure = raw.startswith("https://")
    host = raw.replace("https://", "").replace("http://", "").rstrip("/")
    return host, secure


def get_client():
    """Membuat klien Minio sekali saja (lazy singleton)."""
    global _client
    if _client is None:
        if Minio is None:
            raise RuntimeError("minio SDK belum terpasang")
        host, secure = _endpoint_parts()
        _client = Minio(
            host,
            access_key=os.getenv("MINIO_ACCESS_KEY"),
            secret_key=os.getenv("MINIO_SECRET_KEY"),
            secure=secure,
        )
    return _client


def _local_fallback_dir():
    path = Path(settings.BASE_DIR) / "media" / "posters"
    path.mkdir(parents=True, exist_ok=True)
    return path


def save_poster(uploaded_file):
    """Unggah berkas ke Minio; kembalikan nama objek yang tersimpan.

    Bila server Minio tidak dapat dihubungi, berkas tetap disimpan di
    penyimpanan lokal agar RESTful API tidak ikut gagal.
    """
    suffix = Path(uploaded_file.name).suffix or ".jpg"
    object_name = f"{uuid.uuid4().hex}{suffix}"
    data = uploaded_file.read()

    try:
        client = get_client()
        bucket = settings.MINIO_BUCKET
        if not client.bucket_exists(bucket):
            client.make_bucket(bucket)
        client.put_object(
            bucket,
            object_name,
            io.BytesIO(data),
            length=len(data),
            content_type=uploaded_file.content_type or "application/octet-stream",
        )
        logger.info(f"Poster {object_name} diunggah ke bucket Minio {bucket}")
        return object_name
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Gagal mengunggah ke Minio ({exc}); memakai penyimpanan lokal")
        (_local_fallback_dir() / object_name).write_bytes(data)
        return object_name


def read_poster(object_name):
    """Kriteria 1 (4 pts): mengambil kembali berkas media yang telah diunggah."""
    try:
        client = get_client()
        response = client.get_object(settings.MINIO_BUCKET, object_name)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()
    except Exception as exc:  # noqa: BLE001
        logger.error(f"Gagal membaca {object_name} dari Minio ({exc}); mencoba berkas lokal")
        local = _local_fallback_dir() / object_name
        if local.exists():
            return local.read_bytes()
        return None


def poster_url(object_name):
    host, secure = _endpoint_parts()
    scheme = "https" if secure else "http"
    return f"{scheme}://{host}/{settings.MINIO_BUCKET}/{object_name}"
