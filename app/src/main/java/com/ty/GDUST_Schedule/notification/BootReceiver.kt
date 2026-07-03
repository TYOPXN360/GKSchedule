package com.ty.GDUST_Schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ty.GDUST_Schedule.data.CourseDatabase
import com.ty.GDUST_Schedule.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = SettingsDataStore(context)
        val reminderMinutes = runBlocking { settings.reminderMinutes.first() }
        if (reminderMinutes <= 0) return

        val semesterStart = runBlocking { settings.semesterStart.first() }
        val totalWeeks = runBlocking { settings.totalWeeks.first() }
        val liveUpdate = runBlocking { settings.reminderLiveUpdate.first() }
        val database = CourseDatabase.getDatabase(context)
        val courses = runBlocking {
            database.courseDao().getAllCourses().first()
        }
        val exams = runBlocking {
            database.examDao().getAllExams().first()
        }

        if (courses.isEmpty() && exams.isEmpty()) return

        ReminderScheduler.scheduleUpcomingReminders(
            context = context,
            courses = courses,
            exams = exams,
            semesterStart = semesterStart,
            totalWeeks = totalWeeks,
            reminderMinutes = reminderMinutes,
            liveUpdate = liveUpdate,
            getStartTime = { period -> settings.getStartTime(period) },
            getEndTime = { period -> settings.getEndTime(period) }
        )
    }
}
