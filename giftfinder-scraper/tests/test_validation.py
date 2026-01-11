"""
Tests for validation module.
"""

import pytest
from pydantic import ValidationError
from app.validation import SearchRequest, sanitize_keyword, validate_ml_url


class TestSearchRequest:
    """Tests for SearchRequest validation."""
    
    def test_sanitize_query_valid(self):
        """Test valid query passes validation."""
        req = SearchRequest(query="regalo para amigo de 30 años")
        assert req.query == "regalo para amigo de 30 años"
    
    def test_sanitize_query_with_special_chars(self):
        """Test query with valid special characters."""
        req = SearchRequest(query="¿Qué regalo? ¡Sorpresa!")
        assert req.query == "¿Qué regalo? ¡Sorpresa!"
    
    def test_sanitize_query_invalid_chars(self):
        """Test query with invalid characters raises error."""
        with pytest.raises(ValidationError) as exc_info:
            SearchRequest(query="test<script>alert('xss')</script>")
        assert "invalid characters" in str(exc_info.value).lower()
    
    def test_sanitize_query_too_short(self):
        """Test query that's too short raises error."""
        with pytest.raises(ValidationError):
            SearchRequest(query="ab")
    
    def test_sanitize_query_too_long(self):
        """Test query that's too long raises error."""
        long_query = "a" * 501
        with pytest.raises(ValidationError):
            SearchRequest(query=long_query)
    
    def test_sanitize_query_whitespace_trimmed(self):
        """Test that whitespace is trimmed."""
        req = SearchRequest(query="  test query  ")
        assert req.query == "test query"


class TestSanitizeKeyword:
    """Tests for sanitize_keyword function."""
    
    def test_sanitize_keyword_basic(self):
        """Test basic keyword sanitization."""
        result = sanitize_keyword("Test Keyword")
        assert result == "test-keyword"
    
    def test_sanitize_keyword_special_chars(self):
        """Test removal of special characters."""
        result = sanitize_keyword("test@#$keyword")
        assert result == "testkeyword"
    
    def test_sanitize_keyword_multiple_spaces(self):
        """Test multiple spaces normalized to single hyphen."""
        result = sanitize_keyword("test    keyword")
        assert result == "test-keyword"
    
    def test_sanitize_keyword_too_long(self):
        """Test keyword is truncated to 100 chars."""
        long_keyword = "a" * 150
        result = sanitize_keyword(long_keyword)
        assert len(result) == 100
    
    def test_sanitize_keyword_hyphens(self):
        """Test multiple hyphens normalized."""
        result = sanitize_keyword("test---keyword")
        assert result == "test-keyword"
    
    def test_sanitize_keyword_leading_trailing_hyphens(self):
        """Test leading/trailing hyphens removed."""
        result = sanitize_keyword("-test-keyword-")
        assert result == "test-keyword"


class TestValidateMLURL:
    """Tests for validate_ml_url function."""
    
    def test_validate_ml_url_valid(self):
        """Test valid MercadoLibre URLs."""
        valid_urls = [
            "https://www.mercadolibre.com.ar/test",
            "https://listado.mercadolibre.com.ar/search",
            "https://articulo.mercadolibre.com.ar/MLA-123",
            "http://mercadolibre.com.ar/test"
        ]
        for url in valid_urls:
            assert validate_ml_url(url) is True
    
    def test_validate_ml_url_invalid_domain(self):
        """Test invalid domains are rejected."""
        invalid_urls = [
            "https://evil.com/fake",
            "https://mercadolibre.com.br/test",  # Wrong country
            "https://notmercadolibre.com.ar/test",
            "https://mercadolibre.com.ar.evil.com/test"
        ]
        for url in invalid_urls:
            assert validate_ml_url(url) is False
    
    def test_validate_ml_url_empty(self):
        """Test empty URL returns False."""
        assert validate_ml_url("") is False
        assert validate_ml_url(None) is False
    
    def test_validate_ml_url_malformed(self):
        """Test malformed URLs are rejected."""
        malformed_urls = [
            "not-a-url",
            "ftp://mercadolibre.com.ar/test",
            "javascript:alert('xss')"
        ]
        for url in malformed_urls:
            assert validate_ml_url(url) is False
