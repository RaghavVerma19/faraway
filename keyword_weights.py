CRASH_KEYWORDS = {
    "fraud", "probe", "scam", "arrest", "raids", "seizure",
    "default", "bankruptcy", "insolvency", "loss", "miss",
    "cut", "downgrade", "penalty", "fine", "violation",
    "resignation", "quit", "exit", "withdraw", "cancel",
    "ban", "restriction", "sanction", "debarr", "suspension",
    "pledge", "default", "npa", "write-off", "impairment",
    "plunge", "crash", "tumble", "slump", "fall", "drop",
    "under scrutiny", "investigation", "allegation", "whistleblower",
    "sebi", "sfio", "ed", "cbi", "income tax",
    "auditor", "statutory", "delay", "dispute", "litigation",
    "drought", "flood", "fire", "accident", "strike",
}

SURGE_KEYWORDS = {
    "order", "contract", "win", "acquisition", "merger",
    "investment", "partner", "deal", "approval", "clearance",
    "dividend", "buyback", "split", "bonus", "expansion",
    "record", "high", "surge", "rally", "boom", "breakout",
    "outperform", "upgrade", "target", "estimates", "beat",
    "guidance", "raise", "growth", "profit", "revenue",
}

SOURCE_CREDIBILITY = {
    "reuters": 1.0,
    "bloomberg_quint": 1.0,
    "et_markets": 0.9,
    "moneycontrol": 0.9,
    "cnbc_tv18": 0.9,
    "mint": 0.85,
    "business_standard": 0.8,
    "financial_express": 0.7,
    "finnhub": 0.8,
    "newsapi": 0.6,
    "x_rss": 0.4,
}

FILING_SEVERITY_WEIGHTS = {
    "auditor_resignation": 90,
    "sebi_probe": 85,
    "sebi_penalty": 85,
    "sebi_order": 85,
    "result_miss": 80,
    "guidance_cut": 80,
    "board_meeting_unscheduled": 60,
    "generic_announcement": 30,
    "promoter_pledge": 70,
    "insider_trading": 85,
    "corporate_action": 40,
}

COMPOSITE_SCORE_TABLE = [
    ("filing", "critical", 40),
    ("filing", "high", 30),
    ("filing", "medium", 20),
    ("filing", "low", 10),
    ("news", "high_cred_crash", 25),
    ("news", "medium_cred_crash", 15),
    ("technical", "price_drop_2pct_15min", 20),
    ("technical", "price_drop_1pct_30min", 15),
    ("technical", "volume_spike_3x", 10),
    ("multi_source", "same_story", 10),
    ("groq", "high_panic", 15),
]
