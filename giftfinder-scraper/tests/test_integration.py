"""
Integration tests for provider system with ReferenceProvider only.
"""

import pytest
from unittest.mock import MagicMock, patch
from app.providers.aggregator import ProductAggregator
from app.providers.models import ProductQuery, RecipientProfile
from app.providers.reference import ReferenceProvider


class TestProviderIntegration:
    """Integration tests with ReferenceProvider only (no scraping)."""

    @pytest.mark.asyncio
    async def test_reference_provider_standalone(self):
        """Test that reference provider works standalone without registry."""
        # Create a reference provider directly
        provider = ReferenceProvider()
        
        query = ProductQuery(
            keywords=["tecnología"],
            recipientProfile=RecipientProfile(
                type="friend", age=30, interests=["gaming"]
            ),
            limit=5,
        )
        
        # Search for products
        result = await provider.search(query)
        
        # Should return products from reference provider
        assert len(result.products) > 0
        assert all(p.sourceProvider == "reference" for p in result.products)
        assert all(p.vendor == "Sugerencia" for p in result.products)
        assert result.meta.providerName == "reference"

    @pytest.mark.asyncio
    async def test_aggregator_with_mock_providers(self):
        """Test aggregator works with mocked providers."""
        # Create mock providers
        mock_ref_provider = MagicMock()
        mock_ref_provider.name = "reference"
        mock_ref_provider.supports = MagicMock(return_value=True)
        
        from app.providers.models import Product, ProviderMetadata, ProviderResult
        
        mock_products = [
            Product(
                id="ref-1",
                title="Test Product",
                vendor="Sugerencia",
                url="https://example.com/test",
                sourceProvider="reference",
                currency="ARS",
                score=0.9,
            )
        ]
        
        async def mock_search(query):
            return ProviderResult(
                products=mock_products,
                meta=ProviderMetadata(
                    providerName="reference",
                    latencyMs=10,
                    warnings=[],
                ),
            )
        
        mock_ref_provider.search = mock_search
        
        # Mock the registry
        mock_registry = MagicMock()
        mock_registry.get_all_providers.return_value = [mock_ref_provider]
        
        mock_settings_obj = MagicMock()
        mock_settings_obj.max_concurrent_providers = 3
        mock_settings_obj.provider_timeout_seconds = 15
        
        with patch("app.providers.aggregator.get_registry", return_value=mock_registry):
            with patch("app.providers.aggregator.get_settings", return_value=mock_settings_obj):
                aggregator = ProductAggregator()
                
                query = ProductQuery(keywords=["test"], limit=5)
                products = await aggregator.search_products(query)
                
                assert len(products) == 1
                assert products[0].sourceProvider == "reference"

    @pytest.mark.asyncio
    async def test_reference_provider_with_price_filter(self):
        """Test reference provider with price filter."""
        provider = ReferenceProvider()
        
        # Search with price range
        query = ProductQuery(
            keywords=["regalo"],
            priceMin=5000.0,
            priceMax=15000.0,
            limit=10,
        )
        
        result = await provider.search(query)
        
        # Should return products
        assert len(result.products) > 0
        # All from reference provider
        assert all(p.sourceProvider == "reference" for p in result.products)

    @pytest.mark.asyncio
    async def test_reference_provider_returns_scored_products(self):
        """Test that reference provider returns products with scores."""
        provider = ReferenceProvider()
        
        query = ProductQuery(
            keywords=["tecnología", "gaming"], limit=5
        )
        
        result = await provider.search(query)
        
        # All products should have scores
        assert len(result.products) > 0
        for product in result.products:
            assert product.score is not None
            assert 0.0 <= product.score <= 1.0
        
        # Products should be sorted by score
        scores = [p.score for p in result.products]
        assert scores == sorted(scores, reverse=True)

    @pytest.mark.asyncio
    async def test_empty_query_returns_results(self):
        """Test that empty query still returns results from reference provider."""
        provider = ReferenceProvider()
        
        # Empty query
        query = ProductQuery(keywords=[], limit=5)
        
        result = await provider.search(query)
        
        # Reference provider should still return results (supports all queries)
        assert len(result.products) > 0

    @pytest.mark.asyncio
    async def test_no_matching_keywords_returns_empty(self):
        """Test that completely unmatched keywords return empty results."""
        provider = ReferenceProvider()
        
        # Completely unmatched keywords
        query = ProductQuery(
            keywords=["xyzabc123nonexistent999"], limit=10
        )
        
        result = await provider.search(query)
        
        # Should return empty list
        assert len(result.products) == 0
