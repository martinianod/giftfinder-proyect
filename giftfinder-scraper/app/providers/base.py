"""
Base abstract class for product providers.
"""

from abc import ABC, abstractmethod
from typing import Optional

from app.providers.models import ProductQuery, ProviderResult


class ProductProvider(ABC):
    """
    Abstract base class for all product providers.
    
    Each provider is responsible for:
    1. Checking if it can handle a query (supports)
    2. Fetching products for a query (search)
    3. Returning standardized Product objects
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
    async def search(self, query: ProductQuery) -> ProviderResult:
        """
        Fetch products for the given query.
        
        Args:
            query: The product query to execute
            
        Returns:
            ProviderResult with products and metadata
            
        Raises:
            May raise provider-specific exceptions, but should handle errors gracefully
            and return empty results with warnings when possible
        """
        pass
