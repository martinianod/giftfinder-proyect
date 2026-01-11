# 📘 GiftFinder Operational Runbook

**Version**: 1.0  
**Last Updated**: January 2026  
**Maintainers**: DevOps Team

This runbook provides step-by-step procedures for deploying, monitoring, troubleshooting, and maintaining the GiftFinder application in production.

---

## 📑 Table of Contents

1. [Deployment](#-deployment)
2. [Monitoring](#-monitoring)
3. [Troubleshooting](#-troubleshooting)
4. [Updates & Maintenance](#-updates--maintenance)
5. [Incident Response](#-incident-response)
6. [Weekly Checklist](#-weekly-checklist)

---

## 🚀 Deployment

### Initial Production Deployment

#### Prerequisites

- Server with Docker 24+ and Docker Compose 2+
- 8GB+ RAM, 4+ CPU cores
- 50GB+ disk space
- Domain name configured
- SSL certificates ready

#### Step-by-Step Procedure

**1. Prepare Server**

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Logout and login for group changes to take effect
```

**2. Clone Repository**

```bash
cd /opt
sudo git clone https://github.com/martinianod/giftfinder-proyect.git
cd giftfinder-proyect
```

**3. Configure Environment**

```bash
# Copy and edit environment file
sudo cp .env.example .env
sudo nano .env

# Required changes:
# - Set strong POSTGRES_PASSWORD
# - Configure ALLOWED_ORIGINS for your domain
# - Adjust LOG_LEVEL=WARNING for production
# - Review rate limits based on expected traffic
```

**4. Initialize Services**

```bash
# Start services
sudo docker-compose up -d

# Wait for services to become healthy (2-5 minutes)
sudo docker-compose ps

# Check that all services show "healthy" status
```

**5. Initialize Ollama Model**

```bash
# Pull the LLM model (this takes 5-10 minutes)
sudo docker-compose exec ollama ollama pull qwen2.5:1.5b

# Verify model is available
sudo docker-compose exec ollama ollama list
```

**6. Verify Deployment**

```bash
# Test health endpoints
curl http://localhost:8001/health
curl http://localhost:8001/health/ready

# Test scraper functionality
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{"query": "auriculares bluetooth"}'

# Check frontend is accessible
curl http://localhost:5173
```

**7. Setup Reverse Proxy** (nginx example)

```bash
# Install nginx
sudo apt install nginx -y

# Create configuration
sudo nano /etc/nginx/sites-available/giftfinder
```

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;
    
    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    
    # Frontend
    location / {
        proxy_pass http://localhost:5173;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Rate limiting
        limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
        limit_req zone=api burst=20 nodelay;
    }
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/giftfinder /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

**8. Setup Log Rotation**

```bash
# Docker logs are already rotated (10MB, 3 files)
# Verify configuration
docker inspect giftfinder-scraper | jq '.[0].HostConfig.LogConfig'
```

**9. Final Verification**

- [ ] All services healthy: `docker-compose ps`
- [ ] Scraper health check passes: `curl https://yourdomain.com/api/health/ready`
- [ ] Frontend loads in browser
- [ ] Can perform end-to-end search
- [ ] Logs are being generated: `docker-compose logs -f`

---

## 📊 Monitoring

### Key Metrics to Track

#### 1. Service Health

```bash
# Check all services status
docker-compose ps

# Detailed health check
curl http://localhost:8001/health/ready | jq .

# Expected output:
# {
#   "status": "ready",
#   "checks": {
#     "ollama": {"status": "healthy"},
#     "cache": {"status": "healthy", "stats": {...}}
#   }
# }
```

**Alert if**: Any service shows unhealthy for >5 minutes

#### 2. Service Metrics

```bash
# Get scraper metrics
curl http://localhost:8001/health/metrics | jq .

# Key metrics:
# - cache.current_size (should be < max_size)
# - config.max_concurrent_scrapes
# - config.rate_limit_per_minute
```

**Alert if**:
- Cache size == max_size (need to increase)
- Metrics endpoint returns 500

#### 3. Response Times

```bash
# Monitor scraper logs for duration
docker-compose logs scraper -f | jq 'select(.duration_ms) | {path: .path, duration_ms: .duration_ms}'

# Typical response times:
# - /health: < 10ms
# - /health/ready: < 5000ms (includes Ollama check)
# - /scrape/search: 3000-8000ms
```

**Alert if**: Average response time > 10s for searches

#### 4. Error Rates

```bash
# Count errors in last hour
docker-compose logs scraper --since 1h | jq 'select(.level=="ERROR")' | wc -l

# View recent errors
docker-compose logs scraper --tail 100 | jq 'select(.level=="ERROR")'
```

**Alert if**: Error rate > 5% of total requests

#### 5. Resource Usage

```bash
# Check container resources
docker stats --no-stream

# Expected usage:
# - ollama: ~2GB RAM, 50-100% CPU (during LLM calls)
# - scraper: ~200-500MB RAM, 10-30% CPU
# - backend: ~500MB-1GB RAM, 10-20% CPU
# - postgres: ~100-300MB RAM, 5-10% CPU
```

**Alert if**:
- Any service at memory limit
- Sustained CPU > 80% for >5 minutes

#### 6. Disk Space

```bash
# Check Docker volumes
docker system df -v

# Check logs size
du -sh /var/lib/docker/containers/*
```

**Alert if**: Disk usage > 80%

### Monitoring Commands Reference

```bash
# View logs by service
docker-compose logs -f scraper
docker-compose logs -f backend
docker-compose logs -f ollama

# Filter by log level
docker-compose logs scraper | jq 'select(.level=="ERROR")'
docker-compose logs scraper | jq 'select(.level=="WARNING")'

# Track specific request
docker-compose logs scraper | jq 'select(.request_id=="abc-123")'

# Monitor in real-time with pretty formatting
docker-compose logs -f scraper | jq -C .

# Get last N errors
docker-compose logs scraper --tail 1000 | jq 'select(.level=="ERROR")' | tail -20
```

### Recommended Monitoring Setup

**Option 1: Simple Monitoring Script**

```bash
#!/bin/bash
# save as /opt/giftfinder-proyect/monitor.sh

# Check health
HEALTH=$(curl -s http://localhost:8001/health/ready | jq -r '.status')
if [ "$HEALTH" != "ready" ]; then
    echo "ALERT: Service not ready - $HEALTH"
    # Send alert (email, Slack, etc.)
fi

# Check error rate
ERRORS=$(docker-compose logs scraper --since 1h | jq 'select(.level=="ERROR")' | wc -l)
TOTAL=$(docker-compose logs scraper --since 1h | jq 'select(.level)' | wc -l)
if [ $TOTAL -gt 0 ]; then
    ERROR_RATE=$(echo "scale=2; $ERRORS * 100 / $TOTAL" | bc)
    if (( $(echo "$ERROR_RATE > 5" | bc -l) )); then
        echo "ALERT: High error rate - $ERROR_RATE%"
    fi
fi

# Add to crontab: */5 * * * * /opt/giftfinder-proyect/monitor.sh
```

**Option 2: Professional Monitoring (Recommended)**

- **Prometheus + Grafana**: Metrics collection and visualization
- **ELK Stack**: Log aggregation and analysis
- **Datadog/New Relic**: Full observability platform
- **PagerDuty**: Incident alerting

---

## 🔧 Troubleshooting

### Problem 1: Ollama Not Responding

**Symptoms**:
- Scraper returns 503 errors
- Health check shows Ollama unhealthy
- Logs show "LLM request timeout" or "LLM service unavailable"

**Diagnosis**:

```bash
# Check Ollama health directly
docker-compose exec ollama curl http://localhost:11434/api/tags

# Check Ollama logs
docker-compose logs ollama --tail 100

# Check if model is loaded
docker-compose exec ollama ollama list
```

**Solutions**:

```bash
# Solution 1: Restart Ollama
docker-compose restart ollama
sleep 30  # Wait for startup
curl http://localhost:11434/api/tags

# Solution 2: Reload model
docker-compose exec ollama ollama pull qwen2.5:1.5b

# Solution 3: Check resources
docker stats --no-stream ollama
# If memory is maxed out, increase limit in docker-compose.yml

# Solution 4: Full restart
docker-compose down
docker-compose up -d ollama
# Wait for healthy, then start other services
docker-compose up -d
```

**Prevention**:
- Monitor Ollama memory usage
- Ensure model is cached after first load
- Set appropriate resource limits

---

### Problem 2: Scraping Returns Empty Results

**Symptoms**:
- Query succeeds (200 OK)
- `recommendations` array is empty
- No errors in logs

**Diagnosis**:

```bash
# Check recent scraping attempts
docker-compose logs scraper --tail 100 | jq 'select(.message | contains("Scraping completed"))'

# Look for anti-bot detection
docker-compose logs scraper --tail 200 | jq 'select(.message | contains("anti-bot"))'

# Check cache hit rate
curl http://localhost:8001/health/metrics | jq .cache
```

**Solutions**:

```bash
# Solution 1: Clear cache (might be stale)
docker-compose restart scraper

# Solution 2: Test with simple keyword
curl -X POST http://localhost:8001/scrape/search \
  -H "Content-Type: application/json" \
  -d '{"query": "auriculares"}' | jq .

# Solution 3: Check if IP is blocked
# Look for "blocked", "captcha", "robot" in logs
docker-compose logs scraper | grep -i "blocked\|captcha\|robot"

# Solution 4: Reduce scraping rate
# Edit .env: MAX_CONCURRENT_SCRAPES=1
docker-compose restart scraper

# Solution 5: Wait and retry (might be temporary block)
sleep 300  # Wait 5 minutes
```

**Prevention**:
- Keep scraping rate respectful (1 req/sec)
- Use cache aggressively (1 hour TTL)
- Monitor for block signals in logs
- Rotate user agents if needed (not implemented)

---

### Problem 3: High CPU Usage

**Symptoms**:
- Docker containers consuming >80% CPU
- Slow response times
- System load high

**Diagnosis**:

```bash
# Identify which service
docker stats --no-stream

# Check concurrent operations
curl http://localhost:8001/health/metrics | jq .config.max_concurrent_scrapes

# Check for runaway processes
docker-compose exec scraper ps aux
```

**Solutions**:

```bash
# Solution 1: Reduce concurrency
# Edit .env: MAX_CONCURRENT_SCRAPES=2
docker-compose restart scraper

# Solution 2: Check for infinite loops
docker-compose logs scraper --tail 500 | jq 'select(.duration_ms > 10000)'

# Solution 3: Restart services
docker-compose restart

# Solution 4: Increase timeouts to prevent retries
# Edit .env: LLM_TIMEOUT_SECONDS=20, SCRAPING_TIMEOUT_SECONDS=15
docker-compose restart scraper
```

**Prevention**:
- Set appropriate concurrency limits
- Monitor CPU trends
- Set resource limits in docker-compose.yml

---

### Problem 4: Backend Cannot Connect to Scraper

**Symptoms**:
- Backend logs show connection errors
- Frontend shows "Service unavailable"
- Backend health check fails

**Diagnosis**:

```bash
# Check scraper health
curl http://localhost:8001/health

# Test from backend container
docker-compose exec backend curl http://scraper:8001/health

# Check network
docker network inspect giftfinder-proyect_giftfinder-network
```

**Solutions**:

```bash
# Solution 1: Verify scraper is healthy
docker-compose ps scraper
curl http://localhost:8001/health/ready

# Solution 2: Restart in correct order
docker-compose restart scraper
sleep 30  # Wait for healthy
docker-compose restart backend

# Solution 3: Check environment variables
docker-compose exec backend env | grep SCRAPER_BASE_URL
# Should be: http://scraper:8001

# Solution 4: Recreate network
docker-compose down
docker-compose up -d
```

**Prevention**:
- Use health checks in depends_on
- Monitor service startup order
- Test inter-service connectivity

---

### Problem 5: Cache Growing Unbounded

**Symptoms**:
- Memory usage increases over time
- Eventually hits memory limit
- Service becomes slow or crashes

**Note**: This should NOT happen with TTLCache, but if it does:

**Diagnosis**:

```bash
# Check cache stats
curl http://localhost:8001/health/metrics | jq .cache

# Expected: current_size < max_size
# If current_size == max_size, cache is working correctly (LRU eviction)
# If memory still grows, there's a different leak
```

**Solutions**:

```bash
# Solution 1: Restart to clear cache
docker-compose restart scraper

# Solution 2: Reduce cache size
# Edit .env: Add CACHE_MAX_SIZE=50
docker-compose restart scraper

# Solution 3: Check for other memory leaks
docker stats --no-stream scraper
# Monitor over time

# Solution 4: Reduce TTL
# Edit .env: CACHE_TTL_SECONDS=1800 (30 minutes)
docker-compose restart scraper
```

**Prevention**:
- TTLCache handles this automatically
- Monitor memory trends
- Set appropriate limits

---

### Problem 6: Verbose Logs Filling Disk

**Symptoms**:
- Disk usage growing rapidly
- Logs difficult to parse
- Performance impact from logging

**Diagnosis**:

```bash
# Check logs size
docker system df

# Check individual container logs
du -sh /var/lib/docker/containers/*/

# Check log level
docker-compose exec scraper env | grep LOG_LEVEL
```

**Solutions**:

```bash
# Solution 1: Reduce log level
# Edit .env: LOG_LEVEL=WARNING
docker-compose restart scraper

# Solution 2: Check log rotation is working
docker inspect giftfinder-scraper | jq '.[0].HostConfig.LogConfig'
# Should show: max-size: 10m, max-file: 3

# Solution 3: Manually clean old logs
docker-compose down
docker system prune -a
docker-compose up -d

# Solution 4: Configure external log shipping
# Ship logs to ELK/Splunk and reduce local retention
```

**Prevention**:
- Use WARNING level in production
- Ensure log rotation is configured
- Ship logs to external system

---

## 🔄 Updates & Maintenance

### Rolling Updates

**Procedure for Zero-Downtime Updates**:

```bash
# 1. Pull latest code
cd /opt/giftfinder-proyect
git fetch origin
git checkout <new-version-tag>

# 2. Review changes
git log HEAD..origin/main --oneline

# 3. Backup database
docker-compose exec postgres pg_dump -U giftfinder_user giftfinder > backup-$(date +%Y%m%d).sql

# 4. Update one service at a time
docker-compose up -d --no-deps --build scraper
# Wait for health check to pass
sleep 30
curl http://localhost:8001/health/ready

docker-compose up -d --no-deps --build backend
sleep 30

docker-compose up -d --no-deps --build frontend

# 5. Verify all services
docker-compose ps
curl http://localhost:8001/health/ready
```

### Rollback Procedure

```bash
# 1. Checkout previous version
git checkout <previous-version-tag>

# 2. Rebuild and restart
docker-compose down
docker-compose up -d

# 3. Restore database if needed
docker-compose exec -T postgres psql -U giftfinder_user giftfinder < backup-YYYYMMDD.sql

# 4. Verify rollback
docker-compose ps
curl http://localhost:8001/health/ready
```

### Database Migrations

```bash
# Backend automatically runs migrations on startup
# Check migration status
docker-compose logs backend | grep -i "migration"

# Manual migration (if needed)
docker-compose exec backend ./gradlew flywayMigrate
```

### Dependency Updates

```bash
# Update Python dependencies
cd giftfinder-scraper
pip install --upgrade -r requirements.txt
pip freeze > requirements.txt

# Update Node dependencies
cd giftfinder-frontend
npm update
npm audit fix

# Update Java dependencies
cd giftfinder-backend
./gradlew dependencyUpdates
```

---

## 🚨 Incident Response

### Incident Response Protocol

**Phase 1: Detection** (0-5 minutes)

1. Alert received or issue reported
2. Verify issue is real (check health endpoints)
3. Assess severity:
   - **P0**: Complete service outage
   - **P1**: Major functionality broken
   - **P2**: Degraded performance
   - **P3**: Minor issue, workaround available

**Phase 2: Triage** (5-15 minutes)

1. Check service status: `docker-compose ps`
2. Review recent logs: `docker-compose logs --tail 200`
3. Check resource usage: `docker stats`
4. Identify affected component
5. Document timeline and observations

**Phase 3: Mitigation** (15-30 minutes)

1. Apply quick fix if known:
   - Restart affected service
   - Clear cache
   - Scale back rate limits
2. Implement workaround if possible
3. Update status page/notify users

**Phase 4: Resolution** (30+ minutes)

1. Implement permanent fix
2. Test thoroughly in staging
3. Deploy to production
4. Monitor for regression
5. Update documentation

**Phase 5: Post-Mortem** (24-48 hours after)

1. Document root cause
2. Timeline of events
3. What went well
4. What could be improved
5. Action items to prevent recurrence

### Emergency Contacts

- **DevOps Lead**: [contact info]
- **Backend Team**: [contact info]
- **Scraper Team**: [contact info]
- **On-Call**: [rotation schedule]

---

## ✅ Weekly Checklist

### Every Monday Morning

- [ ] Review health status: `curl http://localhost:8001/health/ready`
- [ ] Check error rates: `docker-compose logs scraper --since 7d | jq 'select(.level=="ERROR")' | wc -l`
- [ ] Review resource usage: `docker stats --no-stream`
- [ ] Check disk space: `df -h`
- [ ] Review cache hit rate: `curl http://localhost:8001/health/metrics | jq .cache`
- [ ] Check for available updates: `git fetch && git log HEAD..origin/main`
- [ ] Backup database: `docker-compose exec postgres pg_dump -U giftfinder_user giftfinder > backup-$(date +%Y%m%d).sql`
- [ ] Review logs for unusual patterns
- [ ] Test critical user flows
- [ ] Update runbook if needed

### Every Month

- [ ] Review and rotate logs older than 30 days
- [ ] Update dependencies (security patches)
- [ ] Review and update documentation
- [ ] Conduct disaster recovery test
- [ ] Review and update monitoring alerts
- [ ] Performance benchmarking
- [ ] Security scan: `docker-compose run --rm trivy fs .`

---

## 📝 Additional Resources

- [Architecture Diagram](../README.md#architecture)
- [API Documentation](http://localhost:8001/docs)
- [Audit Report](./AUDIT_REPORT.md)
- [Configuration Reference](../README.md#configuration)

---

**Document Version**: 1.0  
**Last Review**: January 2026  
**Next Review**: April 2026

---

*This runbook is a living document. Please update it as procedures change or new issues are discovered.*
