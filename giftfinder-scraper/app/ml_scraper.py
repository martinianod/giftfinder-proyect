"""
MercadoLibre scraper with async support, rate limiting, and error handling.
"""

import asyncio
import json
import logging
import uuid
from typing import Any, Dict, List, Optional

import httpx
from bs4 import BeautifulSoup

from app.ai_local import run_llm_json
from app.cache import cache_get, cache_set
from app.config import get_settings
from app.validation import sanitize_keyword, validate_ml_url

logger = logging.getLogger(__name__)
settings = get_settings()


# Global semaphore for concurrency control
_scraping_semaphore: Optional[asyncio.Semaphore] = None


def get_semaphore() -> asyncio.Semaphore:
    """
    Get or create the global scraping semaphore.

    Returns:
        Semaphore for controlling concurrent scrapes
    """
    global _scraping_semaphore
    if _scraping_semaphore is None:
        _scraping_semaphore = asyncio.Semaphore(settings.max_concurrent_scrapes)
    return _scraping_semaphore


def _extract_product(item, interests: List[str]) -> Optional[dict]:
    """
    Extract product data from a BeautifulSoup item.

    Args:
        item: BeautifulSoup item element
        interests: List of user interests for tagging

    Returns:
        Product dict or None if extraction fails
    """
    try:
        title_el = item.select_one("h2.ui-search-item__title")
        price_el = item.select_one("span.andes-money-amount__fraction")
        image_el = item.select_one("img")
        link_el = item.select_one("a")

        # Extract fields
        title = title_el.text.strip() if title_el else None
        image_url = image_el.get("src") if image_el else None
        product_url = link_el.get("href") if link_el else None

        # Validate required fields
        if not title or not product_url:
            return None

        # Validate product URL
        if not validate_ml_url(product_url):
            logger.warning(f"Invalid product URL: {product_url}")
            return None

        # Parse price (handle Argentine format: "12.345" -> 12345.0)
        price = None
        if price_el:
            try:
                price_raw = price_el.text.strip().replace(".", "").replace(",", ".")
                price = float(price_raw)
            except (ValueError, AttributeError) as e:
                logger.debug(f"Could not parse price: {e}")

        return {
            "id": str(uuid.uuid4()),
            "title": title,
            "price": price,
            "image_url": image_url,
            "product_url": product_url,
            "rating": None,
            "currency": "ARS",
            "store": "MercadoLibre",
            "tags": interests or [],
        }

    except Exception as e:
        logger.warning(f"Error extracting product: {e}")
        return None


# ============================================================
# 🔥 ASYNC SCRAPER
# ============================================================
async def scrape_mercadolibre_async(keyword: str, interests: List[str]) -> List[dict]:
    """
    Asynchronously scrape MercadoLibre with rate limiting and caching.

    Args:
        keyword: Search keyword
        interests: List of user interests

    Returns:
        List of product dictionaries
    """
    # Sanitize keyword
    keyword = sanitize_keyword(keyword)
    if not keyword:
        logger.warning("Empty keyword after sanitization")
        return []

    cache_key = f"ml::{keyword}"

    # -------------------------
    # CACHE (NO cachear vacíos)
    # -------------------------
    cached = cache_get(cache_key)
    if cached:
        logger.info(f"Cache hit for keyword: {keyword}")
        return cached

    # Acquire semaphore to limit concurrent scrapes
    async with get_semaphore():
        logger.info(f"Starting scrape for keyword: {keyword}")

        # Construct URL
        url = f"https://listado.mercadolibre.com.ar/{keyword}"

        # Validate URL before making request
        if not validate_ml_url(url):
            logger.error(f"Invalid URL constructed: {url}")
            return []

        # Rate limiting: wait 1 second between requests
        await asyncio.sleep(1)

        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            "Accept-Language": "es-AR,es;q=0.9",
            "Accept": (
                "text/html,application/xhtml+xml,application/xml;"
                "q=0.9,image/avif,image/webp,*/*;q=0.8"
            ),
            "Connection": "keep-alive",
            "Referer": "https://www.mercadolibre.com.ar/",
        }

        try:
            async with httpx.AsyncClient(
                timeout=settings.scraping_timeout_seconds
            ) as client:
                response = await client.get(url, headers=headers)

            # ------------------------------------------------
            # LOG RESPONSE
            # ------------------------------------------------
            logger.info(
                f"MercadoLibre response",
                extra={
                    "status_code": response.status_code,
                    "content_length": len(response.text),
                    "url": url,
                },
            )

            if response.status_code != 200:
                logger.warning(f"MercadoLibre returned status {response.status_code}")
                return []

            html_lower = response.text.lower()

            # ------------------------------------------------
            # DETECCIÓN SIMPLE DE ANTIBOT / CAPTCHA
            # ------------------------------------------------
            block_signals = [
                "captcha",
                "robot",
                "blocked",
                "verificación",
                "incidencia",
                "challenge",
            ]

            if any(signal in html_lower for signal in block_signals):
                logger.warning("MercadoLibre anti-bot detection triggered")
                logger.debug(f"HTML snippet: {response.text[:300]}")
                return []

            soup = BeautifulSoup(response.text, "html.parser")
            items = soup.select("li.ui-search-layout__item")

            logger.info(f"Found {len(items)} items on MercadoLibre")

            if not items:
                logger.warning("MercadoLibre returned valid HTML but no items")
                logger.debug(f"HTML snippet: {response.text[:300]}")
                return []

            # ============================================================
            # EXTRACT PRODUCTS
            # ============================================================
            final_products = []

            for item in items[: settings.max_products_per_scrape]:
                product = _extract_product(item, interests)
                if product:
                    final_products.append(product)

            # ============================================================
            # LOG RESULTS
            # ============================================================
            logger.info(
                f"Scraping completed",
                extra={"keyword": keyword, "product_count": len(final_products)},
            )

            # ❗ cachear SOLO si hay resultados reales
            if final_products:
                cache_set(cache_key, final_products)

            return final_products

        except httpx.TimeoutException:
            logger.error(f"Scraping timeout for keyword: {keyword}")
            return []

        except httpx.RequestError as e:
            logger.error(f"Scraping request error for keyword {keyword}: {e}")
            return []

        except Exception as e:
            logger.error(f"Unexpected scraping error for keyword {keyword}: {e}")
            return []


# ============================================================
# SYNC WRAPPER FOR BACKWARD COMPATIBILITY
# ============================================================
def scrape_mercadolibre(keyword: str, interests: List[str]) -> List[dict]:
    """
    Synchronous wrapper for scrape_mercadolibre_async.

    Args:
        keyword: Search keyword
        interests: List of user interests

    Returns:
        List of product dictionaries
    """
    try:
        # Try to get existing event loop
        loop = asyncio.get_event_loop()
        if loop.is_running():
            # If loop is running, create a new one for this task
            # This can happen in some async contexts
            import nest_asyncio

            nest_asyncio.apply()
            return loop.run_until_complete(
                scrape_mercadolibre_async(keyword, interests)
            )
        else:
            return loop.run_until_complete(
                scrape_mercadolibre_async(keyword, interests)
            )
    except RuntimeError:
        # No event loop exists, create a new one
        return asyncio.run(scrape_mercadolibre_async(keyword, interests))


# ============================================================
# 🔥 AI CLEANING FUNCTION (Legacy - not currently used)
# ============================================================

PRODUCT_PROMPT = """
Eres un experto extrayendo productos desde HTML.
Recibirás una LISTA de HTMLs, cada uno representando un producto.

Para cada item devuelve UN JSON:

{
  "title": string|null,
  "price": number|null,
  "image_url": string|null,
  "product_url": string|null,
  "rating": number|null
}

Reglas:
- NO inventes datos.
- Si un campo no se encuentra, pon null.
- Devuelve una LISTA JSON, misma cantidad y mismo orden que la entrada.
- No uses texto fuera del HTML para adivinar.

HTML_LIST:
{html_list}
"""


def clean_products_with_ai(items_html):
    """
    Legacy function for AI-based product extraction.
    Currently not used in main flow but kept for compatibility.
    """
    prompt = PRODUCT_PROMPT.format(html_list=json.dumps(items_html, ensure_ascii=False))

    result = run_llm_json(prompt)

    if isinstance(result, dict):
        logger.warning("AI returned dict instead of list")
        return []

    return result or []
