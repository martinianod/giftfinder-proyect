from typing import List, Optional

from pydantic import BaseModel


class InterpretedIntent(BaseModel):
    recipient: Optional[str] = None
    age: Optional[int] = None
    budgetMin: Optional[float] = None
    budgetMax: Optional[float] = None
    interests: List[str] = []


class ScrapedProductResponse(BaseModel):
    id: str
    title: Optional[str]
    description: Optional[str]
    price: Optional[float]
    currency: Optional[str]
    image_url: Optional[str]
    product_url: Optional[str]
    store: Optional[str]
    rating: Optional[float]
    tags: List[str] = []


class ScraperResponse(BaseModel):
    interpretedIntent: InterpretedIntent
    recommendations: List[ScrapedProductResponse]
