"""
Health check endpoints for monitoring and orchestration.
"""
import httpx
from fastapi import APIRouter, status
from fastapi.responses import JSONResponse
from app.config import settings
from app.cache import get_cache_stats
from app.logging_config import get_logger

logger = get_logger(__name__)
router = APIRouter(prefix="/health", tags=["health"])


@router.get("")
async def health_check():
    """
    Basic liveness probe.
    Returns 200 if the service is running.
    """
    return {"status": "healthy", "service": settings.app_name}


@router.get("/ready")
async def readiness_check():
    """
    Readiness probe that checks dependencies.
    Returns 200 if ready to serve traffic, 503 if not ready.
    """
    # Check Ollama connectivity
    ollama_healthy = await check_ollama_health()
    
    if not ollama_healthy:
        logger.warning("Readiness check failed: Ollama not responding")
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={
                "status": "not_ready",
                "service": settings.app_name,
                "checks": {
                    "ollama": "unhealthy"
                }
            }
        )
    
    return {
        "status": "ready",
        "service": settings.app_name,
        "checks": {
            "ollama": "healthy"
        }
    }


@router.get("/metrics")
async def metrics():
    """
    Basic metrics endpoint.
    Returns cache statistics and configuration values.
    """
    cache_stats = get_cache_stats()
    
    return {
        "service": settings.app_name,
        "environment": settings.environment,
        "cache": cache_stats,
        "config": {
            "max_concurrent_scrapes": settings.max_concurrent_scrapes,
            "cache_ttl_seconds": settings.cache_ttl_seconds,
            "cache_max_size": settings.cache_max_size,
            "rate_limit_per_minute": settings.rate_limit_per_minute,
            "ollama_model": settings.ollama_model,
        }
    }


async def check_ollama_health() -> bool:
    """
    Check if Ollama service is responding.
    Returns True if healthy, False otherwise.
    """
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(f"{settings.ollama_host}/api/tags")
            return response.status_code == 200
    except Exception as e:
        logger.error(f"Ollama health check failed: {e}")
        return False
