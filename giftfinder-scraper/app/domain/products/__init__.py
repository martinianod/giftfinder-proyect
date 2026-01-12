"""
Product domain module with normalizers, deduplication, and DTO mapping.
"""

from app.domain.products.normalizers import (
    buildStableDedupKey,
    canonicalizeUrl,
    normalizePrice,
    normalizeTitle,
)
from app.domain.products.public_dto import toPublicProduct

__all__ = [
    "normalizeTitle",
    "normalizePrice",
    "canonicalizeUrl",
    "buildStableDedupKey",
    "toPublicProduct",
]
