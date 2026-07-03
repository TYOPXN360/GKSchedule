package com.classapp.schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.classapp.schedule.data.Course
import com.classapp.schedule.data.ExamEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderScheduler {
    private const val PREF_NAME = "course_reminder_alarms"
    private const val KEY_REQUEST_CODES = "request_codes"
    private const val SCHEDULE_LOOKAHEAD_DAYS = 14
    private const val PROGRESS_UPDATE_INTERVAL_MINUTES = 5L
    private const val KIND_COURSE = "course"
    private const val KIND_EXAM = "exam"

    private data class ReminderSession(
        val kind: String,
        val sourceId: Long,
        val name: String,
        val classroom: String,
        val teacher: String,
        val start: LocalDateTime,
        val end: LocalDateTime
    )

    fun scheduleUpcomingReminders(
        context: Context,
        courses: List<Course>,
        exams: List<ExamEntity>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        reminderMinutes: Int,
        liveUpdate: Boolean,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ) {
        cancelAll(context, courses)
        if (reminderMinutes <= 0) return

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newRequestCodes = mutableSetOf<Int>()

        val sessions = buildCourseSessions(
            courses = courses,
            semesterStart = semesterStart,
            totalWeeks = totalWeeks,
            today = today,
            getStartTime = getStartTime,
            getEndTime = getEndTime
        ) + buildExamSessions(exams, today)

        sessions
            .filter { it.end.isAfter(now) }
            .forEach { session ->
                scheduleSession(
                    context = context,
                    alarmManager = alarmManager,
                    session = session,
                    reminderMinutes = reminderMinutes,
                    liveUpdate = liveUpdate,
                    now = now,
                    requestCodes = newRequestCodes
                )
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

    private fun buildCourseSessions(
        courses: List<Course>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        today: LocalDate,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): List<ReminderSession> {
        val sessions = mutableListOf<ReminderSession>()
        for (dayOffset in 0 until SCHEDULE_LOOKAHEAD_DAYS) {
            val date = today.plusDays(dayOffset.toLong())
            val week = weekForDate(semesterStart, date)
            if (week !in 1..totalWeeks) continue

            courses.asSequence()
                .filter { !it.isHidden }
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .filter { it.isInWeek(week) }
                .forEach { course ->
                    val startTime = parseTime(course.getActualStartTime(getStartTime)) ?: return@forEach
                    val endTime = parseTime(course.getActualEndTime(getEndTime)) ?: return@forEach
                    val start = LocalDateTime.of(date, startTime)
                    val end = LocalDateTime.of(date, endTime)
                    if (!end.isAfter(start)) return@forEach
                    sessions.add(
                        ReminderSession(
                            kind = KIND_COURSE,
                            sourceId = course.id,
                            name = course.name,
                            classroom = course.classroom,
                            teacher = course.teacher,
                            start = start,
                            end = end
                        )
                    )
                }
        }
        return sessions.sortedWith(compareBy({ it.start }, { it.name }))
    }

    private fun buildExamSessions(
        exams: List<ExamEntity>,
        today: LocalDate
    ): List<ReminderSession> {
        val latestDate = today.plusDays((SCHEDULE_LOOKAHEAD_DAYS - 1).toLong())
        return exams.mapNotNull { exam ->
            val date = try {
                LocalDate.parse(exam.examDate)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            if (date.isBefore(today) || date.isAfter(latestDate)) return@mapNotNull null
            val (startTime, endTime) = examTimeRange(exam) ?: return@mapNotNull null
            val start = LocalDateTime.of(date, startTime)
            val end = LocalDateTime.of(date, endTime)
            if (!end.isAfter(start)) return@mapNotNull null
            ReminderSession(
                kind = KIND_EXAM,
                sourceId = exam.id,
                name = exam.courseName,
                classroom = exam.classroom,
                teacher = exam.teacherInfo,
                start = start,
                end = end
            )
        }.sortedWith(compareBy({ it.start }, { it.name }))
    }

    private fun scheduleSession(
        context: Context,
        alarmManager: AlarmManager,
        session: ReminderSession,
        reminderMinutes: Int,
        liveUpdate: Boolean,
        now: LocalDateTime,
        requestCodes: MutableSet<Int>
    ) {
        val notificationId = notificationId(session)
        val reminderTime = session.start.minusMinutes(reminderMinutes.toLong())
        if (reminderTime.isAfter(now)) {
            scheduleEvent(
                context = context,
                alarmManager = alarmManager,
                session = session,
                eventType = ReminderReceiver.EVENT_REMINDER,
                triggerAt = reminderTime,
                notificationId = notificationId,
                reminderMinutes = reminderMinutes,
                requestCodes = requestCodes
            )
        }

        if (!liveUpdate) return

        var progressTime = if (now.isAfter(session.start) && now.isBefore(session.end)) {
            now.plusSeconds(2)
        } else {
            session.start
        }
        while (progressTime.isBefore(session.end)) {
            if (progressTime.isAfter(now)) {
                scheduleEvent(
                    context = context,
                    alarmManager = alarmManager,
                    session = session,
                    eventType = ReminderReceiver.EVENT_PROGRESS,
                    triggerAt = progressTime,
                    notificationId = notificationId,
                    reminderMinutes = reminderMinutes,
                    requestCodes = requestCodes
                )
            }
            progressTime = progressTime.plusMinutes(PROGRESS_UPDATE_INTERVAL_MINUTES)
        }

        if (session.end.isAfter(now)) {
            scheduleEvent(
                context = context,
                alarmManager = alarmManager,
                session = session,
                eventType = ReminderReceiver.EVENT_END,
                triggerAt = session.end,
                notificationId = notificationId,
                reminderMinutes = reminderMinutes,
                requestCodes = requestCodes
            )
        }
    }

    private fun scheduleEvent(
        context: Context,
        alarmManager: AlarmManager,
        session: ReminderSession,
        eventType: String,
        triggerAt: LocalDateTime,
        notificationId: Int,
        reminderMinutes: Int,
        requestCodes: MutableSet<Int>
    ) {
        val triggerTime = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val requestCode = requestCode(session, eventType, triggerTime)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_TYPE, eventType)
            putExtra(ReminderReceiver.EXTRA_ITEM_TYPE, session.kind)
            putExtra(ReminderReceiver.EXTRA_COURSE_NAME, session.name)
            putExtra(ReminderReceiver.EXTRA_CLASSROOM, session.classroom)
            putExtra(ReminderReceiver.EXTRA_TEACHER, session.teacher)
            putExtra(ReminderReceiver.EXTRA_START_TIME, formatTime(session.start))
            putExtra(ReminderReceiver.EXTRA_END_TIME, formatTime(session.end))
            putExtra(ReminderReceiver.EXTRA_START_EPOCH_MILLIS, epochMillis(session.start))
            putExtra(ReminderReceiver.EXTRA_END_EPOCH_MILLIS, epochMillis(session.end))
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderReceiver.EXTRA_REMINDER_MINUTES, reminderMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(alarmManager, triggerTime, pendingIntent)
        requestCodes.add(requestCode)
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

    private fun examTimeRange(exam: ExamEntity): Pair<LocalTime, LocalTime>? {
        val customStart = exam.customStartTime.takeIf { it.isNotBlank() }?.let(::parseTime)
        val customEnd = exam.customEndTime.takeIf { it.isNotBlank() }?.let(::parseTime)
        if (customStart != null && customEnd != null) return customStart to customEnd

        val parts = exam.examTimeRange.split("-")
        if (parts.size != 2) return null
        val start = parseTime(parts[0].trim()) ?: return null
        val end = parseTime(parts[1].trim()) ?: return null
        return start to end
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

    private fun requestCode(session: ReminderSession, eventType: String, triggerTime: Long): Int =
        "${session.kind}|${session.sourceId}|${epochMillis(session.start)}|$eventType|$triggerTime".hashCode()

    private fun notificationId(session: ReminderSession): Int =
        "${session.kind}|${session.sourceId}|${epochMillis(session.start)}".hashCode()

    private fun epochMillis(value: LocalDateTime): Long =
        value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun formatTime(value: LocalDateTime): String =
        "%02d:%02d".format(value.hour, value.minute)

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
}
