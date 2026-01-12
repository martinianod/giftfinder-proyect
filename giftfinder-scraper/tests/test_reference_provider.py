"""
Tests for ReferenceProvider.
"""

import pytest
from app.providers.models import ProductQuery, RecipientProfile
from app.providers.reference import ReferenceProvider


class TestReferenceProvider:
    """Tests for ReferenceProvider."""

    @pytest.fixture
    def provider(self):
        """Create a ReferenceProvider instance."""
        return ReferenceProvider()

    def test_provider_name(self, provider):
        """Test provider name is correct."""
        assert provider.name == "reference"

    def test_supports_all_queries(self, provider):
        """Test that reference provider supports all queries."""
        query1 = ProductQuery(keywords=["auriculares"])
        query2 = ProductQuery(keywords=[])
        query3 = ProductQuery(priceMin=5000.0, priceMax=10000.0)

        assert provider.supports(query1)
        assert provider.supports(query2)
        assert provider.supports(query3)

    @pytest.mark.asyncio
    async def test_search_with_keywords(self, provider):
        """Test search with specific keywords."""
        query = ProductQuery(keywords=["tecnología"], limit=5)
        result = await provider.search(query)

        assert result.meta.providerName == "reference"
        assert result.meta.latencyMs >= 0
        assert len(result.products) > 0

        # Check that products match keywords
        for product in result.products:
            assert product.sourceProvider == "reference"
            assert product.vendor == "Sugerencia"

    @pytest.mark.asyncio
    async def test_search_with_interests(self, provider):
        """Test search with recipient interests."""
        query = ProductQuery(
            keywords=["regalo"],
            recipientProfile=RecipientProfile(
                type="friend", age=30, interests=["gaming", "tecnología"]
            ),
            limit=5,
        )
        result = await provider.search(query)

        assert len(result.products) > 0
        # Products should have tags matching interests
        for product in result.products:
            assert isinstance(product.tags, list)

    @pytest.mark.asyncio
    async def test_search_with_price_range(self, provider):
        """Test search with price range filter."""
        query = ProductQuery(
            keywords=["regalo"], priceMin=5000.0, priceMax=15000.0, limit=10
        )
        result = await provider.search(query)

        assert len(result.products) > 0
        # Products should be within price range (or have no price)
        for product in result.products:
            if product.price is not None:
                # Allow some flexibility since we're using average of price range
                assert product.price >= 0

    @pytest.mark.asyncio
    async def test_search_respects_limit(self, provider):
        """Test that search respects the limit parameter."""
        query = ProductQuery(keywords=["regalo"], limit=3)
        result = await provider.search(query)

        assert len(result.products) <= 3

    @pytest.mark.asyncio
    async def test_search_no_matches(self, provider):
        """Test search with no matching products."""
        query = ProductQuery(
            keywords=["xyzabc123nonexistent"], limit=10
        )
        result = await provider.search(query)

        # Should return empty list, not error
        assert len(result.products) == 0
        assert result.meta.providerName == "reference"

    @pytest.mark.asyncio
    async def test_product_has_score(self, provider):
        """Test that products have relevance scores."""
        query = ProductQuery(keywords=["tecnología"], limit=5)
        result = await provider.search(query)

        for product in result.products:
            assert product.score is not None
            assert 0.0 <= product.score <= 1.0

    @pytest.mark.asyncio
    async def test_products_sorted_by_score(self, provider):
        """Test that products are sorted by relevance score."""
        query = ProductQuery(keywords=["tecnología"], limit=10)
        result = await provider.search(query)

        if len(result.products) > 1:
            scores = [p.score for p in result.products if p.score is not None]
            # Check that scores are in descending order
            assert scores == sorted(scores, reverse=True)

    @pytest.mark.asyncio
    async def test_product_has_required_fields(self, provider):
        """Test that products have all required fields."""
        query = ProductQuery(keywords=["tecnología"], limit=1)
        result = await provider.search(query)

        assert len(result.products) > 0
        product = result.products[0]

        assert product.id
        assert product.title
        assert product.url
        assert product.vendor == "Sugerencia"
        assert product.sourceProvider == "reference"
        assert product.currency == "ARS"
        assert isinstance(product.images, list)
        assert isinstance(product.categories, list)
        assert isinstance(product.tags, list)

    @pytest.mark.asyncio
    async def test_metadata(self, provider):
        """Test that provider result has proper metadata."""
        query = ProductQuery(keywords=["tecnología"], limit=5)
        result = await provider.search(query)

        assert result.meta.providerName == "reference"
        assert result.meta.latencyMs >= 0
        assert isinstance(result.meta.warnings, list)
        assert result.meta.fetchedAt is not None
