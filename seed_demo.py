import os
import sys
import json
import random
from datetime import datetime, timezone, timedelta

sys.path.insert(0, os.path.dirname(__file__))

from env_utils import load_local_env
load_local_env()

import db
from settings import settings

DEMO_SYMBOLS = ["RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "TATAMOTORS", "MARUTI"]
DEMO_COMPANIES = {
    "RELIANCE": "Reliance Industries",
    "TCS": "Tata Consultancy Services",
    "HDFCBANK": "HDFC Bank",
    "INFY": "Infosys",
    "ICICIBANK": "ICICI Bank",
    "SBIN": "State Bank of India",
    "TATAMOTORS": "Tata Motors",
    "MARUTI": "Maruti Suzuki",
}

DEMO_HEADLINES = [
    "SEBI initiates probe into {company} for insider trading violations",
    "Auditor resignation raises red flags for {company} quarterly results",
    "{company} reports massive quarterly loss amid global slowdown",
    "Promoter pledge levels rise sharply at {company}",
    "ED raids {company} offices in money laundering investigation",
    "{company} guidance cut: analysts downgrade to SELL",
    "Board meeting unscheduled at {company} amid governance concerns",
    "{company} faces penalty from SEBI for disclosure violations",
    "Insider trading allegations surface against {company} executives",
    "{company} stock plunges 5% on negative news flow",
]

DEMO_FILINGS = [
    {"type": "auditor_resignation", "title": "Auditor resignation - Deloitte resigns as statutory auditor"},
    {"type": "sebi_probe", "title": "SEBI probe into alleged insider trading"},
    {"type": "result_miss", "title": "Q3 results: PAT down 40% YoY"},
    {"type": "guidance_cut", "title": "Management cuts FY25 guidance"},
    {"type": "promoter_pledge", "title": "Promoter pledge increases to 35%"},
]

now = datetime.now(timezone.utc)


def seed_demo():
    db.init_db()
    db.migrate_db()

    for sym in DEMO_SYMBOLS:
        db.add_to_watchlist(sym)

    for _ in range(15):
        sym = random.choice(DEMO_SYMBOLS)
        company = DEMO_COMPANIES[sym]
        headline = random.choice(DEMO_HEADLINES).format(company=company)
        tier = random.choice(["CRITICAL", "HIGH", "WATCH"])
        score = random.randint(35, 95)
        signals = [
            {"type": "news", "source": random.choice(["reuters", "et_markets", "moneycontrol"]), "weight": random.randint(10, 25), "detail": "Keyword match", "event_type_hash": "news"},
            {"type": "filing", "source": "nse", "weight": random.randint(20, 40), "detail": random.choice(["Auditor resignation", "SEBI probe", "Result miss"]), "event_type_hash": "filing"},
        ]
        if score >= 70:
            signals.append({"type": "groq", "source": "groq", "weight": 15, "detail": "High AI panic score", "event_type_hash": "groq"})
        reasoning = ", ".join(s["detail"] for s in signals)

        db.log_alert(
            tier=tier,
            trust_score=score,
            symbol=sym,
            company=company,
            headline=headline,
            action="SELL" if tier in ("CRITICAL", "HIGH") else "WATCH",
            signals=signals,
            reasoning=reasoning,
        )

    for _ in range(25):
        sym = random.choice(DEMO_SYMBOLS)
        source = random.choice(["nse", "bse", "sebi", "reuters", "et_markets", "moneycontrol", "finnhub", "newsapi"])
        event_type = random.choice(["filing", "news", "technical"])
        headline = random.choice(DEMO_HEADLINES).format(company=DEMO_COMPANIES[sym])
        db.log_signal(
            symbol=sym,
            event_type=event_type,
            source=source,
            raw_score=random.randint(5, 50),
            headline=headline,
            details=f"Demo {event_type} signal",
        )

    for _ in range(12):
        sym = random.choice(DEMO_SYMBOLS)
        filing = random.choice(DEMO_FILINGS)
        db.cache_filing(
            source=random.choice(["nse", "bse", "sebi"]),
            symbol=sym,
            filing_type=filing["type"],
            title=filing["title"],
            url="https://www.nseindia.com",
            published_at=(now - timedelta(hours=random.randint(0, 48))).isoformat(),
            severity=random.randint(30, 90),
        )

    print(f"Seeded {len(DEMO_SYMBOLS)} watchlist symbols")
    print(f"Seeded 15 demo alerts")
    print(f"Seeded 25 demo signals")
    print(f"Seeded 12 demo filings")
    print("Demo data ready. Run: python app.py")


if __name__ == "__main__":
    seed_demo()
