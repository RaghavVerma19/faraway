import base64
import json
from pathlib import Path

from settings import settings

VAPID_FILE = Path(settings.resolved_vapid_path)
VAPID_CLAIMS = {"sub": settings.vapid_contact_email}


def _ensure_vapid():
    from py_vapid import Vapid
    if VAPID_FILE.exists():
        return Vapid.from_file(str(VAPID_FILE))
    v = Vapid()
    v.generate_keys()
    VAPID_FILE.parent.mkdir(parents=True, exist_ok=True)
    v.save_key(str(VAPID_FILE))
    return v


def get_vapid_public_key() -> str:
    from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat
    v = _ensure_vapid()
    raw = v.public_key.public_bytes(Encoding.X962, PublicFormat.UncompressedPoint)
    return base64.urlsafe_b64encode(raw).decode()


def _vapid_private_pem(v) -> str:
    pem = v.private_pem
    if callable(pem):
        pem = pem()
    return pem.decode() if isinstance(pem, bytes) else pem


def send_push_to_all(title: str, body: str, url: str = "/") -> bool:
    from pywebpush import webpush, WebPushException
    from db.push import get_push_subscriptions
    subs = get_push_subscriptions()
    if not subs:
        return False

    v = _ensure_vapid()
    payload = json.dumps({"title": title, "body": body, "url": url})
    private_pem = _vapid_private_pem(v)

    sent = 0
    for sub in subs:
        try:
            webpush(
                subscription_info={
                    "endpoint": sub["endpoint"],
                    "keys": {"auth": sub["auth_key"], "p256dh": sub["p256dh_key"]},
                },
                data=payload,
                vapid_private_key=private_pem,
                vapid_claims=VAPID_CLAIMS,
            )
            sent += 1
        except WebPushException as exc:
            if exc.response and exc.response.status_code in (404, 410):
                from db.push import delete_push_subscription
                delete_push_subscription(sub["endpoint"])
            print(f"[PUSH] Failed to send to {sub['endpoint'][:40]}: {exc}")
        except Exception as exc:
            print(f"[PUSH] Error: {exc}")

    return sent > 0
