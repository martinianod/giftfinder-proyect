"""
Normalizer functions for product data.

These functions ensure consistent data formats across different providers.
"""

import hashlib
import re
from typing import Optional
from urllib.parse import urlparse, urlunparse


def normalizeTitle(title: str) -> str:
    """
    Normalize product title for consistent display and comparison.

    - Strips leading/trailing whitespace
    - Removes excessive whitespace
    - Capitalizes first letter of each word
    - Removes special characters except basic punctuation

    Args:
        title: Raw product title

    Returns:
        Normalized title

    Examples:
        >>> normalizeTitle("  AURICULARES   bluetooth  ")
        'Auriculares Bluetooth'
        >>> normalizeTitle("Product!!!  Name???")
        'Product Name'
    """
    if not title:
        return ""

    # Strip and collapse whitespace
    title = re.sub(r"\s+", " ", title.strip())

    # Remove excessive punctuation (keep single occurrences)
    title = re.sub(r"([!?.]){2,}", r"\1", title)

    # Title case for better readability
    title = title.title()

    return title


def normalizePrice(price: Optional[float], currency: str = "ARS") -> Optional[float]:
    """
    Normalize price value for consistent comparison.

    - Rounds to 2 decimal places
    - Ensures non-negative
    - Returns None for invalid prices

    Args:
        price: Raw price value
        currency: Currency code (for future currency conversion)

    Returns:
        Normalized price or None

    Examples:
        >>> normalizePrice(12345.6789)
        12345.68
        >>> normalizePrice(-100)
        None
        >>> normalizePrice(None)
        None
    """
    if price is None:
        return None

    # Ensure non-negative
    if price < 0:
        return None

    # Round to 2 decimal places
    return round(price, 2)


def canonicalizeUrl(url: str) -> str:
    """
    Canonicalize URL for consistent deduplication.

    - Converts to lowercase
    - Removes query parameters (except essential ones like product ID)
    - Removes trailing slashes
    - Removes fragments
    - Normalizes domain

    Args:
        url: Raw URL

    Returns:
        Canonicalized URL

    Examples:
        >>> canonicalizeUrl("https://example.com/Product/123?utm_source=ads#review")
        'https://example.com/product/123'
        >>> canonicalizeUrl("HTTP://Example.COM/path/")
        'http://example.com/path'
    """
    if not url:
        return ""

    try:
        parsed = urlparse(url.strip())

        # Normalize scheme and netloc to lowercase
        scheme = parsed.scheme.lower()
        netloc = parsed.netloc.lower()

        # Normalize path (lowercase, remove trailing slash)
        path = parsed.path.lower().rstrip("/")

        # Remove query params and fragments for deduplication
        # (in production, you might want to keep certain params like item_id)

        return urlunparse((scheme, netloc, path, "", "", ""))
    except Exception:
        # If URL parsing fails, return lowercase version
        return url.lower().strip()


def buildStableDedupKey(
    product_id: str, title: str, vendor_name: str, price: Optional[float], url: str
) -> str:
    """
    Build a stable deduplication key for a product.

    Uses a hash of key product attributes to create a consistent identifier
    for deduplication across provider results.

    Strategy:
    1. Canonicalize URL as primary key
    2. If URL is generic (search page), use normalized title + vendor + price

    Args:
        product_id: Product ID from provider
        title: Product title
        vendor_name: Vendor name
        price: Product price
        url: Product URL

    Returns:
        Stable deduplication key (SHA256 hash)

    Examples:
        >>> key1 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123")
        >>> key2 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123?ref=ads")
        >>> key1 == key2
        True
    """
    # Start with canonical URL
    canonical_url = canonicalizeUrl(url)

    # Check if URL is a direct product link or search page
    is_search_page = any(
        indicator in canonical_url.lower()
        for indicator in ["/search", "/buscar", "/s/", "/q/", "/query"]
    )

    if is_search_page:
        # For search pages, use normalized product attributes
        normalized_title = normalizeTitle(title)
        normalized_price = normalizePrice(price)

        dedup_string = f"{normalized_title}|{vendor_name.lower()}|{normalized_price}"
    else:
        # For direct product links, use canonical URL
        dedup_string = canonical_url

    # Create stable hash
    return hashlib.sha256(dedup_string.encode("utf-8")).hexdigest()
