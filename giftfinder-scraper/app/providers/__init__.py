"""
Provider module for pluggable product data sources.
"""

from app.providers.base import ProductProvider, ProviderResult
from app.providers.models import Product, ProductQuery, RecipientProfile

__all__ = [
    "ProductProvider",
    "ProviderResult",
    "Product",
    "ProductQuery",
    "RecipientProfile",
]
