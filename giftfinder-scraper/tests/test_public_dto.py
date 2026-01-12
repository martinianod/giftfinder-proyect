"""
Tests for PublicProduct DTO mapping.
"""

import pytest
from app.domain.products.public_dto import toPublicProduct
from app.providers.models import Product, VendorInfo


class TestPublicProductMapping:
    """Tests for public product DTO mapping."""

    def test_strips_raw_field_by_default(self):
        """Test that raw field is stripped by default."""
        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Test Store"),
            url="https://example.com/product",
            sourceProvider="test",
            raw={"internal": "data", "debug": "info"},
        )
        
        result = toPublicProduct(product, debug=False)
        
        assert "raw" not in result
        assert result["id"] == "test-123"
        assert result["title"] == "Test Product"

    def test_includes_raw_field_in_debug_mode(self):
        """Test that raw field is included in debug mode."""
        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Test Store"),
            url="https://example.com/product",
            sourceProvider="test",
            raw={"internal": "data"},
        )
        
        result = toPublicProduct(product, debug=True)
        
        assert "raw" in result
        assert result["raw"] == {"internal": "data"}

    def test_handles_product_without_raw_field(self):
        """Test handling of product without raw field."""
        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Test Store"),
            url="https://example.com/product",
            sourceProvider="test",
        )
        
        result = toPublicProduct(product, debug=False)
        
        assert "raw" not in result
        assert result["id"] == "test-123"

    def test_preserves_all_other_fields(self):
        """Test that all non-raw fields are preserved."""
        product = Product(
            id="test-123",
            title="Test Product",
            description="Test description",
            images=["https://example.com/image.jpg"],
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Test Store", id="store-123"),
            url="https://example.com/product",
            sourceProvider="test",
            categories=["electronics"],
            tags=["tech", "gadget"],
            score=0.85,
            raw={"internal": "data"},
        )
        
        result = toPublicProduct(product, debug=False)
        
        assert result["id"] == "test-123"
        assert result["title"] == "Test Product"
        assert result["description"] == "Test description"
        assert result["images"] == ["https://example.com/image.jpg"]
        assert result["price"] == 100.0
        assert result["currency"] == "ARS"
        assert result["vendor"]["name"] == "Test Store"
        assert result["vendor"]["id"] == "store-123"
        assert result["url"] == "https://example.com/product"
        assert result["sourceProvider"] == "test"
        assert result["categories"] == ["electronics"]
        assert result["tags"] == ["tech", "gadget"]
        assert result["score"] == 0.85
        assert "raw" not in result
