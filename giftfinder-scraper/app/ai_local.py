import os
import json
import requests
import re

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://ollama:11434")


# ============================================================
# Ejecuta LLM crudo
# ============================================================
def run_llm(prompt: str):
    try:
        response = requests.post(
            f"{OLLAMA_HOST}/api/generate",
            json={"model": "qwen2.5:1.5b", "prompt": prompt, "stream": False},
            timeout=30
        )
        return response.json().get("response", "")
    except Exception as e:
        print("❌ run_llm error:", e)
        return ""


# ============================================================
# Limpia bloques tipo ```json
# ============================================================
def clean_json_block(text: str):
    text = text.replace("```json", "").replace("```", "").strip()
    return text


# ============================================================
# JSON parser robusto — soporta objetos y listas
# ============================================================
def run_llm_json(prompt: str):
    raw = run_llm(prompt)
    if not raw:
        return {}

    cleaned = clean_json_block(raw)

    # --- Detecta JSON de lista ---
    if cleaned.startswith("[") and cleaned.endswith("]"):
        try:
            return json.loads(cleaned)
        except:
            return []

    # --- Detecta JSON de objeto ---
    if cleaned.startswith("{") and cleaned.endswith("}"):
        try:
            return json.loads(cleaned)
        except:
            return {}

    # --- Busca JSON dentro del texto ---
    match = re.search(r'(\[.*\]|\{.*\})', cleaned, re.DOTALL)
    if match:
        try:
            return json.loads(match.group(0))
        except:
            return {}

    return {}


# ============================================================
# parse_query ROBUSTO — nunca rompe
# ============================================================
def parse_query(text: str):
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

    raw = run_llm_json(prompt)

    # ✔ Si es lista → usar el primer elemento
    if isinstance(raw, list) and len(raw) > 0:
        raw = raw[0]

    # ✔ Si no es dict → fallback
    if not isinstance(raw, dict):
        print("⚠️ parse_query: fallback activado")
        return {
            "recipientType": "unknown",
            "age": None,
            "budgetMin": None,
            "budgetMax": None,
            "interests": [text]
        }

    return raw