package com.newspulse.ai

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.newspulse.ai.service.NotificationHelper
import com.newspulse.ai.service.ScheduledScanWorker
import java.util.concurrent.TimeUnit

class NewsPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this)
        schedulePeriodicBackgroundScan()
    }

    private fun schedulePeriodicBackgroundScan() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<ScheduledScanWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NewsPulsePeriodicScan",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }
}
