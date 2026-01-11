"""
Input validation and sanitization for security.
"""

import re
import logging
from typing import Optional
from pydantic import BaseModel, field_validator, Field
from app.config import get_settings


logger = logging.getLogger(__name__)
settings = get_settings()


class SearchRequest(BaseModel):
    """Validated search request model."""
    
    query: str = Field(
        min_length=3,
        max_length=500,
        description="User search query"
    )
    
    @field_validator("query")
    @classmethod
    def sanitize_query(cls, v: str) -> str:
        """
        Sanitize and validate search query.
        
        Args:
            v: Raw query string
            
        Returns:
            Sanitized query string
            
        Raises:
            ValueError: If query contains invalid characters
        """
        # Trim whitespace
        v = v.strip()
        
        # Validate length against settings
        if len(v) > settings.max_query_length:
            raise ValueError(
                f"Query too long. Maximum length: {settings.max_query_length}"
            )
        
        # Allow only safe characters: letters (including Spanish), numbers, spaces, and basic punctuation
        safe_pattern = r'^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9\s\-\.,;:¿?¡!()]+$'
        
        if not re.match(safe_pattern, v):
            logger.warning(f"Query contains invalid characters: {v}")
            raise ValueError(
                "Query contains invalid characters. "
                "Only letters, numbers, spaces, and basic punctuation allowed."
            )
        
        return v


def sanitize_keyword(keyword: str) -> str:
    """
    Sanitize keyword for URL usage.
    
    Args:
        keyword: Raw keyword string
        
    Returns:
        Sanitized keyword safe for URLs
    """
    # Remove dangerous characters for URLs
    # Keep only alphanumeric, spaces, and hyphens
    keyword = re.sub(r'[^\w\s\-]', '', keyword)
    
    # Normalize multiple spaces to single hyphen
    keyword = re.sub(r'\s+', '-', keyword.strip())
    
    # Remove multiple consecutive hyphens
    keyword = re.sub(r'-+', '-', keyword)
    
    # Convert to lowercase
    keyword = keyword.lower()
    
    # Limit length to 100 characters
    keyword = keyword[:100]
    
    # Remove leading/trailing hyphens
    keyword = keyword.strip('-')
    
    return keyword


def validate_ml_url(url: str) -> bool:
    """
    Validate that URL is from allowed MercadoLibre domains.
    Prevents SSRF attacks.
    
    Args:
        url: URL to validate
        
    Returns:
        True if URL is valid and safe, False otherwise
    """
    if not url:
        return False
    
    # Allowed MercadoLibre domains
    allowed_domains = [
        'mercadolibre.com.ar',
        'listado.mercadolibre.com.ar',
        'articulo.mercadolibre.com.ar'
    ]
    
    # Basic URL format validation
    url_pattern = r'^https?://([a-zA-Z0-9\-\.]+\.)?(' + '|'.join(
        re.escape(d) for d in allowed_domains
    ) + r')(/.*)?$'
    
    if not re.match(url_pattern, url, re.IGNORECASE):
        logger.warning(f"Invalid or suspicious URL detected: {url}")
        return False
    
    # Additional check: ensure domain is exactly one of the allowed domains
    try:
        # Extract domain from URL
        domain_match = re.search(r'https?://([^/]+)', url, re.IGNORECASE)
        if domain_match:
            domain = domain_match.group(1).lower()
            # Check if domain matches or is subdomain of allowed domains
            is_valid = any(
                domain == d or domain.endswith('.' + d)
                for d in allowed_domains
            )
            return is_valid
    except Exception as e:
        logger.error(f"Error validating URL {url}: {e}")
        return False
    
    return False
