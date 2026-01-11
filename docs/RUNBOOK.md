# 📖 GiftFinder Operations Runbook

**Version:** 1.0  
**Last Updated:** January 11, 2024  
**Maintainers:** DevOps Team

---

## Table of Contents

1. [Deployment Procedures](#deployment-procedures)
2. [Monitoring](#monitoring)
3. [Troubleshooting](#troubleshooting)
4. [Maintenance](#maintenance)
5. [Incident Response](#incident-response)
6. [Rollback Procedures](#rollback-procedures)

---

## 1. Deployment Procedures

### 1.1 Initial Deployment

#### Prerequisites
- Docker & Docker Compose installed
- 8GB RAM minimum (12GB recommended)
- 4 CPU cores minimum
- 20GB disk space

#### Steps

1. **Clone repository**
   ```bash
   git clone https://github.com/martinianod/giftfinder-proyect.git
   cd giftfinder-proyect
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   
   # Edit with production values
   nano .env
   
   # CRITICAL: Change POSTGRES_PASSWORD
   sed -i 's/CHANGE_ME_IN_PRODUCTION/your_secure_password/g' .env
   ```

3. **Pull images and build**
   ```bash
   docker-compose pull
   docker-compose build
   ```

4. **Start services**
   ```bash
   docker-compose up -d
   ```

5. **Download Ollama model** (required, ~1GB)
   ```bash
   docker exec -it ollama ollama pull qwen2.5:1.5b
   
   # Verify model is available
   docker exec ollama ollama list
   ```

6. **Verify deployment**
   ```bash
   # Check all services are healthy
   docker-compose ps
   
   # Test scraper health
   curl http://localhost:8001/health/ready
   
   # Test backend health
   curl http://localhost:8080/actuator/health
   
   # Test search endpoint
   curl -X POST http://localhost:8001/scrape/search \
     -H "Content-Type: application/json" \
     -d '{"query": "test search"}'
   ```

### 1.2 Update Deployment

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart services
docker-compose up -d --build

# Verify health
docker-compose ps
```

### 1.3 Configuration Changes

```bash
# Edit .env file
nano .env

# Restart affected services
docker-compose restart scraper  # If scraper config changed
docker-compose restart backend  # If backend config changed

# Verify changes took effect
curl http://localhost:8001/health/metrics | jq .config
```

---

## 2. Monitoring

### 2.1 Health Checks

#### Automated Monitoring
Set up monitoring to check these endpoints every 30 seconds:

```bash
# Scraper liveness
curl http://localhost:8001/health

# Scraper readiness (checks Ollama)
curl http://localhost:8001/health/ready

# Backend health
curl http://localhost:8080/actuator/health
```

#### Manual Health Check Script
```bash
#!/bin/bash
# save as check_health.sh

echo "=== GiftFinder Health Check ==="
echo ""

echo "Scraper Health:"
curl -s http://localhost:8001/health | jq .
echo ""

echo "Scraper Ready:"
curl -s http://localhost:8001/health/ready | jq .
echo ""

echo "Backend Health:"
curl -s http://localhost:8080/actuator/health | jq .
echo ""

echo "Docker Services:"
docker-compose ps
```

### 2.2 Metrics Dashboard

#### Scraper Metrics
```bash
curl http://localhost:8001/health/metrics | jq .
```

**Key Metrics to Monitor:**
- `cache.size` - Current cache entries
- `cache.max_size` - Cache capacity
- `config.max_concurrent_scrapes` - Concurrency limit
- `config.cache_ttl_seconds` - Cache expiration

#### Resource Monitoring
```bash
# Real-time resource usage
docker stats

# CPU and memory by service
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

### 2.3 Log Monitoring

#### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f scraper

# With JSON parsing
docker-compose logs scraper | jq 'select(.level == "ERROR")'

# Search for specific request_id
docker-compose logs scraper | jq 'select(.request_id == "uuid-here")'
```

#### Log Queries
```bash
# Count errors in last 1000 lines
docker-compose logs --tail=1000 scraper | jq -r '.level' | grep ERROR | wc -l

# Find slow requests (>5 seconds)
docker-compose logs scraper | jq 'select(.duration_ms > 5000)'

# Monitor cache hits
docker-compose logs scraper | grep "Cache hit"
```

### 2.4 Alerting Thresholds

Set up alerts for:

| Metric | Warning | Critical |
|--------|---------|----------|
| Error Rate | >5% | >10% |
| Response Time (P95) | >5s | >10s |
| CPU Usage | >70% | >90% |
| Memory Usage | >80% | >95% |
| Cache Size | >800 | >950 |
| Ollama Downtime | >30s | >2min |

---

## 3. Troubleshooting

### 3.1 Scenario: Ollama Not Responding

**Symptoms:**
- `/health/ready` returns 503
- Logs show "Ollama health check failed"
- Searches fail or timeout

**Diagnosis:**
```bash
# Check Ollama container
docker logs ollama --tail 50

# Check if Ollama is running
docker ps | grep ollama

# Test Ollama API directly
curl http://localhost:11434/api/tags
```

**Resolution:**

**Option A: Restart Ollama**
```bash
docker-compose restart ollama

# Wait for it to be healthy
sleep 30
curl http://localhost:11434/api/tags
```

**Option B: Re-download Model**
```bash
docker exec ollama ollama list
docker exec ollama ollama pull qwen2.5:1.5b
```

**Option C: Recreate Container**
```bash
docker-compose stop ollama
docker-compose rm -f ollama
docker-compose up -d ollama

# Wait and download model
sleep 10
docker exec ollama ollama pull qwen2.5:1.5b
```

### 3.2 Scenario: Scraping Returns Empty Results

**Symptoms:**
- Search returns empty `recommendations` array
- Logs show "ML appears to be blocking scraping"

**Diagnosis:**
```bash
# Check recent scraper logs
docker-compose logs scraper --tail 100 | grep -i "blocked\|captcha\|antibot"

# Check if rate limiting is too aggressive
curl http://localhost:8001/health/metrics | jq .config.rate_limit_delay
```

**Resolution:**

**Option A: Increase Rate Limit Delay**
```bash
# Edit .env
nano .env

# Change RATE_LIMIT_DELAY from 1.0 to 2.0
RATE_LIMIT_DELAY=2.0

# Restart scraper
docker-compose restart scraper
```

**Option B: Wait and Retry**
```bash
# MercadoLibre might have temporarily blocked
# Wait 5-10 minutes and try again
sleep 300
```

**Option C: Update User Agent**
```python
# If needed, update User-Agent in ml_scraper.py
# to a more recent browser version
```

### 3.3 Scenario: High CPU Usage

**Symptoms:**
- `docker stats` shows >80% CPU
- Slow response times
- High load average

**Diagnosis:**
```bash
# Check which service is consuming CPU
docker stats --no-stream

# Check concurrent requests
docker-compose logs scraper | grep -c "Starting scrape"

# Check current config
curl http://localhost:8001/health/metrics | jq .config.max_concurrent_scrapes
```

**Resolution:**

**Option A: Reduce Concurrency**
```bash
# Edit .env
nano .env

# Reduce MAX_CONCURRENT_SCRAPES
MAX_CONCURRENT_SCRAPES=1

# Restart scraper
docker-compose restart scraper
```

**Option B: Limit Ollama Resources**
```yaml
# In docker-compose.yml
ollama:
  deploy:
    resources:
      limits:
        cpus: '1'  # Reduce from 2
```

**Option C: Scale Vertically**
```bash
# If running on cloud, increase instance size
# AWS: Resize EC2 instance
# Azure: Resize VM
# GCP: Resize Compute Engine
```

### 3.4 Scenario: Backend Cannot Connect to Scraper

**Symptoms:**
- Backend logs show connection errors
- Frontend shows errors when searching

**Diagnosis:**
```bash
# Check if scraper is running
docker ps | grep scraper

# Check scraper health
curl http://localhost:8001/health

# Check Docker network
docker network inspect giftfinder-proyect_giftfinder-network

# Test from backend container
docker exec giftfinder-backend curl http://scraper:8001/health
```

**Resolution:**

**Option A: Restart Scraper**
```bash
docker-compose restart scraper

# Wait for healthy
sleep 10
curl http://localhost:8001/health/ready
```

**Option B: Check Network Configuration**
```bash
# Recreate network
docker-compose down
docker-compose up -d

# Verify network exists
docker network ls | grep giftfinder
```

**Option C: Check Environment Variables**
```bash
# Verify SCRAPER_BASE_URL in backend
docker exec giftfinder-backend env | grep SCRAPER

# Should be: SCRAPER_BASE_URL=http://scraper:8001
```

### 3.5 Scenario: Cache Growing Uncontrolled

**Symptoms:**
- High memory usage
- Slow cache lookups
- Cache size approaching max

**Diagnosis:**
```bash
# Check cache stats
curl http://localhost:8001/health/metrics | jq .cache

# Check memory usage
docker stats giftfinder-scraper --no-stream
```

**Resolution:**

**Option A: Reduce TTL**
```bash
# Edit .env
CACHE_TTL_SECONDS=1800  # Reduce from 3600 to 30 min

# Restart to clear cache
docker-compose restart scraper
```

**Option B: Reduce Max Size**
```bash
# Edit .env
CACHE_MAX_SIZE=500  # Reduce from 1000

# Restart scraper
docker-compose restart scraper
```

**Option C: Clear Cache (Restart)**
```bash
# Restart clears in-memory cache
docker-compose restart scraper
```

### 3.6 Scenario: Logs Too Verbose

**Symptoms:**
- Disk filling up with logs
- Difficult to find relevant information

**Diagnosis:**
```bash
# Check log size
docker logs giftfinder-scraper 2>&1 | wc -l

# Check disk usage
df -h
```

**Resolution:**

**Option A: Reduce Log Level**
```bash
# Edit .env
LOG_LEVEL=WARNING  # Change from INFO

# Restart scraper
docker-compose restart scraper
```

**Option B: Configure Log Rotation**
```yaml
# Already configured in docker-compose.yml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

**Option C: Manually Clean Logs**
```bash
# Truncate logs
truncate -s 0 $(docker inspect --format='{{.LogPath}}' giftfinder-scraper)
```

---

## 4. Maintenance

### 4.1 Weekly Tasks

```bash
# Check disk usage
df -h

# Check log sizes
du -sh /var/lib/docker/containers/*

# Review error rates
docker-compose logs --since 7d scraper | jq -r '.level' | grep ERROR | wc -l

# Check for updates
git fetch
git status
```

### 4.2 Monthly Tasks

```bash
# Update dependencies
cd giftfinder-scraper
pip list --outdated

cd ../giftfinder-backend
./gradlew dependencyUpdates

cd ../giftfinder-frontend
npm outdated

# Security audit
cd giftfinder-scraper
safety check

# Backup database
docker exec giftfinder-postgres pg_dump -U giftfinder_user giftfinder > backup_$(date +%Y%m%d).sql
```

### 4.3 Backup & Restore

#### Backup
```bash
# Database backup
docker exec giftfinder-postgres pg_dump \
  -U ${POSTGRES_USER} \
  -d ${POSTGRES_DB} \
  > backup_$(date +%Y%m%d_%H%M%S).sql

# Compress
gzip backup_*.sql

# Upload to cloud storage
aws s3 cp backup_*.sql.gz s3://your-backup-bucket/
```

#### Restore
```bash
# Download backup
aws s3 cp s3://your-backup-bucket/backup_20240111.sql.gz .

# Decompress
gunzip backup_20240111.sql.gz

# Restore
docker exec -i giftfinder-postgres psql \
  -U ${POSTGRES_USER} \
  -d ${POSTGRES_DB} \
  < backup_20240111.sql
```

---

## 5. Incident Response

### 5.1 Incident Severity Levels

| Level | Description | Response Time |
|-------|-------------|---------------|
| **P0 - Critical** | Service completely down | 15 minutes |
| **P1 - High** | Major functionality broken | 1 hour |
| **P2 - Medium** | Partial functionality impaired | 4 hours |
| **P3 - Low** | Minor issues, workaround available | 24 hours |

### 5.2 Incident Response Checklist

#### Step 1: Assess (5 minutes)
```bash
# Check all services
docker-compose ps

# Check health endpoints
curl http://localhost:8001/health/ready
curl http://localhost:8080/actuator/health

# Check resource usage
docker stats --no-stream

# Check recent logs
docker-compose logs --tail=100
```

#### Step 2: Contain (10 minutes)
```bash
# If one service is causing issues, isolate it
docker-compose stop <problematic-service>

# If overloaded, reduce rate limit temporarily
# Edit .env: RATE_LIMIT_PER_MINUTE=10

# If memory leak, restart affected service
docker-compose restart <service>
```

#### Step 3: Diagnose (15 minutes)
```bash
# Check logs for errors
docker-compose logs <service> | grep ERROR

# Check metrics
curl http://localhost:8001/health/metrics

# Check external dependencies
curl http://localhost:11434/api/tags  # Ollama
```

#### Step 4: Resolve
- Follow troubleshooting scenarios above
- Apply fixes from this runbook
- Escalate if needed

#### Step 5: Verify (10 minutes)
```bash
# Run health checks
./check_health.sh

# Test end-to-end
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{"query": "test after fix"}'

# Monitor for 10 minutes
watch -n 10 'docker stats --no-stream'
```

#### Step 6: Document
- Create incident report
- Update this runbook if needed
- Share lessons learned

---

## 6. Rollback Procedures

### 6.1 Application Rollback

```bash
# Stop current version
docker-compose down

# Checkout previous version
git checkout <previous-commit>

# Rebuild and start
docker-compose up -d --build

# Verify
curl http://localhost:8001/health/ready
```

### 6.2 Database Rollback

```bash
# Restore from backup (see section 4.3)
docker exec -i giftfinder-postgres psql \
  -U ${POSTGRES_USER} \
  -d ${POSTGRES_DB} \
  < backup_before_change.sql

# Restart backend to reconnect
docker-compose restart backend
```

### 6.3 Configuration Rollback

```bash
# Restore previous .env
cp .env.backup .env

# Restart affected services
docker-compose restart

# Verify configuration
curl http://localhost:8001/health/metrics | jq .config
```

---

## 7. Contact Information

### On-Call Rotation
- **Primary:** [Your Name] - [Phone/Email]
- **Secondary:** [Backup Name] - [Phone/Email]

### Escalation Path
1. On-Call Engineer
2. Tech Lead
3. Engineering Manager

### External Resources
- **Ollama Documentation:** https://ollama.ai/docs
- **FastAPI Documentation:** https://fastapi.tiangolo.com
- **Spring Boot Documentation:** https://spring.io/projects/spring-boot

---

## 8. Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2024-01-11 | 1.0 | Initial runbook | DevOps Team |

---

**This runbook is a living document. Update it as you learn from incidents and operations.**
