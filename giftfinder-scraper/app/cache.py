"""
TTL-based caching with automatic eviction.
"""

import logging
from cachetools import TTLCache
from typing import Any, Optional
from app.config import get_settings


logger = logging.getLogger(__name__)
settings = get_settings()


# Global cache store with TTL and size limits
_cache_store: TTLCache = TTLCache(
    maxsize=settings.cache_max_size,
    ttl=settings.cache_ttl_seconds
)


def cache_get(key: str) -> Optional[Any]:
    """
    Get value from cache.
    
    Args:
        key: Cache key
        
    Returns:
        Cached value or None if not found or error
    """
    try:
        value = _cache_store.get(key)
        if value is not None:
            logger.debug(f"Cache hit: {key}")
        return value
    except Exception as e:
        logger.error(f"Cache get error for key {key}: {e}")
        return None


def cache_set(key: str, value: Any, ttl: Optional[int] = None) -> None:
    """
    Set value in cache.
    
    Args:
        key: Cache key
        value: Value to cache
        ttl: Optional TTL override (not used with TTLCache, but kept for compatibility)
    """
    try:
        _cache_store[key] = value
        logger.debug(f"Cache set: {key}")
    except Exception as e:
        logger.error(f"Cache set error for key {key}: {e}")


def cache_clear() -> None:
    """Clear all cache entries."""
    try:
        _cache_store.clear()
        logger.info("Cache cleared")
    except Exception as e:
        logger.error(f"Cache clear error: {e}")


def cache_stats() -> dict:
    """
    Get cache statistics.
    
    Returns:
        Dictionary with cache stats
    """
    try:
        return {
            "current_size": len(_cache_store),
            "max_size": _cache_store.maxsize,
            "ttl_seconds": _cache_store.ttl
        }
    except Exception as e:
        logger.error(f"Cache stats error: {e}")
        return {
            "current_size": 0,
            "max_size": settings.cache_max_size,
            "ttl_seconds": settings.cache_ttl_seconds
        }

