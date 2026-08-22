import hashlib
import json
import os
import re
import time
import xml.etree.ElementTree as ET
from urllib import request
from urllib.parse import quote_plus

from env_utils import load_local_env
from settings import settings

load_local_env()

NEWS_API_KEY = os.getenv("NEWS_API_KEY")
FINNHUB_API_KEY = os.getenv("FINNHUB_API_KEY")

TARGET_HANDLES = list(settings.news_target_handles)

RSS_SOURCES = [
    ("et_markets", "https://economictimes.indiatimes.com/markets/rssfeeds/21727745.cms"),
    ("moneycontrol", "https://www.moneycontrol.com/rss/latestnews.xml"),
    ("bloomberg_quint", "https://news.google.com/rss/search?q=bq+prime+bloomberg+quint+stocks&hl=en-IN&gl=IN&ceid=IN:en"),
    ("cnbc_tv18", "https://www.cnbctv18.com/commonfeeds/v1/cne/rss/market.xml"),
    ("mint", "https://www.livemint.com/rss/markets"),
    ("business_standard", "https://www.business-standard.com/rss/markets-106.rss"),
    ("financial_express", "https://news.google.com/rss/search?q=financial+express+market+stock&hl=en-IN&gl=IN&ceid=IN:en"),
    ("reuters", "https://news.google.com/rss/search?q=site:reuters.com+stocks&hl=en-IN&gl=IN&ceid=IN:en"),
]

_fetch_errors = {}


def _build_news_item(source, headline, url="", published_at=""):
    normalized = " ".join((headline or "").lower().split())
    fingerprint = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
    return {
        "source": source,
        "headline": headline.strip(),
        "url": url,
        "published_at": published_at,
        "fingerprint": fingerprint,
    }


def _browser_headers():
    return {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept": "application/rss+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
        "Referer": "https://www.google.com/",
    }


def _sanitize_xml(raw):
    raw = raw.replace(b"\x00", b"").replace(b"\x01", b"").replace(b"\x02", b"").replace(b"\x03", b"").replace(b"\x04", b"").replace(b"\x05", b"").replace(b"\x06", b"").replace(b"\x07", b"").replace(b"\x08", b"").replace(b"\x0B", b"").replace(b"\x0C", b"").replace(b"\x0E", b"").replace(b"\x0F", b"").replace(b"\x10", b"").replace(b"\x11", b"").replace(b"\x12", b"").replace(b"\x13", b"").replace(b"\x14", b"").replace(b"\x15", b"").replace(b"\x16", b"").replace(b"\x17", b"").replace(b"\x18", b"").replace(b"\x19", b"").replace(b"\x1A", b"").replace(b"\x1B", b"").replace(b"\x1C", b"").replace(b"\x1D", b"").replace(b"\x1E", b"").replace(b"\x1F", b"")
    text = raw.decode("utf-8", errors="replace")
    text = re.sub(r'&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)', '&amp;', text)
    return text.encode("utf-8")


def _fetch_rss(source_name, url, limit=10):
    headlines = []
    try:
        req = request.Request(url, headers=_browser_headers())
        with request.urlopen(req, timeout=15) as response:
            raw = response.read()
        raw = _sanitize_xml(raw)
        root = ET.fromstring(raw)
        items = root.findall(".//item")
        for entry in items[:limit]:
            headlines.append(
                _build_news_item(
                    source=source_name,
                    headline=(entry.findtext("title") or "").strip(),
                    url=(entry.findtext("link") or "").strip(),
                    published_at=(entry.findtext("pubDate") or "").strip(),
                )
            )
    except Exception as exc:
        err_key = f"rss:{source_name}"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] RSS fetch failed for {source_name}: {exc}")
    return headlines


def fetch_twitter_rss():
    print("[LOG] Fetching X/Twitter watchlist via Google News RSS...")
    headlines = []

    for handle in TARGET_HANDLES:
        try:
            query = quote_plus(f"{handle} Twitter")
            url = f"https://news.google.com/rss/search?q={query}"
            req = request.Request(url, headers=_browser_headers())
            with request.urlopen(req, timeout=10) as response:
                root = ET.fromstring(response.read())
            items = root.findall(".//item")

            for entry in items[:settings.news_rss_items_per_handle]:
                headlines.append(
                    _build_news_item(
                        source=f"x_rss:{handle}",
                        headline=(entry.findtext("title") or "").strip(),
                        url=(entry.findtext("link") or "").strip(),
                        published_at=(entry.findtext("pubDate") or "").strip(),
                    )
                )
        except Exception as exc:
            err_key = f"twitter:{handle}"
            last = _fetch_errors.get(err_key)
            if last != str(exc):
                _fetch_errors[err_key] = str(exc)
                print(f"[WARN] RSS fetch failed for @{handle}: {exc}")

    return headlines


def fetch_finnhub_news():
    print("[LOG] Fetching Finnhub headlines...")
    headlines = []

    if not FINNHUB_API_KEY:
        print("[WARN] FINNHUB_API_KEY missing.")
        return headlines

    url = f"https://finnhub.io/api/v1/news?category=general&token={FINNHUB_API_KEY}"

    try:
        req = request.Request(url, headers={"User-Agent": "NewsPulse/1.0"})
        with request.urlopen(req, timeout=30) as response:
            payload = json.loads(response.read().decode("utf-8"))

        for item in payload[:settings.news_finnhub_max_items]:
            headlines.append(
                _build_news_item(
                    source="finnhub",
                    headline=item.get("headline", ""),
                    url=item.get("url", ""),
                    published_at=str(item.get("datetime", "")),
                )
            )
    except Exception as exc:
        err_key = "finnhub"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] Finnhub fetch failed: {exc}")

    return headlines


def fetch_newsapi_data():
    print("[LOG] Fetching NewsAPI headlines...")
    headlines = []

    if not NEWS_API_KEY:
        print("[WARN] NEWS_API_KEY missing.")
        return headlines

    url = (
        "https://newsapi.org/v2/everything?"
        f"q={quote_plus(settings.news_api_query)}"
        "&language=en&sortBy=publishedAt"
        f"&pageSize={settings.news_newsapi_page_size}"
        f"&apiKey={NEWS_API_KEY}"
    )

    try:
        req = request.Request(url, headers={"User-Agent": "NewsPulse/1.0"})
        with request.urlopen(req, timeout=15) as response:
            payload = json.loads(response.read().decode("utf-8"))

        for article in payload.get("articles", []):
            headlines.append(
                _build_news_item(
                    source="newsapi",
                    headline=article.get("title", ""),
                    url=article.get("url", ""),
                    published_at=article.get("publishedAt", ""),
                )
            )
    except Exception as exc:
        err_key = "newsapi"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] NewsAPI fetch failed: {exc}")

    return headlines


def fetch_rss_sources():
    print("[LOG] Fetching RSS sources...")
    headlines = []
    for source_name, url in RSS_SOURCES:
        items = _fetch_rss(source_name, url, limit=10)
        headlines.extend(items)
    return headlines


def _fetch_with_retry(fetch_fn, name, retries=None):
    if retries is None:
        retries = settings.fetch_retries
    for attempt in range(retries + 1):
        try:
            return fetch_fn()
        except Exception as exc:
            print(f"[WARN] {name} failed (attempt {attempt + 1}/{retries + 1}): {exc}")
            if attempt < retries:
                time.sleep(5)
    return []


def get_all_headlines():
    print("[LOG] Fetching real-time market data...")
    try:
        from concurrent.futures import ThreadPoolExecutor, as_completed
        sources = [
            ("Twitter RSS", fetch_twitter_rss),
            ("Finnhub", fetch_finnhub_news),
            ("NewsAPI", fetch_newsapi_data),
            ("RSS Sources", fetch_rss_sources),
        ]
        items = []
        with ThreadPoolExecutor(max_workers=4) as pool:
            futures = {pool.submit(_fetch_with_retry, fn, name): name for name, fn in sources}
            for future in as_completed(futures):
                try:
                    items.extend(future.result())
                except Exception as exc:
                    print(f"[WARN] Parallel fetch failed: {exc}")
    except Exception:
        items = (
            _fetch_with_retry(fetch_twitter_rss, "Twitter RSS")
            + _fetch_with_retry(fetch_finnhub_news, "Finnhub")
            + _fetch_with_retry(fetch_newsapi_data, "NewsAPI")
            + _fetch_with_retry(fetch_rss_sources, "RSS Sources")
        )

    deduped = {}
    for item in items:
        if item.get("headline") and item.get("fingerprint"):
            deduped[item["fingerprint"]] = item

    print("\n--- DATA SUMMARY ---")
    print(f"Total headlines fetched: {len(items)}")
    print(f"Unique items for analysis: {len(deduped)}")

    return list(deduped.values())


if __name__ == "__main__":
    for idx, item in enumerate(get_all_headlines(), 1):
        print(f"{idx}. [{item['source']}] {item['headline']}")
