package com.newspulse.ai.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "newspulse_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("newspulse_fallback_prefs", Context.MODE_PRIVATE)
    }

    private val _groqApiKey = MutableStateFlow(prefs.getString(KEY_GROQ_API_KEY, "") ?: "")
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private val _upstoxAccessToken = MutableStateFlow(prefs.getString(KEY_UPSTOX_ACCESS_TOKEN, "") ?: "")
    val upstoxAccessToken: StateFlow<String> = _upstoxAccessToken.asStateFlow()

    private val _newsApiKey = MutableStateFlow(prefs.getString(KEY_NEWS_API_KEY, "") ?: "")
    val newsApiKey: StateFlow<String> = _newsApiKey.asStateFlow()

    private val _finnhubApiKey = MutableStateFlow(prefs.getString(KEY_FINNHUB_API_KEY, "") ?: "")
    val finnhubApiKey: StateFlow<String> = _finnhubApiKey.asStateFlow()

    private val _marketHoursOnly = MutableStateFlow(prefs.getBoolean(KEY_MARKET_HOURS_ONLY, true))
    val marketHoursOnly: StateFlow<Boolean> = _marketHoursOnly.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _minPanicThreshold = MutableStateFlow(prefs.getInt(KEY_MIN_PANIC_THRESHOLD, 40))
    val minPanicThreshold: StateFlow<Int> = _minPanicThreshold.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(prefs.getBoolean(KEY_MONITORING_ACTIVE, true))
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    fun setGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_API_KEY, key.trim()).apply()
        _groqApiKey.value = key.trim()
    }

    fun setUpstoxAccessToken(token: String) {
        prefs.edit().putString(KEY_UPSTOX_ACCESS_TOKEN, token.trim()).apply()
        _upstoxAccessToken.value = token.trim()
    }

    fun setNewsApiKey(key: String) {
        prefs.edit().putString(KEY_NEWS_API_KEY, key.trim()).apply()
        _newsApiKey.value = key.trim()
    }

    fun setFinnhubApiKey(key: String) {
        prefs.edit().putString(KEY_FINNHUB_API_KEY, key.trim()).apply()
        _finnhubApiKey.value = key.trim()
    }

    fun setMarketHoursOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MARKET_HOURS_ONLY, enabled).apply()
        _marketHoursOnly.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setMinPanicThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_MIN_PANIC_THRESHOLD, threshold).apply()
        _minPanicThreshold.value = threshold
    }

    fun setMonitoringActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ACTIVE, active).apply()
        _isMonitoringActive.value = active
    }

    companion object {
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_UPSTOX_ACCESS_TOKEN = "upstox_access_token"
        private const val KEY_NEWS_API_KEY = "news_api_key"
        private const val KEY_FINNHUB_API_KEY = "finnhub_api_key"
        private const val KEY_MARKET_HOURS_ONLY = "market_hours_only"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_MIN_PANIC_THRESHOLD = "min_panic_threshold"
        private const val KEY_MONITORING_ACTIVE = "monitoring_active"
    }
}
