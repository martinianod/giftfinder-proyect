"""
Health check and metrics endpoints.
"""

import logging
from typing import Dict, Any
import httpx
from fastapi import APIRouter, status, Response
from app.cache import cache_stats
from app.config import get_settings


logger = logging.getLogger(__name__)
settings = get_settings()

router = APIRouter()


@router.get("/health")
async def health() -> Dict[str, str]:
    """
    Liveness check - basic health status.
    
    Returns:
        Simple healthy status
    """
    return {"status": "healthy"}


@router.get("/health/ready")
async def health_ready(response: Response) -> Dict[str, Any]:
    """
    Readiness check - verifies all dependencies are available.
    
    Returns:
        Detailed readiness status with dependency checks
    """
    checks = {}
    all_healthy = True
    
    # Check Ollama
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            ollama_response = await client.get(f"{settings.ollama_host}/api/tags")
            if ollama_response.status_code == 200:
                checks["ollama"] = {"status": "healthy", "url": settings.ollama_host}
            else:
                checks["ollama"] = {
                    "status": "unhealthy",
                    "error": f"Status code: {ollama_response.status_code}"
                }
                all_healthy = False
    except Exception as e:
        checks["ollama"] = {"status": "unhealthy", "error": str(e)}
        all_healthy = False
        logger.error(f"Ollama health check failed: {e}")
    
    # Check Cache
    try:
        stats = cache_stats()
        checks["cache"] = {"status": "healthy", "stats": stats}
    except Exception as e:
        checks["cache"] = {"status": "unhealthy", "error": str(e)}
        all_healthy = False
        logger.error(f"Cache health check failed: {e}")
    
    # Set response status code
    if not all_healthy:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    
    return {
        "status": "ready" if all_healthy else "not_ready",
        "checks": checks
    }


@router.get("/health/metrics")
async def health_metrics() -> Dict[str, Any]:
    """
    Basic service metrics.
    
    Returns:
        Service configuration and cache metrics
    """
    cache_info = cache_stats()
    
    return {
        "cache": cache_info,
        "config": {
            "max_concurrent_scrapes": settings.max_concurrent_scrapes,
            "llm_timeout_seconds": settings.llm_timeout_seconds,
            "scraping_timeout_seconds": settings.scraping_timeout_seconds,
            "cache_ttl_seconds": settings.cache_ttl_seconds,
            "rate_limit_per_minute": settings.rate_limit_per_minute
        }
    }
