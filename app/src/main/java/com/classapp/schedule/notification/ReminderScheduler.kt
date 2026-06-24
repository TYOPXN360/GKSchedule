package com.classapp.schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.classapp.schedule.data.Course
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    fun scheduleDailyReminders(
        context: Context,
        courses: List<Course>,
        semesterStart: LocalDate,
        reminderMinutes: Int,
        getStartTime: (Int) -> String
    ) {
        if (reminderMinutes <= 0) return

        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule for today's remaining courses
        courses.filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startPeriod }.forEach { course ->
            val startTimeStr = course.getActualStartTime(getStartTime)
            val parts = startTimeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach

            val courseTime = LocalDateTime.of(today, LocalTime.of(hour, minute))
            val reminderTime = courseTime.minusMinutes(reminderMinutes.toLong())
            val now = LocalDateTime.now()

            if (reminderTime.isAfter(now)) {
                val triggerTime = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(ReminderReceiver.EXTRA_COURSE_NAME, course.name)
                    putExtra(ReminderReceiver.EXTRA_CLASSROOM, course.classroom)
                    putExtra(ReminderReceiver.EXTRA_TEACHER, course.teacher)
                    putExtra(ReminderReceiver.EXTRA_START_TIME, startTimeStr)
                }
                val requestCode = (course.id * 100 + course.startPeriod).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                } catch (_: SecurityException) {
                    // Fallback to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                }
            }
        }
    }

    fun cancelAll(context: Context, courses: List<Course>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        courses.forEach { course ->
            val intent = Intent(context, ReminderReceiver::class.java)
            val requestCode = (course.id * 100 + course.startPeriod).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }
}
