package com.newspulse.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.newspulse.ai.R
import com.newspulse.ai.data.model.Alert
import com.newspulse.ai.data.model.SeverityTier
import com.newspulse.ai.domain.HeadlineImpactEstimator
import com.newspulse.ai.ui.MainActivity

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                context.getString(R.string.market_monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.market_monitor_channel_desc)
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                context.getString(R.string.alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alert_channel_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun buildForegroundServiceNotification(statusText: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITOR_ID)
            .setContentTitle("NewsPulse AI • Market Surveillance")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showCrashAlert(alert: Alert) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ALERT_ID", alert.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alert.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedImpact = HeadlineImpactEstimator.format(alert.impactPct)
        val title = "[${alert.tier.name}] ${alert.symbol} (Score ${alert.trustScore}) • Est. $formattedImpact"

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setContentTitle(title)
            .setContentText(alert.headline)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.headline}\n\nReason: ${alert.reasoning}"))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(if (alert.tier == SeverityTier.CRITICAL) 0xFFFF3B30.toInt() else 0xFFFF9500.toInt())

        notificationManager.notify(alert.id.toInt(), builder.build())
    }

    companion object {
        const val CHANNEL_MONITOR_ID = "channel_market_monitor"
        const val CHANNEL_ALERT_ID = "channel_crash_alerts"
        const val FOREGROUND_NOTIFICATION_ID = 1001
    }
}
