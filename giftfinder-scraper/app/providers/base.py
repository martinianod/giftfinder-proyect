"""
Base abstract class for product providers.
"""

from abc import ABC, abstractmethod
from typing import Optional

from app.providers.models import (ProductQuery, ProviderCapabilities,
                                  ProviderContext, ProviderResult)


class ProductProvider(ABC):
    """
    Abstract base class for all product providers.

    Each provider is responsible for:
    1. Declaring its capabilities
    2. Checking if it can handle a query (supports)
    3. Fetching products for a query (search)
    4. Returning standardized Product objects
    """

    @property
    @abstractmethod
    def name(self) -> str:
        """
        Provider identifier (e.g., 'reference', 'scraping', 'affiliate').

        Returns:
            Unique provider name
        """
        pass

    @property
    @abstractmethod
    def capabilities(self) -> ProviderCapabilities:
        """
        Provider capabilities declaration.

        Returns:
            ProviderCapabilities describing what this provider supports
        """
        pass

    @abstractmethod
    def supports(self, query: ProductQuery) -> bool:
        """
        Check if this provider can handle the given query.

        Args:
            query: The product query to evaluate

        Returns:
            True if this provider can handle the query, False otherwise
        """
        pass

    @abstractmethod
    async def search(
        self, query: ProductQuery, ctx: Optional[ProviderContext] = None
    ) -> ProviderResult:
        """
        Fetch products for the given query.

        Args:
            query: The product query to execute
            ctx: Optional context with timeout, tracing, etc.

        Returns:
            ProviderResult with products and metadata

        Raises:
            May raise provider-specific exceptions, but should handle errors gracefully
            and return empty results with warnings when possible
        """
        pass
