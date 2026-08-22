let currentFilter = 'all';
let pushEnabled = false;
let VAPID_PUBLIC_KEY = null;
let refreshInterval = null;
let installPrompt = null;

const ICONS = {
  bellOff: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/><path d="M3 3l18 18"/></svg>',
  bellOn: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
  signal: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 18h18"/><path d="M5 18V8"/><path d="M10 18V4"/><path d="M15 18v-6"/><path d="M20 18V9"/></svg>',
  overview: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 12h6V4H4z"/><path d="M14 20h6v-8h-6z"/><path d="M14 10h6V4h-6z"/><path d="M4 20h6v-4H4z"/></svg>',
  holdings: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/><path d="M8 4v16"/></svg>',
  insights: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v10H7l-3 3z"/><path d="M8 9h8"/><path d="M8 12h5"/></svg>',
  activity: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12h4l2-5 4 10 2-5h6"/></svg>',
  source: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5h16v10H7l-3 3z"/><path d="M8 9h8"/></svg>',
  close: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>',
};

function showToast(message, type, duration) {
  type = type || 'info';
  duration = duration || 4000;
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  const icon = type === 'success' ? ICONS.close : type === 'error' ? ICONS.close : '';
  toast.innerHTML = '<span class="toast-icon">' + (icon || '') + '</span><span class="toast-text">' + message + '</span>';
  container.appendChild(toast);
  requestAnimationFrame(function () { toast.classList.add('toast-visible'); });
  setTimeout(function () {
    toast.classList.remove('toast-visible');
    setTimeout(function () { toast.remove(); }, 300);
  }, duration);
}

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');
}

const urlB64ToUint8Array = function (b64) {
  const raw = atob(b64.replace(/-/g, '+').replace(/_/g, '/'));
  return Uint8Array.from([].map.call(raw, function (c) { return c.charCodeAt(0); }));
};

const setIcon = function (el, markup) {
  if (el) el.innerHTML = markup;
};

function hydrateStaticIcons() {
  document.querySelectorAll('[data-icon="install"]').forEach(function (el) { setIcon(el, ICONS.install); });
  document.querySelectorAll('[data-icon="signal"]').forEach(function (el) { setIcon(el, ICONS.signal); });
  document.querySelectorAll('[data-icon="overview"]').forEach(function (el) { setIcon(el, ICONS.overview); });
  document.querySelectorAll('[data-icon="holdings"]').forEach(function (el) { setIcon(el, ICONS.holdings); });
  document.querySelectorAll('[data-icon="insights"]').forEach(function (el) { setIcon(el, ICONS.insights); });
  document.querySelectorAll('[data-icon="activity"]').forEach(function (el) { setIcon(el, ICONS.activity); });
  document.querySelectorAll('[data-icon="source"]').forEach(function (el) { setIcon(el, ICONS.source); });
  setIcon(document.getElementById('pushToggleBtn'), ICONS.bellOff);
}

async function loadVapidKey() {
  try {
    const data = await fetchJSON('/api/push/vapid-key');
    VAPID_PUBLIC_KEY = data.public_key;
  } catch (_) {}
}

var _fetchCache = {};
async function fetchJSON(url, opts) {
  opts = opts || {};
  var cacheKey = url + '|' + (opts.method || 'GET');
  var cached = _fetchCache[cacheKey];
  if (cached && Date.now() - cached.ts < 30000 && !opts.method) {
    return cached.data;
  }
  const headers = Object.assign({}, opts.headers || {});
  var method = (opts.method || 'GET').toUpperCase();
  if ((method === 'POST' || method === 'PUT' || method === 'PATCH') && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(url, Object.assign({}, opts, { headers: headers }));
  if (!res.ok) {
    const err = await res.json().catch(function () { return {}; });
    throw new Error(err.error || res.statusText);
  }
  const data = await res.json();
  _fetchCache[cacheKey] = { data: data, ts: Date.now() };
  return data;
}
setInterval(function () {
  var now = Date.now();
  for (var k in _fetchCache) {
    if (_fetchCache[k] && now - _fetchCache[k].ts > 60000) delete _fetchCache[k];
  }
}, 60000);

function fmtINR(n) {
  if (n == null || Number.isNaN(Number(n))) return '\u2014';
  return '\u20B9' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtDate(ts) {
  if (!ts) return '\u2014';
  const d = new Date(ts);
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
}

function escHtml(s) {
  if (!s) return '';
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}

function setPushUI(enabled) {
  pushEnabled = enabled;
  const btn = document.getElementById('pushToggleBtn');
  setIcon(btn, enabled ? ICONS.bellOn : ICONS.bellOff);
  btn.title = enabled ? 'Disable Push' : 'Enable Push';
  btn.setAttribute('aria-label', btn.title);
}

function activateTab(tabName) {
  document.querySelectorAll('.nav-link, .dock-link').forEach(function (btn) {
    btn.classList.toggle('active', btn.dataset.tab === tabName);
  });
  document.querySelectorAll('.tab-pane').forEach(function (pane) {
    pane.classList.toggle('active', pane.id === 'tab-' + tabName);
  });
  clearInterval(refreshInterval);
  refreshInterval = null;
  if (tabName === 'alerts') { loadAlerts(); refreshInterval = setInterval(loadAlerts, 30000); }
  if (tabName === 'watchlist') { loadWatchlist(); }
  if (tabName === 'filings') { loadFilings(); }
}

document.querySelectorAll('.nav-link, .dock-link').forEach(function (btn) {
  btn.addEventListener('click', function () { activateTab(btn.dataset.tab); });
});

document.getElementById('alertTabs').addEventListener('click', function (e) {
  const btn = e.target.closest('.filter-pill');
  if (!btn) return;
  document.querySelectorAll('#alertTabs .filter-pill').forEach(function (pill) { pill.classList.remove('active'); });
  btn.classList.add('active');
  currentFilter = btn.dataset.filter;
  loadAlerts();
});

async function loadAlerts() {
  try {
    const alerts = await fetchJSON('/api/alerts?limit=100');
    const list = document.getElementById('alertsList');
    list.innerHTML = '';
    const filtered = currentFilter === 'all' ? alerts : alerts.filter(function (a) { return a.tier === currentFilter; });
    if (!filtered.length) {
      list.innerHTML = '<p class="empty-msg">No alerts yet. The engine scans continuously.</p>';
      return;
    }
    filtered.forEach(function (alert) {
      const item = document.createElement('article');
      item.className = 'alert-item';
      item.innerHTML = '<div class="alert-topline"><span class="alert-tier ' + alert.tier.toLowerCase() + '">' + alert.tier + '</span><span class="alert-impact ' + (alert.impact_pct < 0 ? 'impact-negative' : 'impact-positive') + '">' + (alert.impact_formatted || '~') + '</span><span class="alert-time">' + fmtDate(alert.timestamp) + '</span></div><div class="alert-meta"><strong>' + escHtml(alert.symbol) + '</strong> <span class="alert-company">' + escHtml(alert.company) + '</span></div>';
      item.addEventListener('click', function () {
        showAlertDetail(alert);
      });
      list.appendChild(item);
    });
  } catch (err) {
    console.error('loadAlerts error:', err);
  }
}

async function showAlertDetail(alert) {
  const existing = document.getElementById('alertDetailModal');
  if (existing) existing.remove();
  const modal = document.createElement('div');
  modal.id = 'alertDetailModal';
  modal.className = 'modal';
  modal.hidden = false;
  modal.innerHTML = '<div class="modal-content"><button class="modal-close" type="button" aria-label="Close modal"></button><p class="eyebrow">Alert Detail</p><h2>' + escHtml(alert.tier) + ' | ' + escHtml(alert.symbol) + '</h2><div class="detail-meta"><span>Action: <strong>' + escHtml(alert.action) + '</strong></span><span>Est. Impact: <strong class="alert-impact ' + (alert.impact_pct < 0 ? 'impact-negative' : 'impact-positive') + '">' + (alert.impact_formatted || '~') + '</strong></span><span>' + fmtDate(alert.timestamp) + '</span></div><p class="detail-headline">' + escHtml(alert.headline) + '</p></div>';
  document.body.appendChild(modal);
  modal.querySelector('.modal-close').addEventListener('click', function () { modal.remove(); });
  modal.addEventListener('click', function (e) { if (e.target === modal) modal.remove(); });
}

async function loadWatchlist() {
  try {
    const items = await fetchJSON('/api/watchlist');
    const list = document.getElementById('watchlistList');
    list.innerHTML = '';
    if (!items.length) {
      list.innerHTML = '<p class="empty-msg">No symbols on watchlist.</p>';
      return;
    }
    items.forEach(function (item) {
      const card = document.createElement('article');
      card.className = 'watchlist-item';
      card.innerHTML = '<div class="watchlist-symbol">' + escHtml(item.symbol) + '</div><div class="watchlist-date">' + fmtDate(item.added_at) + '</div><button class="btn btn-ghost btn-small remove-symbol" data-symbol="' + escHtml(item.symbol) + '">Remove</button>';
      list.appendChild(card);
    });
    document.querySelectorAll('.remove-symbol').forEach(function (btn) {
      btn.addEventListener('click', async function () {
        const sym = btn.dataset.symbol;
        await fetchJSON('/api/watchlist/' + encodeURIComponent(sym), { method: 'DELETE' });
        showToast('Removed ' + sym, 'success');
        loadWatchlist();
      });
    });
  } catch (err) {
    console.error('loadWatchlist error:', err);
  }
}

document.getElementById('addSymbolBtn').addEventListener('click', function () {
  const modal = document.getElementById('addSymbolModal');
  modal.hidden = false;
});
document.querySelectorAll('#addSymbolModal .modal-close').forEach(function (el) {
  el.addEventListener('click', function () { el.closest('.modal').hidden = true; });
});
document.getElementById('addSymbolSubmit').addEventListener('click', async function () {
  const input = document.getElementById('symbolInput');
  const result = document.getElementById('addSymbolResult');
  const symbol = input.value.trim().toUpperCase();
  if (!symbol) {
    result.textContent = 'Enter a symbol';
    result.className = 'import-result error';
    result.hidden = false;
    return;
  }
  try {
    await fetchJSON('/api/watchlist', {
      method: 'POST',
      body: JSON.stringify({ symbol: symbol }),
    });
    result.textContent = 'Added ' + symbol;
    result.className = 'import-result success';
    result.hidden = false;
    input.value = '';
    showToast('Added ' + symbol + ' to watchlist', 'success');
    setTimeout(function () { document.getElementById('addSymbolModal').hidden = true; }, 800);
    loadWatchlist();
  } catch (err) {
    result.textContent = err.message;
    result.className = 'import-result error';
    result.hidden = false;
  }
});

async function loadFilings() {
  try {
    const filings = await fetchJSON('/api/filings?limit=50');
    const list = document.getElementById('filingsList');
    list.innerHTML = '';
    if (!filings.length) {
      list.innerHTML = '<p class="empty-msg">No filings cached yet.</p>';
      return;
    }
    filings.forEach(function (f) {
      const item = document.createElement('article');
      item.className = 'filing-item';
      item.innerHTML = '<div class="filing-source ' + escHtml(f.source) + '">' + escHtml(f.source) + '</div><div class="filing-body"><div class="filing-title">' + escHtml(f.title) + '</div><div class="filing-meta">' + escHtml(f.symbol) + ' | ' + escHtml(f.filing_type) + ' | ' + fmtDate(f.published_at) + '</div></div>';
      list.appendChild(item);
    });
  } catch (err) {
    console.error('loadFilings error:', err);
  }
}

async function loadStats() {
  try {
    const stats = await fetchJSON('/api/stats');
    document.getElementById('statsAlertsToday').textContent = stats.alerts_today != null ? stats.alerts_today : '\u2014';
    document.getElementById('statsCriticalToday').textContent = stats.critical_today != null ? stats.critical_today : '\u2014';
    document.getElementById('statsWatchlistCount').textContent = stats.watchlist_count != null ? stats.watchlist_count : '\u2014';
  } catch (_) {}
}

async function subscribePush() {
  if (!VAPID_PUBLIC_KEY) await loadVapidKey();
  if (!VAPID_PUBLIC_KEY) { showToast('Could not load push configuration', 'error'); return; }
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlB64ToUint8Array(VAPID_PUBLIC_KEY),
    });
    await fetchJSON('/api/push/subscribe', {
      method: 'POST',
      body: JSON.stringify(sub.toJSON()),
    });
    setPushUI(true);
    showToast('Push notifications enabled', 'success');
  } catch (err) {
    showToast('Failed to enable push: ' + err.message, 'error');
  }
}

async function unsubscribePush() {
  try {
    const sub = await navigator.serviceWorker.ready.then(function (r) { return r.pushManager.getSubscription(); });
    if (sub) await sub.unsubscribe();
    await fetchJSON('/api/push/unsubscribe', { method: 'POST' });
    setPushUI(false);
    showToast('Push notifications disabled', 'info');
  } catch (err) {
    console.error('Push unsubscribe error:', err);
  }
}

document.getElementById('pushToggleBtn').addEventListener('click', async function () {
  if (pushEnabled) { await unsubscribePush(); }
  else {
    const perm = await Notification.requestPermission();
    if (perm === 'granted') { await subscribePush(); }
    else { showToast('Notification permission denied', 'error'); }
  }
});

document.getElementById('refreshBtn').addEventListener('click', function () {
  const btn = document.getElementById('refreshBtn');
  btn.disabled = true;
  loadAlerts();
  loadWatchlist();
  loadFilings();
  loadStats();
  showToast('Refreshed', 'success');
  setTimeout(function () { btn.disabled = false; }, 1000);
});

window.addEventListener('beforeinstallprompt', function (e) {
  e.preventDefault();
  installPrompt = e;
  document.getElementById('installBanner').hidden = false;
});

document.getElementById('installBtn').addEventListener('click', async function () {
  if (!installPrompt) return;
  installPrompt.prompt();
  const result = await installPrompt.userChoice;
  if (result.outcome === 'accepted') document.getElementById('installBanner').hidden = true;
  installPrompt = null;
});

document.getElementById('installDismiss').addEventListener('click', function () {
  document.getElementById('installBanner').hidden = true;
});

(function () {
  hydrateStaticIcons();
  loadVapidKey();
  activateTab('alerts');
  loadStats();
})();
