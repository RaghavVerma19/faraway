def get_setting(key, default=""):
    conn = __import__('db').conn.get_conn()
    row = conn.execute(
        "SELECT value FROM settings WHERE key = ?",
        (key,)
    ).fetchone()
    if row:
        return row["value"]
    return default


def set_setting(key, value):
    conn = __import__('db').conn.get_conn()
    conn.execute(
        "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)",
        (key, str(value))
    )
    conn.commit()


def get_all_settings():
    conn = __import__('db').conn.get_conn()
    rows = conn.execute("SELECT key, value FROM settings").fetchall()
    return {r["key"]: r["value"] for r in rows}
