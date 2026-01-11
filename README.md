# 🎁 GiftFinder Project

[![CI/CD Pipeline](https://github.com/martinianod/giftfinder-proyect/actions/workflows/ci.yml/badge.svg)](https://github.com/martinianod/giftfinder-proyect/actions/workflows/ci.yml)

An intelligent gift recommendation system that uses AI to understand user intent and scrapes MercadoLibre for relevant products. Built with microservices architecture using Python FastAPI, Spring Boot, React, and Ollama LLM.

## 📋 Table of Contents

- [Architecture](#architecture)
- [Features](#features)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Usage](#api-usage)
- [Development](#development)
- [Monitoring & Logging](#monitoring--logging)
- [Troubleshooting](#troubleshooting)
- [Security](#security)
- [License](#license)

## 🏗️ Architecture

```
┌─────────────────┐
│   React SPA     │  (Port 80)
│   + Nginx       │
└────────┬────────┘
         │
         │ /api/*
         ▼
┌─────────────────┐      ┌──────────────────┐
│  Spring Boot    │─────▶│  PostgreSQL      │
│  Backend        │      │  Database        │
│  (Port 8080)    │      │  (Port 5432)     │
└────────┬────────┘      └──────────────────┘
         │
         │ HTTP
         ▼
┌─────────────────┐      ┌──────────────────┐
│  FastAPI        │─────▶│  Ollama LLM      │
│  Scraper        │      │  (qwen2.5:1.5b)  │
│  (Port 8001)    │      │  (Port 11434)    │
└─────────────────┘      └──────────────────┘
         │
         │ HTTPS
         ▼
┌─────────────────┐
│  MercadoLibre   │
│  (Web Scraping) │
└─────────────────┘
```

### Services

1. **Frontend (React + Nginx)**
   - Single Page Application
   - Vite build system
   - Nginx for static serving with security headers
   - Port: 80

2. **Backend (Spring Boot)**
   - REST API
   - User authentication (JWT)
   - Favorites management
   - Database integration
   - Port: 8080

3. **Scraper (FastAPI + Python)**
   - Query parsing with LLM
   - MercadoLibre web scraping
   - Caching with TTL
   - Rate limiting
   - Structured JSON logging
   - Port: 8001

4. **Ollama (LLM)**
   - Local LLM inference
   - Model: qwen2.5:1.5b
   - Port: 11434

5. **PostgreSQL**
   - User data
   - Favorites storage
   - Port: 5432

## ✨ Features

### Security
- ✅ Input validation and sanitization
- ✅ URL validation (anti-SSRF)
- ✅ Rate limiting (30 req/min per IP)
- ✅ JWT authentication
- ✅ Security headers (CSP, X-Frame-Options, etc.)
- ✅ No secrets in logs

### Performance
- ✅ Async scraping with concurrency control (max 3 concurrent)
- ✅ TTL cache (1 hour default, LRU eviction)
- ✅ Resource limits (CPU & memory)
- ✅ Explicit timeouts everywhere
- ✅ Connection pooling

### Observability
- ✅ Structured JSON logging
- ✅ Request ID tracking
- ✅ Health endpoints (/health, /health/ready, /health/metrics)
- ✅ Performance metrics
- ✅ Error tracking with context

### Reliability
- ✅ Docker healthchecks
- ✅ Service dependencies management
- ✅ Graceful error handling
- ✅ Circuit breaker pattern (LLM fallback)
- ✅ Retry logic

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- 8GB RAM minimum (12GB recommended)
- 4 CPU cores minimum

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/martinianod/giftfinder-proyect.git
   cd giftfinder-proyect
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and change POSTGRES_PASSWORD
   nano .env
   ```

3. **Start services**
   ```bash
   docker-compose up -d
   ```

4. **Download Ollama model** (required, ~1GB)
   ```bash
   docker exec -it ollama ollama pull qwen2.5:1.5b
   ```

5. **Verify services are healthy**
   ```bash
   # Check all services
   docker-compose ps
   
   # Check scraper health
   curl http://localhost:8001/health/ready
   
   # Check backend health
   curl http://localhost:8080/actuator/health
   ```

6. **Access the application**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - Scraper API: http://localhost:8001

### First Search Test

```bash
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{"query": "regalo para amigo de 30 años"}'
```

## ⚙️ Configuration

### Environment Variables

All configuration is managed through environment variables. See `.env.example` for complete documentation.

#### Critical Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | `CHANGE_ME_IN_PRODUCTION` | ⚠️ **MUST CHANGE** |
| `MAX_CONCURRENT_SCRAPES` | `3` | Concurrent scraping limit |
| `CACHE_TTL_SECONDS` | `3600` | Cache expiration (1 hour) |
| `RATE_LIMIT_PER_MINUTE` | `30` | API rate limit per IP |
| `LOG_LEVEL` | `INFO` | Logging verbosity |

#### Performance Tuning

**High Traffic:**
```env
MAX_CONCURRENT_SCRAPES=5
CACHE_TTL_SECONDS=7200
CACHE_MAX_SIZE=2000
RATE_LIMIT_PER_MINUTE=60
```

**Low Resources:**
```env
MAX_CONCURRENT_SCRAPES=1
CACHE_MAX_SIZE=500
MAX_ITEMS_PER_SCRAPE=10
```

### Resource Limits (Docker Compose)

| Service | CPU Limit | Memory Limit |
|---------|-----------|--------------|
| Ollama | 2 cores | 4GB |
| Backend | 1.5 cores | 2GB |
| Scraper | 1 core | 1GB |
| PostgreSQL | 1 core | 1GB |

## 📡 API Usage

### Scraper API

#### Search Endpoint
```bash
POST /scrape/search
Content-Type: application/json

{
  "query": "regalo para mamá que le gusta cocinar"
}
```

**Response:**
```json
{
  "interpretedIntent": {
    "recipient": "madre",
    "age": null,
    "budgetMin": null,
    "budgetMax": null,
    "interests": ["cocina", "utensilios de cocina"]
  },
  "recommendations": [
    {
      "id": "uuid",
      "title": "Product title",
      "price": 5000.0,
      "currency": "ARS",
      "image_url": "https://...",
      "product_url": "https://...",
      "store": "MercadoLibre",
      "rating": 4.5,
      "tags": ["cocina"]
    }
  ]
}
```

#### Health Endpoints
```bash
# Liveness probe
GET /health

# Readiness probe (checks Ollama)
GET /health/ready

# Metrics
GET /health/metrics
```

### Backend API

#### Authentication
```bash
# Register
POST /api/auth/signup
{
  "username": "user",
  "email": "user@example.com",
  "password": "password"
}

# Login
POST /api/auth/login
{
  "username": "user",
  "password": "password"
}
```

#### Favorites
```bash
# Get favorites (requires JWT)
GET /api/favorites
Authorization: Bearer <token>

# Add favorite
POST /api/favorites
Authorization: Bearer <token>
{
  "productId": "uuid",
  "title": "Product",
  "price": 5000.0
}
```

## 🛠️ Development

### Scraper (Python)

```bash
cd giftfinder-scraper

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run tests
pytest --cov=app

# Format code
black app
isort app

# Lint
flake8 app

# Run locally
uvicorn app.main:app --reload --port 8001
```

### Backend (Java)

```bash
cd giftfinder-backend

# Build
./gradlew clean build

# Run tests
./gradlew test

# Run locally
./gradlew bootRun

# Generate coverage report
./gradlew jacocoTestReport
```

### Frontend (React)

```bash
cd giftfinder-frontend

# Install dependencies
npm install

# Run dev server
npm run dev

# Build
npm run build

# Lint
npm run lint
```

## 📊 Monitoring & Logging

### View Logs

```bash
# All logs
docker-compose logs -f

# Specific service
docker-compose logs -f scraper

# With JSON parsing
docker-compose logs scraper | jq .
```

### Log Structure (JSON)

```json
{
  "timestamp": "2024-01-11T19:42:55",
  "level": "INFO",
  "logger": "app.main",
  "module": "main",
  "function": "scrape_search",
  "message": "New search request",
  "request_id": "uuid",
  "query": "regalo para amigo"
}
```

### Metrics

```bash
# Scraper metrics
curl http://localhost:8001/health/metrics

# Backend metrics (Actuator)
curl http://localhost:8080/actuator/metrics
```

### Health Checks

```bash
# Check all services
for service in scraper backend; do
  echo "Checking $service..."
  curl -s http://localhost:${service}/health | jq .
done
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Ollama Not Responding
```bash
# Check Ollama status
docker logs ollama

# Verify model is downloaded
docker exec ollama ollama list

# Download model if missing
docker exec ollama ollama pull qwen2.5:1.5b

# Check connectivity
curl http://localhost:11434/api/tags
```

#### 2. Scraping Returns Empty Results
```bash
# Check logs for antibot detection
docker-compose logs scraper | grep -i "blocked\|captcha"

# Increase rate limit delay
# In .env: RATE_LIMIT_DELAY=2.0

# Restart scraper
docker-compose restart scraper
```

#### 3. High CPU Usage
```bash
# Check current resource usage
docker stats

# Reduce concurrent scrapes
# In .env: MAX_CONCURRENT_SCRAPES=1

# Restart services
docker-compose restart
```

#### 4. Cache Issues
```bash
# Check cache stats
curl http://localhost:8001/health/metrics | jq .cache

# Clear cache (restart service)
docker-compose restart scraper
```

#### 5. Database Connection Errors
```bash
# Check PostgreSQL health
docker exec giftfinder-postgres pg_isready

# Check backend logs
docker-compose logs backend | tail -50

# Verify credentials in .env
cat .env | grep POSTGRES
```

### Service Recovery

```bash
# Restart specific service
docker-compose restart <service>

# Rebuild and restart
docker-compose up -d --build <service>

# Complete reset (⚠️ data loss)
docker-compose down -v
docker-compose up -d
```

## 🔒 Security

### Security Best Practices

1. **Environment Configuration**
   - Never commit `.env` files
   - Use strong passwords (min 16 chars)
   - Rotate credentials regularly
   - Use secrets management in production

2. **Rate Limiting**
   - Default: 30 requests/minute per IP
   - Adjust based on your needs
   - Monitor for abuse

3. **Input Validation**
   - All inputs are sanitized
   - Query length limited to 500 chars
   - URL validation prevents SSRF

4. **Headers**
   - CSP policy configured
   - X-Frame-Options: SAMEORIGIN
   - X-Content-Type-Options: nosniff

5. **Dependencies**
   - All versions pinned
   - Regular security audits
   - Automated vulnerability scanning

### Legal Considerations

⚠️ **Web Scraping Disclaimer:**

This project scrapes MercadoLibre for educational purposes. Before deploying to production:

1. Review MercadoLibre's Terms of Service
2. Implement appropriate rate limiting
3. Add User-Agent identification
4. Consider using official APIs if available
5. Respect robots.txt
6. Add delays between requests

## 📚 Additional Documentation

- [Audit Report](docs/AUDIT_REPORT.md) - Complete security and performance audit
- [Runbook](docs/RUNBOOK.md) - Operations and incident response guide

## 📝 License

This project is for educational purposes. See LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linters
5. Submit a pull request

## 📧 Support

For issues and questions:
- Create a GitHub issue
- Check the [Troubleshooting](#troubleshooting) section
- Review the [Runbook](docs/RUNBOOK.md)

---

**Made with ❤️ by the GiftFinder Team**
