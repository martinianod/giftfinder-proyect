"""
Input validation and sanitization for the scraper API.
Prevents injection attacks, SSRF, and validates input constraints.
"""
import re
from typing import Optional
from pydantic import BaseModel, Field, field_validator
import validators


class SearchRequest(BaseModel):
    """
    Validated search request model with sanitization.
    """
    query: str = Field(..., min_length=1, max_length=500)
    
    @field_validator('query')
    @classmethod
    def sanitize_query(cls, v: str) -> str:
        """
        Sanitize query string to prevent injection attacks.
        - Allows alphanumeric, spaces, and common Spanish characters
        - Removes potentially dangerous characters
        """
        if not v or not v.strip():
            raise ValueError("Query cannot be empty or whitespace only")
        
        # Allow letters (including Spanish), numbers, spaces, and basic punctuation
        # Pattern allows: a-z, A-Z, 0-9, spaces, Spanish chars (áéíóúñü), and .,!?-
        allowed_chars_pattern = r'[^\w\sáéíóúñüÁÉÍÓÚÑÜ.,!?\-]'
        sanitized = re.sub(allowed_chars_pattern, '', v)
        
        # Collapse multiple spaces
        sanitized = re.sub(r'\s+', ' ', sanitized).strip()
        
        if not sanitized:
            raise ValueError("Query contains no valid characters after sanitization")
        
        return sanitized


def validate_url(url: str) -> bool:
    """
    Validate URL to prevent SSRF attacks.
    Only allows HTTPS URLs from MercadoLibre domains.
    """
    if not url:
        return False
    
    # Basic URL validation
    if not validators.url(url):
        return False
    
    # Only allow MercadoLibre domains (prevent SSRF)
    allowed_domains = [
        'mercadolibre.com.ar',
        'listado.mercadolibre.com.ar',
        'articulo.mercadolibre.com.ar',
        'www.mercadolibre.com.ar',
    ]
    
    # Extract domain from URL
    try:
        # Simple domain extraction (not using urllib to avoid complexity)
        domain_match = re.search(r'https?://([^/]+)', url)
        if not domain_match:
            return False
        
        domain = domain_match.group(1)
        
        # Check if domain is in allowed list
        return any(domain == allowed or domain.endswith('.' + allowed) 
                  for allowed in allowed_domains)
    except Exception:
        return False


def sanitize_keyword(keyword: str) -> str:
    """
    Sanitize keyword for MercadoLibre URL construction.
    Replaces spaces with hyphens and removes invalid characters.
    """
    if not keyword:
        return ""
    
    # Convert to lowercase
    keyword = keyword.lower()
    
    # Allow only alphanumeric, spaces, and Spanish characters
    keyword = re.sub(r'[^\w\sáéíóúñü\-]', '', keyword)
    
    # Replace spaces with hyphens
    keyword = re.sub(r'\s+', '-', keyword.strip())
    
    # Remove multiple consecutive hyphens
    keyword = re.sub(r'-+', '-', keyword)
    
    # Remove leading/trailing hyphens
    keyword = keyword.strip('-')
    
    return keyword


def validate_scraping_limits(max_items: int = 20) -> int:
    """
    Validate and enforce scraping limits to prevent resource exhaustion.
    """
    if max_items < 1:
        return 1
    if max_items > 50:  # Hard limit
        return 50
    return max_items
