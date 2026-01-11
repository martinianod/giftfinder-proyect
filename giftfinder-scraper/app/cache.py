"""
Thread-safe cache with TTL and LRU eviction policy.
"""
from cachetools import TTLCache
from threading import Lock
from typing import Any, Optional
from app.logging_config import get_logger

logger = get_logger(__name__)

# Global cache instance with TTL and LRU eviction
# Will be initialized in setup_cache()
cache_store: Optional[TTLCache] = None
cache_lock = Lock()


def setup_cache(max_size: int = 1000, ttl: int = 3600) -> None:
    """
    Initialize cache with specified size and TTL.
    
    Args:
        max_size: Maximum number of items in cache
        ttl: Time to live in seconds
    """
    global cache_store
    cache_store = TTLCache(maxsize=max_size, ttl=ttl)
    logger.info(f"Cache initialized with max_size={max_size}, ttl={ttl}s")


def cache_get(key: str) -> Any:
    """
    Get value from cache.
    Thread-safe operation.
    
    Args:
        key: Cache key
    
    Returns:
        Cached value or None if not found
    """
    if cache_store is None:
        return None
    
    try:
        with cache_lock:
            return cache_store.get(key)
    except Exception as e:
        logger.error(f"Cache get error for key {key}: {e}")
        return None


def cache_set(key: str, value: Any, ttl: Optional[int] = None) -> None:
    """
    Set value in cache.
    Thread-safe operation.
    
    Args:
        key: Cache key
        value: Value to cache
        ttl: Optional TTL override (not used with cachetools.TTLCache)
    """
    if cache_store is None:
        return
    
    try:
        with cache_lock:
            cache_store[key] = value
    except Exception as e:
        logger.error(f"Cache set error for key {key}: {e}")


def get_cache_stats() -> dict:
    """
    Get cache statistics.
    
    Returns:
        Dictionary with cache stats
    """
    if cache_store is None:
        return {
            "size": 0,
            "max_size": 0,
            "hit_rate": 0.0
        }
    
    try:
        with cache_lock:
            return {
                "size": len(cache_store),
                "max_size": cache_store.maxsize,
                "ttl_seconds": cache_store.ttl
            }
    except Exception as e:
        logger.error(f"Cache stats error: {e}")
        return {
            "size": 0,
            "max_size": 0,
            "error": str(e)
        }


def clear_cache() -> None:
    """Clear all items from cache."""
    if cache_store is None:
        return
    
    try:
        with cache_lock:
            cache_store.clear()
        logger.info("Cache cleared")
    except Exception as e:
        logger.error(f"Cache clear error: {e}")
