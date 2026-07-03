package com.classapp.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.classapp.schedule.data.CourseDatabase
import com.classapp.schedule.data.SettingsDataStore
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
        val courses = runBlocking {
            CourseDatabase.getDatabase(context).courseDao().getAllCourses().first()
        }

        if (courses.isEmpty()) return

        ReminderScheduler.scheduleUpcomingReminders(
            context, courses, semesterStart, totalWeeks, reminderMinutes
        ) { period -> settings.getStartTime(period) }
    }
}
