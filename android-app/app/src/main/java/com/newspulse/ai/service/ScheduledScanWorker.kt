package com.newspulse.ai.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.newspulse.ai.domain.CrashEngine

class ScheduledScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val engine = CrashEngine(applicationContext)
            engine.runScanCycle()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
