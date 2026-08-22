import hashlib
import json
import os
import re
import time
from urllib import request as urllib_request
from urllib.parse import urljoin

from env_utils import load_local_env
from settings import settings
from validator import resolve_company

load_local_env()

_fetch_errors = {}


def _session_request(url, headers=None, timeout=20):
    session = urllib_request.build_opener()
    session.addheaders = [
        ("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
        ("Accept", "application/json, text/plain, */*"),
        ("Accept-Language", "en-US,en;q=0.9"),
        ("Accept-Encoding", "gzip, deflate, br"),
        ("Connection", "keep-alive"),
    ]
    if headers:
        for k, v in headers.items():
            session.addheaders.append((k, v))
    req = urllib_request.Request(url)
    for h, v in session.addheaders:
        req.add_header(h, v)
    with urllib_request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _classify_filing_type(title):
    title_lower = title.lower()
    if any(k in title_lower for k in ["auditor", "audit"]):
        return "auditor_resignation"
    if any(k in title_lower for k in ["sebi"]):
        if any(k in title_lower for k in ["penalty", "fine", "violation"]):
            return "sebi_penalty"
        if any(k in title_lower for k in ["order", "probe", "investigation"]):
            return "sebi_order"
        return "sebi_probe"
    if any(k in title_lower for k in ["guidance", "cut", "downgrade"]):
        return "guidance_cut"
    if any(k in title_lower for k in ["result", "quarterly", "earnings", "loss", "miss"]):
        return "result_miss"
    if any(k in title_lower for k in ["board meeting", "agm", "egm"]):
        return "board_meeting"
    if any(k in title_lower for k in ["dividend", "split", "bonus"]):
        return "corporate_action"
    if any(k in title_lower for k in ["promoter", "pledge"]):
        return "promoter_pledge"
    if any(k in title_lower for k in ["insider"]):
        return "insider_trading"
    return "generic_announcement"


def _filing_hash(source, symbol, filing_type, title):
    normalized = " ".join(title.lower().split())
    return hashlib.sha256(f"{source}|{symbol}|{filing_type}|{normalized}".encode("utf-8")).hexdigest()


def fetch_nse_filings():
    filings = []
    url = "https://www.nseindia.com/api/announcements"
    headers = {
        "Referer": "https://www.nseindia.com/",
        "X-Requested-With": "XMLHttpRequest",
    }
    try:
        data = _session_request(url, headers=headers, timeout=15)
        announcements = data.get("data", {}).get("announcements", [])
        for item in announcements:
            symbol = item.get("symbol") or item.get("isin") or ""
            if not symbol:
                continue
            symbol = symbol.upper()
            company_info = resolve_company(symbol) or {"symbol": symbol}
            title = item.get("desc") or item.get("subject") or item.get("headline") or ""
            if not title:
                continue
            filing_type = _classify_filing_type(title)
            published_at = item.get("date") or item.get("dt") or ""
            filings.append({
                "source": "nse",
                "symbol": company_info.get("symbol", symbol),
                "company": symbol,
                "filing_type": filing_type,
                "title": title,
                "url": item.get("attachUrl", "") or url,
                "published_at": published_at,
                "hash": _filing_hash("nse", symbol, filing_type, title),
            })
    except Exception as exc:
        err_key = "nse_filings"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] NSE filings fetch failed: {exc}")
    return filings


def fetch_bse_filings():
    filings = []
    url = "https://api.bseindia.com/BseIndiaAPI/api/Announcements/w"
    try:
        data = _session_request(url, timeout=15)
        announcements = data if isinstance(data, list) else data.get("data", [])
        for item in announcements:
            symbol = item.get("scrip_code") or item.get("symbol") or ""
            if not symbol:
                continue
            symbol = str(symbol).upper()
            company_info = resolve_company(symbol) or {"symbol": symbol}
            title = item.get("subject") or item.get("headline") or item.get("news_title") or ""
            if not title:
                continue
            filing_type = _classify_filing_type(title)
            published_at = item.get("news_date") or item.get("dt_tm") or ""
            filings.append({
                "source": "bse",
                "symbol": company_info.get("symbol", symbol),
                "company": symbol,
                "filing_type": filing_type,
                "title": title,
                "url": item.get("attachement", "") or url,
                "published_at": published_at,
                "hash": _filing_hash("bse", symbol, filing_type, title),
            })
    except Exception as exc:
        err_key = "bse_filings"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] BSE filings fetch failed: {exc}")
    return filings


def fetch_sebi_orders():
    filings = []
    url = "https://www.sebi.gov.in/sebiweb/other/OtherAction.do?doPublish=yes"
    try:
        data = _session_request(url, timeout=15)
        items = data if isinstance(data, list) else data.get("data", [])
        for item in items:
            title = item.get("subject") or item.get("title") or item.get("news_title") or ""
            if not title:
                continue
            symbol = resolve_company(title)
            symbol_str = symbol.get("symbol", "") if symbol else ""
            if not symbol_str:
                continue
            filing_type = "sebi_order"
            published_at = item.get("date") or item.get("dt_tm") or ""
            filings.append({
                "source": "sebi",
                "symbol": symbol_str,
                "company": title,
                "filing_type": filing_type,
                "title": title,
                "url": item.get("url", "") or url,
                "published_at": published_at,
                "hash": _filing_hash("sebi", symbol_str, filing_type, title),
            })
    except Exception as exc:
        err_key = "sebi_orders"
        last = _fetch_errors.get(err_key)
        if last != str(exc):
            _fetch_errors[err_key] = str(exc)
            print(f"[WARN] SEBI orders fetch failed: {exc}")
    return filings


def get_all_filings():
    print("[LOG] Fetching filings...")
    nse = fetch_nse_filings()
    bse = fetch_bse_filings()
    sebi = fetch_sebi_orders()
    all_filings = nse + bse + sebi
    seen = set()
    unique = []
    for f in all_filings:
        if f["hash"] not in seen:
            seen.add(f["hash"])
            unique.append(f)
    print(f"[LOG] Got {len(unique)} unique filings")
    return unique
