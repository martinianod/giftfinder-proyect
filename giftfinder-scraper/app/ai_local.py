"""
AI/LLM interaction module with robust error handling and timeouts.
"""

import json
import logging
import re
from typing import Any, Dict, Optional

import requests

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


class LLMError(Exception):
    """Custom exception for LLM-related errors."""

    pass


# ============================================================
# Ejecuta LLM crudo
# ============================================================
def run_llm(prompt: str, timeout: Optional[int] = None) -> str:
    """
    Execute LLM request with timeout and error handling.

    Args:
        prompt: The prompt to send to the LLM
        timeout: Optional timeout in seconds (uses settings default if not provided)

    Returns:
        LLM response string

    Raises:
        LLMError: If LLM request fails
    """
    if timeout is None:
        timeout = settings.llm_timeout_seconds

    try:
        logger.debug(f"Sending LLM request", extra={"prompt_length": len(prompt)})

        response = requests.post(
            f"{settings.ollama_host}/api/generate",
            json={"model": settings.ollama_model, "prompt": prompt, "stream": False},
            timeout=timeout,
        )
        response.raise_for_status()

        result = response.json().get("response", "")

        logger.debug(f"LLM response received", extra={"response_length": len(result)})

        return result

    except requests.exceptions.Timeout:
        logger.error(f"LLM request timeout after {timeout}s")
        raise LLMError(f"LLM request timeout after {timeout}s")

    except requests.exceptions.RequestException as e:
        logger.error(f"LLM request error: {e}")
        raise LLMError(f"LLM request failed: {str(e)}")

    except Exception as e:
        logger.error(f"Unexpected LLM error: {e}")
        raise LLMError(f"Unexpected error: {str(e)}")


# ============================================================
# Limpia bloques tipo ```json
# ============================================================
def clean_json_block(text: str):
    text = text.replace("```json", "").replace("```", "").strip()
    return text


# ============================================================
# JSON parser robusto — soporta objetos y listas
# ============================================================
def run_llm_json(prompt: str) -> Dict[str, Any]:
    """
    Execute LLM request and parse JSON response.

    Args:
        prompt: The prompt to send to the LLM

    Returns:
        Parsed JSON as dict or list, or empty dict/list on failure
    """
    try:
        raw = run_llm(prompt)
    except LLMError:
        logger.warning("LLM request failed, returning empty result")
        return {}

    if not raw:
        return {}

    cleaned = clean_json_block(raw)

    # --- Detecta JSON de lista ---
    if cleaned.startswith("[") and cleaned.endswith("]"):
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse JSON list: {e}")
            return []

    # --- Detecta JSON de objeto ---
    if cleaned.startswith("{") and cleaned.endswith("}"):
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse JSON object: {e}")
            return {}

    # --- Busca JSON dentro del texto ---
    match = re.search(r"(\[.*\]|\{.*\})", cleaned, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse extracted JSON: {e}")
            return {}

    logger.warning("No valid JSON found in LLM response")
    return {}


def _get_fallback_parse(text: str) -> Dict[str, Any]:
    """
    Provide fallback parse result when LLM fails.

    Args:
        text: Original query text

    Returns:
        Valid parse structure with default values
    """
    return {
        "recipientType": "unknown",
        "age": None,
        "budgetMin": None,
        "budgetMax": None,
        "interests": [text.strip()[:50]],  # Use first 50 chars as interest
    }


# ============================================================
# parse_query ROBUSTO — nunca rompe
# ============================================================
def parse_query(text: str) -> Dict[str, Any]:
    """
    Parse user query into structured format using LLM.

    Args:
        text: User query string

    Returns:
        Structured query data with recipientType, age, budgets, and interests
    """
    # Sanitize input
    text = text.strip()
    if len(text) > settings.max_query_length:
        text = text[: settings.max_query_length]
        logger.warning(f"Query truncated to {settings.max_query_length} chars")

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

        logger.debug(
            f"Query parsed",
            extra={
                "query_length": len(text),
                "parsed_keys": list(raw.keys()) if isinstance(raw, dict) else [],
            },
        )

    except Exception as e:
        logger.warning(f"Parse query failed, using fallback: {e}")
        return _get_fallback_parse(text)

    # ✔ Si es lista → usar el primer elemento
    if isinstance(raw, list) and len(raw) > 0:
        raw = raw[0]

    # ✔ Si no es dict → fallback
    if not isinstance(raw, dict):
        logger.warning("parse_query: fallback activado (not a dict)")
        return _get_fallback_parse(text)

    # Validate and normalize fields
    result = {
        "recipientType": raw.get("recipientType", "unknown"),
        "age": raw.get("age"),
        "budgetMin": raw.get("budgetMin"),
        "budgetMax": raw.get("budgetMax"),
        "interests": raw.get("interests", [text.strip()[:50]]),
    }

    # Ensure interests is a list
    if not isinstance(result["interests"], list):
        result["interests"] = [text.strip()[:50]]

    return result
