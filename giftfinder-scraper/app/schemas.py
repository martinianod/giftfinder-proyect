from pydantic import BaseModel
from typing import List, Optional

class ScrapedProduct(BaseModel):
    id: str
    title: Optional[str]
    description: Optional[str]
    price: Optional[float]
    currency: str
    image_url: Optional[str]
    product_url: Optional[str]
    store: str
    rating: Optional[float]
    tags: List[str]
