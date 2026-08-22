from datetime import datetime
# Step 3: invalidate stats cache when new alert arrives
try:
    from cache import stats_cache
except Exception:
    stats_cache = None


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
    # Step 3: clear cached stats so next /api/stats shows fresh count
    if stats_cache:
        try:
            stats_cache.delete("stats")
        except Exception:
            pass
    return cur.lastrowid


def get_alerts(limit=100, tier=None, offset=0):
    conn = __import__('db').conn.get_conn()
    # Filter by tier in SQL - fast with idx_alerts_tier_timestamp
    if tier and tier != 'all' and tier != '':
        rows = conn.execute(
            "SELECT * FROM alerts WHERE tier = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?",
            (tier, limit, offset)
        ).fetchall()
    else:
        rows = conn.execute(
            "SELECT * FROM alerts ORDER BY timestamp DESC LIMIT ? OFFSET ?",
            (limit, offset)
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


def count_alerts_by_tier_today():
    conn = __import__('db').conn.get_conn()
    row = conn.execute("""
        SELECT 
          COUNT(*) as total,
          COALESCE(SUM(CASE WHEN tier='CRITICAL' THEN 1 ELSE 0 END),0) as critical,
          COALESCE(SUM(CASE WHEN tier='HIGH' THEN 1 ELSE 0 END),0) as high,
          COALESCE(SUM(CASE WHEN tier='WATCH' THEN 1 ELSE 0 END),0) as watch
        FROM alerts 
        WHERE date(timestamp) = date('now', 'localtime')
    """).fetchone()
    if row:
        return dict(row)
    return {"total": 0, "critical": 0, "high": 0, "watch": 0}


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