from datetime import datetime


def log_alert(tier, trust_score, symbol, company, headline, action, signals, reasoning):
    conn = __import__('db').conn.get_conn()
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    signals_json = __import__('json').dumps(signals)
    cur = conn.execute(
        """INSERT INTO alerts (timestamp, tier, trust_score, symbol, company, headline, action, signals_json, reasoning)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (now, tier, trust_score, symbol, company, headline, action, signals_json, reasoning)
    )
    conn.commit()
    return cur.lastrowid


def get_alerts(limit=100):
    conn = __import__('db').conn.get_conn()
    rows = conn.execute(
        "SELECT * FROM alerts ORDER BY timestamp DESC LIMIT ?",
        (limit,)
    ).fetchall()
    result = []
    for row in rows:
        r = dict(row)
        try:
            r["signals"] = __import__('json').loads(r.get("signals_json", "[]"))
        except Exception:
            r["signals"] = []
        del r["signals_json"]
        result.append(r)
    return result


def get_recent_alerts_for_symbol(symbol, hours=4):
    conn = __import__('db').conn.get_conn()
    rows = conn.execute(
        """SELECT * FROM alerts
           WHERE symbol = ?
           AND datetime(timestamp) >= datetime('now', ? || ' hours')
           ORDER BY timestamp DESC""",
        (symbol, -hours)
    ).fetchall()
    return [dict(r) for r in rows]
