from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from app.ai_local import parse_query
from app.ml_scraper import scrape_mercadolibre
from app.schemas import ScrapedProduct
from app.models.scraper_response import ScraperResponse, InterpretedIntent, ScrapedProductResponse

app = FastAPI()

class SearchRequest(BaseModel):
    query: str


@app.get("/")
def root():
    return {"status": "scraper online", "ollama": "ok"}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/scrape/search", response_model=ScraperResponse)
def scrape_search(req: SearchRequest):
    original_query = req.query

    print(f"\n🟦 Nueva búsqueda: {original_query}")

    parsed = parse_query(original_query) or {}

    interpreted = {
        "recipient": parsed.get("recipientType"),
        "age": parsed.get("age"),
        "budgetMin": parsed.get("budgetMin"),
        "budgetMax": parsed.get("budgetMax"),
        "interests": parsed.get("interests", [])
    }

    interests = parsed.get("interests", [])
    print("🟪 Intereses detectados:", interests)

    keyword = interests[0].replace(" ", "-") if interests else original_query.replace(" ", "-")
    print("🟩 Keyword final:", keyword)

    try:
        results = scrape_mercadolibre(keyword, interests)
    except Exception as e:
        print("❌ ERROR scrapeando ML:", e)
        results = []

    print(f"🟧 Total productos devueltos: {len(results)}")

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

