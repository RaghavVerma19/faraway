from datetime import datetime, timezone


def cache_filing(source, symbol, filing_type, title, url, published_at, severity=0):
    conn = __import__('db').conn.get_conn()
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    conn.execute(
        """INSERT OR REPLACE INTO filings_cache
           (source, symbol, filing_type, title, url, published_at, severity, fetched_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
        (source, symbol.upper(), filing_type, title, url, published_at, severity, now)
    )
    conn.commit()


def get_cached_filings(symbol=None, limit=100):
    conn = __import__('db').conn.get_conn()
    if symbol:
        rows = conn.execute(
            "SELECT * FROM filings_cache WHERE symbol = ? ORDER BY published_at DESC LIMIT ?",
            (symbol.upper(), limit)
        ).fetchall()
    else:
        rows = conn.execute(
            "SELECT * FROM filings_cache ORDER BY published_at DESC LIMIT ?",
            (limit,)
        ).fetchall()
    return [dict(r) for r in rows]


def clear_old_filings(max_age_hours=72):
    conn = __import__('db').conn.get_conn()
    conn.execute(
        "DELETE FROM filings_cache WHERE datetime(fetched_at) < datetime('now', ? || ' hours')",
        (-max_age_hours,)
    )
    conn.commit()
