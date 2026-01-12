"""
Pytest configuration and fixtures.
"""

import pytest
from unittest.mock import Mock, patch


@pytest.fixture
def mock_settings():
    """Mock settings for testing."""
    with patch('app.config.get_settings') as mock:
        settings = Mock()
        settings.ollama_host = "http://localhost:11434"
        settings.ollama_model = "qwen2.5:1.5b"
        settings.llm_timeout_seconds = 15
        settings.scraping_timeout_seconds = 10
        settings.max_concurrent_scrapes = 3
        settings.max_products_per_scrape = 20
        settings.cache_ttl_seconds = 3600
        settings.cache_max_size = 100
        settings.max_query_length = 500
        settings.rate_limit_per_minute = 30
        settings.log_level = "INFO"
        mock.return_value = settings
        yield settings


@pytest.fixture
def mock_ollama():
    """Mock Ollama API responses."""
    return {
        "response": '{"recipientType": "friend", "age": 30, "budgetMin": null, "budgetMax": null, "interests": ["technology", "gaming"]}'
    }


@pytest.fixture
def mock_ml_html():
    """Mock MercadoLibre HTML response."""
    return """
    <html>
        <body>
            <li class="ui-search-layout__item">
                <h2 class="ui-search-item__title">Test Product</h2>
                <span class="andes-money-amount__fraction">12.345</span>
                <img src="https://http2.mlstatic.com/test.jpg" />
                <a href="https://www.mercadolibre.com.ar/test-product/p/MLA123"></a>
            </li>
        </body>
    </html>
    """
