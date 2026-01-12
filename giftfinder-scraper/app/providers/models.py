"""
Core data models for the provider system.
"""

from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class RecipientProfile(BaseModel):
    """Profile information about the gift recipient."""

    type: str = Field(default="unknown", description="Recipient type (friend, family, colleague, etc.)")
    age: Optional[int] = Field(default=None, description="Recipient age")
    interests: List[str] = Field(default_factory=list, description="Recipient interests")


class ProductQuery(BaseModel):
    """
    Structured query for product search.
    Represents the user's intent extracted from natural language.
    """

    keywords: List[str] = Field(default_factory=list, description="Search keywords")
    category: Optional[str] = Field(default=None, description="Product category")
    priceMin: Optional[float] = Field(default=None, description="Minimum price filter")
    priceMax: Optional[float] = Field(default=None, description="Maximum price filter")
    location: Optional[str] = Field(default=None, description="User location")
    occasion: Optional[str] = Field(default=None, description="Gift occasion")
    recipientProfile: RecipientProfile = Field(
        default_factory=RecipientProfile, description="Recipient profile"
    )
    limit: int = Field(default=10, ge=1, le=100, description="Max results to return")


class Product(BaseModel):
    """
    Standardized product model used across all providers.
    """

    id: str = Field(description="Unique product identifier")
    title: str = Field(description="Product name/title")
    description: Optional[str] = Field(default=None, description="Product description")
    images: List[str] = Field(default_factory=list, description="Product image URLs")
    price: Optional[float] = Field(default=None, description="Product price")
    currency: str = Field(default="ARS", description="Currency code")
    vendor: str = Field(description="Seller/store name")
    url: str = Field(description="Product or search URL")
    sourceProvider: str = Field(description="Provider name that supplied this product")
    categories: List[str] = Field(default_factory=list, description="Product categories")
    tags: List[str] = Field(default_factory=list, description="User interest tags")
    score: Optional[float] = Field(default=None, description="Relevance score (0-1)")


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
