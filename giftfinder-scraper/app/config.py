"""
Centralized configuration management using Pydantic Settings.
All configuration is loaded from environment variables with validation.
"""
from pydantic_settings import BaseSettings
from pydantic import Field, field_validator


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables.
    Validates configuration on startup (fail-fast).
    """
    
    # Ollama Configuration
    ollama_host: str = Field(default="http://ollama:11434", description="Ollama API host URL")
    ollama_model: str = Field(default="qwen2.5:1.5b", description="Ollama model to use")
    ollama_timeout: int = Field(default=15, ge=5, le=60, description="Ollama API timeout in seconds")
    
    # Scraping Configuration
    scraper_timeout: int = Field(default=12, ge=5, le=30, description="HTTP scraping timeout in seconds")
    max_concurrent_scrapes: int = Field(default=3, ge=1, le=10, description="Max concurrent scraping operations")
    rate_limit_delay: float = Field(default=1.0, ge=0.1, le=5.0, description="Delay between scraping requests in seconds")
    max_items_per_scrape: int = Field(default=20, ge=5, le=50, description="Maximum items to scrape per request")
    
    # Cache Configuration
    cache_ttl_seconds: int = Field(default=3600, ge=300, le=86400, description="Cache TTL in seconds (5min - 24h)")
    cache_max_size: int = Field(default=1000, ge=100, le=10000, description="Maximum cache entries")
    
    # API Configuration
    rate_limit_per_minute: int = Field(default=30, ge=5, le=100, description="Rate limit per IP per minute")
    query_max_length: int = Field(default=500, ge=10, le=2000, description="Maximum query length")
    
    # Logging Configuration
    log_level: str = Field(default="INFO", description="Logging level")
    log_json: bool = Field(default=True, description="Use JSON structured logging")
    
    # Application Configuration
    app_name: str = Field(default="giftfinder-scraper", description="Application name")
    environment: str = Field(default="production", description="Environment name")
    
    @field_validator('log_level')
    @classmethod
    def validate_log_level(cls, v: str) -> str:
        """Validate log level is one of the allowed values."""
        allowed = ['DEBUG', 'INFO', 'WARNING', 'ERROR', 'CRITICAL']
        v_upper = v.upper()
        if v_upper not in allowed:
            raise ValueError(f"log_level must be one of {allowed}")
        return v_upper
    
    @field_validator('ollama_host')
    @classmethod
    def validate_ollama_host(cls, v: str) -> str:
        """Validate Ollama host URL format."""
        if not v.startswith(('http://', 'https://')):
            raise ValueError("ollama_host must start with http:// or https://")
        return v.rstrip('/')  # Remove trailing slash
    
    class Config:
        env_file = '.env'
        env_file_encoding = 'utf-8'
        case_sensitive = False


# Global settings instance
settings = Settings()
