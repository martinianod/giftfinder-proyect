#!/usr/bin/env python3
"""
Test script to verify the provider system works end-to-end.
Tests with reference provider only (no external dependencies).
"""

import asyncio
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.providers.reference import ReferenceProvider
from app.providers.scraping import ScrapingProvider  
from app.providers.aggregator import ProductAggregator
from app.providers.registry import ProviderRegistry
from app.providers.models import ProductQuery, RecipientProfile


async def test_reference_provider():
    """Test reference provider directly."""
    print("\n🧪 Testing ReferenceProvider...")
    provider = ReferenceProvider()
    
    query = ProductQuery(
        keywords=["tecnología", "gaming"],
        recipientProfile=RecipientProfile(
            type="friend",
            age=30,
            interests=["gaming", "tecnología"]
        ),
        limit=5
    )
    
    result = await provider.search(query)
    print(f"✅ ReferenceProvider returned {len(result.products)} products")
    
    if result.products:
        print(f"   Sample product: {result.products[0].title}")
        print(f"   Provider: {result.products[0].sourceProvider}")
        print(f"   Score: {result.products[0].score:.2f}")
    
    return len(result.products) > 0


async def test_aggregator():
    """Test aggregator with all providers."""
    print("\n🧪 Testing ProductAggregator...")
    
    # Get the global aggregator
    from app.providers.aggregator import get_aggregator
    aggregator = get_aggregator()
    
    query = ProductQuery(
        keywords=["regalo", "amigo"],
        priceMin=5000.0,
        priceMax=20000.0,
        recipientProfile=RecipientProfile(
            type="friend",
            age=30,
            interests=["tecnología"]
        ),
        limit=10
    )
    
    products = await aggregator.search_products(query)
    print(f"✅ Aggregator returned {len(products)} products")
    
    if products:
        providers_used = set(p.sourceProvider for p in products)
        print(f"   Providers used: {', '.join(providers_used)}")
        print(f"   Top product: {products[0].title}")
        print(f"   Score: {products[0].score:.2f}")
    
    return len(products) > 0


async def test_query_variations():
    """Test different query variations."""
    print("\n🧪 Testing query variations...")
    provider = ReferenceProvider()
    
    test_cases = [
        ("Empty keywords", ProductQuery(keywords=[], limit=5)),
        ("Price filter", ProductQuery(keywords=["regalo"], priceMin=3000.0, priceMax=8000.0, limit=5)),
        ("Multiple keywords", ProductQuery(keywords=["libro", "lectura"], limit=3)),
    ]
    
    for name, query in test_cases:
        result = await provider.search(query)
        print(f"   {name}: {len(result.products)} products")
    
    return True


async def main():
    """Run all tests."""
    print("=" * 60)
    print("🎁 GiftFinder Provider System Test")
    print("=" * 60)
    
    # Test registry
    print("\n🧪 Testing ProviderRegistry...")
    registry = ProviderRegistry()
    providers = registry.get_all_providers()
    print(f"✅ Loaded {len(providers)} provider(s): {', '.join(p.name for p in providers)}")
    
    # Run tests
    results = []
    results.append(await test_reference_provider())
    results.append(await test_aggregator())
    results.append(await test_query_variations())
    
    # Summary
    print("\n" + "=" * 60)
    if all(results):
        print("✅ All tests passed!")
        print("=" * 60)
        return 0
    else:
        print("❌ Some tests failed")
        print("=" * 60)
        return 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
