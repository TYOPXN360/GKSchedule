package com.ty.gkschedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ty.gkschedule.data.CourseDatabase
import com.ty.gkschedule.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // ponytail: 开机主线程排几十个闹钟会ANR，扔后台；闹钟数暴涨致goAsync超时再改WorkManager
        val pending = goAsync()
        kotlin.concurrent.thread {
            try {
                val settings = SettingsDataStore(context)
                val database = CourseDatabase.getDatabase(context)
                val courses = runBlocking {
                    database.courseDao().getAllCourses().first()
                }
                val reminderMinutes = runBlocking { settings.reminderMinutes.first() }
                val liveUpdate = runBlocking { settings.reminderLiveUpdate.first() }
                val examLiveUpdate = runBlocking { settings.reminderExamLiveUpdate.first() }
                // ponytail: 全关才删完return；任一开都排，进度独立于课前提醒
                if (reminderMinutes <= 0 && !liveUpdate && !examLiveUpdate) {
                    ReminderScheduler.cancelAll(context, courses)
                    return@thread
                }
                val semesterStart = runBlocking { settings.semesterStart.first() }
                val totalWeeks = runBlocking { settings.totalWeeks.first() }
                val reminderMode = runBlocking { settings.reminderMode.first() }
                val exams = runBlocking {
                    database.examDao().getAllExams().first()
                }

                if (courses.isEmpty() && exams.isEmpty()) return@thread

                ReminderScheduler.scheduleUpcomingReminders(
                    context = context,
                    courses = courses,
                    exams = exams,
                    semesterStart = semesterStart,
                    totalWeeks = totalWeeks,
                    reminderMinutes = reminderMinutes,
                    reminderMode = reminderMode,
                    liveUpdate = liveUpdate,
                    examLiveUpdate = examLiveUpdate,
                    getStartTime = { period -> settings.getStartTime(period) },
                    getEndTime = { period -> settings.getEndTime(period) }
                )
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
