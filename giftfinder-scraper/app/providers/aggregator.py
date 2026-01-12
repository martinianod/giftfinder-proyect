"""
Product aggregator - orchestrates multiple providers and merges results.
"""

import asyncio
import logging
from typing import List, Set, Tuple

from app.config import get_settings
from app.providers.base import ProductProvider
from app.providers.models import Product, ProductQuery, ProviderResult
from app.providers.registry import get_registry

logger = logging.getLogger(__name__)
settings = get_settings()


class ProductAggregator:
    """
    Aggregates products from multiple providers.

    Responsibilities:
    1. Call all enabled providers in parallel
    2. Merge results from multiple providers
    3. Deduplicate products
    4. Score and rank by relevance
    5. Limit to query.limit results
    """

    def __init__(self):
        """Initialize the product aggregator."""
        self.registry = get_registry()
        self.max_concurrent_providers = getattr(settings, "max_concurrent_providers", 3)

    def _normalize_for_dedup(self, product: Product) -> str:
        """
        Create a normalized string for deduplication.

        Uses URL as primary key, falls back to title+vendor+price.

        Args:
            product: Product to normalize

        Returns:
            Normalized string for deduplication
        """
        # Primary: Use URL if available and not a search URL
        if product.url and not product.url.startswith(
            "https://listado.mercadolibre.com.ar/"
        ):
            return product.url.lower().strip()

        # Fallback: title + vendor + price
        title = product.title.lower().strip() if product.title else ""
        vendor = product.vendor.lower().strip() if product.vendor else ""
        price = str(product.price) if product.price else "0"

        return f"{title}|{vendor}|{price}"

    def _deduplicate(self, products: List[Product]) -> List[Product]:
        """
        Deduplicate products based on normalized keys.

        When duplicates are found, keeps the one with highest score.

        Args:
            products: List of products to deduplicate

        Returns:
            Deduplicated list of products
        """
        seen: Set[str] = set()
        deduped: List[Product] = []

        # Sort by score (highest first) so we keep best duplicates
        sorted_products = sorted(
            products, key=lambda p: p.score if p.score else 0.0, reverse=True
        )

        for product in sorted_products:
            key = self._normalize_for_dedup(product)
            if key not in seen:
                seen.add(key)
                deduped.append(product)

        logger.debug(
            f"Deduplication: {len(products)} -> {len(deduped)} products",
            extra={"before": len(products), "after": len(deduped)},
        )

        return deduped

    def _enhance_score(self, product: Product, query: ProductQuery) -> float:
        """
        Calculate or enhance relevance score for a product.

        Args:
            product: Product to score
            query: Original query

        Returns:
            Score between 0 and 1
        """
        # If product already has a score, use it
        if product.score is not None:
            base_score = product.score
        else:
            base_score = 0.5

        # Apply provider weight
        provider_weights = {
            "scraping": 1.0,  # Live data gets highest weight
            "reference": 0.7,  # Reference data gets lower weight
        }
        provider_weight = provider_weights.get(product.sourceProvider, 0.5)

        # Keyword match bonus
        keyword_bonus = 0.0
        if query.keywords and product.title:
            title_lower = product.title.lower()
            matches = sum(1 for kw in query.keywords if kw.lower() in title_lower)
            if query.keywords:
                keyword_bonus = (matches / len(query.keywords)) * 0.2

        # Price fit bonus
        price_bonus = 0.0
        if product.price and (query.priceMin or query.priceMax):
            if query.priceMin and query.priceMax:
                mid_price = (query.priceMin + query.priceMax) / 2
                if query.priceMin <= product.price <= query.priceMax:
                    # Price in range - bonus based on how close to middle
                    distance = abs(product.price - mid_price)
                    range_size = query.priceMax - query.priceMin
                    if range_size > 0:
                        price_bonus = 0.1 * (1 - min(distance / range_size, 1.0))
            elif query.priceMin and product.price >= query.priceMin:
                price_bonus = 0.05
            elif query.priceMax and product.price <= query.priceMax:
                price_bonus = 0.05

        # Calculate final score
        final_score = (
            base_score * 0.6 + provider_weight * 0.2 + keyword_bonus + price_bonus
        )

        return min(final_score, 1.0)

    def _merge_and_rank(
        self, provider_results: List[ProviderResult], query: ProductQuery
    ) -> List[Product]:
        """
        Merge, deduplicate, score, and rank products from multiple providers.

        Args:
            provider_results: List of results from providers
            query: Original query

        Returns:
            Ranked list of products
        """
        # Collect all products
        all_products: List[Product] = []
        for result in provider_results:
            all_products.extend(result.products)

        logger.info(
            f"Merging {len(all_products)} products from {len(provider_results)} providers"
        )

        # Deduplicate
        deduped_products = self._deduplicate(all_products)

        # Enhance scores
        for product in deduped_products:
            product.score = self._enhance_score(product, query)

        # Sort by score (highest first)
        ranked_products = sorted(
            deduped_products, key=lambda p: p.score if p.score else 0.0, reverse=True
        )

        # Limit to query.limit
        limited_products = ranked_products[: query.limit]

        logger.info(
            f"Final results: {len(limited_products)} products",
            extra={
                "total": len(all_products),
                "deduped": len(deduped_products),
                "final": len(limited_products),
            },
        )

        return limited_products

    async def _search_provider(
        self, provider: ProductProvider, query: ProductQuery
    ) -> ProviderResult:
        """
        Search a single provider with timeout protection.

        Args:
            provider: Provider to search
            query: Product query

        Returns:
            ProviderResult (may be empty on error)
        """
        try:
            # Check if provider supports this query
            if not provider.supports(query):
                logger.debug(f"Provider {provider.name} does not support this query")
                return ProviderResult(
                    products=[],
                    meta={
                        "providerName": provider.name,
                        "latencyMs": 0,
                        "warnings": ["Provider does not support this query"],
                    },
                )

            # Execute search with timeout
            provider_timeout = getattr(settings, "provider_timeout_seconds", 15)
            result = await asyncio.wait_for(
                provider.search(query), timeout=provider_timeout
            )

            logger.info(
                f"Provider {provider.name} returned {len(result.products)} products",
                extra={
                    "provider": provider.name,
                    "products": len(result.products),
                    "latency_ms": result.meta.latencyMs,
                },
            )

            return result

        except asyncio.TimeoutError:
            logger.error(f"Provider {provider.name} timed out")
            return ProviderResult(
                products=[],
                meta={
                    "providerName": provider.name,
                    "latencyMs": provider_timeout * 1000,
                    "warnings": [f"Provider timed out after {provider_timeout}s"],
                },
            )

        except Exception as e:
            logger.error(f"Provider {provider.name} failed: {e}")
            return ProviderResult(
                products=[],
                meta={
                    "providerName": provider.name,
                    "latencyMs": 0,
                    "warnings": [f"Provider error: {str(e)}"],
                },
            )

    async def search_products(self, query: ProductQuery) -> List[Product]:
        """
        Search for products across all enabled providers.

        Args:
            query: Product query

        Returns:
            Merged, deduplicated, and ranked list of products
        """
        logger.info(
            f"Aggregator searching with keywords: {query.keywords}",
            extra={
                "keywords": query.keywords,
                "interests": query.recipientProfile.interests,
                "limit": query.limit,
            },
        )

        # Get all providers
        providers = self.registry.get_all_providers()

        if not providers:
            logger.warning("No providers available!")
            return []

        # Call providers in parallel with concurrency limit
        semaphore = asyncio.Semaphore(self.max_concurrent_providers)

        async def search_with_semaphore(
            provider: ProductProvider,
        ) -> ProviderResult:
            async with semaphore:
                return await self._search_provider(provider, query)

        # Execute searches
        tasks = [search_with_semaphore(provider) for provider in providers]
        provider_results = await asyncio.gather(*tasks)

        # Merge and rank results
        final_products = self._merge_and_rank(provider_results, query)

        return final_products


# Global aggregator instance
_aggregator: ProductAggregator = None


def get_aggregator() -> ProductAggregator:
    """
    Get the global product aggregator instance (singleton).

    Returns:
        ProductAggregator instance
    """
    global _aggregator
    if _aggregator is None:
        _aggregator = ProductAggregator()
    return _aggregator
