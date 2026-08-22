from datetime import datetime, timezone


def log_signal(symbol, event_type, source, raw_score, headline, details=""):
    conn = __import__('db').conn.get_conn()
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    conn.execute(
        """INSERT INTO signal_log (timestamp, symbol, event_type, source, raw_score, headline, details)
           VALUES (?, ?, ?, ?, ?, ?, ?)""",
        (now, symbol.upper(), event_type, source, raw_score, headline, details)
    )
    conn.commit()


def get_signal_log(symbol=None, limit=200):
    conn = __import__('db').conn.get_conn()
    if symbol:
        rows = conn.execute(
            "SELECT * FROM signal_log WHERE symbol = ? ORDER BY timestamp DESC LIMIT ?",
            (symbol.upper(), limit)
        ).fetchall()
    else:
        rows = conn.execute(
            "SELECT * FROM signal_log ORDER BY timestamp DESC LIMIT ?",
            (limit,)
        ).fetchall()
    return [dict(r) for r in rows]
