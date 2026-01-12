"""
Tests for domain normalizer functions.
"""

import pytest
from app.domain.products.normalizers import (
    buildStableDedupKey,
    canonicalizeUrl,
    normalizePrice,
    normalizeTitle,
)


class TestNormalizeTitle:
    """Tests for title normalization."""

    def test_strips_whitespace(self):
        """Test that title normalization strips whitespace."""
        assert normalizeTitle("  Product Name  ") == "Product Name"

    def test_collapses_whitespace(self):
        """Test that multiple spaces are collapsed."""
        assert normalizeTitle("Product    Name") == "Product Name"

    def test_title_case(self):
        """Test that title is converted to title case."""
        assert normalizeTitle("auriculares bluetooth") == "Auriculares Bluetooth"

    def test_removes_excessive_punctuation(self):
        """Test that excessive punctuation is removed."""
        assert normalizeTitle("Product!!!") == "Product!"
        assert normalizeTitle("Product???") == "Product?"

    def test_empty_string(self):
        """Test normalization of empty string."""
        assert normalizeTitle("") == ""

    def test_none_handling(self):
        """Test normalization handles None-like values."""
        assert normalizeTitle("") == ""


class TestNormalizePrice:
    """Tests for price normalization."""

    def test_rounds_to_two_decimals(self):
        """Test that price is rounded to 2 decimal places."""
        assert normalizePrice(12345.6789) == 12345.68
        assert normalizePrice(100.999) == 101.0

    def test_rejects_negative_prices(self):
        """Test that negative prices return None."""
        assert normalizePrice(-100.0) is None

    def test_none_input(self):
        """Test that None input returns None."""
        assert normalizePrice(None) is None

    def test_zero_price(self):
        """Test that zero price is valid."""
        assert normalizePrice(0.0) == 0.0


class TestCanonicalizeUrl:
    """Tests for URL canonicalization."""

    def test_lowercases_url(self):
        """Test that URL is converted to lowercase."""
        assert canonicalizeUrl("HTTPS://EXAMPLE.COM/Product") == "https://example.com/product"

    def test_removes_query_params(self):
        """Test that query parameters are removed."""
        url = "https://example.com/product?utm_source=ads&ref=social"
        expected = "https://example.com/product"
        assert canonicalizeUrl(url) == expected

    def test_removes_fragments(self):
        """Test that URL fragments are removed."""
        url = "https://example.com/product#reviews"
        expected = "https://example.com/product"
        assert canonicalizeUrl(url) == expected

    def test_removes_trailing_slash(self):
        """Test that trailing slashes are removed."""
        assert canonicalizeUrl("https://example.com/product/") == "https://example.com/product"

    def test_normalizes_domain(self):
        """Test that domain is normalized."""
        assert canonicalizeUrl("HTTP://Example.COM/path") == "http://example.com/path"

    def test_empty_url(self):
        """Test handling of empty URL."""
        assert canonicalizeUrl("") == ""

    def test_invalid_url_fallback(self):
        """Test that invalid URLs are lowercased."""
        assert canonicalizeUrl("not a url") == "not a url"


class TestBuildStableDedupKey:
    """Tests for deduplication key generation."""

    def test_stability_for_same_product(self):
        """Test that same product generates same key."""
        key1 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123")
        key2 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123")
        assert key1 == key2

    def test_ignores_query_params_in_url(self):
        """Test that query parameters don't affect dedup key."""
        key1 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123")
        key2 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123?ref=ads")
        assert key1 == key2

    def test_different_products_different_keys(self):
        """Test that different products generate different keys."""
        key1 = buildStableDedupKey("123", "Product A", "Store", 100.0, "https://example.com/p/123")
        key2 = buildStableDedupKey("456", "Product B", "Store", 200.0, "https://example.com/p/456")
        assert key1 != key2

    def test_search_page_uses_attributes(self):
        """Test that search pages use product attributes for dedup."""
        # Two different products from same search page should have different keys
        key1 = buildStableDedupKey("1", "Headphones", "Store A", 100.0, "https://example.com/search?q=audio")
        key2 = buildStableDedupKey("2", "Speakers", "Store B", 200.0, "https://example.com/search?q=audio")
        assert key1 != key2

    def test_product_page_uses_url(self):
        """Test that product pages use URL for dedup."""
        # Same URL with different attributes should have same key
        url = "https://example.com/product/123"
        key1 = buildStableDedupKey("123", "Product A", "Store", 100.0, url)
        key2 = buildStableDedupKey("123", "Product A Updated", "Store", 150.0, url)
        assert key1 == key2

    def test_returns_hash_string(self):
        """Test that key is a hash string."""
        key = buildStableDedupKey("123", "Product", "Store", 100.0, "https://example.com/p/123")
        assert isinstance(key, str)
        assert len(key) == 64  # SHA256 hex digest length

    def test_case_insensitive_vendor(self):
        """Test that vendor name is case-insensitive in search pages."""
        key1 = buildStableDedupKey("1", "Product", "Store", 100.0, "https://example.com/search?q=test")
        key2 = buildStableDedupKey("1", "Product", "STORE", 100.0, "https://example.com/search?q=test")
        assert key1 == key2
