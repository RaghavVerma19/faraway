import json
import os
import threading
from datetime import datetime, timezone
from pathlib import Path

from env_utils import load_local_env
from settings import settings
from notifier import send_alert

load_local_env()

DATA_DIR = Path(settings.data_dir)
ALERTS_FILE = DATA_DIR / "alerts.json"


def _ensure_data_dir():
    DATA_DIR.mkdir(parents=True, exist_ok=True)


def _load_local_alerts():
    _ensure_data_dir()
    if not ALERTS_FILE.exists():
        return []
    try:
        with open(str(ALERTS_FILE), "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return []


def _save_local_alerts(alerts):
    _ensure_data_dir()
    try:
        with open(str(ALERTS_FILE), "w", encoding="utf-8") as f:
            json.dump(alerts, f, indent=2, default=str)
    except Exception as exc:
        print(f"[ALERTS] Failed to save local alerts: {exc}")


def dispatch_alert(alert_data):
    tier = alert_data.get("tier", "WATCH")
    trust_score = alert_data.get("trust_score", 0)
    symbol = alert_data.get("symbol", "")
    headline = alert_data.get("headline", "")
    reasoning = alert_data.get("reasoning", "")
    signals = alert_data.get("signals", [])

    action = "SELL" if tier in ("CRITICAL", "HIGH") else "WATCH"
    title = f"[{tier}] {symbol} - Trust Score {trust_score}"

    webhook_payload = {
        "alert_id": alert_data.get("id"),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "tier": tier,
        "trust_score": trust_score,
        "company": alert_data.get("company", symbol),
        "symbol": symbol,
        "action": action,
        "signals": signals,
        "headline": headline,
        "groq_reasoning": reasoning,
        "suggested_action": "Consider exit. Monitor for gap-down opening." if tier in ("CRITICAL", "HIGH") else "Monitor closely.",
    }

    send_alert(title=title, body=headline, webhook_payload=webhook_payload)

    local_alerts = _load_local_alerts()
    local_alerts.insert(0, webhook_payload)
    local_alerts = local_alerts[:500]
    _save_local_alerts(local_alerts)

    return webhook_payload
