import re

CRASH_IMPACT_KEYWORDS = {
    "collapse": 15, "crash": 12, "plunge": 10, "tumble": 10,
    "crater": 15, "nosedive": 12, "slump": 8, "free fall": 15,
    "plummet": 10, "dive": 8, "skid": 6, "slid": 6,
    "default": 10, "bankruptcy": 15, "insolvency": 12,
    "resignation": 8, "probe": 6, "scam": 12, "fraud": 15,
    "sebi": 6, "penalty": 5, "fine": 4, "violation": 5,
    "investigation": 6, "lawsuit": 6, "charge": 6,
    "arrest": 10, "raids": 8, "seizure": 10, "loss": 5,
    "miss": 5, "cut": 6, "downgrade": 5, "warning": 4,
    "dispute": 4, "withdraw": 4, "cancel": 4, "delay": 3,
    "pledge": 6, "npa": 10, "write-off": 8, "impairment": 8,
    "restriction": 5, "ban": 6, "suspension": 5, "under scrutiny": 6,
    "allegation": 6, "whistleblower": 6, "drought": 3, "fire": 5,
    "accident": 6, "strike": 4,
}

SURGE_IMPACT_KEYWORDS = {
    "surge": 10, "rally": 8, "boom": 10, "record": 5,
    "beat": 6, "upgrade": 5, "outperform": 5, "win": 5,
    "deal": 4, "dividend": 3, "buyback": 4, "expansion": 5,
    "growth": 4, "profit": 5, "revenue": 5, "acquisition": 5,
    "approval": 4, "clearance": 4, "partner": 3, "investment": 4,
    "breakout": 6, "high": 3,
}

PCT_RE = re.compile(r'(\d+(?:\.\d+)?)\s*%')


def estimate_price_impact(headline, source_credibility=1.0):
    """
    Estimate the potential stock price impact (%) from a headline.
    Returns a signed percentage: negative for drop, positive for gain.
    """
    if not headline:
        return 0.0

    text = headline.lower()
    explicit = PCT_RE.findall(text)
    pcts = [float(p) for p in explicit]

    if "loss" in text and pcts:
        return round(-pcts[0] * source_credibility, 1)
    if "plunge" in text and pcts:
        return round(-pcts[0] * source_credibility, 1)
    if ("surge" in text or "rally" in text) and pcts:
        return round(pcts[0] * source_credibility, 1)

    for kw, pct in CRASH_IMPACT_KEYWORDS.items():
        if kw in text:
            return round(-pct * source_credibility, 1)

    for kw, pct in SURGE_IMPACT_KEYWORDS.items():
        if kw in text:
            return round(pct * source_credibility, 1)

    return 0.0


def format_impact(impact_pct):
    """Format the impact percentage for display."""
    if impact_pct > 0:
        return "+" + str(impact_pct) + "%"
    if impact_pct < 0:
        return str(impact_pct) + "%"
    return "~"
