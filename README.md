# 🎁 GiftFinder - AI-Powered Gift Recommendation System

[![CI Pipeline](https://github.com/martinianod/giftfinder-proyect/actions/workflows/ci.yml/badge.svg)](https://github.com/martinianod/giftfinder-proyect/actions/workflows/ci.yml)

GiftFinder is a production-ready gift recommendation system that uses AI to understand natural language queries and find relevant products using a pluggable provider architecture.

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌─────────────┐
│  Frontend   │────▶│   Backend    │────▶│     Scraper     │────▶│   Ollama    │
│   (React)   │     │(Spring Boot) │     │   (FastAPI)     │     │    (LLM)    │
└─────────────┘     └──────────────┘     └─────────────────┘     └─────────────┘
                           │                      │
                           ▼                      ▼
                    ┌──────────────┐     ┌──────────────────┐
                    │  PostgreSQL  │     │  Provider System  │
                    └──────────────┘     │  - Reference      │
                                         │  - Scraping (ML)  │
                                         │  - (Extensible)   │
                                         └──────────────────┘
```

### Components

- **Frontend**: React + Vite SPA with Nginx
- **Backend**: Spring Boot 3 REST API with JWT authentication
- **Scraper**: FastAPI service with provider-based product retrieval and LLM integration
- **Ollama**: Local LLM (qwen2.5:1.5b) for natural language processing
- **PostgreSQL**: User data and favorites storage
- **Provider System**: Pluggable product data sources (reference data, web scraping, etc.)

### Provider Architecture

The scraper now uses a **provider-based architecture** for flexible product retrieval:

- **ReferenceProvider**: Curated fallback dataset (20+ product ideas) - always available
- **ScrapingProvider**: Live MercadoLibre web scraping - real product data
- **Extensible**: Easy to add affiliate networks, merchant APIs, or other sources

**Key Benefits**:
- ✅ Resilient: If scraping fails, reference data still provides results
- ✅ Flexible: Enable/disable providers via configuration
- ✅ Extensible: Add new providers without changing core logic
- ✅ Observable: Per-provider metrics and logging

See [Provider Architecture Documentation](docs/architecture/providers.md) for details.

## ✨ Features

### Core Features

- 🎁 **AI-Powered Gift Search**: Natural language query understanding with local LLM
- 🔍 **Multi-Provider Product Search**: Reference data + live web scraping
- 👤 **User Authentication**: JWT-based secure authentication
- ⭐ **Favorites Management**: Save and organize favorite gift ideas
- 💳 **Gift Cards**: Create, send, and redeem internal gift cards with wallet integration

### Proactive Automation Features (NEW! 🎉)

- 📅 **Event Reminders**: Store recipients and important dates, receive automated email reminders
- 💰 **Price Drop Tracking**: Save products and get notified when prices drop
- 🔔 **Configurable Notifications**: Customize reminder days and notification preferences
- 📊 **Admin Dashboard**: Monitor scheduled jobs and notification queue

### Gift Card System (NEW! 🎉)

- 💳 **Gift Card Creation**: Buy and send gift cards to recipients with custom messages
- 🔐 **Secure Codes**: Unguessable codes with BCrypt hashing for security
- 📧 **Email Delivery**: Automatic or scheduled delivery via email
- 💰 **Wallet Integration**: Redeem gift cards into an internal wallet balance
- 🔄 **Idempotent Redemption**: Prevent double redemption with atomic operations
- 📝 **Transaction Ledger**: Complete audit trail of all wallet operations
- ⚡ **Status Lifecycle**: CREATED → SENT → REDEEMED / EXPIRED / CANCELLED
- ⏰ **Expiration Policy**: Default 12 months, configurable
- 👨‍💼 **Admin Controls**: Cancel or expire gift cards as needed

See [Gift Card API Documentation](docs/GIFT_CARD_API.md) for detailed information.

### Production-Ready Improvements

- ✅ **Security**: Input validation, sanitization, SSRF protection, rate limiting
- ✅ **Performance**: Async scraping, concurrency control, TTL caching
- ✅ **Observability**: Structured JSON logging, request tracking, health checks
- ✅ **Reliability**: Timeouts, error handling, fallbacks, graceful degradation
- ✅ **DevOps**: Docker Compose orchestration, resource limits, CI/CD pipeline
- ✅ **Maintainability**: Centralized config, comprehensive tests (74 tests), detailed docs
- ✅ **Extensibility**: Provider-based architecture for multiple data sources

## 🚀 Quick Start

### Prerequisites

- Docker 24+ and Docker Compose 2+
- 8GB+ RAM available
- 20GB+ disk space for Ollama models

### Step 1: Clone Repository

```bash
git clone https://github.com/martinianod/giftfinder-proyect.git
cd giftfinder-proyect
```

### Step 2: Configure Environment

```bash
cp .env.example .env
# Edit .env with your configuration (defaults work for local development)
```

### Step 3: Start Services

```bash
docker-compose up -d
```

This will start all services with proper health checks and dependencies.

### Step 4: Initialize Ollama Model

```bash
# Wait for Ollama to be healthy
docker-compose exec ollama ollama pull qwen2.5:1.5b
```

### Step 5: Verify Setup (Run Smoke Tests)

```bash
# Run automated smoke tests
./scripts/smoke.sh
```

This will verify:
- ✅ Backend health endpoint
- ✅ Frontend accessibility  
- ✅ User signup and login
- ✅ Error handling (409, 401, 400)
- ✅ Request correlation IDs

**For detailed local setup instructions**, see [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md)

### Step 6: Access Services

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Backend Health**: http://localhost:8080/actuator/health
- **Scraper API**: http://localhost:8001
- **Scraper Health**: http://localhost:8001/health/ready
- **Scraper Metrics**: http://localhost:8001/health/metrics

## 📖 API Usage

### Search for Gifts

```bash
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "regalo para amigo de 30 años que le gusta la tecnología"
  }'
```

### Response Example

```json
{
  "interpretedIntent": {
    "recipient": "friend",
    "age": 30,
    "budgetMin": null,
    "budgetMax": null,
    "interests": ["tecnología", "gadgets"]
  },
  "recommendations": [
    {
      "id": "uuid",
      "title": "Product Name",
      "price": 12345.0,
      "currency": "ARS",
      "image_url": "https://...",
      "product_url": "https://mercadolibre.com.ar/...",
      "store": "MercadoLibre",
      "tags": ["tecnología"]
    }
  ]
}
```

### Health Check Endpoints

```bash
# Liveness check
curl http://localhost:8001/health

# Readiness check (verifies all dependencies)
curl http://localhost:8001/health/ready

# Service metrics
curl http://localhost:8001/health/metrics
```

## 🛠️ Development

### Run Scraper Locally

```bash
cd giftfinder-scraper

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run service
uvicorn app.main:app --reload --port 8001
```

### Run Backend Locally

```bash
cd giftfinder-backend

# Build
./gradlew build

# Run
./gradlew bootRun
```

### Run Frontend Locally

```bash
cd giftfinder-frontend

# Install dependencies
npm install

# Run dev server
npm run dev
```

## 🧪 Testing

### Scraper Tests

```bash
cd giftfinder-scraper
pytest tests/ -v --cov=app --cov-report=term
```

### Backend Tests

```bash
cd giftfinder-backend
./gradlew test
```

### Frontend Tests

```bash
cd giftfinder-frontend
npm test
```

## 📊 Monitoring

### View Structured Logs

```bash
# View scraper logs with jq
docker-compose logs scraper -f | jq .

# Filter by log level
docker-compose logs scraper -f | jq 'select(.level=="ERROR")'

# Track specific request
docker-compose logs scraper -f | jq 'select(.request_id=="xxx")'
```

### Health Checks

```bash
# Check all services
docker-compose ps

# Detailed health status
curl http://localhost:8001/health/ready | jq .
```

### Metrics

```bash
# Service metrics
curl http://localhost:8001/health/metrics | jq .
```

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SCRAPER_PORT` | `8001` | Scraper service port |
| `BACKEND_PORT` | `8080` | Backend service port |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `OLLAMA_HOST` | `http://ollama:11434` | Ollama service URL |
| `OLLAMA_MODEL` | `qwen2.5:1.5b` | LLM model to use |
| **Provider Configuration** | | |
| `ENABLED_PROVIDERS` | `reference,scraping` | Comma-separated list of enabled providers |
| `MAX_CONCURRENT_PROVIDERS` | `3` | Max parallel provider calls |
| `PROVIDER_TIMEOUT_SECONDS` | `15` | Timeout for provider calls |
| **Performance & Limits** | | |
| `MAX_CONCURRENT_SCRAPES` | `3` | Max parallel scraping ops |
| `LLM_TIMEOUT_SECONDS` | `15` | LLM request timeout |
| `SCRAPING_TIMEOUT_SECONDS` | `10` | Scraping request timeout |
| `CACHE_TTL_SECONDS` | `3600` | Cache entry lifetime |
| `MAX_QUERY_LENGTH` | `500` | Max query string length |
| `RATE_LIMIT_PER_MINUTE` | `30` | Rate limit per IP |
| `LOG_LEVEL` | `INFO` | Logging level |

### Provider Configuration Examples

```bash
# Use only reference provider (no web scraping)
ENABLED_PROVIDERS=reference

# Use only scraping provider (no fallback)
ENABLED_PROVIDERS=scraping

# Use both providers (default - recommended)
ENABLED_PROVIDERS=reference,scraping

# Adjust provider performance
MAX_CONCURRENT_PROVIDERS=3
PROVIDER_TIMEOUT_SECONDS=15
```

### Resource Limits

**Default Docker Compose Limits:**

- **Ollama**: 2 CPU, 4GB RAM
- **Scraper**: 1 CPU, 1GB RAM
- **Backend**: 1.5 CPU, 2GB RAM
- **PostgreSQL**: 1 CPU, 1GB RAM
- **Frontend**: 0.5 CPU, 512MB RAM

## 🔒 Security Features

### Implemented

- ✅ Input validation and sanitization
- ✅ SSRF protection for URL validation
- ✅ Rate limiting (30 req/min per IP)
- ✅ Request ID tracking
- ✅ Security headers in nginx
- ✅ No secrets in code/logs
- ✅ Timeout protection
- ✅ Safe character filtering

### Production Recommendations

- [ ] Use HTTPS with valid certificates
- [ ] Set strong `POSTGRES_PASSWORD` in production
- [ ] Configure firewall rules
- [ ] Enable authentication for all services
- [ ] Regular dependency updates
- [ ] Monitor rate limiting violations
- [ ] Use secrets management (e.g., HashiCorp Vault)

## 🐛 Troubleshooting

### Ollama Not Responding

**Symptoms**: Scraper returns 503 errors, health check fails

**Solutions**:
```bash
# Check Ollama health
docker-compose exec ollama curl http://localhost:11434/api/tags

# Check Ollama logs
docker-compose logs ollama

# Restart Ollama
docker-compose restart ollama

# Verify model is downloaded
docker-compose exec ollama ollama list
```

### Scraping Returns Empty Results

**Symptoms**: Query succeeds but no products returned

**Possible Causes**:
1. MercadoLibre antibot detection
2. Invalid search keyword
3. Network issues

**Solutions**:
```bash
# Check scraper logs
docker-compose logs scraper -f | jq 'select(.message | contains("anti-bot"))'

# Test with simple keyword
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{"query": "auriculares"}'

# Check rate limiting
curl http://localhost:8001/health/metrics | jq .config.rate_limit_per_minute
```

### High CPU Usage

**Symptoms**: Services consuming excessive CPU

**Solutions**:
```bash
# Check concurrent scrapes
curl http://localhost:8001/health/metrics | jq .config.max_concurrent_scrapes

# Reduce concurrency in .env
MAX_CONCURRENT_SCRAPES=2

# Restart scraper
docker-compose restart scraper

# Monitor resources
docker stats
```

### Backend Cannot Connect to Scraper

**Symptoms**: Backend logs show connection errors to scraper

**Solutions**:
```bash
# Check scraper health
curl http://localhost:8001/health

# Check network connectivity
docker-compose exec backend curl http://scraper:8001/health

# Verify scraper is healthy before backend starts
docker-compose ps

# Check depends_on conditions in docker-compose.yml
```

### Cache Growing Unbounded

**Symptoms**: Memory usage increases over time

**Note**: This should NOT happen with TTLCache, but if it does:

```bash
# Check cache stats
curl http://localhost:8001/health/metrics | jq .cache

# Cache should show:
# - current_size < max_size (100)
# - ttl_seconds (3600)

# If needed, restart to clear cache
docker-compose restart scraper
```

### Verbose Logs

**Symptoms**: Too many logs, hard to debug

**Solutions**:
```bash
# Set log level to WARNING in .env
LOG_LEVEL=WARNING

# Restart scraper
docker-compose restart scraper

# Or set to DEBUG for troubleshooting
LOG_LEVEL=DEBUG
```

## 🚀 Deployment

### Production Deployment

1. **Update Configuration**:
   ```bash
   # Set production values in .env
   POSTGRES_PASSWORD=your-strong-password
   LOG_LEVEL=WARNING
   ALLOWED_ORIGINS=https://yourdomain.com
   ```

2. **Deploy with Docker Compose**:
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

3. **Configure Reverse Proxy** (nginx example):
   ```nginx
   server {
       listen 443 ssl http2;
       server_name yourdomain.com;
       
       ssl_certificate /path/to/cert.pem;
       ssl_certificate_key /path/to/key.pem;
       
       location / {
           proxy_pass http://localhost:5173;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
       
       location /api/ {
           proxy_pass http://localhost:8080/;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

4. **Setup Monitoring**:
   - Configure log aggregation (e.g., ELK stack)
   - Set up health check monitoring
   - Configure alerting for errors

5. **Backup Strategy**:
   ```bash
   # Backup PostgreSQL
   docker-compose exec postgres pg_dump -U giftfinder_user giftfinder > backup.sql
   ```

## ⚖️ Legal Considerations

### Web Scraping Notice

⚠️ **Important**: This project scrapes MercadoLibre for educational purposes.

**Before using in production**:

1. Review MercadoLibre's Terms of Service
2. Consider using their official API if available
3. Implement respectful scraping:
   - Honor robots.txt
   - Use reasonable rate limits (implemented: 1 req/sec)
   - Cache results aggressively (implemented: 1 hour TTL)
   - Monitor for blocking signals

**Ethical Use**:
- Do not overwhelm their servers
- Do not scrape personal user data
- Do not use for competitive intelligence
- Consider the commercial impact

## 📚 Additional Documentation

- [Audit Report](docs/AUDIT_REPORT.md) - Complete technical audit
- [Runbook](docs/RUNBOOK.md) - Operational procedures
- [Provider Architecture](docs/architecture/providers.md) - Provider system design and implementation
- [Proactive Automation](docs/PROACTIVE_AUTOMATION.md) - Event reminders and price drop tracking
- [Gift Card API](docs/GIFT_CARD_API.md) - Gift card system API documentation (NEW!)
- [Gift Card Migration](docs/GIFT_CARD_MIGRATION.md) - Database migration guide for gift cards (NEW!)
- [API Documentation](http://localhost:8001/docs) - Interactive API docs (when running)

## 🔌 Extending with New Providers

The provider system is designed for easy extension. Here's how to add a new product provider:

### 1. Create Provider Class

```python
# giftfinder-scraper/app/providers/my_provider.py

from app.providers.base import ProductProvider
from app.providers.models import Product, ProductQuery, ProviderResult

class MyProvider(ProductProvider):
    @property
    def name(self) -> str:
        return "my_provider"
    
    def supports(self, query: ProductQuery) -> bool:
        # Return True if this provider can handle the query
        return bool(query.keywords)
    
    async def search(self, query: ProductQuery) -> ProviderResult:
        # Fetch products from your source
        products = []
        # ... your implementation here ...
        
        return ProviderResult(
            products=products,
            meta=ProviderMetadata(
                providerName=self.name,
                latencyMs=...,
                warnings=[]
            )
        )
```

### 2. Register Provider

Add your provider to the registry:

```python
# giftfinder-scraper/app/providers/registry.py

from app.providers.my_provider import MyProvider

available_providers = {
    "reference": ReferenceProvider,
    "scraping": ScrapingProvider,
    "my_provider": MyProvider,  # Add your provider
}
```

### 3. Enable Provider

Update your `.env`:

```bash
ENABLED_PROVIDERS=reference,scraping,my_provider
```

### 4. Test Your Provider

```python
# giftfinder-scraper/tests/test_my_provider.py

import pytest
from app.providers.my_provider import MyProvider

class TestMyProvider:
    @pytest.mark.asyncio
    async def test_search(self):
        provider = MyProvider()
        query = ProductQuery(keywords=["test"])
        result = await provider.search(query)
        assert len(result.products) >= 0
```

### Example Providers You Can Add

- **AffiliateProvider**: Fetch from affiliate networks (ShareASale, CJ, Amazon)
- **APIProvider**: Use official MercadoLibre API instead of scraping
- **DatabaseProvider**: Query your own product catalog
- **MLRecommendationProvider**: ML-based personalized recommendations
- **CachedProvider**: Decorator to add caching to any provider

See [Provider Architecture Documentation](docs/architecture/providers.md) for more details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Ensure CI passes (74+ tests)
5. Submit a pull request

## 📄 License

This project is for educational purposes. Check individual component licenses.

## 🙏 Acknowledgments

- Ollama for local LLM capabilities
- FastAPI for excellent async Python framework
- Spring Boot for robust backend framework
- React for modern frontend development

---

**Built with ❤️ for learning production-ready software development**
