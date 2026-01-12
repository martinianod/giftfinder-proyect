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
```python
{
    "id": str,              # Unique identifier
    "title": str,           # Product name
    "description": str?,    # Optional description
    "images": [str],        # Image URLs
    "price": float,         # Price value
    "currency": str,        # Currency code (ARS, USD)
    "vendor": str,          # Seller/store name
    "url": str,             # Product or search URL
    "sourceProvider": str,  # Provider name (reference, scraping)
    "categories": [str],    # Product categories
    "tags": [str],          # User interest tags
    "score": float?         # Optional relevance score
}
```

#### 2. ProductQuery (Request Model)
```python
{
    "keywords": [str],           # Search keywords
    "category": str?,            # Product category
    "priceMin": float?,          # Min price filter
    "priceMax": float?,          # Max price filter
    "location": str?,            # User location
    "occasion": str?,            # Gift occasion
    "recipientProfile": {        # Recipient details
        "type": str,             # friend, family, colleague
        "age": int?,
        "interests": [str]
    },
    "limit": int = 10            # Max results to return
}
```

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
```python
class ProductProvider(ABC):
    @property
    def name(self) -> str:
        """Provider identifier"""
        
    def supports(self, query: ProductQuery) -> bool:
        """Check if this provider can handle the query"""
        
    async def search(self, query: ProductQuery) -> ProviderResult:
        """Fetch products for the query"""
```

### Provider Implementations

#### ReferenceProvider
- **Purpose**: Fallback provider with curated product ideas
- **Data Source**: Local JSON file with reference products
- **Features**:
  - Category and keyword filtering
  - Always available (no external dependencies)
  - Returns realistic product-like objects
  - Clearly labeled as `sourceProvider="reference"`
  - URLs point to search pages, not specific products

#### ScrapingProvider
- **Purpose**: Wrapper around existing MercadoLibre scraper
- **Data Source**: Live web scraping
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
