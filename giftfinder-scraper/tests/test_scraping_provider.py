"""
Tests for ScrapingProvider.
"""

import pytest
from unittest.mock import AsyncMock, patch
from app.providers.models import ProductQuery, RecipientProfile
from app.providers.scraping import ScrapingProvider


class TestScrapingProvider:
    """Tests for ScrapingProvider."""

    @pytest.fixture
    def provider(self):
        """Create a ScrapingProvider instance."""
        return ScrapingProvider()

    def test_provider_name(self, provider):
        """Test provider name is correct."""
        assert provider.name == "scraping"

    def test_supports_queries_with_keywords(self, provider):
        """Test that scraping provider supports queries with keywords."""
        query_with_keywords = ProductQuery(keywords=["auriculares"])
        query_without_keywords = ProductQuery(keywords=[])

        assert provider.supports(query_with_keywords)
        assert not provider.supports(query_without_keywords)

    @pytest.mark.asyncio
    async def test_search_with_keywords(self, provider):
        """Test search with keywords using mock scraper."""
        mock_scraped_data = [
            {
                "id": "ml-123",
                "title": "Auriculares Bluetooth",
                "price": 10000.0,
                "currency": "ARS",
                "image_url": "https://example.com/image.jpg",
                "product_url": "https://www.mercadolibre.com.ar/producto/ml-123",
                "store": "MercadoLibre",
                "tags": ["tecnología"],
            }
        ]

        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=mock_scraped_data),
        ):
            query = ProductQuery(keywords=["auriculares"], limit=10)
            result = await provider.search(query)

            assert result.meta.providerName == "scraping"
            assert len(result.products) == 1
            assert result.products[0].title == "Auriculares Bluetooth"
            assert result.products[0].sourceProvider == "scraping"

    @pytest.mark.asyncio
    async def test_search_without_keywords(self, provider):
        """Test search without keywords returns empty result."""
        query = ProductQuery(keywords=[], limit=10)
        result = await provider.search(query)

        assert len(result.products) == 0
        assert len(result.meta.warnings) > 0
        assert "No keywords" in result.meta.warnings[0]

    @pytest.mark.asyncio
    async def test_search_filters_by_price_range(self, provider):
        """Test that search filters products by price range."""
        mock_scraped_data = [
            {
                "id": "ml-1",
                "title": "Product 1",
                "price": 5000.0,
                "currency": "ARS",
                "image_url": "https://example.com/1.jpg",
                "product_url": "https://www.mercadolibre.com.ar/producto/ml-1",
                "store": "MercadoLibre",
                "tags": [],
            },
            {
                "id": "ml-2",
                "title": "Product 2",
                "price": 15000.0,
                "currency": "ARS",
                "image_url": "https://example.com/2.jpg",
                "product_url": "https://www.mercadolibre.com.ar/producto/ml-2",
                "store": "MercadoLibre",
                "tags": [],
            },
            {
                "id": "ml-3",
                "title": "Product 3",
                "price": 25000.0,
                "currency": "ARS",
                "image_url": "https://example.com/3.jpg",
                "product_url": "https://www.mercadolibre.com.ar/producto/ml-3",
                "store": "MercadoLibre",
                "tags": [],
            },
        ]

        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=mock_scraped_data),
        ):
            query = ProductQuery(
                keywords=["producto"], priceMin=10000.0, priceMax=20000.0, limit=10
            )
            result = await provider.search(query)

            # Should only return Product 2 (15000.0)
            assert len(result.products) == 1
            assert result.products[0].price == 15000.0

    @pytest.mark.asyncio
    async def test_search_respects_limit(self, provider):
        """Test that search respects the limit parameter."""
        mock_scraped_data = [
            {
                "id": f"ml-{i}",
                "title": f"Product {i}",
                "price": 10000.0,
                "currency": "ARS",
                "image_url": f"https://example.com/{i}.jpg",
                "product_url": f"https://www.mercadolibre.com.ar/producto/ml-{i}",
                "store": "MercadoLibre",
                "tags": [],
            }
            for i in range(20)
        ]

        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=mock_scraped_data),
        ):
            query = ProductQuery(keywords=["producto"], limit=5)
            result = await provider.search(query)

            assert len(result.products) <= 5

    @pytest.mark.asyncio
    async def test_search_handles_scraper_error(self, provider):
        """Test that search handles scraper errors gracefully."""
        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(side_effect=Exception("Scraper failed")),
        ):
            query = ProductQuery(keywords=["auriculares"], limit=10)
            result = await provider.search(query)

            # Should return empty result with warning, not crash
            assert len(result.products) == 0
            assert len(result.meta.warnings) > 0
            assert "Scraping failed" in result.meta.warnings[0]

    @pytest.mark.asyncio
    async def test_search_handles_empty_result(self, provider):
        """Test that search handles empty scraper result."""
        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=[]),
        ):
            query = ProductQuery(keywords=["xyznonexistent"], limit=10)
            result = await provider.search(query)

            assert len(result.products) == 0
            assert len(result.meta.warnings) > 0
            assert "No products found" in result.meta.warnings[0]

    @pytest.mark.asyncio
    async def test_product_has_score(self, provider):
        """Test that products have relevance scores."""
        mock_scraped_data = [
            {
                "id": "ml-123",
                "title": "Auriculares Bluetooth",
                "price": 10000.0,
                "currency": "ARS",
                "image_url": "https://example.com/image.jpg",
                "product_url": "https://www.mercadolibre.com.ar/producto/ml-123",
                "store": "MercadoLibre",
                "tags": ["tecnología"],
            }
        ]

        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=mock_scraped_data),
        ):
            query = ProductQuery(keywords=["auriculares"], limit=10)
            result = await provider.search(query)

            assert len(result.products) > 0
            for product in result.products:
                assert product.score is not None
                assert 0.0 <= product.score <= 1.0

    @pytest.mark.asyncio
    async def test_metadata(self, provider):
        """Test that provider result has proper metadata."""
        mock_scraped_data = []

        with patch(
            "app.providers.scraping.scrape_mercadolibre_async",
            new=AsyncMock(return_value=mock_scraped_data),
        ):
            query = ProductQuery(keywords=["test"], limit=10)
            result = await provider.search(query)

            assert result.meta.providerName == "scraping"
            assert result.meta.latencyMs >= 0
            assert isinstance(result.meta.warnings, list)
            assert result.meta.fetchedAt is not None
