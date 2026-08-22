# NewsPulse-AI — Project Context

## Objective
Integrate real trading APIs (Upstox) to make the app usable for live Indian market trading, and fix remaining UX/technical issues.

## Constraints
- Must run on lightweight devices (Raspberry Pi, low-end VPS, old hardware)
- Upstox API is free but limited — chosen over Zerodha Kite Connect (₹500/month after trial)
- All source files at `C:\Users\DELL\Desktop\NewsPulse-AI-project`. Virtual env is `.venv`
- ngrok blocked (`ERR_NGROK_6024`). Tunnel switched to **Trapdoor** at `https://aether.trapdoor.sh`
- Upstox requires two-step auth: API Key + Secret (identifies the app) + OAuth Access Token (authorizes trading)

## Architecture

### Backend
| File | Purpose |
|---|---|
| `server.py` | Main entry — uvicorn server |
| `dashboard.py` | API routes for portfolio, stats, signals, trades. Has `before_request` enforcing JSON content-type (skipped for bodyless POSTs) |
| `executor.py` | Trade execution — delegates to `broker/paper` or `broker/upstox` per user's `broker_mode` |
| `price_service.py` | Price fetching via yfinance + Finnhub fallback. Batch fetch fixed (individual `yf.Ticker()` calls) |
| `cache.py` | `TimedCache` with 60s TTL for prices |
| `crash_engine.py` | Crash/surge scanning engine — fetches headlines/filings, scores with LLM, generates trade signals |
| `brain.py` | Groq LLM wrapper — batch/single headline crash analysis, 401 error suppression |
| `fetcher.py` | Multi-source headline fetcher — Twitter RSS, Finnhub, NewsAPI, RSS Sources with dedup |
| `settings.py` | App configuration |
| `crypto_utils.py` | Fernet encrypt/decrypt for sensitive settings (key derived from `JWT_SECRET`) |
| `broker/__init__.py` | Broker package init |
| `broker/paper.py` | Paper trading implementation |
| `broker/upstox.py` | Upstox API wrapper — OAuth URL gen, code exchange, order placement, fund/position queries |
| `broker_blueprint.py` | Broker API routes — status, mode, connect/disconnect, OAuth URL/callback |
| `db/conn.py` | SQLite schema (includes `user_settings` and `oauth_states` tables) |
| `db/users.py` | User CRUD + setting get/set with auto-encrypt for sensitive keys |
| `db/oauth.py` | OAuth state storage/retrieval |
| `db/alerts.py` | Alert logging and retrieval with dedup queries |
| `db/filings_cache.py` | Filing cache storage |
| `db/signal_log.py` | Signal logging |
| `notifier.py` | Telegram + webhook + push notification dispatch |
| `alert_dispatcher.py` | Alert dispatch and local alerts.json persistence |
| `push_utils.py` | WebPush/VAPID push notification helper |
| `validator.py` | Company/symbol resolution from headlines |
| `keyword_weights.py` | Crash/surge keywords, source credibility weights, filing severity weights |
| `headline_impact.py` | Price impact estimation from headlines |
| `filings.py` | Filing fetchers (NSE, BSE, etc.) |

### Frontend (`static/`)
| File | Purpose |
|---|---|
| `index.html` | SPA — onboarding wizard, broker tab, mode toggle, connectedDot |
| `app.js` | All client logic — broker connection flow, mode toggle, `postMessage` listener, `fetchJSON` with cache-busting |
| `style.css` | Dark theme styles, mode toggle, connectedDot pulse animation |

## Key Fixes Applied

| Issue | Fix |
|---|---|
| `UDAPI100068` (Upstox blocked) | Added `User-Agent: NewsPulse/1.0` to all Upstox API requests |
| `exchange_code` silent errors | Now captures full HTTP error body, returns `(bool, error_msg)` tuple |
| OAuth callback page | Shows actual Upstox error in `<pre>`, success uses `postMessage` + auto-close + 1.5s redirect |
| Wrong settings key | Was saving to `upstox_broker_mode` — changed to `broker_mode` |
| Polling for broker status | Removed `setInterval` / `visibilitychange`. Only `postMessage` updates after OAuth |
| 415 Content-Type errors | `before_request` only enforces JSON when POST has body; `fetchJSON` always sets Content-Type for POST/PUT/PATCH |
| Stale broker status | `loadBrokerStatus` passes `{method:'GET'}` to bypass 30s fetchJSON cache |
| Stale price cache | `api_portfolio_refresh` clears `price_cache` before yfinance batch fetch |
| Sensitive keys in plaintext | `crypto_utils.py` + wrapped `get_user_setting`/`set_user_setting` to encrypt/decrypt on the fly |
| Upstox card scrolls instead of switching | Click handler checks status text — switches to Live if connected, scrolls to config only when not set |
| Duplicate alerts | `db/alerts.py` timestamps changed from ISO 8601 with timezone to local `YYYY-MM-DD HH:MM:SS` so SQLite `datetime()` dedup query works correctly. `crash_engine._dedup_alert` also matches on `tier` + `action` as fallback |
| Groq 401 spam | `brain.py` sets `_groq_401` flag on first 401, stops retries, and skips N+1 single-headline fallback. `is_groq_available()` returns `False` after 401 |

## .env Variables
- `NEWS_API_KEY` — NewsAPI
- `FINNHUB_API_KEY` — Finnhub
- `GROQ_API_KEY` — Groq (LLM)
- `TELEGRAM_BOT_TOKEN` — Telegram bot
- `JWT_SECRET` — JWT signing + encryption key derivation
- `ENCRYPTION_KEY` — (reserved, not used — currently derived from JWT_SECRET)
- `UPSTOX_REDIRECT_URI` — `https://aether.trapdoor.sh/api/broker/connect/upstox/callback`

## Future Work
- Handle Upstox token refresh (access tokens expire daily)
- Add more brokers (Zerodha, Angel One)
- Order status polling / webhook
- Real-time market data via WebSocket
