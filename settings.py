import os
from dataclasses import dataclass
from pathlib import Path

from env_utils import load_local_env

load_local_env()


def _env_int(name, default):
    value = os.getenv(name)
    if value is None:
        return default
    try:
        return int(value)
    except ValueError:
        return default


def _env_float(name, default):
    value = os.getenv(name)
    if value is None:
        return default
    try:
        return float(value)
    except ValueError:
        return default


def _env_list(name, default):
    value = os.getenv(name)
    if value is None:
        return default
    return [v.strip() for v in value.split(",") if v.strip()]


@dataclass(frozen=True)
class Settings:
    scan_interval_seconds: int = _env_int("SCAN_INTERVAL_SECONDS", 60)

    panic_threshold: int = _env_int("PANIC_THRESHOLD", 50)

    data_dir: str = os.getenv("NEWSPULSE_DATA_DIR", "data")

    news_target_handles: tuple = tuple(
        _env_list("NEWS_TARGET_HANDLES",
                  ["ETMarkets", "moneycontrolcom", "DeItaone", "HindenburgRes", "FinMinIndia"])
    )
    news_rss_items_per_handle: int = _env_int("NEWS_RSS_ITEMS_PER_HANDLE", 1)
    news_finnhub_max_items: int = _env_int("NEWS_FINNHUB_MAX_ITEMS", 5)
    news_newsapi_page_size: int = _env_int("NEWS_NEWSAPI_PAGE_SIZE", 7)
    news_api_query: str = os.getenv("NEWS_API_QUERY",
                                     "Nifty OR Sensex OR crash OR plunge OR surge OR result OR penalty OR probe OR SEBI")
    fetch_retries: int = _env_int("FETCH_RETRIES", 2)

    db_path: str = os.getenv("NEWSPULSE_DB_PATH", "")

    vapid_contact_email: str = os.getenv("VAPID_CONTACT_EMAIL", "mailto:news-pulse@localhost")
    vapid_key_path: str = os.getenv("VAPID_KEY_PATH", "")

    alert_webhook_url: str = os.getenv("ALERT_WEBHOOK_URL", "")

    alert_tiers: tuple = (
        ("CRITICAL", 70),
        ("HIGH", 50),
        ("WATCH", 30),
    )

    @property
    def resolved_db_path(self):
        return self.db_path or str(Path(self.data_dir) / "news_pulse.db")

    @property
    def resolved_vapid_path(self):
        return self.vapid_key_path or str(Path(self.data_dir) / "vapid.pem")


settings = Settings()
