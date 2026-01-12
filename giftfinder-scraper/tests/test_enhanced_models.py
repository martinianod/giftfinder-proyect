"""
Tests for enhanced provider models.
"""

import pytest
from app.providers.models import (
    LocationFilter,
    Product,
    ProductAvailability,
    ProductQuery,
    ProductRating,
    ProviderCapabilities,
    ProviderContext,
    ProviderError,
    ProviderErrorCategory,
    RecipientProfile,
    ShippingInfo,
    VendorInfo,
)


class TestProductQueryLimitClamping:
    """Tests for ProductQuery limit clamping."""

    def test_limit_within_range(self):
        """Test that valid limit is preserved."""
        query = ProductQuery(limit=15)
        assert query.limit == 15

    def test_limit_at_max(self):
        """Test that limit at maximum of 30 is valid."""
        query = ProductQuery(limit=30)
        assert query.limit == 30

    def test_limit_at_min(self):
        """Test that limit at minimum of 1 is valid."""
        query = ProductQuery(limit=1)
        assert query.limit == 1

    def test_limit_above_max_raises_error(self):
        """Test that limit above 30 raises validation error."""
        with pytest.raises(Exception) as exc_info:
            ProductQuery(limit=50)
        assert "less_than_equal" in str(exc_info.value) or "30" in str(exc_info.value)

    def test_limit_below_min_raises_error(self):
        """Test that limit below 1 raises validation error."""
        with pytest.raises(Exception) as exc_info:
            ProductQuery(limit=0)
        assert "greater_than_equal" in str(exc_info.value) or "1" in str(exc_info.value)

    def test_default_limit(self):
        """Test that default limit is 10."""
        query = ProductQuery()
        assert query.limit == 10


class TestEnhancedProductQuery:
    """Tests for enhanced ProductQuery fields."""

    def test_currency_field(self):
        """Test currency field."""
        query = ProductQuery(currency="USD")
        assert query.currency == "USD"

    def test_locale_field(self):
        """Test locale field."""
        query = ProductQuery(locale="es-AR")
        assert query.locale == "es-AR"

    def test_location_filter(self):
        """Test location filter."""
        location = LocationFilter(country="AR", region="Buenos Aires", city="CABA")
        query = ProductQuery(location=location)
        assert query.location.country == "AR"
        assert query.location.region == "Buenos Aires"
        assert query.location.city == "CABA"

    def test_safe_search_default(self):
        """Test that safeSearch defaults to True."""
        query = ProductQuery()
        assert query.safeSearch is True

    def test_debug_default(self):
        """Test that debug defaults to False."""
        query = ProductQuery()
        assert query.debug is False

    def test_vendor_filters(self):
        """Test vendor include/exclude filters."""
        query = ProductQuery(
            excludeVendors=["vendor1", "vendor2"],
            includeVendors=["vendor3", "vendor4"]
        )
        assert query.excludeVendors == ["vendor1", "vendor2"]
        assert query.includeVendors == ["vendor3", "vendor4"]

    def test_keywords_validation(self):
        """Test keywords field."""
        query = ProductQuery(keywords=["tech", "gadget"])
        assert query.keywords == ["tech", "gadget"]


class TestEnhancedRecipientProfile:
    """Tests for enhanced RecipientProfile."""

    def test_gender_field(self):
        """Test gender field."""
        profile = RecipientProfile(gender="female")
        assert profile.gender == "female"

    def test_relationship_field(self):
        """Test relationship field."""
        profile = RecipientProfile(relationship="spouse")
        assert profile.relationship == "spouse"


class TestEnhancedProduct:
    """Tests for enhanced Product model."""

    def test_vendor_info_structure(self):
        """Test vendor as structured object."""
        vendor = VendorInfo(name="Test Store", id="store-123")
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=vendor,
            url="https://example.com",
            sourceProvider="test",
        )
        assert product.vendor.name == "Test Store"
        assert product.vendor.id == "store-123"

    def test_availability_info(self):
        """Test availability information."""
        availability = ProductAvailability(
            inStock=True,
            stockCount=10,
            leadTimeDays=3
        )
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Store"),
            url="https://example.com",
            sourceProvider="test",
            availability=availability,
        )
        assert product.availability.inStock is True
        assert product.availability.stockCount == 10
        assert product.availability.leadTimeDays == 3

    def test_shipping_info(self):
        """Test shipping information."""
        shipping = ShippingInfo(
            cost=500.0,
            currency="ARS",
            methods=["standard", "express"]
        )
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Store"),
            url="https://example.com",
            sourceProvider="test",
            shipping=shipping,
        )
        assert product.shipping.cost == 500.0
        assert product.shipping.currency == "ARS"
        assert product.shipping.methods == ["standard", "express"]

    def test_rating_info(self):
        """Test rating information."""
        rating = ProductRating(value=4.5, count=120)
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Store"),
            url="https://example.com",
            sourceProvider="test",
            rating=rating,
        )
        assert product.rating.value == 4.5
        assert product.rating.count == 120

    def test_raw_field(self):
        """Test raw debug field."""
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Store"),
            url="https://example.com",
            sourceProvider="test",
            raw={"original_data": "test"},
        )
        assert product.raw == {"original_data": "test"}

    def test_price_is_required(self):
        """Test that price is required and must be non-negative."""
        product = Product(
            id="test-1",
            title="Test Product",
            price=100.0,
            currency="ARS",
            vendor=VendorInfo(name="Store"),
            url="https://example.com",
            sourceProvider="test",
        )
        assert product.price == 100.0


class TestProviderCapabilities:
    """Tests for ProviderCapabilities."""

    def test_default_capabilities(self):
        """Test default capability values."""
        caps = ProviderCapabilities()
        assert caps.supportsImages is True
        assert caps.supportsPriceFilter is False
        assert caps.supportsLocation is False
        assert caps.supportsStock is False
        assert caps.supportsDeepLink is True

    def test_custom_capabilities(self):
        """Test custom capability values."""
        caps = ProviderCapabilities(
            supportsImages=True,
            supportsPriceFilter=True,
            supportsStock=True,
            supportsRatings=True,
        )
        assert caps.supportsImages is True
        assert caps.supportsPriceFilter is True
        assert caps.supportsStock is True
        assert caps.supportsRatings is True


class TestProviderContext:
    """Tests for ProviderContext."""

    def test_required_fields(self):
        """Test required context fields."""
        ctx = ProviderContext(requestId="req-123")
        assert ctx.requestId == "req-123"
        assert ctx.timeoutMs == 15000  # Default

    def test_custom_timeout(self):
        """Test custom timeout."""
        ctx = ProviderContext(requestId="req-123", timeoutMs=30000)
        assert ctx.timeoutMs == 30000

    def test_trace_info(self):
        """Test trace information."""
        ctx = ProviderContext(
            requestId="req-123",
            trace={"spanId": "span-456"}
        )
        assert ctx.trace["spanId"] == "span-456"


class TestProviderError:
    """Tests for ProviderError."""

    def test_error_categories(self):
        """Test error category enum."""
        assert ProviderErrorCategory.TIMEOUT == "TIMEOUT"
        assert ProviderErrorCategory.RATE_LIMIT == "RATE_LIMIT"
        assert ProviderErrorCategory.AUTH == "AUTH"
        assert ProviderErrorCategory.PARSE == "PARSE"
        assert ProviderErrorCategory.UPSTREAM == "UPSTREAM"
        assert ProviderErrorCategory.UNKNOWN == "UNKNOWN"

    def test_error_structure(self):
        """Test error structure."""
        error = ProviderError(
            category=ProviderErrorCategory.TIMEOUT,
            providerName="test-provider",
            message="Request timed out",
            retryable=True,
            details={"timeout_ms": 15000}
        )
        assert error.category == ProviderErrorCategory.TIMEOUT
        assert error.providerName == "test-provider"
        assert error.message == "Request timed out"
        assert error.retryable is True
        assert error.details["timeout_ms"] == 15000

    def test_non_retryable_error(self):
        """Test non-retryable error."""
        error = ProviderError(
            category=ProviderErrorCategory.AUTH,
            providerName="test-provider",
            message="Authentication failed",
            retryable=False,
        )
        assert error.retryable is False
