package com.newspulse.ai.domain

object KeywordWeights {
    val CRASH_KEYWORDS = setOf(
        // Severe regulatory & forensic
        "fraud", "probe", "scam", "arrest", "raids", "seizure",
        "default", "bankruptcy", "insolvency", "loss", "miss",
        "cut", "downgrade", "penalty", "fine", "violation",
        "resignation", "quit", "exit", "withdraw", "cancel",
        "ban", "restriction", "sanction", "debarr", "suspension",
        "pledge", "npa", "write-off", "impairment",
        "plunge", "crash", "tumble", "slump", "fall", "drop", "skid", "slide",
        "under scrutiny", "investigation", "allegation", "whistleblower",
        "sebi", "sfio", "ed", "cbi", "income tax", "tax notice", "gst notice",
        "auditor", "statutory", "delay", "dispute", "litigation", "lawsuit",
        "debt", "downgraded", "pat down", "profit falls", "revenue misses",
        "target cut", "sell rating", "underperform", "forensic audit"
    )

    val SURGE_KEYWORDS = setOf(
        "order", "contract", "win", "acquisition", "merger",
        "investment", "partner", "deal", "approval", "clearance",
        "dividend", "buyback", "split", "bonus", "expansion",
        "record", "high", "surge", "rally", "boom", "breakout", "jump", "gains",
        "outperform", "upgrade", "target raised", "estimates", "beat",
        "guidance", "raise", "growth", "profit jumps", "revenue up", "pat surges"
    )

    val SOURCE_CREDIBILITY = mapOf(
        "reuters_in" to 1.0,
        "et_markets" to 0.95,
        "moneycontrol_latest" to 0.95,
        "moneycontrol_market" to 0.95,
        "moneycontrol_buzzing" to 0.95,
        "mint_markets" to 0.90,
        "mint_companies" to 0.90,
        "cnbc_tv18" to 0.90,
        "business_standard" to 0.85,
        "business_standard_companies" to 0.85,
        "financial_express" to 0.85,
        "ndtv_profit" to 0.85,
        "the_hindu_businessline" to 0.85,
        "zee_business" to 0.80,
        "google_market_crash" to 0.90,
        "google_sebi_action" to 0.95,
        "google_corporate_actions" to 0.90,
        "google_earnings_miss" to 0.90,
        "finnhub" to 0.80,
        "newsapi" to 0.70
    )

    val FILING_SEVERITY_WEIGHTS = mapOf(
        "auditor_resignation" to 90,
        "sebi_probe" to 85,
        "sebi_penalty" to 85,
        "sebi_order" to 85,
        "result_miss" to 80,
        "guidance_cut" to 80,
        "promoter_pledge" to 70,
        "insider_trading" to 85,
        "generic_announcement" to 30
    )
}
