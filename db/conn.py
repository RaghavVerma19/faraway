import sqlite3
import threading
from pathlib import Path

from settings import settings

DB_PATH = Path(settings.resolved_db_path)
_thread_local = threading.local()


def get_conn():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = getattr(_thread_local, "conn", None)
    if conn is None:
        conn = sqlite3.connect(str(DB_PATH), timeout=5)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA synchronous=NORMAL")
        conn.execute("PRAGMA foreign_keys=ON")
        conn.execute("PRAGMA cache_size=-4000")
        conn.execute("PRAGMA temp_store=MEMORY")
        conn.execute("PRAGMA busy_timeout=5000")
        _thread_local.conn = conn
    return conn


def init_db():
    conn = get_conn()
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS watchlist (
            symbol      TEXT PRIMARY KEY,
            added_at    TEXT NOT NULL DEFAULT (datetime('now'))
        );

        CREATE TABLE IF NOT EXISTS alerts (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp   TEXT NOT NULL DEFAULT (datetime('now')),
            tier        TEXT NOT NULL,
            trust_score INTEGER NOT NULL,
            symbol      TEXT NOT NULL,
            company     TEXT NOT NULL DEFAULT '',
            headline    TEXT NOT NULL DEFAULT '',
            action      TEXT NOT NULL DEFAULT 'WATCH',
            signals_json TEXT NOT NULL DEFAULT '[]',
            reasoning   TEXT NOT NULL DEFAULT '',
            downward_potential INTEGER NOT NULL DEFAULT 0,
            created_at  TEXT NOT NULL DEFAULT (datetime('now'))
        );

        CREATE TABLE IF NOT EXISTS filings_cache (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            source      TEXT NOT NULL,
            symbol      TEXT NOT NULL,
            filing_type TEXT NOT NULL,
            title       TEXT NOT NULL DEFAULT '',
            url         TEXT NOT NULL DEFAULT '',
            published_at TEXT NOT NULL DEFAULT '',
            severity    INTEGER NOT NULL DEFAULT 0,
            fetched_at  TEXT NOT NULL DEFAULT (datetime('now'))
        );

        CREATE TABLE IF NOT EXISTS signal_log (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp   TEXT NOT NULL DEFAULT (datetime('now')),
            symbol      TEXT NOT NULL,
            event_type  TEXT NOT NULL,
            source      TEXT NOT NULL,
            raw_score   INTEGER NOT NULL DEFAULT 0,
            headline    TEXT NOT NULL DEFAULT '',
            details     TEXT NOT NULL DEFAULT ''
        );

        CREATE TABLE IF NOT EXISTS settings (
            key         TEXT PRIMARY KEY,
            value       TEXT NOT NULL DEFAULT ''
        );

        CREATE TABLE IF NOT EXISTS seen_news (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            fingerprint TEXT NOT NULL,
            seen_at     TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(fingerprint)
        );

        CREATE TABLE IF NOT EXISTS price_cache (
            symbol      TEXT PRIMARY KEY,
            price       REAL NOT NULL,
            updated_at  TEXT NOT NULL
        );

        CREATE INDEX IF NOT EXISTS idx_alerts_symbol ON alerts(symbol);
        CREATE INDEX IF NOT EXISTS idx_alerts_timestamp ON alerts(timestamp);
        CREATE INDEX IF NOT EXISTS idx_alerts_tier ON alerts(tier);
        CREATE INDEX IF NOT EXISTS idx_alerts_tier_timestamp ON alerts(tier, timestamp DESC);
        CREATE INDEX IF NOT EXISTS idx_signal_log_symbol ON signal_log(symbol);

        CREATE TABLE IF NOT EXISTS push_subscriptions (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            endpoint    TEXT NOT NULL,
            auth_key    TEXT NOT NULL,
            p256dh_key  TEXT NOT NULL,
            created_at  TEXT NOT NULL DEFAULT (datetime('now'))
        );
    """)
    conn.commit()


def migrate_db():
    conn = get_conn()
    try:
        conn.execute("ALTER TABLE alerts ADD COLUMN downward_potential INTEGER NOT NULL DEFAULT 0")
    except sqlite3.OperationalError:
        pass
    try:
        conn.execute("ALTER TABLE push_subscriptions RENAME TO push_subscriptions_old")
        conn.execute("""
            CREATE TABLE IF NOT EXISTS push_subscriptions (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                endpoint    TEXT NOT NULL,
                auth_key    TEXT NOT NULL,
                p256dh_key  TEXT NOT NULL,
                created_at  TEXT NOT NULL DEFAULT (datetime('now'))
            )
        """)
        conn.execute("INSERT OR IGNORE INTO push_subscriptions (id, endpoint, auth_key, p256dh_key, created_at) SELECT id, endpoint, auth_key, p256dh_key, created_at FROM push_subscriptions_old")
        conn.execute("DROP TABLE push_subscriptions_old")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS users")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS user_settings")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS holdings")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS cash_balance")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS events")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS trades")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS portfolio_events")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS seen_news")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS pending_trades")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS approval_requests")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS oauth_states")
    except Exception:
        pass
    try:
        conn.execute("DROP TABLE IF EXISTS price_cache")
    except Exception:
        pass
    conn.commit()
