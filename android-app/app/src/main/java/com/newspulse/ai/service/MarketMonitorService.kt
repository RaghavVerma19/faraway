package com.newspulse.ai.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.newspulse.ai.data.preferences.UserPreferences
import com.newspulse.ai.domain.CrashEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MarketMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    private lateinit var crashEngine: CrashEngine
    private lateinit var preferences: UserPreferences
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        crashEngine = CrashEngine(this)
        preferences = UserPreferences(this)
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NotificationHelper.FOREGROUND_NOTIFICATION_ID,
            notificationHelper.buildForegroundServiceNotification(MarketSchedule.getMarketStatusDescription())
        )

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        preferences.setMonitoringActive(true)

        monitorJob = serviceScope.launch {
            while (isActive) {
                val marketHoursOnly = preferences.marketHoursOnly.value
                val isMarketOpen = MarketSchedule.isMarketHours()

                if (!marketHoursOnly || isMarketOpen) {
                    try {
                        val result = crashEngine.runScanCycle()
                        val status = "Scanned ${result.totalFetched} items (${result.alertsGenerated} alerts)"
                        val notification = notificationHelper.buildForegroundServiceNotification(status)
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        notificationManager.notify(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
                    } catch (e: Exception) {
                        // Resilient error catch
                    }
                } else {
                    val status = MarketSchedule.getMarketStatusDescription()
                    val notification = notificationHelper.buildForegroundServiceNotification(status)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.notify(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
                }

                // Adaptive delay: fast 60s in market, 5m outside market
                val intervalSeconds = if (isMarketOpen) 60 else 300
                delay(intervalSeconds * 1000L)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        preferences.setMonitoringActive(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.newspulse.ai.action.START_MONITOR"
        const val ACTION_STOP = "com.newspulse.ai.action.STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, MarketMonitorService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MarketMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
