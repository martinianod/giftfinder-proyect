"""
FastAPI application with production-ready features:
- Structured JSON logging
- Request ID tracking
- Rate limiting
- Health checks
- Comprehensive error handling
"""

import logging
import traceback
from contextlib import asynccontextmanager
from typing import Any, Dict

from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from app.ai_local import LLMError, parse_query
from app.config import get_settings, validate_config_on_startup
from app.health import router as health_router
from app.logging_config import get_request_id, setup_logging
from app.middleware import RequestIdMiddleware
from app.ml_scraper import scrape_mercadolibre
from app.models.scraper_response import InterpretedIntent, ScraperResponse
from app.providers.aggregator import get_aggregator
from app.providers.models import ProductQuery, RecipientProfile
from app.validation import SearchRequest

# Initialize settings and logging
settings = get_settings()
setup_logging(settings.log_level)
logger = logging.getLogger(__name__)

# Initialize rate limiter
limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events."""
    # Startup
    logger.info("=" * 60)
    logger.info("🚀 GiftFinder Scraper Service Starting")
    logger.info("=" * 60)

    try:
        validate_config_on_startup()
        logger.info("✅ Configuration validated successfully")
    except Exception as e:
        logger.error(f"❌ Configuration validation failed: {e}")
        raise

    logger.info(f"📊 Service ready on port {settings.port}")
    logger.info("=" * 60)

    yield

    # Shutdown
    logger.info("🛑 GiftFinder Scraper Service Shutting Down")


# Create FastAPI app
app = FastAPI(
    title="GiftFinder Scraper API",
    description="Production-ready scraper service with LLM integration",
    version="1.0.0",
    lifespan=lifespan,
)

# Add middleware
app.add_middleware(RequestIdMiddleware)

# Add rate limiter to app state
app.state.limiter = limiter

# Add rate limit exceeded handler
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Include health check router
app.include_router(health_router, tags=["health"])


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """
    Global exception handler for unhandled errors.
    """
    request_id = get_request_id()

    logger.error(
        f"Unhandled exception: {str(exc)}",
        extra={
            "path": request.url.path,
            "method": request.method,
            "error_type": type(exc).__name__,
            "traceback": traceback.format_exc(),
        },
    )

    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "Internal server error",
            "request_id": request_id,
            "detail": (
                str(exc)
                if settings.log_level == "DEBUG"
                else "An unexpected error occurred"
            ),
        },
    )


@app.get("/", tags=["info"])
async def root() -> Dict[str, Any]:
    """
    Service information endpoint.
    """
    return {
        "service": "GiftFinder Scraper API",
        "version": "1.0.0",
        "status": "online",
        "ollama": {"host": settings.ollama_host, "model": settings.ollama_model},
        "features": [
            "Structured JSON logging",
            "Request ID tracking",
            "Rate limiting",
            "Health checks",
            "Input validation",
            "TTL caching",
            "Async scraping",
        ],
    }


@app.post("/scrape/search", response_model=ScraperResponse, tags=["scraper"])
@limiter.limit(f"{settings.rate_limit_per_minute}/minute")
async def scrape_search(request: Request, req: SearchRequest) -> ScraperResponse:
    """
    Search for gift products based on natural language query.

    Args:
        request: FastAPI request object (for rate limiting)
        req: Validated search request with query

    Returns:
        ScraperResponse with interpreted intent and product recommendations

    Raises:
        503: If LLM service is unavailable
        400: If query validation fails
    """
    # Query is already validated by Pydantic
    original_query = req.query

    logger.info(f"New search request: {original_query}")

    try:
        # Parse query with LLM
        parsed = parse_query(original_query) or {}

        logger.info(
            "Query parsed successfully",
            extra={
                "query": original_query,
                "recipient": parsed.get("recipientType"),
                "interests_count": len(parsed.get("interests", [])),
            },
        )

    except LLMError as e:
        logger.error(f"LLM error during query parsing: {e}")
        return JSONResponse(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            content={
                "error": "LLM service unavailable",
                "detail": str(e),
                "request_id": get_request_id(),
            },
        )

    except ValueError as e:
        logger.warning(f"Validation error: {e}")
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "error": "Invalid query",
                "detail": str(e),
                "request_id": get_request_id(),
            },
        )

    # Extract interests and keywords
    interests = parsed.get("interests", [])
    logger.debug(f"Detected interests: {interests}")

    # Generate keywords from interests or original query
    keywords = interests if interests else [original_query]

    # Create ProductQuery for aggregator
    product_query = ProductQuery(
        keywords=keywords,
        priceMin=parsed.get("budgetMin"),
        priceMax=parsed.get("budgetMax"),
        recipientProfile=RecipientProfile(
            type=parsed.get("recipientType", "unknown"),
            age=parsed.get("age"),
            interests=interests,
        ),
        limit=20,  # Request more from aggregator, response will show top results
    )

    logger.info(
        "Searching products via aggregator",
        extra={
            "keywords": keywords,
            "interests": interests,
            "priceMin": parsed.get("budgetMin"),
            "priceMax": parsed.get("budgetMax"),
        },
    )

    try:
        # Search products via aggregator (provider-based)
        aggregator = get_aggregator()
        products = await aggregator.search_products(product_query)

        logger.info(
            "Product search completed",
            extra={
                "query": original_query,
                "product_count": len(products),
            },
        )

        # Convert Product objects to ScrapedProductResponse format (backward compatibility)
        results = []
        for product in products:
            results.append(
                {
                    "id": product.id,
                    "title": product.title,
                    "description": product.description,
                    "price": product.price,
                    "currency": product.currency,
                    "image_url": product.images[0] if product.images else None,
                    "product_url": product.url,
                    "store": product.vendor,
                    "rating": None,
                    "tags": product.tags,
                }
            )

    except Exception as e:
        logger.error(f"Error during product search: {e}")
        results = []

    # Build response
    return ScraperResponse(
        interpretedIntent=InterpretedIntent(
            recipient=parsed.get("recipientType"),
            age=parsed.get("age"),
            budgetMin=parsed.get("budgetMin"),
            budgetMax=parsed.get("budgetMax"),
            interests=parsed.get("interests", []),
        ),
        recommendations=results,
    )
