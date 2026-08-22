import json
import os
import time
from urllib import error, request

from env_utils import load_local_env

load_local_env()
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
BATCH_SIZE = 5
_groq_401 = False


def _normalize_crash_analysis(payload):
    crash_prob = payload.get("crash_probability", 0)
    try:
        crash_prob = int(round(float(crash_prob)))
    except (TypeError, ValueError):
        crash_prob = 0
    crash_prob = max(0, min(100, crash_prob))
    confidence = payload.get("confidence", "LOW")
    conf = (confidence or "").strip().upper()
    if conf not in ("HIGH", "MEDIUM", "LOW"):
        conf = "LOW"
    return {
        "crash_probability": crash_prob,
        "primary_signals": payload.get("primary_signals", []),
        "reasoning": str(payload.get("reasoning", "")).strip() or "No reasoning provided.",
        "confidence": conf,
    }


def _call_groq(messages, max_tokens=300, temperature=0):
    groq_api_key = os.getenv("GROQ_API_KEY")
    if not groq_api_key:
        print("[AI ERROR] GROQ_API_KEY is missing.")
        return None

    last_error = None
    for attempt in range(3):
        try:
            req = request.Request(
                GROQ_URL,
                headers={
                    "Authorization": f"Bearer {groq_api_key}",
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "User-Agent": "NewsPulseAI/1.0 (+local-debug)",
                },
                data=json.dumps({
                    "model": "openai/gpt-oss-120b",
                    "messages": messages,
                    "temperature": temperature,
                    "max_tokens": max_tokens,
                }).encode("utf-8"),
                method="POST",
            )
            with request.urlopen(req, timeout=45) as response:
                payload = json.loads(response.read().decode("utf-8"))
            content = payload["choices"][0]["message"]["content"]
            return json.loads(content)
        except error.HTTPError as exc:
            try:
                body = exc.read().decode("utf-8", errors="replace")
            except Exception:
                body = "<unable to read>"
            print(f"[AI ERROR] HTTP {exc.code}: {exc.reason} | body={body[:500]}")
            last_error = exc
            if exc.code == 401:
                global _groq_401
                _groq_401 = True
                return None
        except Exception as exc:
            print(f"[AI ERROR] {exc}")
            last_error = exc
        if attempt < 2:
            time.sleep(5)
    return None


def analyze_crash_headline(headline, source):
    system_prompt = (
        "You analyze financial headlines for Indian market crash/surge detection. "
        "Return only minified JSON with keys: crash_probability (0-100 integer), "
        "primary_signals (array of strings), reasoning (string), confidence (HIGH/MEDIUM/LOW). "
        "crash_probability is the likelihood of a significant negative price move. "
        "confidence reflects how certain you are."
    )

    user_prompt = (
        f"Source: {source}\n"
        f"Headline: {headline.strip()[:300]}\n"
        "Assess crash probability for relevant Indian listed companies."
    )

    result = _call_groq([
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ])
    if result:
        return _normalize_crash_analysis(result)
    return None


def analyze_crash_batch(headlines):
    if not headlines:
        return []

    system_prompt = (
        "You analyze financial headlines for Indian market crash/surge detection. "
        "For each headline, return a JSON array of objects with keys: "
        "crash_probability (0-100 integer), primary_signals (array of strings), "
        "reasoning (string), confidence (HIGH/MEDIUM/LOW). "
        "crash_probability is the likelihood of a significant negative price move. "
        "Return a valid JSON array only, no markdown fences."
    )

    items_text = "\n---\n".join(
        f"Source: {h['source']}\nHeadline: {h['headline'].strip()[:300]}"
        for h in headlines
    )

    user_prompt = (
        f"Analyze each of the following {len(headlines)} headlines:\n\n{items_text}"
    )

    result = _call_groq([
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt},
    ], max_tokens=500 * len(headlines))

    if not result:
        global _groq_401
        if _groq_401:
            return [None] * len(headlines)
        return [analyze_crash_headline(h["headline"], h["source"]) for h in headlines]

    if isinstance(result, list) and len(result) == len(headlines):
        return [_normalize_crash_analysis(item) for item in result]

    print(f"[AI WARN] Batch response shape unexpected ({type(result).__name__}), falling back to single calls")
    return [analyze_crash_headline(h["headline"], h["source"]) for h in headlines]


def is_groq_available():
    global _groq_401
    if _groq_401:
        return False
    return bool(os.getenv("GROQ_API_KEY"))
