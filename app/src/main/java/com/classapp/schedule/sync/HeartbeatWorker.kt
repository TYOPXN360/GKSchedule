package com.classapp.schedule.sync

import android.content.Context
import androidx.work.*
import com.classapp.schedule.api.GdustApi
import com.classapp.schedule.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Lightweight heartbeat worker to keep the auth token alive.
 * Calls getUserInfo every 15 minutes - minimal data transfer.
 */
class HeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = SettingsDataStore(applicationContext)
        val token = settings.savedToken.first()
        val studentId = settings.savedStudentId.first()

        if (token.isEmpty() || studentId.isEmpty()) return Result.failure()

        val api = GdustApi()
        api.setToken(token)

        return try {
            // Lightweight call - just getUserInfo to keep token alive
            val result = api.getUserInfo()
            if (result.isSuccess) {
                android.util.Log.d("Heartbeat", "Token keepalive success")
                Result.success()
            } else {
                android.util.Log.w("Heartbeat", "Token may be expired")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            android.util.Log.e("Heartbeat", "Heartbeat failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "token_heartbeat"
        private const val INTERVAL_MINUTES = 15L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
