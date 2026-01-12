"""
Tests for ProductAggregator.
"""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from app.providers.aggregator import ProductAggregator
from app.providers.models import (
    Product,
    ProductQuery,
    ProviderMetadata,
    ProviderResult,
)


class TestProductAggregator:
    """Tests for ProductAggregator."""

    @pytest.fixture
    def aggregator(self):
        """Create a ProductAggregator instance with mocked registry."""
        with patch("app.providers.aggregator.get_registry") as mock_registry:
            # Create mock providers
            provider1 = MagicMock()
            provider1.name = "test1"
            provider2 = MagicMock()
            provider2.name = "test2"

            mock_registry.return_value.get_all_providers.return_value = [
                provider1,
                provider2,
            ]

            aggregator = ProductAggregator()
            aggregator.test_provider1 = provider1
            aggregator.test_provider2 = provider2
            return aggregator

    def test_normalize_for_dedup_with_url(self, aggregator):
        """Test product normalization using URL."""
        product = Product(
            id="test-1",
            title="Test Product",
            vendor="Test Vendor",
            url="https://www.mercadolibre.com.ar/producto/MLA123",
            sourceProvider="test",
            currency="ARS",
        )
        normalized = aggregator._normalize_for_dedup(product)
        assert normalized == "https://www.mercadolibre.com.ar/producto/mla123"

    def test_normalize_for_dedup_with_search_url(self, aggregator):
        """Test product normalization with search URL (falls back to title+vendor+price)."""
        product = Product(
            id="test-1",
            title="Test Product",
            vendor="Test Vendor",
            url="https://listado.mercadolibre.com.ar/search",
            price=10000.0,
            sourceProvider="test",
            currency="ARS",
        )
        normalized = aggregator._normalize_for_dedup(product)
        assert "test product" in normalized
        assert "test vendor" in normalized
        assert "10000" in normalized

    def test_deduplicate_removes_duplicates(self, aggregator):
        """Test that deduplication removes duplicate products."""
        products = [
            Product(
                id="1",
                title="Product A",
                vendor="Vendor",
                url="https://example.com/a",
                sourceProvider="test",
                currency="ARS",
                score=0.8,
            ),
            Product(
                id="2",
                title="Product A",
                vendor="Vendor",
                url="https://example.com/a",
                sourceProvider="test",
                currency="ARS",
                score=0.7,
            ),
            Product(
                id="3",
                title="Product B",
                vendor="Vendor",
                url="https://example.com/b",
                sourceProvider="test",
                currency="ARS",
                score=0.9,
            ),
        ]

        deduped = aggregator._deduplicate(products)

        # Should have 2 unique products
        assert len(deduped) == 2
        # Product with highest overall score should be first
        assert deduped[0].score == 0.9  # Product 3 has highest score

    def test_deduplicate_keeps_highest_score(self, aggregator):
        """Test that deduplication keeps product with highest score."""
        products = [
            Product(
                id="1",
                title="Product",
                vendor="Vendor",
                url="https://example.com/product",
                sourceProvider="test",
                currency="ARS",
                score=0.5,
            ),
            Product(
                id="2",
                title="Product",
                vendor="Vendor",
                url="https://example.com/product",
                sourceProvider="test",
                currency="ARS",
                score=0.9,
            ),
        ]

        deduped = aggregator._deduplicate(products)

        assert len(deduped) == 1
        assert deduped[0].id == "2"  # Higher score

    def test_enhance_score_applies_provider_weight(self, aggregator):
        """Test that score enhancement applies provider weights."""
        product_scraping = Product(
            id="1",
            title="Product",
            vendor="Vendor",
            url="https://example.com/1",
            sourceProvider="scraping",
            currency="ARS",
            score=0.5,
        )
        product_reference = Product(
            id="2",
            title="Product",
            vendor="Vendor",
            url="https://example.com/2",
            sourceProvider="reference",
            currency="ARS",
            score=0.5,
        )

        query = ProductQuery(keywords=["test"])

        score_scraping = aggregator._enhance_score(product_scraping, query)
        score_reference = aggregator._enhance_score(product_reference, query)

        # Scraping should have higher weight
        assert score_scraping > score_reference

    def test_enhance_score_keyword_match(self, aggregator):
        """Test that score enhancement considers keyword matches."""
        product_match = Product(
            id="1",
            title="Auriculares Bluetooth",
            vendor="Vendor",
            url="https://example.com/1",
            sourceProvider="test",
            currency="ARS",
            score=0.5,
        )
        product_no_match = Product(
            id="2",
            title="Producto sin relación",
            vendor="Vendor",
            url="https://example.com/2",
            sourceProvider="test",
            currency="ARS",
            score=0.5,
        )

        query = ProductQuery(keywords=["auriculares", "bluetooth"])

        score_match = aggregator._enhance_score(product_match, query)
        score_no_match = aggregator._enhance_score(product_no_match, query)

        # Product with keyword match should have higher score
        assert score_match > score_no_match

    def test_enhance_score_price_fit(self, aggregator):
        """Test that score enhancement considers price fit."""
        product_in_range = Product(
            id="1",
            title="Product",
            vendor="Vendor",
            url="https://example.com/1",
            sourceProvider="test",
            currency="ARS",
            price=10000.0,
            score=0.5,
        )
        product_out_range = Product(
            id="2",
            title="Product",
            vendor="Vendor",
            url="https://example.com/2",
            sourceProvider="test",
            currency="ARS",
            price=50000.0,
            score=0.5,
        )

        query = ProductQuery(keywords=["test"], priceMin=8000.0, priceMax=12000.0)

        score_in_range = aggregator._enhance_score(product_in_range, query)
        score_out_range = aggregator._enhance_score(product_out_range, query)

        # Product in price range should have higher score
        assert score_in_range > score_out_range

    @pytest.mark.asyncio
    async def test_search_products_calls_providers(self, aggregator):
        """Test that search_products calls all providers."""
        query = ProductQuery(keywords=["test"], limit=10)

        # Mock provider search results
        result1 = ProviderResult(
            products=[
                Product(
                    id="1",
                    title="Product 1",
                    vendor="Vendor",
                    url="https://example.com/1",
                    sourceProvider="test1",
                    currency="ARS",
                )
            ],
            meta=ProviderMetadata(providerName="test1", latencyMs=100, warnings=[]),
        )
        result2 = ProviderResult(
            products=[
                Product(
                    id="2",
                    title="Product 2",
                    vendor="Vendor",
                    url="https://example.com/2",
                    sourceProvider="test2",
                    currency="ARS",
                )
            ],
            meta=ProviderMetadata(providerName="test2", latencyMs=150, warnings=[]),
        )

        aggregator.test_provider1.supports.return_value = True
        aggregator.test_provider1.search = AsyncMock(return_value=result1)
        aggregator.test_provider2.supports.return_value = True
        aggregator.test_provider2.search = AsyncMock(return_value=result2)

        products = await aggregator.search_products(query)

        # Should call both providers
        aggregator.test_provider1.search.assert_called_once()
        aggregator.test_provider2.search.assert_called_once()

        # Should return merged results
        assert len(products) == 2

    @pytest.mark.asyncio
    async def test_search_products_handles_provider_failure(self, aggregator):
        """Test that search_products handles provider failures gracefully."""
        query = ProductQuery(keywords=["test"], limit=10)

        # One provider succeeds, one fails
        result1 = ProviderResult(
            products=[
                Product(
                    id="1",
                    title="Product 1",
                    vendor="Vendor",
                    url="https://example.com/1",
                    sourceProvider="test1",
                    currency="ARS",
                )
            ],
            meta=ProviderMetadata(providerName="test1", latencyMs=100, warnings=[]),
        )

        aggregator.test_provider1.supports.return_value = True
        aggregator.test_provider1.search = AsyncMock(return_value=result1)
        aggregator.test_provider2.supports.return_value = True
        aggregator.test_provider2.search = AsyncMock(
            side_effect=Exception("Provider failed")
        )

        products = await aggregator.search_products(query)

        # Should still return results from successful provider
        assert len(products) > 0

    @pytest.mark.asyncio
    async def test_search_products_respects_limit(self, aggregator):
        """Test that search_products respects the query limit."""
        query = ProductQuery(keywords=["test"], limit=3)

        # Return more products than limit
        products_list = [
            Product(
                id=f"{i}",
                title=f"Product {i}",
                vendor="Vendor",
                url=f"https://example.com/{i}",
                sourceProvider="test1",
                currency="ARS",
                score=0.5,
            )
            for i in range(10)
        ]

        result = ProviderResult(
            products=products_list,
            meta=ProviderMetadata(providerName="test1", latencyMs=100, warnings=[]),
        )

        aggregator.test_provider1.supports.return_value = True
        aggregator.test_provider1.search = AsyncMock(return_value=result)
        aggregator.test_provider2.supports.return_value = False

        products = await aggregator.search_products(query)

        # Should limit to 3 products
        assert len(products) <= 3

    @pytest.mark.asyncio
    async def test_search_products_merges_and_deduplicates(self, aggregator):
        """Test that search_products merges and deduplicates results."""
        query = ProductQuery(keywords=["test"], limit=10)

        # Both providers return same product (duplicate)
        duplicate_product = Product(
            id="1",
            title="Product",
            vendor="Vendor",
            url="https://example.com/product",
            sourceProvider="test1",
            currency="ARS",
            score=0.8,
        )

        result1 = ProviderResult(
            products=[duplicate_product],
            meta=ProviderMetadata(providerName="test1", latencyMs=100, warnings=[]),
        )
        result2 = ProviderResult(
            products=[duplicate_product],
            meta=ProviderMetadata(providerName="test2", latencyMs=150, warnings=[]),
        )

        aggregator.test_provider1.supports.return_value = True
        aggregator.test_provider1.search = AsyncMock(return_value=result1)
        aggregator.test_provider2.supports.return_value = True
        aggregator.test_provider2.search = AsyncMock(return_value=result2)

        products = await aggregator.search_products(query)

        # Should deduplicate to 1 product
        assert len(products) == 1
