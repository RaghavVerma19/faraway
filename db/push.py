from .conn import get_conn


def save_push_subscription(endpoint: str, auth_key: str, p256dh_key: str):
    conn = get_conn()
    conn.execute(
        "INSERT OR REPLACE INTO push_subscriptions (endpoint, auth_key, p256dh_key) "
        "VALUES (?, ?, ?)",
        (endpoint, auth_key, p256dh_key),
    )
    conn.commit()


def get_push_subscriptions() -> list[dict]:
    conn = get_conn()
    rows = conn.execute(
        "SELECT * FROM push_subscriptions"
    ).fetchall()
    return [dict(r) for r in rows]


def delete_push_subscription(endpoint: str):
    conn = get_conn()
    conn.execute(
        "DELETE FROM push_subscriptions WHERE endpoint = ?",
        (endpoint,)
    )
    conn.commit()


def delete_all_push_subscriptions():
    conn = get_conn()
    conn.execute("DELETE FROM push_subscriptions")
    conn.commit()
