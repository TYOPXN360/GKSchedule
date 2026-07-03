package com.ty.GDUST_Schedule.sync

import android.content.Context
import androidx.work.*
import com.ty.GDUST_Schedule.api.GdustApi
import com.ty.GDUST_Schedule.data.SettingsDataStore
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
                val msg = result.exceptionOrNull()?.message ?: ""
                if (msg.contains("离线") || msg.contains("重新登录") || msg.contains("token", ignoreCase = true)) {
                    // Token expired - keep identity/credentials for quick re-login.
                    settings.markTokenExpired()
                    android.util.Log.w("Heartbeat", "Token expired, marked for re-login")
                    Result.failure()
                } else {
                    android.util.Log.w("Heartbeat", "API error: $msg")
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("离线") || msg.contains("重新登录") || msg.contains("token", ignoreCase = true)) {
                settings.markTokenExpired()
                android.util.Log.w("Heartbeat", "Token expired, marked for re-login")
                Result.failure()
            } else {
                android.util.Log.e("Heartbeat", "Heartbeat failed: $msg")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
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
