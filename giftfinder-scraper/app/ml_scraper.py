import json
import uuid
import requests
import time
from bs4 import BeautifulSoup
from app.cache import cache_get, cache_set
from app.ai_local import run_llm_json


# ============================================================
# 🔥 SCRAPER PRINCIPAL
# ============================================================

def scrape_mercadolibre(keyword: str, interests):
    cache_key = f"ml::{keyword}"

    # -------------------------
    # CACHE (NO cachear vacíos)
    # -------------------------
    cached = cache_get(cache_key)
    if cached:
        print(f"⚡ CACHE HIT for {keyword}")
        return cached

    url = f"https://listado.mercadolibre.com.ar/{keyword}"
    time.sleep(1)  # rate-limit suave

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
        response = requests.get(url, headers=headers, timeout=12)
    except Exception as e:
        print("❌ Error request ML:", e)
        return []

    # ------------------------------------------------
    # LOG BÁSICO DE RESPUESTA
    # ------------------------------------------------
    print(
        f"🧾 ML response status={response.status_code} "
        f"bytes={len(response.text)} url={url}"
    )

    if response.status_code != 200:
        print(f"❌ ML respondió status {response.status_code}")
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
        print("🚫 ML parece estar bloqueando el scraping (captcha/antibot).")
        print("🔍 HTML snippet:", response.text[:300].replace("\n", " "))
        return []

    soup = BeautifulSoup(response.text, "html.parser")
    items = soup.select("li.ui-search-layout__item")

    print(f"📦 Encontrados {len(items)} items en ML")

    if not items:
        print("⚠️ ML devolvió HTML válido pero sin items.")
        print("🔍 HTML snippet:", response.text[:300].replace("\n", " "))
        return []

    # ============================================================
    # 1) SCRAPER CLÁSICO + HTML PARA IA
    # ============================================================
    raw_products = []
    items_html = []

    for item in items[:20]:
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

    print("\n====== DEBUG IA RAW ======")
    print(ai_results)
    print("====== END DEBUG ======\n")

    if not isinstance(ai_results, list):
        print("⚠️ IA devolvió formato inválido. Ignorando IA.")
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
    print("\n================ RESULTS ================")
    print(json.dumps(final_products, indent=2, ensure_ascii=False))
    print("========================================\n")

    # ❗ cachear SOLO si hay resultados reales
    if final_products:
        cache_set(cache_key, final_products)

    return final_products


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


def clean_products_with_ai(items_html):
    prompt = PRODUCT_PROMPT.format(
        html_list=json.dumps(items_html, ensure_ascii=False)
    )

    result = run_llm_json(prompt)

    if isinstance(result, dict):
        print("⚠️ IA devolvió dict en lugar de lista.")
        return []

    return result or []