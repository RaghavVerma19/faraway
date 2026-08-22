import json
import re
from datetime import datetime, time, timedelta, timezone
from pathlib import Path

from settings import settings


_COMPANY_REGISTRY_FILE = Path(settings.data_dir) / "company_registry.json"

COMPANY_REGISTRY = {}
if _COMPANY_REGISTRY_FILE.exists():
    try:
        with open(str(_COMPANY_REGISTRY_FILE), encoding="utf-8") as f:
            COMPANY_REGISTRY = json.load(f)
    except Exception as exc:
        print(f"[WARN] Failed to load {_COMPANY_REGISTRY_FILE}: {exc}")

if not COMPANY_REGISTRY:
    print("[INFO] Loading built-in company registry.")
    COMPANY_REGISTRY = {
        "RELIANCE": {"symbol": "RELIANCE", "exchange": "NSE"},
        "RELIANCE INDUSTRIES": {"symbol": "RELIANCE", "exchange": "NSE"},
        "RIL": {"symbol": "RELIANCE", "exchange": "NSE"},
        "ADANI": {"symbol": "ADANIENT", "exchange": "NSE"},
        "ADANI GROUP": {"symbol": "ADANIENT", "exchange": "NSE"},
        "ADANI ENTERPRISES": {"symbol": "ADANIENT", "exchange": "NSE"},
        "ADANI PORTS": {"symbol": "ADANIPORTS", "exchange": "NSE"},
        "ADANI POWER": {"symbol": "ADANIPOWER", "exchange": "NSE"},
        "ADANI GREEN": {"symbol": "ADANIGREEN", "exchange": "NSE"},
        "ADANI TOTAL GAS": {"symbol": "ADANITOTAL", "exchange": "NSE"},
        "TATA MOTORS": {"symbol": "TATAMOTORS", "exchange": "NSE"},
        "TATA": {"symbol": "TATAMOTORS", "exchange": "NSE"},
        "TATA STEEL": {"symbol": "TATASTEEL", "exchange": "NSE"},
        "TATA CONSULTANCY": {"symbol": "TCS", "exchange": "NSE"},
        "TCS": {"symbol": "TCS", "exchange": "NSE"},
        "TATA POWER": {"symbol": "TATAPOWER", "exchange": "NSE"},
        "TATA CONSUMER": {"symbol": "TATACONSUM", "exchange": "NSE"},
        "TATA COMMUNICATIONS": {"symbol": "TATACOMM", "exchange": "NSE"},
        "TATA ELXSI": {"symbol": "TATAELXSI", "exchange": "NSE"},
        "INFOSYS": {"symbol": "INFY", "exchange": "NSE"},
        "INFY": {"symbol": "INFY", "exchange": "NSE"},
        "HDFC BANK": {"symbol": "HDFCBANK", "exchange": "NSE"},
        "HDFC": {"symbol": "HDFCBANK", "exchange": "NSE"},
        "HDFC LIFE": {"symbol": "HDFCLIFE", "exchange": "NSE"},
        "HDFC ASSET": {"symbol": "HDFCAMC", "exchange": "NSE"},
        "ICICI BANK": {"symbol": "ICICIBANK", "exchange": "NSE"},
        "ICICI": {"symbol": "ICICIBANK", "exchange": "NSE"},
        "ICICI PRUDENTIAL": {"symbol": "ICICIPRULI", "exchange": "NSE"},
        "SBI": {"symbol": "SBIN", "exchange": "NSE"},
        "STATE BANK": {"symbol": "SBIN", "exchange": "NSE"},
        "AXIS BANK": {"symbol": "AXISBANK", "exchange": "NSE"},
        "AXIS": {"symbol": "AXISBANK", "exchange": "NSE"},
        "KOTAK MAHINDRA": {"symbol": "KOTAKBANK", "exchange": "NSE"},
        "KOTAK": {"symbol": "KOTAKBANK", "exchange": "NSE"},
        "YES BANK": {"symbol": "YESBANK", "exchange": "NSE"},
        "MARUTI": {"symbol": "MARUTI", "exchange": "NSE"},
        "MARUTI SUZUKI": {"symbol": "MARUTI", "exchange": "NSE"},
        "MSIL": {"symbol": "MARUTI", "exchange": "NSE"},
        "MAHINDRA": {"symbol": "M&M", "exchange": "NSE"},
        "MAHINDRA & MAHINDRA": {"symbol": "M&M", "exchange": "NSE"},
        "M&M": {"symbol": "M&M", "exchange": "NSE"},
        "BAJAJ FINANCE": {"symbol": "BAJFINANCE", "exchange": "NSE"},
        "BAJAJ FINSERV": {"symbol": "BAJAJFINSV", "exchange": "NSE"},
        "BAJAJ AUTO": {"symbol": "BAJAJ-AUTO", "exchange": "NSE"},
        "BAJAJ": {"symbol": "BAJAJ-AUTO", "exchange": "NSE"},
        "WIPRO": {"symbol": "WIPRO", "exchange": "NSE"},
        "HCL": {"symbol": "HCLTECH", "exchange": "NSE"},
        "HCL TECH": {"symbol": "HCLTECH", "exchange": "NSE"},
        "HCL TECHNOLOGIES": {"symbol": "HCLTECH", "exchange": "NSE"},
        "TECH MAHINDRA": {"symbol": "TECHM", "exchange": "NSE"},
        "NTPC": {"symbol": "NTPC", "exchange": "NSE"},
        "NATIONAL THERMAL": {"symbol": "NTPC", "exchange": "NSE"},
        "ONGC": {"symbol": "ONGC", "exchange": "NSE"},
        "OIL AND NATURAL GAS": {"symbol": "ONGC", "exchange": "NSE"},
        "COAL INDIA": {"symbol": "COALINDIA", "exchange": "NSE"},
        "POWER GRID": {"symbol": "POWERGRID", "exchange": "NSE"},
        "L&T": {"symbol": "LT", "exchange": "NSE"},
        "LARSEN": {"symbol": "LT", "exchange": "NSE"},
        "LARSEN & TOUBRO": {"symbol": "LT", "exchange": "NSE"},
        "ITC": {"symbol": "ITC", "exchange": "NSE"},
        "HUL": {"symbol": "HINDUNILVR", "exchange": "NSE"},
        "HINDUSTAN UNILEVER": {"symbol": "HINDUNILVR", "exchange": "NSE"},
        "ASIAN PAINTS": {"symbol": "ASIANPAINT", "exchange": "NSE"},
        "NESTLE": {"symbol": "NESTLEIND", "exchange": "NSE"},
        "NESTLE INDIA": {"symbol": "NESTLEIND", "exchange": "NSE"},
        "BHARTI AIRTEL": {"symbol": "BHARTIARTL", "exchange": "NSE"},
        "AIRTEL": {"symbol": "BHARTIARTL", "exchange": "NSE"},
        "JIO": {"symbol": "JIOFIN", "exchange": "NSE"},
        "RELIANCE JIO": {"symbol": "JIOFIN", "exchange": "NSE"},
        "SUN PHARMA": {"symbol": "SUNPHARMA", "exchange": "NSE"},
        "SUN PHARMACEUTICAL": {"symbol": "SUNPHARMA", "exchange": "NSE"},
        "DR REDDY": {"symbol": "DRREDDY", "exchange": "NSE"},
        "DR. REDDY": {"symbol": "DRREDDY", "exchange": "NSE"},
        "CIPLA": {"symbol": "CIPLA", "exchange": "NSE"},
        "DIVIS": {"symbol": "DIVISLAB", "exchange": "NSE"},
        "DIVI'S": {"symbol": "DIVISLAB", "exchange": "NSE"},
        "APOLLO HOSPITALS": {"symbol": "APOLLOHOSP", "exchange": "NSE"},
        "APOLLO": {"symbol": "APOLLOHOSP", "exchange": "NSE"},
        "HINDALCO": {"symbol": "HINDALCO", "exchange": "NSE"},
        "HINDALCO INDUSTRIES": {"symbol": "HINDALCO", "exchange": "NSE"},
        "JSW STEEL": {"symbol": "JSWSTEEL", "exchange": "NSE"},
        "JSW": {"symbol": "JSWSTEEL", "exchange": "NSE"},
        "ULTRATECH": {"symbol": "ULTRACEMCO", "exchange": "NSE"},
        "ULTRATECH CEMENT": {"symbol": "ULTRACEMCO", "exchange": "NSE"},
        "GRASIM": {"symbol": "GRASIM", "exchange": "NSE"},
        "EICHER MOTORS": {"symbol": "EICHERMOT", "exchange": "NSE"},
        "ROYAL ENFIELD": {"symbol": "EICHERMOT", "exchange": "NSE"},
        "HERO MOTOCORP": {"symbol": "HEROMOTOCO", "exchange": "NSE"},
        "HERO": {"symbol": "HEROMOTOCO", "exchange": "NSE"},
        "BRITANNIA": {"symbol": "BRITANNIA", "exchange": "NSE"},
        "TITAN": {"symbol": "TITAN", "exchange": "NSE"},
        "TITAN COMPANY": {"symbol": "TITAN", "exchange": "NSE"},
        "AVENUE SUPERMARTS": {"symbol": "DMART", "exchange": "NSE"},
        "DMART": {"symbol": "DMART", "exchange": "NSE"},
        "ZOMATO": {"symbol": "ZOMATO", "exchange": "NSE"},
        "PAYTM": {"symbol": "PAYTM", "exchange": "NSE"},
        "ONE97": {"symbol": "PAYTM", "exchange": "NSE"},
        "VEDANTA": {"symbol": "VEDL", "exchange": "NSE"},
        "VODAFONE": {"symbol": "IDEA", "exchange": "NSE"},
        "VODAFONE IDEA": {"symbol": "IDEA", "exchange": "NSE"},
        "IDEA": {"symbol": "IDEA", "exchange": "NSE"},
    }

_WORD_RE = re.compile(r"[A-Z0-9&.]+")


def resolve_company(company_name):
    lookup = (company_name or "").strip().upper()
    if not lookup:
        return None

    if lookup in COMPANY_REGISTRY:
        return COMPANY_REGISTRY[lookup]

    input_tokens = set(_WORD_RE.findall(lookup))

    best = None
    best_count = -1
    for alias, company in COMPANY_REGISTRY.items():
        alias_tokens = set(_WORD_RE.findall(alias))
        if alias_tokens and alias_tokens.issubset(input_tokens):
            if len(alias_tokens) > best_count:
                best_count = len(alias_tokens)
                best = company

    if best:
        return best

    return None
