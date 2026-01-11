import json
import uuid
import asyncio
import httpx
from bs4 import BeautifulSoup
from typing import List, Dict, Any
from app.cache import cache_get, cache_set
from app.ai_local import run_llm_json
from app.config import settings
from app.logging_config import get_logger
from app.validation import validate_url, sanitize_keyword

logger = get_logger(__name__)

# Global semaphore for concurrency control
_scraping_semaphore = None


def get_semaphore():
    """Get or create the global semaphore for concurrency control."""
    global _scraping_semaphore
    if _scraping_semaphore is None:
        _scraping_semaphore = asyncio.Semaphore(settings.max_concurrent_scrapes)
    return _scraping_semaphore


# ============================================================
# 🔥 SCRAPER PRINCIPAL - ASYNC VERSION
# ============================================================

async def scrape_mercadolibre_async(keyword: str, interests: List[str]) -> List[Dict[str, Any]]:
    """
    Async scraper for MercadoLibre with concurrency control.
    
    Args:
        keyword: Search keyword
        interests: List of interests for tagging
        
    Returns:
        List of scraped products
    """
    # Sanitize keyword to prevent injection
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

    url = f"https://listado.mercadolibre.com.ar/{keyword}"
    
    # Validate URL before scraping (anti-SSRF)
    if not validate_url(url):
        logger.error(f"Invalid or unauthorized URL: {url}")
        return []

    # Get semaphore for concurrency control
    semaphore = get_semaphore()
    
    async with semaphore:
        logger.info(f"Starting scrape for keyword: {keyword}, URL: {url}")
        
        # Rate limiting
        await asyncio.sleep(settings.rate_limit_delay)

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
            async with httpx.AsyncClient(timeout=settings.scraper_timeout) as client:
                response = await client.get(url, headers=headers)
        except httpx.TimeoutException as e:
            logger.error(f"Scraping timeout for {url}: {e}")
            return []
        except Exception as e:
            logger.error(f"Scraping request error for {url}: {e}", exc_info=True)
            return []

        # ------------------------------------------------
        # LOG BÁSICO DE RESPUESTA
        # ------------------------------------------------
        logger.info(
            f"ML response received",
            extra={
                'status_code': response.status_code,
                'content_length': len(response.text),
                'url': url
            }
        )

        if response.status_code != 200:
            logger.error(f"ML returned status {response.status_code}")
            return []

        html_lower = response.text.lower()

        # ------------------------------------------------
        # DETECCIÓN DE ANTIBOT / CAPTCHA
        # ------------------------------------------------
        block_signals = [
            "captcha",
            "robot",
            "blocked",
            "verificación",
            "incidencia",
            "challenge",
            "security check",
        ]

        if any(signal in html_lower for signal in block_signals):
            logger.warning("ML appears to be blocking scraping (captcha/antibot)")
            logger.debug(f"HTML snippet: {response.text[:300]}")
            return []

        soup = BeautifulSoup(response.text, "html.parser")
        items = soup.select("li.ui-search-layout__item")

        logger.info(f"Found {len(items)} items in ML response")

        if not items:
            logger.warning("ML returned valid HTML but no items found")
            logger.debug(f"HTML snippet: {response.text[:300]}")
            return []

        # ============================================================
        # 1) SCRAPER CLÁSICO + HTML PARA IA
        # ============================================================
        raw_products = []
        items_html = []

        max_items = min(len(items), settings.max_items_per_scrape)
        
        for item in items[:max_items]:
            title_el = item.select_one("h2.ui-search-item__title")
            price_el = item.select_one("span.andes-money-amount__fraction")
            image_el = item.select_one("img")
            link_el = item.select_one("a")

            title = title_el.text.strip() if title_el else None
            price_raw = (
                price_el.text.strip()
                .replace(".", "")
                .replace(",", ".")
                if price_el else None
            )

            product = {
                "title": title,
                "price": float(price_raw) if price_raw else None,
                "image_url": image_el.get("src") if image_el else None,
                "product_url": link_el.get("href") if link_el else None,
                "rating": None,
            }

            raw_products.append(product)
            items_html.append(str(item)[:500])  # límite para LLMs chicos

        # ============================================================
        # 2) 🔥 IA — UNA SOLA LLAMADA
        # ============================================================
        ai_results = clean_products_with_ai(items_html)

        logger.debug(f"AI processing returned {len(ai_results)} results")

        if not isinstance(ai_results, list):
            logger.warning(f"AI returned invalid format: {type(ai_results).__name__}. Ignoring AI.")
            ai_results = [{} for _ in raw_products]

        while len(ai_results) < len(raw_products):
            ai_results.append({})

        # ============================================================
        # 3) FUSIÓN SCRAPER + IA
        # ============================================================
        final_products = []

        for i, classic in enumerate(raw_products):
            ai = ai_results[i] if i < len(ai_results) else {}

            merged = {
                "id": str(uuid.uuid4()),
                "title": ai.get("title") or classic["title"],
                "price": ai.get("price") or classic["price"],
                "image_url": ai.get("image_url") or classic["image_url"],
                "product_url": ai.get("product_url") or classic["product_url"],
                "rating": ai.get("rating") or classic["rating"],
                "currency": "ARS",
                "store": "MercadoLibre",
                "tags": interests or [],
            }

            final_products.append(merged)

        # ============================================================
        # LOG FINAL
        # ============================================================
        logger.info(f"Successfully scraped {len(final_products)} products for keyword: {keyword}")
        logger.debug(f"Products: {json.dumps(final_products[:2], indent=2, ensure_ascii=False)}")

        # ❗ cachear SOLO si hay resultados reales
        if final_products:
            cache_set(cache_key, final_products)

        return final_products


# ============================================================
# BACKWARD COMPATIBLE SYNC WRAPPER
# ============================================================
def scrape_mercadolibre(keyword: str, interests: List[str]) -> List[Dict[str, Any]]:
    """
    Synchronous wrapper for backward compatibility.
    Creates event loop if needed and runs async scraper.
    """
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    return loop.run_until_complete(scrape_mercadolibre_async(keyword, interests))


# ============================================================
# 🔥 FUNCIÓN IA
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


def clean_products_with_ai(items_html: List[str]) -> List[Dict[str, Any]]:
    """
    Use AI to clean and enhance product data from HTML.
    
    Args:
        items_html: List of HTML strings for each product
        
    Returns:
        List of cleaned product dictionaries
    """
    prompt = PRODUCT_PROMPT.format(
        html_list=json.dumps(items_html, ensure_ascii=False)
    )

    try:
        result = run_llm_json(prompt)
    except Exception as e:
        logger.error(f"AI product cleaning failed: {e}")
        return []

    if isinstance(result, dict):
        logger.warning("AI returned dict instead of list")
        return []

    return result or []