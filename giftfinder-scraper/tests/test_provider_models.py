"""
Tests for provider models.
"""

import pytest
from app.providers.models import Product, ProductQuery, RecipientProfile, ProviderResult


class TestRecipientProfile:
    """Tests for RecipientProfile model."""

    def test_default_values(self):
        """Test RecipientProfile with default values."""
        profile = RecipientProfile()
        assert profile.type == "unknown"
        assert profile.age is None
        assert profile.interests == []

    def test_with_values(self):
        """Test RecipientProfile with custom values."""
        profile = RecipientProfile(
            type="friend", age=30, interests=["technology", "gaming"]
        )
        assert profile.type == "friend"
        assert profile.age == 30
        assert profile.interests == ["technology", "gaming"]


class TestProductQuery:
    """Tests for ProductQuery model."""

    def test_default_values(self):
        """Test ProductQuery with default values."""
        query = ProductQuery()
        assert query.keywords == []
        assert query.category is None
        assert query.priceMin is None
        assert query.priceMax is None
        assert query.limit == 10
        assert isinstance(query.recipientProfile, RecipientProfile)

    def test_with_keywords(self):
        """Test ProductQuery with keywords."""
        query = ProductQuery(keywords=["auriculares", "bluetooth"])
        assert query.keywords == ["auriculares", "bluetooth"]

    def test_with_price_range(self):
        """Test ProductQuery with price range."""
        query = ProductQuery(priceMin=5000.0, priceMax=20000.0)
        assert query.priceMin == 5000.0
        assert query.priceMax == 20000.0

    def test_limit_validation(self):
        """Test ProductQuery limit validation."""
        query = ProductQuery(limit=30)  # Changed from 50 to 30 (new max)
        assert query.limit == 30

        # Test minimum
        with pytest.raises(Exception):
            ProductQuery(limit=0)

        # Test maximum - now 30 instead of 100
        with pytest.raises(Exception):
            ProductQuery(limit=31)

    def test_with_recipient_profile(self):
        """Test ProductQuery with recipient profile."""
        profile = RecipientProfile(type="friend", age=30)
        query = ProductQuery(recipientProfile=profile)
        assert query.recipientProfile.type == "friend"
        assert query.recipientProfile.age == 30


class TestProduct:
    """Tests for Product model."""

    def test_minimal_product(self):
        """Test Product with minimal required fields."""
        from app.providers.models import VendorInfo
        
        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,  # Now required
            vendor=VendorInfo(name="Test Vendor"),  # Now a structured object
            url="https://example.com/product",
            sourceProvider="test",
            currency="ARS",
        )
        assert product.id == "test-123"
        assert product.title == "Test Product"
        assert product.price == 100.0
        assert product.vendor.name == "Test Vendor"
        assert product.url == "https://example.com/product"
        assert product.sourceProvider == "test"
        assert product.currency == "ARS"

    def test_full_product(self):
        """Test Product with all fields."""
        from app.providers.models import VendorInfo
        
        product = Product(
            id="test-123",
            title="Test Product",
            description="Test description",
            images=["https://example.com/image.jpg"],
            price=10000.0,
            currency="ARS",
            vendor=VendorInfo(name="Test Vendor"),  # Now a structured object
            url="https://example.com/product",
            sourceProvider="test",
            categories=["technology"],
            tags=["tech", "gadget"],
            score=0.85,
        )
        assert product.description == "Test description"
        assert len(product.images) == 1
        assert product.price == 10000.0
        assert product.categories == ["technology"]
        assert product.tags == ["tech", "gadget"]
        assert product.score == 0.85

    def test_default_lists(self):
        """Test that list fields default to empty lists."""
        from app.providers.models import VendorInfo
        
        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,  # Now required
            vendor=VendorInfo(name="Test Vendor"),  # Now a structured object
            url="https://example.com/product",
            sourceProvider="test",
            currency="ARS",
        )
        assert product.images == []
        assert product.categories == []
        assert product.tags == []


class TestProviderResult:
    """Tests for ProviderResult model."""

    def test_empty_result(self):
        """Test ProviderResult with no products."""
        from app.providers.models import ProviderMetadata

        result = ProviderResult(
            products=[],
            meta=ProviderMetadata(
                providerName="test", latencyMs=100, warnings=["No products found"]
            ),
        )
        assert len(result.products) == 0
        assert result.meta.providerName == "test"
        assert result.meta.latencyMs == 100
        assert len(result.meta.warnings) == 1

    def test_with_products(self):
        """Test ProviderResult with products."""
        from app.providers.models import ProviderMetadata, VendorInfo

        product = Product(
            id="test-123",
            title="Test Product",
            price=100.0,  # Now required
            vendor=VendorInfo(name="Test Vendor"),  # Now a structured object
            url="https://example.com/product",
            sourceProvider="test",
            currency="ARS",
        )
        result = ProviderResult(
            products=[product],
            meta=ProviderMetadata(providerName="test", latencyMs=100, warnings=[]),
        )
        assert len(result.products) == 1
        assert result.products[0].id == "test-123"
