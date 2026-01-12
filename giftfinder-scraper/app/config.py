"""
Configuration management using Pydantic Settings.
Provides centralized, validated configuration with safe defaults.
"""

import logging
from functools import lru_cache
from typing import Optional

import validators
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings

logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    """Application settings with automatic validation."""

    # Server Configuration
    host: str = Field(default="0.0.0.0", description="Server host")
    port: int = Field(default=8001, description="Server port")

    # Ollama Configuration
    ollama_host: str = Field(
        default="http://ollama:11434", description="Ollama service URL"
    )
    ollama_model: str = Field(default="qwen2.5:1.5b", description="Ollama model to use")

    # Timeout Configuration
    llm_timeout_seconds: int = Field(
        default=15, description="Timeout for LLM requests in seconds"
    )
    scraping_timeout_seconds: int = Field(
        default=10, description="Timeout for scraping requests in seconds"
    )

    # Concurrency & Performance
    max_concurrent_scrapes: int = Field(
        default=3, description="Maximum concurrent scraping operations"
    )
    max_products_per_scrape: int = Field(
        default=20, description="Maximum products to return per scrape"
    )

    # Cache Configuration
    cache_ttl_seconds: int = Field(
        default=3600, description="Cache Time-To-Live in seconds"
    )
    cache_max_size: int = Field(
        default=100, description="Maximum number of cache entries"
    )

    # Input Validation
    max_query_length: int = Field(
        default=500, description="Maximum query string length"
    )

    # Rate Limiting
    rate_limit_per_minute: int = Field(
        default=30, description="Maximum requests per minute"
    )

    # Logging
    log_level: str = Field(
        default="INFO",
        description="Logging level (DEBUG, INFO, WARNING, ERROR, CRITICAL)",
    )

    # Provider Configuration
    enabled_providers: str = Field(
        default="reference,scraping",
        description="Comma-separated list of enabled providers",
    )
    max_concurrent_providers: int = Field(
        default=3, description="Maximum concurrent provider calls"
    )
    provider_timeout_seconds: int = Field(
        default=15, description="Timeout for provider calls in seconds"
    )

    @field_validator("ollama_host")
    @classmethod
    def validate_ollama_host(cls, v: str) -> str:
        """Validate that ollama_host is a valid URL."""
        # Basic URL format validation (allow hostnames without TLD for docker)
        if not v.startswith(("http://", "https://")):
            raise ValueError(
                f"OLLAMA_HOST must start with http:// or https://, got: {v}"
            )
        return v

    @field_validator("log_level")
    @classmethod
    def validate_log_level(cls, v: str) -> str:
        """Validate that log_level is a valid logging level."""
        valid_levels = ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]
        v_upper = v.upper()
        if v_upper not in valid_levels:
            raise ValueError(f"LOG_LEVEL must be one of {valid_levels}, got: {v}")
        return v_upper

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance (singleton pattern)."""
    return Settings()


def validate_config_on_startup() -> None:
    """
    Validate configuration at startup for fail-fast behavior.
    Raises exception if configuration is invalid.
    """
    try:
        settings = get_settings()
        logger.info("Configuration validated successfully")
        logger.info(f"Ollama host: {settings.ollama_host}")
        logger.info(f"Ollama model: {settings.ollama_model}")
        logger.info(f"Max concurrent scrapes: {settings.max_concurrent_scrapes}")
        logger.info(f"Cache TTL: {settings.cache_ttl_seconds}s")
        logger.info(f"Log level: {settings.log_level}")
    except Exception as e:
        logger.error(f"Configuration validation failed: {e}")
        raise
