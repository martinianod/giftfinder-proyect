# GiftFinder Local Setup Guide 🎁

This guide will help you set up and run the GiftFinder application locally on your Mac (or any system with Docker).

## Prerequisites

Before you begin, make sure you have:

- **Docker Desktop** installed and running
  - Download from: https://www.docker.com/products/docker-desktop/
  - Minimum version: 20.10+
  - Make sure Docker Desktop is started before proceeding

- **At least 8GB RAM** available for Docker
- **At least 10GB free disk space**

## Quick Start (TL;DR)

```bash
# Clone the repository (if not already done)
git clone https://github.com/martinianod/giftfinder-proyect.git
cd giftfinder-proyect

# Start everything
docker compose up --build

# Wait for services to start (2-3 minutes)
# Access the app at: http://localhost:5173
```

## Detailed Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/martinianod/giftfinder-proyect.git
cd giftfinder-proyect
```

### 2. Create Environment File

The `.env` file should already exist. If not, create it from the example:

```bash
cp .env.example .env
```

**Important:** For local development, the default values in `.env` are fine. **DO NOT** use these defaults in production!

### 3. Start the Application

Run this single command to start all services:

```bash
docker compose up --build
```

This will:
- Build the backend (Java/Spring Boot)
- Build the frontend (React/Vite)
- Start PostgreSQL database
- Start the Ollama service (for AI features)
- Start the scraper service

**Expected startup time:** 2-4 minutes (first time may take longer to download images)

### 4. Verify Services are Running

Open a new terminal and check the services:

```bash
docker compose ps
```

You should see all services in "Up" state:
- `giftfinder-backend` - Port 8080
- `giftfinder-frontend` - Port 5173
- `giftfinder-postgres` - Port 5432
- `giftfinder-scraper` - Port 8001
- `ollama` - Port 11434

### 5. Access the Application

**Frontend (UI):** http://localhost:5173

**Backend API:** http://localhost:8080

**Health Check:** http://localhost:8080/actuator/health

## Running Smoke Tests

After the services are up, run the automated smoke tests:

```bash
./scripts/smoke.sh
```

This will test:
- ✅ Backend health endpoint
- ✅ Frontend accessibility
- ✅ User signup
- ✅ User login
- ✅ Duplicate email handling (409 error)
- ✅ Invalid credentials (401 error)
- ✅ Validation errors (400 error)

**Expected output:** All tests should pass with green checkmarks ✓

## Viewing Logs

### View all logs together:
```bash
docker compose logs -f
```

### View logs for a specific service:

**Backend:**
```bash
docker compose logs -f backend
```

**Frontend:**
```bash
docker compose logs -f frontend
```

**Database:**
```bash
docker compose logs -f postgres
```

**Scraper:**
```bash
docker compose logs -f scraper
```

**Ollama:**
```bash
docker compose logs -f ollama
```

### View last 100 lines:
```bash
docker compose logs --tail=100 backend
```

### Save logs to file:
```bash
docker compose logs backend > backend-logs.txt
```

## Stopping the Application

To stop all services:

```bash
docker compose down
```

To stop and remove volumes (deletes database data):

```bash
docker compose down -v
```

## Resetting the Application

If you want to start fresh (clear all data):

```bash
# Stop and remove everything including volumes
docker compose down -v

# Remove built images (optional, forces rebuild)
docker compose down --rmi all -v

# Start fresh
docker compose up --build
```

## Common Issues and Solutions

### 1. Port Already in Use

**Problem:** Error like `port 8080 is already allocated`

**Solution:**
```bash
# Find what's using the port (Mac)
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change the port in .env file
BACKEND_PORT=8081
```

### 2. Database Connection Errors

**Problem:** Backend shows `Connection refused` or `database does not exist`

**Solution:**
```bash
# Wait longer - database might still be initializing
# Check postgres logs:
docker compose logs postgres

# Restart just the backend:
docker compose restart backend
```

### 3. "Out of Memory" Errors

**Problem:** Docker shows memory errors

**Solution:**
- Open Docker Desktop → Settings → Resources
- Increase Memory to at least 8GB
- Click "Apply & Restart"

### 4. Frontend Shows Blank Page

**Problem:** Frontend loads but shows blank screen

**Solution:**
```bash
# Check browser console for errors
# Verify VITE_API_URL in logs:
docker compose logs frontend | grep VITE_API_URL

# Rebuild frontend:
docker compose up --build frontend
```

### 5. Signup Returns 500 Error

**Problem:** POST /api/auth/signup returns Internal Server Error

**Solution:**
```bash
# Check backend logs with request ID:
docker compose logs backend | grep "requestId"

# Verify database connection:
docker compose exec postgres psql -U giftfinder_user -d giftfinder -c "\dt"

# Restart backend:
docker compose restart backend
```

### 6. CORS Errors in Browser Console

**Problem:** Browser shows CORS policy errors

**Solution:**
- Verify `CORS_ALLOWED_ORIGINS` in `.env` includes `http://localhost:5173`
- Restart backend: `docker compose restart backend`
- Clear browser cache and reload

### 7. Slow Performance on Mac M1/M2

**Problem:** Services are very slow to start or run

**Solution:**
- Ensure you're using native ARM images (they should be pulled automatically)
- Increase Docker resources in Docker Desktop settings
- Close other resource-intensive applications

## Understanding the Services

### Backend (Port 8080)
- **Technology:** Java 21 + Spring Boot
- **Features:** REST API, JWT auth, PostgreSQL
- **Logs:** Structured with request IDs
- **Health:** http://localhost:8080/actuator/health

### Frontend (Port 5173)
- **Technology:** React + Vite
- **Features:** SPA, responsive UI
- **Config:** API URL via VITE_API_URL env var

### Database (Port 5432)
- **Technology:** PostgreSQL 15
- **Database:** giftfinder
- **User:** giftfinder_user
- **Password:** CHANGE_ME_IN_PRODUCTION (local only!)

### Scraper (Port 8001)
- **Technology:** Python + FastAPI
- **Features:** Web scraping with AI processing
- **Health:** http://localhost:8001/health

### Ollama (Port 11434)
- **Technology:** Ollama with qwen2.5:1.5b model
- **Features:** Local AI model for gift suggestions

## Testing the API Manually

### Signup:
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"password123"}'
```

**Expected Response (200):**
```json
{
  "token": "eyJhbGc...",
  "name": "John Doe",
  "email": "john@example.com"
}
```

### Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}'
```

**Expected Response (200):**
```json
{
  "token": "eyJhbGc...",
  "name": "John Doe",
  "email": "john@example.com"
}
```

### Duplicate Email (should fail):
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane","email":"john@example.com","password":"pass123"}'
```

**Expected Response (409):**
```json
{
  "success": false,
  "error": "Email already registered",
  "code": "DUPLICATE_EMAIL",
  "requestId": "uuid-here",
  "timestamp": "2024-01-01T12:00:00Z",
  "path": "/api/auth/signup"
}
```

### Health Check:
```bash
curl http://localhost:8080/actuator/health
```

**Expected Response (200):**
```json
{
  "status": "UP"
}
```

## Accessing the Database

Connect to PostgreSQL directly:

```bash
docker compose exec postgres psql -U giftfinder_user -d giftfinder
```

Useful commands:
```sql
-- List tables
\dt

-- View users
SELECT id, email, name, role FROM users;

-- Exit
\q
```

## Development Tips

### Hot Reload

- **Frontend:** Changes auto-reload (Vite HMR)
- **Backend:** Requires rebuild: `docker compose up --build backend`

### Debugging Backend

Add this to docker-compose.yml under backend environment:
```yaml
JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Then connect your IDE debugger to `localhost:5005`

### Viewing Request IDs

All backend logs include `[requestId]` for request correlation:
```
2024-01-01 12:00:00.000 [http-nio-8080-exec-1] [abc-123-def] INFO  c.f.g.auth.service.AuthServiceImpl - Signup attempt for email: test@example.com
```

Use this to trace a request through the logs.

## Architecture Overview

```
┌─────────────────┐
│   Browser       │
│ localhost:5173  │
└────────┬────────┘
         │ HTTP
         v
┌─────────────────┐
│   Frontend      │
│   (Nginx)       │
└────────┬────────┘
         │ HTTP /api
         v
┌─────────────────┐      ┌──────────────┐
│   Backend       │─────>│  PostgreSQL  │
│  (Spring Boot)  │      │   Database   │
└────────┬────────┘      └──────────────┘
         │
         v
┌─────────────────┐      ┌──────────────┐
│   Scraper       │─────>│   Ollama     │
│  (FastAPI)      │      │  (AI Model)  │
└─────────────────┘      └──────────────┘
```

## Default Credentials

**Test User (create via signup):**
- Email: any valid email
- Password: minimum 6 characters

**Database:**
- Host: localhost
- Port: 5432
- Database: giftfinder
- User: giftfinder_user
- Password: CHANGE_ME_IN_PRODUCTION

## Security Notes

⚠️ **WARNING:** The default configuration is for LOCAL DEVELOPMENT ONLY!

Never use these settings in production:
- Default passwords
- Debug logging
- Open CORS policy
- Weak JWT secret

## Getting Help

1. **Check logs first:** `docker compose logs -f`
2. **Check this guide's troubleshooting section**
3. **Verify services are running:** `docker compose ps`
4. **Check service health:** http://localhost:8080/actuator/health
5. **Run smoke tests:** `./scripts/smoke.sh`

## Next Steps

- ✅ Services running? Great!
- ✅ Smoke tests passing? Excellent!
- ✅ Try the UI at http://localhost:5173
- ✅ Create a test account via signup
- ✅ Explore the gift finder features

Happy coding! 🎉
