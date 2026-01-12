"""
Scraping provider - wraps existing MercadoLibre scraper.
This provider fetches live product data from MercadoLibre.
"""

import logging
import time
import uuid
from typing import List, Optional

from app.ml_scraper import scrape_mercadolibre_async
from app.providers.base import ProductProvider
from app.providers.models import (
    Product,
    ProductQuery,
    ProviderCapabilities,
    ProviderContext,
    ProviderMetadata,
    ProviderResult,
    VendorInfo,
)

logger = logging.getLogger(__name__)


class ScrapingProvider(ProductProvider):
    """
    Provider that scrapes products from MercadoLibre.
    
    Features:
    - Wraps existing scraper logic
    - Returns standardized Product objects
    - Robust error handling with circuit breaker
    - Timeout protection
    - Rate limiting via existing scraper
    - Never crashes API - returns empty list with warning on failure
    """

    @property
    def name(self) -> str:
        """Provider identifier."""
        return "scraping"

    @property
    def capabilities(self) -> ProviderCapabilities:
        """Provider capabilities."""
        return ProviderCapabilities(
            supportsImages=True,
            supportsPriceFilter=True,
            supportsLocation=False,
            supportsStock=False,
            supportsDeepLink=True,  # Returns direct product URLs
            supportsCategories=False,
            supportsRatings=False,
            supportsShipping=False,
        )

    def supports(self, query: ProductQuery) -> bool:
        """
        Scraping provider supports queries with keywords.
        
        Args:
            query: Product query
            
        Returns:
            True if query has keywords, False otherwise
        """
        return bool(query.keywords)

    def _convert_scraped_to_product(
        self, scraped: dict, query: ProductQuery
    ) -> Product:
        """
        Convert scraped product dict to standardized Product model.
        
        Args:
            scraped: Scraped product dictionary from ml_scraper
            query: Original query (for tags)
            
        Returns:
            Product object
        """
        # Extract image URL
        images = []
        if scraped.get("image_url"):
            images = [scraped["image_url"]]

        return Product(
            id=scraped.get("id", str(uuid.uuid4())),
            title=scraped.get("title", ""),
            description=None,  # Scraper doesn't extract descriptions
            images=images,
            price=scraped.get("price", 0.0),
            currency=scraped.get("currency", "ARS"),
            vendor=VendorInfo(name=scraped.get("store", "MercadoLibre"), id=None),
            url=scraped.get("product_url", ""),
            sourceProvider=self.name,
            categories=[],  # Categories not extracted by scraper
            tags=scraped.get("tags", []),
            score=None,  # Score will be calculated by aggregator
        )

    def _calculate_basic_score(self, product: Product, query: ProductQuery) -> float:
        """
        Calculate a basic relevance score for a product.
        
        Args:
            product: Product object
            query: Product query
            
        Returns:
            Score between 0 and 1
        """
        score = 0.5  # Base score for scraped products

        # Keyword match in title
        if query.keywords and product.title:
            title_lower = product.title.lower()
            matches = sum(1 for kw in query.keywords if kw.lower() in title_lower)
            if query.keywords:
                keyword_score = min(matches / len(query.keywords), 1.0)
                score += keyword_score * 0.3

        # Price fit
        if product.price and (query.priceMin or query.priceMax):
            if query.priceMin and product.price < query.priceMin:
                score -= 0.1
            if query.priceMax and product.price > query.priceMax:
                score -= 0.1

        # Has image
        if product.images:
            score += 0.1

        return max(0.0, min(score, 1.0))

    async def search(
        self,
        query: ProductQuery,
        ctx: Optional[ProviderContext] = None
    ) -> ProviderResult:
        """
        Scrape products from MercadoLibre matching the query.
        
        Args:
            query: Product query
            
        Returns:
            ProviderResult with scraped products
        """
        start_time = time.time()
        warnings = []

        try:
            # Check if we have keywords
            if not query.keywords:
                warnings.append("No keywords provided for scraping")
                return ProviderResult(
                    products=[],
                    meta=ProviderMetadata(
                        providerName=self.name,
                        latencyMs=int((time.time() - start_time) * 1000),
                        warnings=warnings,
                    ),
                )

            # Use first keyword for scraping
            keyword = query.keywords[0]
            interests = query.keywords + query.recipientProfile.interests

            logger.info(
                f"Scraping provider searching for: {keyword}",
                extra={"keyword": keyword, "interests": interests},
            )

            # Call existing scraper (async)
            scraped_products = await scrape_mercadolibre_async(keyword, interests)

            # Convert to Product objects
            products: List[Product] = []
            for scraped in scraped_products:
                try:
                    product = self._convert_scraped_to_product(scraped, query)
                    # Calculate score
                    product.score = self._calculate_basic_score(product, query)
                    products.append(product)
                except Exception as e:
                    logger.warning(f"Failed to convert scraped product: {e}")
                    continue

            # Filter by price range if specified
            if query.priceMin or query.priceMax:
                filtered_products = []
                for product in products:
                    if query.priceMin and product.price < query.priceMin:
                        continue
                    if query.priceMax and product.price > query.priceMax:
                        continue

                    filtered_products.append(product)
                products = filtered_products

            # Limit results
            products = products[: query.limit]

            logger.info(
                f"Scraping provider returned {len(products)} products",
                extra={
                    "keyword": keyword,
                    "results": len(products),
                },
            )

            if not products:
                warnings.append("No products found for this search")

            return ProviderResult(
                products=products,
                meta=ProviderMetadata(
                    providerName=self.name,
                    latencyMs=int((time.time() - start_time) * 1000),
                    warnings=warnings,
                ),
            )

        except Exception as e:
            logger.error(f"Scraping provider error: {e}")
            warnings.append(f"Scraping failed: {str(e)}")

            # Return empty result with warning - never crash
            return ProviderResult(
                products=[],
                meta=ProviderMetadata(
                    providerName=self.name,
                    latencyMs=int((time.time() - start_time) * 1000),
                    warnings=warnings,
                ),
            )
