"""Kriteria 4: custom logging dengan loguru.

Format memuat timestamp, log level, lokasi, dan pesan log. Log level info
disimpan di application.log, log level error di error.log, keduanya dirotasi
setiap 1 hari sekali.
"""

import sys
from pathlib import Path

from loguru import logger

LOG_FORMAT = (
    "{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | "
    "{name}:{function}:{line} - {message}"
)


def setup_logging(base_dir):
    log_dir = Path(base_dir) / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)

    logger.remove()
    logger.add(sys.stdout, format=LOG_FORMAT, level="INFO", colorize=True)
    logger.add(
        log_dir / "application.log",
        format=LOG_FORMAT,
        level="INFO",
        rotation="1 day",
        retention="14 days",
        encoding="utf-8",
    )
    logger.add(
        log_dir / "error.log",
        format=LOG_FORMAT,
        level="ERROR",
        rotation="1 day",
        retention="14 days",
        encoding="utf-8",
    )
    return logger
