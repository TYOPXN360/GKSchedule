package com.classapp.schedule.sync

import android.content.Context
import androidx.work.*
import com.classapp.schedule.api.GdustApi
import com.classapp.schedule.api.CourseImporter
import com.classapp.schedule.data.CourseDatabase
import com.classapp.schedule.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoSyncWorker(
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
            val calendar = api.getSchoolCalendar(studentId).getOrNull()
            val year = calendar?.year ?: ""
            val semester = calendar?.semester ?: ""
            val weeks = calendar?.allWeek ?: 20

            val allCourses = mutableListOf<com.classapp.schedule.api.RemoteCourse>()
            for (week in 1..weeks) {
                val result = api.getStudentCourse(studentId, week = "$week", year = year, semester = semester)
                result.onSuccess { allCourses.addAll(it) }
                    .onFailure { e ->
                        val msg = e.message ?: ""
                        if (msg.contains("离线") || msg.contains("重新登录") || msg.contains("token", ignoreCase = true)) {
                            settings.markTokenExpired()
                            return Result.failure()
                        }
                        return if (runAttemptCount < 3) Result.retry() else Result.failure()
                    }
            }

            if (allCourses.isEmpty()) return Result.success()

            val courses = CourseImporter.convertRemoteCourses(allCourses)
            val dao = CourseDatabase.getDatabase(applicationContext).courseDao()
            dao.deleteRemoteCourses()
            courses.forEach { dao.insertCourse(it) }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_sync_courses"

        fun schedule(context: Context, intervalValue: Int, intervalUnit: String) {
            val duration = when (intervalUnit) {
                "min" -> intervalValue.toLong().coerceIn(30, 60)
                "h" -> intervalValue.toLong().coerceIn(1, 24)
                "d" -> intervalValue.toLong().coerceIn(1, 31)
                else -> 60L
            }
            val unit = when (intervalUnit) {
                "min" -> TimeUnit.MINUTES
                "h" -> TimeUnit.HOURS
                "d" -> TimeUnit.DAYS
                else -> TimeUnit.MINUTES
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoSyncWorker>(duration, unit)
                .setConstraints(constraints)
                .setInitialDelay(duration, unit)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
