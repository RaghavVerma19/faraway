package com.newspulse.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newspulse.ai.data.local.AppDatabase
import com.newspulse.ai.data.model.Alert
import com.newspulse.ai.data.model.Filing
import com.newspulse.ai.data.model.LiveQuote
import com.newspulse.ai.data.model.WatchlistItem
import com.newspulse.ai.data.preferences.UserPreferences
import com.newspulse.ai.data.remote.GroqApiService
import com.newspulse.ai.data.remote.UpstoxBrokerService
import com.newspulse.ai.data.remote.UpstoxOrderResponse
import com.newspulse.ai.data.remote.UpstoxPosition
import com.newspulse.ai.domain.CrashEngine
import com.newspulse.ai.service.MarketMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val preferences = UserPreferences(application)
    private val crashEngine = CrashEngine(application, database, preferences)
    private val groqApiService = GroqApiService()
    val upstoxBrokerService = UpstoxBrokerService()

    val alerts: StateFlow<List<Alert>> = database.alertDao()
        .getAllAlerts(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val watchlist: StateFlow<List<WatchlistItem>> = database.watchlistDao()
        .getWatchlist()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filings: StateFlow<List<Filing>> = database.filingDao()
        .getRecentFilings(50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val upstoxAccessToken = preferences.upstoxAccessToken
    val groqApiKey = preferences.groqApiKey
    val newsApiKey = preferences.newsApiKey
    val finnhubApiKey = preferences.finnhubApiKey
    val marketHoursOnly = preferences.marketHoursOnly
    val notificationsEnabled = preferences.notificationsEnabled
    val isMonitoringActive = preferences.isMonitoringActive

    private val _liveQuotes = MutableStateFlow<Map<String, LiveQuote>>(emptyMap())
    val liveQuotes: StateFlow<Map<String, LiveQuote>> = _liveQuotes.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _upstoxPositions = MutableStateFlow<List<UpstoxPosition>>(emptyList())
    val upstoxPositions: StateFlow<List<UpstoxPosition>> = _upstoxPositions.asStateFlow()

    private val _isFetchingPositions = MutableStateFlow(false)
    val isFetchingPositions: StateFlow<Boolean> = _isFetchingPositions.asStateFlow()

    private val _lastOrderExecutionResult = MutableStateFlow<UpstoxOrderResponse?>(null)
    val lastOrderExecutionResult: StateFlow<UpstoxOrderResponse?> = _lastOrderExecutionResult.asStateFlow()

    private val _groqTestStatus = MutableStateFlow<String?>(null)
    val groqTestStatus: StateFlow<String?> = _groqTestStatus.asStateFlow()

    private val _upstoxConnectionStatus = MutableStateFlow<String?>(null)
    val upstoxConnectionStatus: StateFlow<String?> = _upstoxConnectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            if (database.watchlistDao().getWatchlistSync().isEmpty()) {
                val defaults = listOf(
                    WatchlistItem("RELIANCE", "Reliance Industries"),
                    WatchlistItem("TCS", "Tata Consultancy Services"),
                    WatchlistItem("HDFCBANK", "HDFC Bank"),
                    WatchlistItem("INFY", "Infosys"),
                    WatchlistItem("TATAMOTORS", "Tata Motors"),
                    WatchlistItem("ADANIENT", "Adani Enterprises")
                )
                defaults.forEach { database.watchlistDao().addToWatchlist(it) }
            }
            if (preferences.upstoxAccessToken.value.isNotBlank()) {
                refreshUpstoxData()
            }
        }
    }

    fun triggerManualScan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                crashEngine.runScanCycle()
                refreshUpstoxData()
            } catch (e: Exception) {
                // Ignore error
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun executeRealUpstoxOrder(symbol: String, transactionType: String = "SELL", quantity: Int = 1) {
        viewModelScope.launch {
            val res = upstoxBrokerService.placeRealOrder(
                token = preferences.upstoxAccessToken.value,
                symbol = symbol,
                transactionType = transactionType,
                quantity = quantity
            )
            _lastOrderExecutionResult.value = res
            refreshUpstoxData()
        }
    }

    fun refreshUpstoxData() {
        val token = preferences.upstoxAccessToken.value
        if (token.isBlank()) return
        viewModelScope.launch {
            _isFetchingPositions.value = true
            try {
                // Fetch live quotes for top tracked stocks
                val symbols = listOf("RELIANCE", "TCS", "INFY", "HDFCBANK", "TATAMOTORS", "ADANIENT", "SBIN", "ICICIBANK", "HCLTECH")
                val quotes = upstoxBrokerService.fetchLiveQuotes(token, symbols)
                if (quotes.isNotEmpty()) {
                    _liveQuotes.value = quotes
                }

                // Fetch real user positions
                val pos = upstoxBrokerService.fetchPositions(token)
                _upstoxPositions.value = pos
            } catch (e: Exception) {
                // Ignore error
            } finally {
                _isFetchingPositions.value = false
            }
        }
    }

    fun setUpstoxAccessToken(token: String) {
        preferences.setUpstoxAccessToken(token)
        _upstoxConnectionStatus.value = "Upstox Token Saved"
        refreshUpstoxData()
    }

    fun testUpstoxConnection(token: String) {
        if (token.isBlank()) {
            _upstoxConnectionStatus.value = "Enter an Upstox Token"
            return
        }
        viewModelScope.launch {
            _upstoxConnectionStatus.value = "Testing connection..."
            val isValid = upstoxBrokerService.testConnection(token)
            if (isValid) {
                _upstoxConnectionStatus.value = "Connected to Upstox API v2"
                preferences.setUpstoxAccessToken(token)
                refreshUpstoxData()
            } else {
                _upstoxConnectionStatus.value = "Invalid or Expired Token"
            }
        }
    }

    fun addToWatchlist(symbol: String, companyName: String) {
        viewModelScope.launch {
            database.watchlistDao().addToWatchlist(
                WatchlistItem(symbol = symbol.uppercase().trim(), companyName = companyName.trim())
            )
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            database.watchlistDao().removeFromWatchlist(symbol)
        }
    }

    fun toggleWatchlist(symbol: String, companyName: String) {
        viewModelScope.launch {
            if (database.watchlistDao().isSymbolOnWatchlist(symbol)) {
                database.watchlistDao().removeFromWatchlist(symbol)
            } else {
                database.watchlistDao().addToWatchlist(
                    WatchlistItem(symbol = symbol.uppercase().trim(), companyName = companyName.trim())
                )
            }
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            database.alertDao().deleteAlert(alertId)
        }
    }

    fun setGroqApiKey(key: String) = preferences.setGroqApiKey(key)
    fun setNewsApiKey(key: String) = preferences.setNewsApiKey(key)
    fun setFinnhubApiKey(key: String) = preferences.setFinnhubApiKey(key)
    fun setMarketHoursOnly(enabled: Boolean) = preferences.setMarketHoursOnly(enabled)
    fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)

    fun toggleMonitoring(active: Boolean) {
        preferences.setMonitoringActive(active)
        if (active) {
            MarketMonitorService.start(getApplication())
        } else {
            MarketMonitorService.stop(getApplication())
        }
    }

    fun testGroqApiKey(key: String) {
        if (key.isBlank()) {
            _groqTestStatus.value = "Enter an API Key"
            return
        }
        viewModelScope.launch {
            _groqTestStatus.value = "Testing..."
            val res = groqApiService.analyzeCrashHeadline(
                apiKey = key,
                headline = "SEBI initiates investigation into company disclosure violations",
                source = "test"
            )
            if (res != null) {
                _groqTestStatus.value = "Success! (OpenAI GPT-OSS 120B Connected)"
            } else {
                _groqTestStatus.value = "Failed: Invalid Key or Network Error"
            }
        }
    }
}
