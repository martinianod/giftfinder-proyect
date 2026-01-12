"""
Core data models for the provider system.
"""

from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field, field_validator


class ProviderErrorCategory(str, Enum):
    """Categories of provider errors."""
    TIMEOUT = "TIMEOUT"
    RATE_LIMIT = "RATE_LIMIT"
    AUTH = "AUTH"
    PARSE = "PARSE"
    UPSTREAM = "UPSTREAM"
    UNKNOWN = "UNKNOWN"


class ProviderError(BaseModel):
    """Typed error for provider failures."""
    
    category: ProviderErrorCategory = Field(description="Error category")
    providerName: str = Field(description="Provider that generated the error")
    message: str = Field(description="Human-readable error message")
    retryable: bool = Field(description="Whether the operation can be retried")
    details: Optional[Dict[str, Any]] = Field(default=None, description="Additional error details")


class ProviderCapabilities(BaseModel):
    """Describes what features a provider supports."""
    
    supportsImages: bool = Field(default=True, description="Provider returns product images")
    supportsPriceFilter: bool = Field(default=False, description="Provider can filter by price range")
    supportsLocation: bool = Field(default=False, description="Provider supports location-based filtering")
    supportsStock: bool = Field(default=False, description="Provider returns stock availability")
    supportsDeepLink: bool = Field(default=True, description="Provider returns direct product URLs")
    supportsCategories: bool = Field(default=False, description="Provider supports category filtering")
    supportsRatings: bool = Field(default=False, description="Provider returns product ratings")
    supportsShipping: bool = Field(default=False, description="Provider returns shipping information")


class LocationFilter(BaseModel):
    """Location-based filtering details."""
    
    country: Optional[str] = Field(default=None, description="Country code (ISO 3166-1 alpha-2)")
    region: Optional[str] = Field(default=None, description="Region/state")
    city: Optional[str] = Field(default=None, description="City name")


class RecipientProfile(BaseModel):
    """Profile information about the gift recipient."""

    type: str = Field(default="unknown", description="Recipient type (friend, family, colleague, etc.)")
    age: Optional[int] = Field(default=None, description="Recipient age")
    gender: Optional[str] = Field(default=None, description="Recipient gender")
    interests: List[str] = Field(default_factory=list, description="Recipient interests")
    relationship: Optional[str] = Field(default=None, description="Relationship to recipient")


class ProductQuery(BaseModel):
    """
    Structured query for product search.
    Represents the user's intent extracted from natural language.
    """

    keywords: List[str] = Field(
        default_factory=list,
        description="2-6 commercial keywords",
        min_length=0,
        max_length=6
    )
    category: Optional[str] = Field(default=None, description="Product category")
    priceMin: Optional[float] = Field(default=None, ge=0, description="Minimum price filter")
    priceMax: Optional[float] = Field(default=None, ge=0, description="Maximum price filter")
    currency: Optional[str] = Field(default=None, description="Currency code (ISO 4217)")
    locale: Optional[str] = Field(default=None, description="Locale (e.g., 'es-AR', 'en-US')")
    location: Optional[LocationFilter] = Field(default=None, description="Location-based filtering")
    occasion: Optional[str] = Field(default=None, description="Gift occasion")
    recipientProfile: RecipientProfile = Field(
        default_factory=RecipientProfile, description="Recipient profile"
    )
    limit: int = Field(default=10, ge=1, le=30, description="Max results to return (clamped to 30)")
    excludeVendors: List[str] = Field(default_factory=list, description="Vendor IDs to exclude")
    includeVendors: List[str] = Field(default_factory=list, description="Vendor IDs to include (whitelist)")
    safeSearch: bool = Field(default=True, description="Enable safe search filtering")
    debug: bool = Field(default=False, description="Include debug information in response")
    
    @field_validator('limit')
    @classmethod
    def clamp_limit(cls, v: int) -> int:
        """Ensure limit is clamped between 1 and 30."""
        return max(1, min(30, v))


class VendorInfo(BaseModel):
    """Vendor/seller information."""
    
    name: str = Field(description="Vendor/store name")
    id: Optional[str] = Field(default=None, description="Vendor unique identifier")


class ProductAvailability(BaseModel):
    """Product availability information."""
    
    inStock: Optional[bool] = Field(default=None, description="Whether product is in stock")
    stockCount: Optional[int] = Field(default=None, ge=0, description="Available stock count")
    leadTimeDays: Optional[int] = Field(default=None, ge=0, description="Lead time in days for delivery")


class ShippingInfo(BaseModel):
    """Shipping information."""
    
    cost: Optional[float] = Field(default=None, ge=0, description="Shipping cost")
    currency: Optional[str] = Field(default=None, description="Shipping cost currency")
    methods: List[str] = Field(default_factory=list, description="Available shipping methods")


class ProductRating(BaseModel):
    """Product rating information."""
    
    value: Optional[float] = Field(default=None, ge=0, le=5, description="Rating value (0-5)")
    count: Optional[int] = Field(default=None, ge=0, description="Number of ratings")


class Product(BaseModel):
    """
    Standardized product model used across all providers.
    """

    id: str = Field(description="Unique product identifier (stable within provider)")
    title: str = Field(description="Product name/title")
    description: Optional[str] = Field(default=None, description="Product description")
    images: List[str] = Field(default_factory=list, description="Product image URLs")
    price: float = Field(ge=0, description="Product price")
    currency: str = Field(default="ARS", description="Currency code (ISO 4217 if possible)")
    vendor: VendorInfo = Field(description="Seller/store information")
    url: str = Field(description="Product or search URL")
    sourceProvider: str = Field(description="Provider name that supplied this product")
    categories: List[str] = Field(default_factory=list, description="Product categories")
    tags: List[str] = Field(default_factory=list, description="User interest tags")
    availability: Optional[ProductAvailability] = Field(default=None, description="Availability information")
    shipping: Optional[ShippingInfo] = Field(default=None, description="Shipping information")
    rating: Optional[ProductRating] = Field(default=None, description="Product rating")
    score: Optional[float] = Field(default=None, ge=0, le=1, description="Relevance score (0-1)")
    raw: Optional[Dict[str, Any]] = Field(default=None, description="Raw provider data (for debugging)")


class ProviderMetadata(BaseModel):
    """Metadata about a provider's response."""

    providerName: str = Field(description="Provider identifier")
    fetchedAt: datetime = Field(default_factory=datetime.now, description="Timestamp of fetch")
    latencyMs: int = Field(description="Response time in milliseconds")
    warnings: List[str] = Field(default_factory=list, description="Any warnings or errors")


class ProviderResult(BaseModel):
    """Result from a product provider."""

    products: List[Product] = Field(default_factory=list, description="List of products")
    meta: ProviderMetadata = Field(description="Provider metadata")


class ProviderContext(BaseModel):
    """Context information for provider execution."""
    
    requestId: str = Field(description="Unique request identifier for tracing")
    timeoutMs: int = Field(default=15000, gt=0, description="Timeout in milliseconds")
    signal: Optional[Any] = Field(default=None, description="Cancellation signal/token")
    logger: Optional[Any] = Field(default=None, description="Logger instance")
    trace: Optional[Dict[str, str]] = Field(default=None, description="Tracing information (spanId, etc.)")
