from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from typing import List

from app.validation import SearchRequest
from app.ai_local import parse_query, LLMError
from app.ml_scraper import scrape_mercadolibre
from app.schemas import ScrapedProduct
from app.models.scraper_response import ScraperResponse, InterpretedIntent, ScrapedProductResponse
from app.config import settings
from app.logging_config import setup_logging, get_logger, get_request_id
from app.middleware import RequestIdMiddleware
from app.cache import setup_cache
from app.health import router as health_router

# Setup logging
setup_logging(log_level=settings.log_level, json_logs=settings.log_json)
logger = get_logger(__name__)

# Setup rate limiter
limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan events for startup and shutdown."""
    # Startup
    logger.info(
        "Starting application",
        extra={
            'app_name': settings.app_name,
            'environment': settings.environment,
            'ollama_host': settings.ollama_host,
            'ollama_model': settings.ollama_model,
        }
    )
    
    # Initialize cache
    setup_cache(max_size=settings.cache_max_size, ttl=settings.cache_ttl_seconds)
    
    logger.info("Application startup complete")
    
    yield
    
    # Shutdown
    logger.info("Shutting down application")


app = FastAPI(
    title=settings.app_name,
    lifespan=lifespan
)

# Add middleware
app.add_middleware(RequestIdMiddleware)

# Add rate limiter
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Include health router
app.include_router(health_router)

# ============================================================
# GLOBAL EXCEPTION HANDLER
# ============================================================

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler with proper error responses."""
    request_id = get_request_id()
    
    logger.error(
        f"Unhandled exception: {str(exc)}",
        extra={
            'path': request.url.path,
            'method': request.method,
            'error_type': type(exc).__name__,
        },
        exc_info=True
    )
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "Internal server error",
            "message": "An unexpected error occurred",
            "request_id": request_id
        }
    )


# ============================================================
# ENDPOINTS
# ============================================================

@app.get("/")
def root():
    logger.debug("Root endpoint called")
    return {
        "status": "scraper online",
        "service": settings.app_name,
        "version": "1.0.0"
    }


@app.post("/scrape/search", response_model=ScraperResponse)
@limiter.limit(f"{settings.rate_limit_per_minute}/minute")
async def scrape_search(request: Request, req: SearchRequest):
    """
    Search and scrape products from MercadoLibre.
    Rate limited to prevent abuse.
    """
    original_query = req.query

    logger.info(f"New search request", extra={'query': original_query})

    try:
        parsed = parse_query(original_query) or {}
    except LLMError as e:
        logger.error(f"LLM error during query parsing: {e}")
        parsed = {
            "recipientType": "unknown",
            "age": None,
            "budgetMin": None,
            "budgetMax": None,
            "interests": [original_query]
        }
    except Exception as e:
        logger.error(f"Unexpected error during query parsing: {e}", exc_info=True)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "error": "Query parsing failed",
                "message": str(e),
                "request_id": get_request_id()
            }
        )

    interpreted = {
        "recipient": parsed.get("recipientType"),
        "age": parsed.get("age"),
        "budgetMin": parsed.get("budgetMin"),
        "budgetMax": parsed.get("budgetMax"),
        "interests": parsed.get("interests", [])
    }

    interests = parsed.get("interests", [])
    logger.info(f"Detected interests", extra={'interests': interests, 'count': len(interests)})

    keyword = interests[0].replace(" ", "-") if interests else original_query.replace(" ", "-")
    logger.info(f"Using keyword for scraping", extra={'keyword': keyword})

    try:
        results = scrape_mercadolibre(keyword, interests)
    except Exception as e:
        logger.error(f"Scraping error: {e}", exc_info=True)
        results = []

    logger.info(f"Scraping completed", extra={'result_count': len(results)})

    return ScraperResponse(
        interpretedIntent=InterpretedIntent(
            recipient=parsed.get("recipientType"),
            age=parsed.get("age"),
            budgetMin=parsed.get("budgetMin"),
            budgetMax=parsed.get("budgetMax"),
            interests=parsed.get("interests", []),
        ),
        recommendations=results
    )


