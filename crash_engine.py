import hashlib
import time
import threading
from collections import defaultdict
from datetime import datetime

from env_utils import load_local_env
from settings import settings
from fetcher import get_all_headlines
from brain import analyze_crash_batch, is_groq_available
from filings import get_all_filings
from price_service import fetch_prices
from keyword_weights import (
    CRASH_KEYWORDS,
    SURGE_KEYWORDS,
    SOURCE_CREDIBILITY,
    FILING_SEVERITY_WEIGHTS,
)
from validator import resolve_company
from db.alerts import log_alert, get_alerts, get_recent_alerts_for_symbol
from db.watchlist import get_watchlist, is_on_watchlist
from db.signal_log import log_signal
from db.filings_cache import cache_filing
from notifier import send_alert


load_local_env()


def _check_keywords(text):
    text_lower = (text or "").lower()
    crash_hits = [kw for kw in CRASH_KEYWORDS if kw in text_lower]
    surge_hits = [kw for kw in SURGE_KEYWORDS if kw in text_lower]
    return crash_hits, surge_hits


def _score_filing(filing):
    severity_map = {
        "auditor_resignation": "critical",
        "sebi_probe": "critical",
        "sebi_penalty": "critical",
        "sebi_order": "critical",
        "promoter_pledge": "high",
        "insider_trading": "critical",
        "result_miss": "high",
        "guidance_cut": "high",
        "board_meeting": "medium",
        "corporate_action": "medium",
        "generic_announcement": "low",
    }
    ftype = filing.get("filing_type", "generic_announcement")
    severity = severity_map.get(ftype, "low")
    weight = FILING_SEVERITY_WEIGHTS.get(ftype, 30)
    source = filing.get("source", "")
    cred = SOURCE_CREDIBILITY.get(source, 0.5)
    score = int(weight * cred)
    event_hash = filing.get("hash", "")
    return score, severity, source, event_hash


def _score_news(headline, source):
    crash_hits, surge_hits = _check_keywords(headline)
    cred = SOURCE_CREDIBILITY.get(source, 0.5)
    score = 0
    if crash_hits:
        if cred >= 0.9:
            score += 25
        else:
            score += 15
    if surge_hits:
        score += 5
    return score, crash_hits, surge_hits


def _score_technical(symbol):
    try:
        prices = fetch_prices([symbol])
        price = prices.get(symbol.upper())
        if not price:
            return 0, []
        signals = []
        # Simple technical scoring based on price service data
        # In a fuller implementation, we'd use yfinance for historical data
        # For now, we just return 0 if we can't compute MAs
        return 0, signals
    except Exception:
        return 0, []


def _dedup_alert(symbol, signal_hashes, tier, window_hours=2):
    recent = get_recent_alerts_for_symbol(symbol, hours=window_hours)
    for alert in recent:
        details = alert.get("signals", [])
        for sig in details:
            if sig.get("event_type_hash") in signal_hashes:
                return True
        if alert.get("tier") == tier and alert.get("action") == ("SELL" if tier in ("CRITICAL", "HIGH") else "WATCH"):
            return True
    return False


def _compute_composite_score(filing_scores, news_scores, tech_score, groq_score):
    total = 0
    signals = []

    for score, severity, source, event_hash in filing_scores:
        total += score
        signals.append({
            "type": "filing",
            "source": source,
            "weight": score,
            "detail": f"{severity} filing",
            "event_type_hash": event_hash,
        })

    for score, crash_hits, source, event_hash in news_scores:
        total += score
        if score > 0:
            signals.append({
                "type": "news",
                "source": source,
                "weight": score,
                "detail": f"Keywords: {', '.join(crash_hits[:3])}",
                "event_type_hash": event_hash,
            })

    if tech_score > 0:
        total += tech_score
        signals.append({
            "type": "technical",
            "source": "price_service",
            "weight": tech_score,
            "detail": "Technical signal",
            "event_type_hash": "tech",
        })

    if groq_score >= 80:
        total += 15
        signals.append({
            "type": "groq",
            "source": "groq",
            "weight": 15,
            "detail": "High AI panic score",
            "event_type_hash": "groq",
        })

    return total, signals


def _tier_from_score(score):
    if score >= 70:
        return "CRITICAL"
    if score >= 50:
        return "HIGH"
    if score >= 30:
        return "WATCH"
    return "IGNORE"


def _process_headline(headline):
    text = headline.get("headline", "")
    source = headline.get("source", "")
    fingerprint = headline.get("fingerprint", "")

    resolved = resolve_company(text)
    if not resolved:
        return None

    symbol = resolved.get("symbol", "")
    if not symbol:
        return None

    crash_hits, surge_hits = _check_keywords(text)
    if not crash_hits and not surge_hits:
        return None

    score, crash_hits_out, surge_hits_out = _score_news(text, source)
    if score <= 0:
        return None

    return {
        "symbol": symbol,
        "company": resolved.get("symbol", symbol),
        "score": score,
        "source": source,
        "headline": text,
        "fingerprint": fingerprint,
        "crash_hits": crash_hits_out,
        "surge_hits": surge_hits_out,
    }


def _process_filing(filing):
    score, severity, source = _score_filing(filing)
    symbol = filing.get("symbol", "")
    return {
        "symbol": symbol,
        "company": filing.get("company", symbol),
        "score": score,
        "source": source,
        "filing_type": filing.get("filing_type", ""),
        "title": filing.get("title", ""),
        "hash": filing.get("hash", ""),
        "severity": severity,
    }


def run_crash_engine():
    print("NEWSPULSE CRASH TRACKER ENGINE: active")
    cycle = 0
    while True:
        try:
            cycle += 1
            scan_time = time.time()

            filings = get_all_filings()
            for f in filings:
                cache_filing(
                    f["source"], f["symbol"], f["filing_type"],
                    f["title"], f["url"], f["published_at"],
                    FILING_SEVERITY_WEIGHTS.get(f["filing_type"], 30)
                )
                log_signal(
                    f["symbol"], "filing", f["source"],
                    FILING_SEVERITY_WEIGHTS.get(f["filing_type"], 30),
                    f["title"], f"filing_type={f['filing_type']}"
                )

            headlines = get_all_headlines()
            processed_headlines = []
            for h in headlines:
                result = _process_headline(h)
                if result:
                    processed_headlines.append(result)

            symbol_scores = defaultdict(lambda: {
                "filing_scores": [],
                "news_scores": [],
                "tech_score": 0,
                "groq_score": 0,
                "headline": "",
                "sources": set(),
            })

            for f in filings:
                score, severity, source, event_hash = _score_filing(f)
                key = f["symbol"]
                symbol_scores[key]["filing_scores"].append(
                    (score, severity, source, event_hash)
                )
                symbol_scores[key]["headline"] = f["title"]
                symbol_scores[key]["sources"].add(source)

            for h in processed_headlines:
                key = h["symbol"]
                symbol_scores[key]["news_scores"].append(
                    (h["score"], h["crash_hits"], h["source"], h["fingerprint"])
                )
                if len(h["headline"]) > len(symbol_scores[key]["headline"]):
                    symbol_scores[key]["headline"] = h["headline"]
                symbol_scores[key]["sources"].add(h["source"])

            ambiguous_symbols = []
            for symbol, data in symbol_scores.items():
                filing_score = sum(s[0] for s in data["filing_scores"])
                news_score = sum(s[0] for s in data["news_scores"])
                base_score = filing_score + news_score
                data["tech_score"], _ = _score_technical(symbol)

                total, signals = _compute_composite_score(
                    data["filing_scores"],
                    data["news_scores"],
                    data["tech_score"],
                    data["groq_score"],
                )
                tier = _tier_from_score(total)

                if total >= 30 and len(data["sources"]) >= 2:
                    ambiguous_symbols.append((symbol, data, total, tier, signals))

            groq_candidates = []
            for symbol, data, total, tier, signals in ambiguous_symbols:
                if total >= 30 and is_groq_available():
                    groq_candidates.append((symbol, data, total, tier, signals))

            groq_results = {}
            if groq_candidates:
                headlines_for_groq = []
                for symbol, data, total, tier, signals in groq_candidates:
                    headlines_for_groq.append({
                        "headline": data["headline"],
                        "source": list(data["sources"])[0],
                    })
                batch_size = 5
                for i in range(0, len(headlines_for_groq), batch_size):
                    batch = headlines_for_groq[i:i + batch_size]
                    analyses = analyze_crash_batch(batch)
                    for j, analysis in enumerate(analyses):
                        if analysis and analysis.get("crash_probability", 0) >= 30:
                            idx = i + j
                            if idx < len(groq_candidates):
                                sym = groq_candidates[idx][0]
                                groq_results[sym] = analysis.get("crash_probability", 0)

            alerts_fired = 0
            for symbol, data, total, tier, signals in ambiguous_symbols:
                groq_score = groq_results.get(symbol, 0)
                if groq_score >= 80:
                    total = min(100, total + 15)
                    tier = _tier_from_score(total)
                    signals.append({
                        "type": "groq",
                        "source": "groq",
                        "weight": 15,
                        "detail": f"AI crash_probability={groq_score}",
                        "event_type_hash": "groq",
                    })

                if tier == "IGNORE":
                    continue

                signal_hashes = set(s.get("event_type_hash", "") for s in signals if s.get("event_type_hash"))
                if _dedup_alert(symbol, signal_hashes, tier):
                    continue

                headline = data["headline"]
                reasoning = "; ".join(s.get("detail", "") for s in signals if s.get("detail"))

                alert_id = log_alert(
                    tier=tier,
                    trust_score=total,
                    symbol=symbol,
                    company=data.get("company", symbol),
                    headline=headline,
                    action="SELL" if tier in ("CRITICAL", "HIGH") else "WATCH",
                    signals=signals,
                    reasoning=reasoning,
                )

                webhook_payload = {
                    "alert_id": alert_id,
                    "timestamp": datetime.now().isoformat(),
                    "tier": tier,
                    "trust_score": total,
                    "company": data.get("company", symbol),
                    "symbol": symbol,
                    "action": "SELL" if tier in ("CRITICAL", "HIGH") else "WATCH",
                    "signals": signals,
                    "headline": headline,
                    "reasoning": reasoning,
                }

                if tier in ("CRITICAL", "HIGH"):
                    send_alert(
                        title=f"[{tier}] {symbol} - Trust Score {total}",
                        body=headline,
                        webhook_payload=webhook_payload,
                    )

                alerts_fired += 1
                print(f"[ALERT] {tier} | {symbol} | Score={total} | {headline[:60]}")

            if cycle % 5 == 0 or alerts_fired > 0:
                print(f"[SCAN] Cycle {cycle}: {len(filings)} filings, {len(processed_headlines)} headlines, {alerts_fired} alerts")

        except Exception as exc:
            print(f"[ERROR] Crash engine cycle failed: {exc}")
            import traceback
            traceback.print_exc()

        time.sleep(settings.scan_interval_seconds)
