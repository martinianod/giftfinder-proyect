"""
Reference provider - returns curated product ideas from local dataset.
This provider serves as a fallback when scraping fails or for testing.
"""

import json
import logging
import random
import time
import uuid
from pathlib import Path
from typing import Dict, List, Optional

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


class ReferenceProvider(ProductProvider):
    """
    Provider that returns reference/idea products from a curated local dataset.
    
    Features:
    - Always available (no external dependencies)
    - Fast response time
    - Category and keyword filtering
    - Returns placeholder URLs (search links)
    - Clearly labeled as 'reference' source
    """

    def __init__(self):
        """Initialize the reference provider and load data."""
        self._data: List[Dict] = []
        self._load_data()

    @property
    def name(self) -> str:
        """Provider identifier."""
        return "reference"

    @property
    def capabilities(self) -> ProviderCapabilities:
        """Provider capabilities."""
        return ProviderCapabilities(
            supportsImages=True,
            supportsPriceFilter=True,
            supportsLocation=False,
            supportsStock=False,
            supportsDeepLink=False,  # Returns search URLs, not direct links
            supportsCategories=True,
            supportsRatings=False,
            supportsShipping=False,
        )

    def _load_data(self) -> None:
        """Load reference data from JSON file."""
        try:
            data_path = Path(__file__).parent / "reference_data.json"
            with open(data_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                self._data = data.get("products", [])
            logger.info(f"Loaded {len(self._data)} reference products")
        except Exception as e:
            logger.error(f"Failed to load reference data: {e}")
            self._data = []

    def supports(self, query: ProductQuery) -> bool:
        """
        Reference provider supports all queries.
        It's a fallback provider that always works.
        
        Args:
            query: Product query
            
        Returns:
            Always True
        """
        return True

    def _matches_keywords(self, product: Dict, keywords: List[str]) -> bool:
        """
        Check if product matches any of the keywords.
        
        Args:
            product: Product data dictionary
            keywords: List of keywords to match
            
        Returns:
            True if product matches any keyword
        """
        if not keywords:
            return True

        # Normalize keywords to lowercase
        keywords_lower = [k.lower() for k in keywords]

        # Check product keywords
        product_keywords = [k.lower() for k in product.get("keywords", [])]
        if any(kw in product_keywords for kw in keywords_lower):
            return True

        # Check product interests
        product_interests = [i.lower() for i in product.get("interests", [])]
        if any(kw in product_interests for kw in keywords_lower):
            return True

        # Check title
        title = product.get("title", "").lower()
        if any(kw in title for kw in keywords_lower):
            return True

        # Check description
        description = product.get("description", "").lower()
        if any(kw in description for kw in keywords_lower):
            return True

        return False

    def _matches_price_range(
        self, product: Dict, price_min: Optional[float], price_max: Optional[float]
    ) -> bool:
        """
        Check if product's price range overlaps with query price range.
        
        Args:
            product: Product data dictionary
            price_min: Minimum price from query
            price_max: Maximum price from query
            
        Returns:
            True if product price range overlaps with query range
        """
        product_price_range = product.get("priceRange", [])
        if not product_price_range or len(product_price_range) < 2:
            # If no price range, assume it matches
            return True

        product_min, product_max = product_price_range

        # Check for overlap
        if price_min is not None and product_max < price_min:
            return False
        if price_max is not None and product_min > price_max:
            return False

        return True

    def _calculate_score(
        self, product: Dict, query: ProductQuery
    ) -> float:
        """
        Calculate relevance score for a product.
        
        Args:
            product: Product data dictionary
            query: Product query
            
        Returns:
            Score between 0 and 1
        """
        score = 0.0
        
        # Keyword match score (0.5 weight)
        if query.keywords:
            keywords_lower = [k.lower() for k in query.keywords]
            product_keywords = [k.lower() for k in product.get("keywords", [])]
            product_interests = [i.lower() for i in product.get("interests", [])]
            
            matches = 0
            for kw in keywords_lower:
                if kw in product_keywords or kw in product_interests:
                    matches += 1
            
            if keywords_lower:
                keyword_score = min(matches / len(keywords_lower), 1.0)
                score += keyword_score * 0.5
        else:
            score += 0.5  # No keywords, full score for this component

        # Interest match score (0.3 weight)
        if query.recipientProfile.interests:
            interests_lower = [i.lower() for i in query.recipientProfile.interests]
            product_interests = [i.lower() for i in product.get("interests", [])]
            
            matches = 0
            for interest in interests_lower:
                if interest in product_interests:
                    matches += 1
            
            if interests_lower:
                interest_score = min(matches / len(interests_lower), 1.0)
                score += interest_score * 0.3
        else:
            score += 0.3

        # Price fit score (0.2 weight)
        if query.priceMin or query.priceMax:
            product_price_range = product.get("priceRange", [])
            if product_price_range and len(product_price_range) >= 2:
                product_avg = (product_price_range[0] + product_price_range[1]) / 2
                
                if query.priceMin and query.priceMax:
                    query_avg = (query.priceMin + query.priceMax) / 2
                    # Calculate how close product price is to query price
                    diff = abs(product_avg - query_avg)
                    max_diff = max(query_avg, product_avg)
                    if max_diff > 0:
                        price_score = 1.0 - min(diff / max_diff, 1.0)
                        score += price_score * 0.2
                    else:
                        score += 0.2
                else:
                    score += 0.2  # Price range exists but can't compare
            else:
                score += 0.2  # No price range, assume good fit
        else:
            score += 0.2

        return min(score, 1.0)

    def _create_product(
        self, ref_data: Dict, query: ProductQuery, score: float
    ) -> Product:
        """
        Create a Product object from reference data.
        
        Args:
            ref_data: Reference product data
            query: Product query (for tags)
            score: Relevance score
            
        Returns:
            Product object
        """
        # Generate a search URL for MercadoLibre
        search_term = ref_data.get("title", "").replace(" ", "-").lower()
        url = f"https://listado.mercadolibre.com.ar/{search_term}"

        # Calculate average price from price range
        price_range = ref_data.get("priceRange", [])
        price = None
        if price_range and len(price_range) >= 2:
            price = (price_range[0] + price_range[1]) / 2

        # Get the first image keyword for a placeholder
        keywords = ref_data.get("keywords", [])
        image_keyword = keywords[0] if keywords else "product"

        return Product(
            id=ref_data.get("id", str(uuid.uuid4())),
            title=ref_data.get("title", ""),
            description=ref_data.get("description"),
            images=[
                f"https://http2.mlstatic.com/D_NQ_NP_2X_{image_keyword}_placeholder.jpg"
            ],
            price=price if price is not None else 0.0,
            currency="ARS",
            vendor=VendorInfo(name="Sugerencia", id=None),
            url=url,
            sourceProvider=self.name,
            categories=[ref_data.get("category", "general")],
            tags=query.keywords + query.recipientProfile.interests,
            score=score,
        )

    async def search(
        self,
        query: ProductQuery,
        ctx: Optional[ProviderContext] = None
    ) -> ProviderResult:
        """
        Search reference products matching the query.
        
        Args:
            query: Product query
            
        Returns:
            ProviderResult with matching products
        """
        start_time = time.time()
        warnings = []

        try:
            # If no data loaded, return empty result
            if not self._data:
                warnings.append("Reference data not available")
                return ProviderResult(
                    products=[],
                    meta=ProviderMetadata(
                        providerName=self.name,
                        latencyMs=int((time.time() - start_time) * 1000),
                        warnings=warnings,
                    ),
                )

            # Filter products by keywords and price range
            matched_products = []
            for product_data in self._data:
                if not self._matches_keywords(product_data, query.keywords):
                    continue
                if not self._matches_price_range(
                    product_data, query.priceMin, query.priceMax
                ):
                    continue

                # Calculate score
                score = self._calculate_score(product_data, query)
                matched_products.append((product_data, score))

            # Sort by score (highest first)
            matched_products.sort(key=lambda x: x[1], reverse=True)

            # Limit results
            matched_products = matched_products[: query.limit]

            # Create Product objects
            products = [
                self._create_product(data, query, score)
                for data, score in matched_products
            ]

            logger.info(
                f"Reference provider returned {len(products)} products",
                extra={
                    "keywords": query.keywords,
                    "interests": query.recipientProfile.interests,
                    "results": len(products),
                },
            )

            return ProviderResult(
                products=products,
                meta=ProviderMetadata(
                    providerName=self.name,
                    latencyMs=int((time.time() - start_time) * 1000),
                    warnings=warnings,
                ),
            )

        except Exception as e:
            logger.error(f"Reference provider error: {e}")
            warnings.append(f"Error: {str(e)}")

            return ProviderResult(
                products=[],
                meta=ProviderMetadata(
                    providerName=self.name,
                    latencyMs=int((time.time() - start_time) * 1000),
                    warnings=warnings,
                ),
            )
