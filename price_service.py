import os
import json
from urllib import request as urllib_request

from cache import price_cache

FINNHUB_KEY = os.getenv("FINNHUB_API_KEY")


def fetch_price(symbol):
    symbol = symbol.upper()
    cached = price_cache.get(symbol)
    if cached is not None:
        return cached

    price = _fetch_yfinance(symbol)
    if price is not None:
        price_cache.set(symbol, price)
        return price

    price = _fetch_finnhub(symbol)
    if price is not None:
        price_cache.set(symbol, price)
        return price

    return None


def fetch_prices(symbols):
    results = {}
    remaining = []
    for s in symbols:
        cached = price_cache.get(s.upper())
        if cached is not None:
            results[s.upper()] = cached
        else:
            remaining.append(s)

    if remaining:
        batch = _fetch_yfinance_batch(remaining)
        for s, p in batch.items():
            if p is not None:
                price_cache.set(s.upper(), p)
                results[s.upper()] = p

    still_missing = [s for s in remaining if s.upper() not in results]
    for s in still_missing:
        price = _fetch_finnhub(s)
        if price is not None:
            price_cache.set(s.upper(), price)
            results[s.upper()] = price

    return results


def _fetch_yfinance(symbol):
    try:
        import yfinance as yf
        ticker = yf.Ticker(symbol + ".NS")
        info = ticker.info
        price = info.get("currentPrice") or info.get("regularMarketPrice") or info.get("previousClose")
        if price:
            return float(price)
    except Exception:
        pass
    return None


def _fetch_yfinance_batch(symbols):
    result = {}
    try:
        import yfinance as yf
        for s in symbols:
            try:
                ticker = yf.Ticker(s + ".NS")
                info = ticker.info
                price = info.get("currentPrice") or info.get("regularMarketPrice") or info.get("previousClose")
                if price:
                    result[s] = float(price)
            except Exception:
                pass
    except Exception:
        pass
    return result


def _fetch_finnhub(symbol):
    if not FINNHUB_KEY:
        return None
    try:
        url = f"https://finnhub.io/api/v1/quote?symbol={symbol}.NSE&token={FINNHUB_KEY}"
        with urllib_request.urlopen(url, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            c = data.get("c")
            if c and c > 0:
                return float(c)
    except Exception:
        pass
    return None


def clear_cache():
    price_cache.clear()
