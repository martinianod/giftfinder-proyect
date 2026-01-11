import json
import requests
import re
from typing import Dict, Any, Union, List
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class LLMError(Exception):
    """Custom exception for LLM-related errors."""
    pass


# ============================================================
# Ejecuta LLM con timeout explícito
# ============================================================
def run_llm(prompt: str) -> str:
    """
    Execute LLM with explicit timeout and error handling.
    
    Args:
        prompt: The prompt to send to the LLM
        
    Returns:
        LLM response text or empty string on error
        
    Raises:
        LLMError: If LLM request fails
    """
    try:
        response = requests.post(
            f"{settings.ollama_host}/api/generate",
            json={
                "model": settings.ollama_model,
                "prompt": prompt,
                "stream": False
            },
            timeout=settings.ollama_timeout
        )
        response.raise_for_status()
        
        result = response.json().get("response", "")
        logger.debug(f"LLM response received, length: {len(result)}")
        return result
        
    except requests.Timeout as e:
        logger.error(f"LLM timeout after {settings.ollama_timeout}s: {e}")
        raise LLMError(f"LLM request timed out after {settings.ollama_timeout}s")
    except requests.RequestException as e:
        logger.error(f"LLM request error: {e}")
        raise LLMError(f"LLM request failed: {str(e)}")
    except Exception as e:
        logger.error(f"Unexpected LLM error: {e}", exc_info=True)
        raise LLMError(f"Unexpected error: {str(e)}")


# ============================================================
# Limpia bloques tipo ```json
# ============================================================
def clean_json_block(text: str):
    text = text.replace("```json", "").replace("```", "").strip()
    return text


# ============================================================
# JSON parser robusto con múltiples estrategias de fallback
# ============================================================
def run_llm_json(prompt: str) -> Union[Dict[str, Any], List[Any]]:
    """
    Execute LLM and parse JSON response with multiple fallback strategies.
    
    Args:
        prompt: The prompt to send to the LLM
        
    Returns:
        Parsed JSON (dict or list) or empty dict on error
    """
    try:
        raw = run_llm(prompt)
    except LLMError as e:
        logger.warning(f"LLM call failed, returning empty dict: {e}")
        return {}
    
    if not raw:
        logger.warning("LLM returned empty response")
        return {}

    cleaned = clean_json_block(raw)

    # Strategy 1: Try to parse as-is if it looks like JSON
    if (cleaned.startswith("[") and cleaned.endswith("]")) or \
       (cleaned.startswith("{") and cleaned.endswith("}")):
        try:
            result = json.loads(cleaned)
            logger.debug(f"Successfully parsed JSON (strategy 1): type={type(result).__name__}")
            return result
        except json.JSONDecodeError as e:
            logger.debug(f"Strategy 1 failed: {e}")

    # Strategy 2: Search for JSON structure in text
    match = re.search(r'(\[.*\]|\{.*\})', cleaned, re.DOTALL)
    if match:
        try:
            result = json.loads(match.group(0))
            logger.debug(f"Successfully parsed JSON (strategy 2): type={type(result).__name__}")
            return result
        except json.JSONDecodeError as e:
            logger.debug(f"Strategy 2 failed: {e}")

    # Strategy 3: Return empty dict/list based on what we expected
    logger.warning(f"All JSON parsing strategies failed, returning empty dict. Raw text: {cleaned[:200]}")
    return {}


# ============================================================
# parse_query con fallback robusto
# ============================================================
def parse_query(text: str) -> Dict[str, Any]:
    """
    Parse user query using LLM with robust fallback.
    
    Args:
        text: User query text
        
    Returns:
        Dictionary with parsed query fields
    """
    prompt = f"""
Devuelve SOLO JSON con esta estructura:

{{
  "recipientType": string,
  "age": number|null,
  "budgetMin": number|null,
  "budgetMax": number|null,
  "interests": [string]
}}

Mensaje: "{text}"
"""

    try:
        raw = run_llm_json(prompt)
    except Exception as e:
        logger.error(f"parse_query failed, using fallback: {e}")
        return _fallback_parse(text)

    # ✔ Si es lista → usar el primer elemento
    if isinstance(raw, list) and len(raw) > 0:
        raw = raw[0]

    # ✔ Si no es dict → fallback
    if not isinstance(raw, dict):
        logger.warning(f"parse_query: invalid response type {type(raw).__name__}, using fallback")
        return _fallback_parse(text)

    # Validate structure
    if not _validate_parsed_query(raw):
        logger.warning("parse_query: invalid structure, using fallback")
        return _fallback_parse(text)

    logger.info(f"Successfully parsed query: recipient={raw.get('recipientType')}, "
                f"interests={len(raw.get('interests', []))}")
    return raw


def _fallback_parse(text: str) -> Dict[str, Any]:
    """
    Fallback parser when LLM fails.
    Returns a basic structure with the query as interest.
    """
    logger.info("Using fallback parser")
    return {
        "recipientType": "unknown",
        "age": None,
        "budgetMin": None,
        "budgetMax": None,
        "interests": [text]
    }


def _validate_parsed_query(parsed: Dict[str, Any]) -> bool:
    """
    Validate parsed query structure.
    """
    required_keys = ["recipientType", "age", "budgetMin", "budgetMax", "interests"]
    if not all(key in parsed for key in required_keys):
        return False
    
    if not isinstance(parsed.get("interests"), list):
        return False
    
    return True