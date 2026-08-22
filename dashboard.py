import os
import traceback
from datetime import datetime
from pathlib import Path

from flask import Flask, jsonify, request, send_from_directory

from env_utils import load_local_env
from headline_impact import estimate_price_impact, format_impact
from keyword_weights import SOURCE_CREDIBILITY

load_local_env()

import db
from push_utils import send_push_to_all, get_vapid_public_key
from cache import price_cache, stats_cache
from price_service import fetch_prices

DATA_DIR = Path(__file__).parent / "data"

app = Flask(__name__, static_folder="static", static_url_path="")

db.init_db()
db.migrate_db()

_agent_status = "offline"
_agent_last_scan = 0


_API_PREFIX = "/api/"


def _json_error(msg, code=400):
    return jsonify({"error": msg}), code


@app.before_request
def _validate_api_request():
    if request.method == "POST" and request.path.startswith(_API_PREFIX) and request.content_length and request.content_length > 0:
        ct = (request.content_type or "").lower()
        if "multipart/form-data" not in ct and "application/json" not in ct:
            return _json_error("Content-Type must be application/json", 415)


@app.after_request
def _set_json_header(response):
    if request.path.startswith(_API_PREFIX):
        response.headers.setdefault("Content-Type", "application/json")
    return response


@app.after_request
def _no_cache_for_static(response):
    if request.path.endswith(".html") or request.path.endswith(".js"):
        response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
    return response


@app.errorhandler(400)
def _handle_400(exc):
    return _json_error(str(exc) or "Bad request", 400)


@app.errorhandler(404)
def _handle_404(exc):
    return _json_error("Not found", 404)


@app.errorhandler(405)
def _handle_405(exc):
    return _json_error("Method not allowed", 405)


@app.errorhandler(500)
def _handle_500(exc):
    traceback.print_exc()
    return _json_error("Internal server error", 500)


def set_agent_status(status, last_scan=0):
    global _agent_status, _agent_last_scan
    _agent_status = status
    if last_scan:
        _agent_last_scan = last_scan


@app.route("/api/agent-status")
def api_agent_status():
    return jsonify({
        "status": _agent_status,
        "last_scan": _agent_last_scan,
    })


@app.route("/api/alerts")
def api_alerts():
    limit = request.args.get("limit", 50, type=int)
    tier = request.args.get("tier", "all")
    offset = request.args.get("offset", 0, type=int)
    # sanitize limit
    limit = max(1, min(limit, 100))
    if tier:
        tier = tier.strip()
        # keep 'all' as is, others upper
        if tier.lower() != "all":
            tier = tier.upper()
    alerts = db.get_alerts(limit=limit, tier=tier, offset=offset)
    for a in alerts:
        cred = SOURCE_CREDIBILITY.get(a.get("source", "newsapi"), 0.5) if "source" in a else 0.5
        impact = estimate_price_impact(a.get("headline", ""), cred)
        a["impact_pct"] = impact
        a["impact_formatted"] = format_impact(impact)
    return jsonify(alerts)


@app.route("/api/alerts/<int:alert_id>")
def api_alert_detail(alert_id):
    conn = db.conn.get_conn()
    row = conn.execute("SELECT * FROM alerts WHERE id = ?", (alert_id,)).fetchone()
    if not row:
        return jsonify({"error": "Alert not found"}), 404
    alert = dict(row)
    try:
        alert["signals"] = __import__('json').loads(alert.get("signals_json", "[]"))
    except Exception:
        alert["signals"] = []
    del alert["signals_json"]
    cred = SOURCE_CREDIBILITY.get(alert.get("source", "newsapi"), 0.5) if "source" in alert else 0.5
    impact = estimate_price_impact(alert.get("headline", ""), cred)
    alert["impact_pct"] = impact
    alert["impact_formatted"] = format_impact(impact)
    return jsonify(alert)


@app.route("/api/watchlist")
def api_watchlist():
    items = db.get_watchlist()
    return jsonify(items)


@app.route("/api/watchlist", methods=["POST"])
def api_watchlist_add():
    data = request.get_json(silent=True) or {}
    symbol = (data.get("symbol") or "").strip().upper()
    if not symbol:
        return jsonify({"error": "symbol required"}), 400
    db.add_to_watchlist(symbol)
    return jsonify({"ok": True})


@app.route("/api/watchlist/<symbol>", methods=["DELETE"])
def api_watchlist_remove(symbol):
    db.remove_from_watchlist(symbol)
    return jsonify({"ok": True})


@app.route("/api/filings")
def api_filings():
    limit = request.args.get("limit", 50, type=int)
    symbol = request.args.get("symbol")
    filings = db.get_cached_filings(symbol=symbol, limit=limit)
    return jsonify(filings)


@app.route("/api/signals")
def api_signals():
    limit = request.args.get("limit", 200, type=int)
    symbol = request.args.get("symbol")
    signals = db.get_signal_log(symbol=symbol, limit=limit)
    return jsonify(signals)


@app.route("/api/stats")
def api_stats():
    # Step 3: Return cached stats if fresh - 15 sec instant reload
    cached = stats_cache.get("stats")
    if cached is not None:
        return jsonify(cached)
    # Fast SQL count - no Python loop, uses idx_alerts_tier_timestamp
    counts = db.count_alerts_by_tier_today()
    # total_alerts still needs fast count - use same SQL without date filter for speed
    conn = db.conn.get_conn()
    row = conn.execute("SELECT COUNT(*) as cnt FROM alerts").fetchone()
    total = row["cnt"] if row else 0
    result = {
        "alerts_today": counts.get("total", 0),
        "critical_today": counts.get("critical", 0),
        "high_today": counts.get("high", 0),
        "watch_today": counts.get("watch", 0),
        "total_alerts": total,
        "watchlist_count": len(db.get_watchlist()),
    }
    stats_cache.set("stats", result, ttl=15)
    return jsonify(result)


@app.route("/api/push/subscribe", methods=["POST"])
def push_subscribe():
    data = request.get_json(silent=True) or {}
    endpoint = data.get("endpoint", "")
    keys = data.get("keys", {})
    if not endpoint:
        return jsonify({"error": "endpoint required"}), 400
    db.save_push_subscription(endpoint, keys.get("auth", ""), keys.get("p256dh", ""))
    return jsonify({"ok": True})


@app.route("/api/push/unsubscribe", methods=["POST"])
def push_unsubscribe():
    data = request.get_json(silent=True) or {}
    endpoint = data.get("endpoint", "")
    if endpoint:
        db.delete_push_subscription(endpoint)
    else:
        db.delete_all_push_subscriptions()
    return jsonify({"ok": True})


@app.route("/api/push/vapid-key")
def push_vapid_key():
    return jsonify({"public_key": get_vapid_public_key()})


@app.route("/api/push/test")
def push_test():
    ok = send_push_to_all("Test Notification", "This is a test push from NewsPulse Crash Tracker.")
    return jsonify({"sent": ok})


@app.route("/health")
def health():
    return "ok"


@app.route("/")
def index():
    response = send_from_directory("static", "index.html")
    response.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0"
    return response


if __name__ == "__main__":
    debug_mode = os.getenv("FLASK_DEBUG", "false").strip().lower() == "true"
    port = int(os.getenv("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=debug_mode)
