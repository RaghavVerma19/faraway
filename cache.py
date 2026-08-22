import threading
import time


class TimedCache:
    def __init__(self, default_ttl=60, max_size=256):
        self._store = {}
        self._ttl = default_ttl
        self._max_size = max_size
        self._lock = threading.Lock()

    def get(self, key):
        with self._lock:
            entry = self._store.get(key)
            if entry is None:
                return None
            value, expires = entry
            if time.monotonic() > expires:
                del self._store[key]
                return None
            return value

    def set(self, key, value, ttl=None):
        with self._lock:
            if len(self._store) >= self._max_size:
                self._evict_lru()
            expires = time.monotonic() + (ttl if ttl is not None else self._ttl)
            self._store[key] = (value, expires)

    def delete(self, key):
        with self._lock:
            self._store.pop(key, None)

    def clear(self):
        with self._lock:
            self._store.clear()

    def _evict_lru(self):
        if not self._store:
            return
        oldest = min(self._store.items(), key=lambda x: x[1][1])
        del self._store[oldest[0]]


price_cache = TimedCache(default_ttl=60, max_size=128)
news_cache = TimedCache(default_ttl=300, max_size=64)
# Step 3: Stats cache - Overview counts (15 sec TTL = instant reload, still fresh)
stats_cache = TimedCache(default_ttl=15, max_size=16)
