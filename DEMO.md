# NewsPulse Crash Tracker - Demo Guide

## Quick Start (for judges)

1. Double-click `run_demo.bat`
2. Open browser to `http://localhost:5000`
3. Dashboard loads with pre-seeded demo data

## What you'll see

- **Alerts tab**: 15 pre-seeded crash/surge alerts with trust scores (35-95), signal breakdowns, and reasoning
- **Watchlist tab**: 8 major Indian stocks (RELIANCE, TCS, HDFCBANK, etc.)
- **Filings tab**: 12 cached NSE/BSE/SEBI filings with severity scores
- **Signals tab**: 25 processed headlines with raw scores and source attribution
- **Stats bar**: Real-time counts of alerts, critical alerts, and watchlist size

## Architecture highlights

- **Single-user, no auth**: Open dashboard, no login required
- **Composite scoring**: Multi-signal detection (filings + news + technical + AI)
- **Deduplication**: Same event from multiple sources = one alert
- **Groq optional**: Rule-based scoring works independently; AI only resolves ambiguity
- **Lightweight**: Flask + SQLite, runs on Raspberry Pi

## Demo mode vs Production

| Feature | Demo | Production |
|---------|------|------------|
| Data source | Pre-seeded | Live RSS, filings, APIs |
| Groq AI | Disabled | Called for ambiguous hits |
| Alerts | Static | Real-time Telegram/push/webhook |
| Scan cycle | N/A | 60s interval |

## Production startup

```bash
# Set API keys in .env
cp .env.example .env

# Run normally (no demo data)
python app.py
```
