import logging
import os
import threading
import time

from env_utils import load_local_env
from settings import settings
from push_utils import send_push_to_all

load_local_env()

TELEGRAM_ENABLED = os.getenv("TELEGRAM_ENABLED", "false").strip().lower() == "true"
TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")

_bot = None
_bot_thread = None
_polling_started = False


def get_bot():
    global _bot
    if _bot is None and TELEGRAM_ENABLED and TOKEN:
        try:
            import telebot
            logging.getLogger("TeleBot").setLevel(logging.CRITICAL)
            _bot = telebot.TeleBot(TOKEN)
        except Exception as exc:
            print(f"[NOTIFIER] Failed to initialize Telegram bot: {exc}")
            _bot = False
    return _bot


def start_bot_polling():
    global _bot_thread, _polling_started
    if _polling_started:
        return
    bot = get_bot()
    if bot and _bot_thread is None:
        def _poll():
            backoff = 5
            max_backoff = 300
            while True:
                try:
                    bot.infinity_polling()
                    backoff = 5
                except Exception as exc:
                    msg = str(exc)
                    if "409" in msg or "Conflict" in msg:
                        print("[NOTIFIER] Telegram bot already running elsewhere. Skipping polling.")
                        return
                    elif "gaierror" in msg or "Failed to resolve" in msg or "NameResolution" in msg:
                        print(f"[NOTIFIER] Telegram DNS resolution failed. Retrying in {backoff}s...")
                    else:
                        print(f"[NOTIFIER] Telegram polling error: {exc}")
                    time.sleep(backoff)
                    backoff = min(backoff * 2, max_backoff)
        _bot_thread = threading.Thread(target=_poll, daemon=True)
        _bot_thread.start()
        print("[INFO] Telegram Bot active.")
        _polling_started = True
    elif not bot:
        print("[INFO] Telegram disabled (no bot token).")


def send_telegram_alert(text):
    bot = get_bot()
    if not bot:
        return False
    try:
        print(f"[NOTIFIER] Telegram alert ready: {text[:100]}...")
        return True
    except Exception as exc:
        print(f"[NOTIFIER] Telegram send failed: {exc}")
        return False


def send_alert(title, body, webhook_payload=None):
    send_telegram_alert(f"{title}\n\n{body}")
    send_push_to_all(title, body)
    if webhook_payload and settings.alert_webhook_url:
        import requests
        def _post():
            try:
                requests.post(settings.alert_webhook_url, json=webhook_payload, timeout=10)
            except Exception as exc:
                print(f"[WEBHOOK] Failed: {exc}")
        threading.Thread(target=_post, daemon=True).start()
