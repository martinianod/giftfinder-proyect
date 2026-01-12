"""
PublicProduct DTO for external API responses.

Filters sensitive/debug fields from the internal Product model.
"""

from typing import Dict, List, Optional

from pydantic import BaseModel, Field

from app.providers.models import (Product, ProductAvailability, ProductRating,
                                  ShippingInfo, VendorInfo)


class PublicProduct(BaseModel):
    """
    Public-facing product model with debug fields stripped.

    This DTO is returned to clients and excludes:
    - raw: internal provider data
    - Other debug fields when debug=false
    """

    id: str = Field(description="Unique product identifier")
    title: str = Field(description="Product name/title")
    description: Optional[str] = Field(default=None, description="Product description")
    images: List[str] = Field(default_factory=list, description="Product image URLs")
    price: float = Field(description="Product price")
    currency: str = Field(description="Currency code")
    vendor: VendorInfo = Field(description="Seller/store information")
    url: str = Field(description="Product or search URL")
    sourceProvider: str = Field(description="Provider name that supplied this product")
    categories: List[str] = Field(
        default_factory=list, description="Product categories"
    )
    tags: List[str] = Field(default_factory=list, description="User interest tags")
    availability: Optional[ProductAvailability] = Field(
        default=None, description="Availability information"
    )
    shipping: Optional[ShippingInfo] = Field(
        default=None, description="Shipping information"
    )
    rating: Optional[ProductRating] = Field(default=None, description="Product rating")
    score: Optional[float] = Field(default=None, description="Relevance score (0-1)")
    # Note: 'raw' field is intentionally excluded


def toPublicProduct(product: Product, debug: bool = False) -> Dict:
    """
    Convert internal Product to public DTO.

    Strips 'raw' field unless debug=true.

    Args:
        product: Internal product model
        debug: Whether to include debug fields

    Returns:
        Dictionary representation suitable for API response

    Examples:
        >>> product = Product(id="1", title="Test", price=100, currency="ARS",
        ...                   vendor=VendorInfo(name="Store"), url="http://ex.com",
        ...                   sourceProvider="test", raw={"internal": "data"})
        >>> result = toPublicProduct(product, debug=False)
        >>> "raw" in result
        False
        >>> result = toPublicProduct(product, debug=True)
        >>> "raw" in result
        True
    """
    # Convert to dict
    product_dict = product.model_dump()

    # Remove raw field unless debug mode
    if not debug and "raw" in product_dict:
        del product_dict["raw"]

    return product_dict
