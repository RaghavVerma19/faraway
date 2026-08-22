def add_to_watchlist(symbol):
    conn = __import__('db').conn.get_conn()
    conn.execute(
        "INSERT OR IGNORE INTO watchlist (symbol) VALUES (?)",
        (symbol.upper(),)
    )
    conn.commit()
    try:
        from cache import stats_cache
        stats_cache.delete("stats")
    except Exception:
        pass


def remove_from_watchlist(symbol):
    conn = __import__('db').conn.get_conn()
    conn.execute(
        "DELETE FROM watchlist WHERE symbol = ?",
        (symbol.upper(),)
    )
    conn.commit()
    try:
        from cache import stats_cache
        stats_cache.delete("stats")
    except Exception:
        pass


def get_watchlist():
    conn = __import__('db').conn.get_conn()
    rows = conn.execute("SELECT symbol, added_at FROM watchlist ORDER BY symbol").fetchall()
    return [dict(r) for r in rows]


def is_on_watchlist(symbol):
    conn = __import__('db').conn.get_conn()
    row = conn.execute(
        "SELECT 1 FROM watchlist WHERE symbol = ?",
        (symbol.upper(),)
    ).fetchone()
    return row is not None
