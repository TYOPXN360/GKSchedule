package com.classapp.schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.classapp.schedule.data.Course
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderScheduler {
    private const val PREF_NAME = "course_reminder_alarms"
    private const val KEY_REQUEST_CODES = "request_codes"
    private const val SCHEDULE_LOOKAHEAD_DAYS = 14

    fun scheduleUpcomingReminders(
        context: Context,
        courses: List<Course>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        reminderMinutes: Int,
        getStartTime: (Int) -> String
    ) {
        cancelAll(context, courses)
        if (reminderMinutes <= 0) return

        val today = LocalDate.now()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newRequestCodes = mutableSetOf<Int>()

        for (dayOffset in 0 until SCHEDULE_LOOKAHEAD_DAYS) {
            val date = today.plusDays(dayOffset.toLong())
            val week = weekForDate(semesterStart, date)
            if (week !in 1..totalWeeks) continue

            courses.asSequence()
                .filter { !it.isHidden }
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .filter { it.isInWeek(week) }
                .sortedBy { it.startPeriod }
                .forEach { course ->
                    val startTimeStr = course.getActualStartTime(getStartTime)
                    val startTime = parseTime(startTimeStr) ?: return@forEach
                    val reminderTime = LocalDateTime.of(date, startTime)
                        .minusMinutes(reminderMinutes.toLong())
                    if (!reminderTime.isAfter(LocalDateTime.now())) return@forEach

                    val triggerTime = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val requestCode = requestCode(course, date)
                    val intent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra(ReminderReceiver.EXTRA_COURSE_NAME, course.name)
                        putExtra(ReminderReceiver.EXTRA_CLASSROOM, course.classroom)
                        putExtra(ReminderReceiver.EXTRA_TEACHER, course.teacher)
                        putExtra(ReminderReceiver.EXTRA_START_TIME, startTimeStr)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, triggerTime, pendingIntent)
                    newRequestCodes.add(requestCode)
                }
        }

        saveRequestCodes(context, newRequestCodes)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        loadRequestCodes(context).forEach { requestCode ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
        clearRequestCodes(context)
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
                // Fall back below.
            }
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    private fun weekForDate(semesterStart: LocalDate, date: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(semesterStart, date).toInt()
        if (days < 0) return 0
        return (days / 7) + 1
    }

    private fun parseTime(value: String): LocalTime? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return try {
            LocalTime.of(hour, minute)
        } catch (_: Exception) {
            null
        }
    }

    private fun requestCode(course: Course, date: LocalDate): Int =
        "${course.id}|${date.toEpochDay()}|${course.startPeriod}".hashCode()

    private fun loadRequestCodes(context: Context): Set<Int> =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_REQUEST_CODES, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()

    private fun saveRequestCodes(context: Context, requestCodes: Set<Int>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_REQUEST_CODES, requestCodes.map { it.toString() }.toSet())
            .apply()
    }

    private fun clearRequestCodes(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_REQUEST_CODES)
            .apply()
    }

    fun cancelAll(context: Context, courses: List<Course>) {
        cancelAll(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        courses.forEach { course ->
            val legacyRequestCode = (course.id * 100 + course.startPeriod).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                legacyRequestCode,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

}
