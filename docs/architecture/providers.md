# Provider-Based Architecture

## Current Flow Analysis

### Existing Architecture (Before Refactor)

The current GiftFinder system uses a direct MercadoLibre scraping pipeline:

```
User Query → LLM Parsing → Keyword Extraction → MercadoLibre Scraping → Response
```

**Key Components:**

1. **main.py** - FastAPI endpoint handler
   - Receives user query via `/scrape/search`
   - Orchestrates the entire flow
   - Returns `ScraperResponse` with intent and products

2. **ai_local.py** - LLM Integration
   - `parse_query()`: Parses natural language into structured data
   - Returns: `recipientType`, `age`, `budgetMin`, `budgetMax`, `interests`
   - Uses Ollama with qwen2.5:1.5b model
   - Timeout: 15 seconds

3. **validation.py** - Input Sanitization
   - `sanitize_keyword()`: Cleans keywords for URL usage
   - `validate_ml_url()`: SSRF protection for MercadoLibre domains
   - XSS and injection prevention

4. **ml_scraper.py** - Web Scraping
   - `scrape_mercadolibre()`: Fetches products from MercadoLibre
   - Uses httpx async client
   - Semaphore-based concurrency control (max 3)
   - Timeout: 10 seconds
   - Anti-bot detection

5. **cache.py** - TTL Caching
   - TTLCache with 1-hour expiration
   - Max 100 entries
   - Key format: `ml::{keyword}`

6. **config.py** - Configuration Management
   - Pydantic-based settings
   - Environment variable support
   - Validation on startup

### Limitations of Current Architecture

1. **Tight Coupling**: Scraping is hardcoded into the main flow
2. **Single Source**: Only MercadoLibre, no fallback
3. **Fragile**: If scraping fails (anti-bot), entire request fails
4. **Not Extensible**: Adding affiliate networks or merchant catalogs requires refactoring
5. **No Abstraction**: No concept of "providers" or pluggable data sources

## New Architecture (Provider-Based)

### Design Goals

1. **Decoupling**: Separate "what to recommend" from "where to fetch"
2. **Extensibility**: Easy to add new providers (affiliate, merchant, API-based)
3. **Resilience**: Fallback to reference data if scraping fails
4. **Flexibility**: Enable/disable providers via configuration
5. **Compatibility**: Maintain existing API response format

### Architecture Overview

```
User Query → LLM Parsing → ProductQuery
                              ↓
                      ProductAggregator
                      /       |       \
                     /        |        \
            Reference    Scraping    (Future)
            Provider     Provider    Affiliate
                     \        |        /
                      \       |       /
                       Merge & Dedupe
                              ↓
                      Ranked Products → Response
```

### Core Abstractions

#### 1. Product (Standard Model)

**Enhanced with additional fields:**

```python
{
    "id": str,                          # Unique identifier (stable within provider)
    "title": str,                       # Product name
    "description": str?,                # Optional description
    "images": [str],                    # Image URLs (can be empty)
    "price": float,                     # Price value (required, non-negative)
    "currency": str,                    # Currency code (ISO 4217 if possible)
    "vendor": {                         # Vendor information
        "name": str,                    # Vendor name (required)
        "id": str?                      # Vendor ID (optional)
    },
    "url": str,                         # Product or search URL
    "sourceProvider": str,              # Provider name (reference, scraping)
    "categories": [str],                # Product categories
    "tags": [str],                      # User interest tags
    "availability": {                   # Optional availability info
        "inStock": bool?,               # Whether in stock
        "stockCount": int?,             # Stock count
        "leadTimeDays": int?            # Lead time for delivery
    },
    "shipping": {                       # Optional shipping info
        "cost": float?,                 # Shipping cost
        "currency": str?,               # Shipping currency
        "methods": [str]                # Available shipping methods
    },
    "rating": {                         # Optional rating info
        "value": float?,                # Rating value (0-5)
        "count": int?                   # Number of ratings
    },
    "score": float?,                    # Optional relevance score (0-1)
    "raw": any?                         # Raw provider data (for debugging, stripped by default)
}
```

**Key Changes:**
- `price` is now required (was optional)
- `vendor` is now a structured object with `name` and optional `id`
- Added `availability`, `shipping`, and `rating` nested objects
- Added `raw` field for debugging (must be stripped from public API)

#### 2. ProductQuery (Request Model)

**Enhanced with additional filtering and control fields:**

```python
{
    "keywords": [str],               # 2-6 commercial keywords
    "category": str?,                # Product category
    "priceMin": float?,              # Min price filter (>= 0)
    "priceMax": float?,              # Max price filter (>= 0)
    "currency": str?,                # Currency code (ISO 4217)
    "locale": str?,                  # Locale (e.g., "es-AR", "en-US")
    "location": {                    # Location-based filtering
        "country": str?,             # Country code (ISO 3166-1)
        "region": str?,              # Region/state
        "city": str?                 # City name
    },
    "occasion": str?,                # Gift occasion
    "recipientProfile": {            # Recipient details
        "type": str,                 # friend, family, colleague
        "age": int?,
        "gender": str?,              # NEW: Recipient gender
        "interests": [str],
        "relationship": str?         # NEW: Relationship to recipient
    },
    "limit": int = 10,               # Max results (default 10, clamped to 1-30)
    "excludeVendors": [str],         # NEW: Vendor IDs to exclude
    "includeVendors": [str],         # NEW: Vendor IDs to include (whitelist)
    "safeSearch": bool = True,       # NEW: Enable safe search filtering
    "debug": bool = False            # NEW: Include debug information
}
```

**Key Changes:**
- `limit` is now clamped to max 30 (was 100)
- Added `currency` and `locale` for internationalization
- `location` is now a structured object (was simple string)
- `recipientProfile` now includes `gender` and `relationship`
- Added vendor filtering: `excludeVendors` and `includeVendors`
- Added `safeSearch` and `debug` flags

#### 3. ProviderResult (Response Model)
```python
{
    "products": [Product],
    "meta": {
        "providerName": str,     # Provider identifier
        "fetchedAt": datetime,   # Timestamp
        "latencyMs": int,        # Response time
        "warnings": [str]        # Any warnings/errors
    }
}
```

#### 4. ProductProvider (Interface)

**Enhanced with capabilities and context:**

```python
class ProductProvider(ABC):
    @property
    def name(self) -> str:
        """Provider identifier"""
    
    @property
    def capabilities(self) -> ProviderCapabilities:
        """Provider capabilities declaration"""
        
    def supports(self, query: ProductQuery) -> bool:
        """Check if this provider can handle the query"""
        
    async def search(
        self,
        query: ProductQuery,
        ctx: Optional[ProviderContext] = None
    ) -> ProviderResult:
        """Fetch products for the query with optional context"""
```

#### 5. ProviderCapabilities (NEW)

Describes what features a provider supports:

```python
{
    "supportsImages": bool,          # Provider returns product images
    "supportsPriceFilter": bool,     # Can filter by price range
    "supportsLocation": bool,        # Supports location-based filtering
    "supportsStock": bool,           # Returns stock availability
    "supportsDeepLink": bool,        # Returns direct product URLs
    "supportsCategories": bool,      # Supports category filtering
    "supportsRatings": bool,         # Returns product ratings
    "supportsShipping": bool         # Returns shipping information
}
```

#### 6. ProviderContext (NEW)

Context information for provider execution:

```python
{
    "requestId": str,                # Unique request ID for tracing
    "timeoutMs": int = 15000,        # Timeout in milliseconds
    "signal": Any?,                  # Cancellation signal/token
    "logger": Any?,                  # Logger instance
    "trace": {                       # Optional tracing info
        "spanId": str?
    }
}
```

#### 7. ProviderError (NEW)

Typed error for provider failures:

```python
{
    "category": ProviderErrorCategory,  # Error category enum
    "providerName": str,                # Provider that generated error
    "message": str,                     # Human-readable error message
    "retryable": bool,                  # Whether operation can be retried
    "details": dict?                    # Additional error details
}

# Error Categories
enum ProviderErrorCategory {
    TIMEOUT,      # Request timeout
    RATE_LIMIT,   # Rate limit exceeded
    AUTH,         # Authentication failure
    PARSE,        # Parsing error
    UPSTREAM,     # Upstream service error
    UNKNOWN       # Unknown error
}
```

### Normalization and Deduplication

#### Normalizer Functions

Located in `app/domain/products/normalizers.py`:

- **`normalizeTitle(title: str) -> str`**: Normalize product titles for consistent display
  - Strips whitespace, collapses multiple spaces
  - Converts to title case
  - Removes excessive punctuation

- **`normalizePrice(price: float?, currency: str) -> float?`**: Normalize prices
  - Rounds to 2 decimal places
  - Ensures non-negative
  - Returns None for invalid prices

- **`canonicalizeUrl(url: str) -> str`**: Canonicalize URLs for deduplication
  - Converts to lowercase
  - Removes query parameters and fragments
  - Removes trailing slashes

- **`buildStableDedupKey(product) -> str`**: Build stable deduplication key
  - Uses SHA256 hash of canonical URL or normalized attributes
  - For direct product links: uses canonical URL
  - For search pages: uses normalized title + vendor + price
  - Ensures stability across provider results

#### PublicProduct DTO

Located in `app/domain/products/public_dto.py`:

- **`toPublicProduct(product: Product, debug: bool) -> dict`**: Convert to public DTO
  - Strips `raw` field unless `debug=True`
  - Maintains all other fields
  - Safe for external API responses

### Provider Implementations

#### ReferenceProvider
- **Purpose**: Fallback provider with curated product ideas
- **Data Source**: Local JSON file with reference products
- **Capabilities**:
  - `supportsImages`: True
  - `supportsPriceFilter`: True
  - `supportsLocation`: False
  - `supportsStock`: False
  - `supportsDeepLink`: False (returns search URLs)
  - `supportsCategories`: True
  - `supportsRatings`: False
  - `supportsShipping`: False
- **Features**:
  - Category and keyword filtering
  - Always available (no external dependencies)
  - Returns realistic product-like objects
  - Clearly labeled as `sourceProvider="reference"`
  - URLs point to search pages, not specific products

#### ScrapingProvider
- **Purpose**: Wrapper around existing MercadoLibre scraper
- **Data Source**: Live web scraping
- **Capabilities**:
  - `supportsImages`: True
  - `supportsPriceFilter`: True
  - `supportsLocation`: False
  - `supportsStock`: False
  - `supportsDeepLink`: True (returns direct product URLs)
  - `supportsCategories`: False
  - `supportsRatings`: False
  - `supportsShipping`: False
- **Features**:
  - Reuses existing `ml_scraper.py` logic
  - Returns standardized Product objects
  - Robust error handling with circuit breaker
  - Timeout protection (10s)
  - Rate limiting and concurrency control
  - Returns empty list + warning on failure (never crashes)

### Orchestration Layer

#### ProviderRegistry
- Loads enabled providers from `ENABLED_PROVIDERS` env variable
- Validates provider availability on startup
- Provides provider discovery interface

#### ProductAggregator
- **Responsibilities**:
  1. Accept ProductQuery
  2. Call all enabled providers in parallel (max concurrency: 3)
  3. Merge results from multiple providers
  4. Deduplicate by canonical URL or normalized title+vendor+price
  5. Score and rank by relevance (keyword match + price fit + provider weight)
  6. Limit to `query.limit` results (default 10)

- **Scoring Algorithm** (basic):
  ```
  score = keyword_match_score * 0.5 + 
          price_fit_score * 0.3 + 
          provider_weight * 0.2
  ```

### Configuration

```bash
# Enable/disable providers
ENABLED_PROVIDERS=reference,scraping

# Provider weights for ranking
PROVIDER_WEIGHT_REFERENCE=0.7
PROVIDER_WEIGHT_SCRAPING=1.0

# Performance limits
MAX_CONCURRENT_PROVIDERS=3
PROVIDER_TIMEOUT_SECONDS=15
```

### Migration Strategy

1. **Phase 1**: Add providers alongside existing code
2. **Phase 2**: Update endpoint to use aggregator
3. **Phase 3**: Keep old scraper code as fallback
4. **Phase 4**: Remove old code after validation

### Benefits

✅ **Resilience**: If scraping fails, reference provider still returns results  
✅ **Extensibility**: New providers added without touching core logic  
✅ **Testability**: Each provider tested independently  
✅ **Observability**: Per-provider metrics and logging  
✅ **Flexibility**: Enable/disable providers via config  
✅ **Compatibility**: Existing API contracts maintained

### Future Providers

- **AffiliateProvider**: Fetch from affiliate networks (e.g., ShareASale, CJ)
- **MerchantCatalogProvider**: Direct integration with merchant APIs
- **APIProvider**: Use official MercadoLibre API instead of scraping
- **CachedProvider**: Decorator for any provider to add caching
- **MLRecommendationProvider**: ML-based personalized recommendations

### Usage Examples

#### Creating a Provider

```python
from app.providers.base import ProductProvider
from app.providers.models import (
    Product,
    ProductQuery,
    ProviderCapabilities,
    ProviderContext,
    ProviderResult,
    ProviderMetadata,
    VendorInfo,
)

class MyCustomProvider(ProductProvider):
    @property
    def name(self) -> str:
        return "my-provider"
    
    @property
    def capabilities(self) -> ProviderCapabilities:
        return ProviderCapabilities(
            supportsImages=True,
            supportsPriceFilter=True,
            supportsDeepLink=True,
        )
    
    def supports(self, query: ProductQuery) -> bool:
        return bool(query.keywords)
    
    async def search(
        self,
        query: ProductQuery,
        ctx: Optional[ProviderContext] = None
    ) -> ProviderResult:
        # Fetch products...
        products = []
        
        return ProviderResult(
            products=products,
            meta=ProviderMetadata(
                providerName=self.name,
                latencyMs=100,
                warnings=[]
            )
        )
```

#### Using Normalizers

```python
from app.domain.products import (
    normalizeTitle,
    normalizePrice,
    canonicalizeUrl,
    buildStableDedupKey,
)

# Normalize product data
title = normalizeTitle("  AURICULARES   bluetooth  ")  # -> "Auriculares Bluetooth"
price = normalizePrice(12345.6789)  # -> 12345.68
url = canonicalizeUrl("https://example.com/Product?ref=ads")  # -> "https://example.com/product"

# Build deduplication key
dedup_key = buildStableDedupKey(
    product_id="123",
    title="Product Name",
    vendor_name="Store",
    price=100.0,
    url="https://example.com/product/123"
)
```

#### Converting to Public DTO

```python
from app.domain.products import toPublicProduct
from app.providers.models import Product, VendorInfo

product = Product(
    id="123",
    title="Test Product",
    price=100.0,
    currency="ARS",
    vendor=VendorInfo(name="Store"),
    url="https://example.com",
    sourceProvider="test",
    raw={"debug": "data"}
)

# For client API (strips raw field)
public_product = toPublicProduct(product, debug=False)
# raw field is removed

# For admin/debug mode
debug_product = toPublicProduct(product, debug=True)
# raw field is included
```

### Testing

Tests for the contract are located in:
- `tests/test_enhanced_models.py` - Model validation and limit clamping
- `tests/test_domain_normalizers.py` - Normalizer functions and deduplication
- `tests/test_public_dto.py` - Public DTO mapping and raw field filtering

Run tests:
```bash
cd giftfinder-scraper
pytest tests/ -v
```
