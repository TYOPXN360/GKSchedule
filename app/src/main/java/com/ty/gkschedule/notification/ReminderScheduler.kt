package com.ty.gkschedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.data.ExamEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderScheduler {
    private const val PREF_NAME = "course_reminder_alarms"
    private const val KEY_REQUEST_CODES = "request_codes"
    // ponytail: 只排当天，每节≤3个(课前+首帧+解散)+午夜重排×1，远离500上限
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
        reminderMode: String = "notify",
        liveUpdate: Boolean,
        examLiveUpdate: Boolean,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ) = scheduleToday(context, courses, exams, semesterStart, totalWeeks, reminderMinutes, reminderMode, liveUpdate, examLiveUpdate, getStartTime, getEndTime)

    // 午夜重排入口：读DB排当天，不依赖调用方传参
    fun scheduleTodayFromStore(context: Context) {
        val settings = com.ty.gkschedule.data.SettingsDataStore(context)
        val courses: List<Course>
        val db = com.ty.gkschedule.data.CourseDatabase.getDatabase(context)
        courses = runBlocking { db.courseDao().getAllCourses().first() }
        // ponytail: 全关才删完return；任一开都进scheduleToday，里面各自独立判断
        val reminderMinutes = runBlocking { settings.reminderMinutes.first() }
        val liveUpdate = runBlocking { settings.reminderLiveUpdate.first() }
        val examLiveUpdate = runBlocking { settings.reminderExamLiveUpdate.first() }
        if (reminderMinutes <= 0 && !liveUpdate && !examLiveUpdate) {
            cancelAll(context, courses)
            return
        }
        val semesterStart = runBlocking { settings.semesterStart.first() }
        val totalWeeks = runBlocking { settings.totalWeeks.first() }
        val reminderMode = runBlocking { settings.reminderMode.first() }
        val exams = runBlocking { db.examDao().getAllExams().first() }
        scheduleToday(context, courses, exams, semesterStart, totalWeeks, reminderMinutes, reminderMode, liveUpdate, examLiveUpdate, { p -> settings.getStartTime(p) }, { p -> settings.getEndTime(p) })
    }

    private fun scheduleToday(
        context: Context,
        courses: List<Course>,
        exams: List<ExamEntity>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        reminderMinutes: Int,
        reminderMode: String = "notify",
        liveUpdate: Boolean,
        examLiveUpdate: Boolean,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ) {
        // ponytail: 先删后排；双开关全关才return——课程提醒与进度通知各自独立
        cancelAll(context, courses)
        if (reminderMinutes <= 0 && !liveUpdate && !examLiveUpdate) return

        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newRequestCodes = mutableSetOf<Int>()

        val sessions = buildTodayCourseSessions(
            courses = courses,
            semesterStart = semesterStart,
            totalWeeks = totalWeeks,
            today = today,
            getStartTime = getStartTime,
            getEndTime = getEndTime
        ) + buildTodayExamSessions(exams, today)

        sessions
            .filter { it.end.isAfter(now) }
            .forEach { session ->
                val notificationId = notificationId(session)
                val countdown = reminderMode == "countdown"
                // ponytail: 倒计时常驻Live Update（课前提醒点→上课），上课后进度链自动接管
                if (countdown && reminderMinutes > 0 && session.start.isAfter(now)) {
                    val cdTime = session.start.minusMinutes(reminderMinutes.toLong())
                    if (cdTime.isAfter(now)) {
                        scheduleEvent(
                            context = context,
                            alarmManager = alarmManager,
                            session = session,
                            eventType = ReminderReceiver.EVENT_COUNTDOWN,
                            triggerAt = cdTime,
                            notificationId = notificationId,
                            reminderMinutes = reminderMinutes,
                            requestCodes = newRequestCodes
                        )
                    }
                    // ponytail: 到点自动转进度——END闹钟当接力棒，Receiver里重排当天触发PROGRESS
                    if (session.end.isAfter(now)) {
                        scheduleEvent(
                            context = context,
                            alarmManager = alarmManager,
                            session = session,
                            eventType = ReminderReceiver.EVENT_END,
                            triggerAt = session.start,
                            notificationId = notificationId,
                            reminderMinutes = reminderMinutes,
                            requestCodes = newRequestCodes
                        )
                    }
                }
                // ponytail: 课前提醒只在分钟数>0时排，独立于进度开关；倒计时模式不弹一次性提醒
                val reminderTime = session.start.minusMinutes(reminderMinutes.toLong())
                if (!countdown && reminderMinutes > 0 && reminderTime.isAfter(now)) {
                    scheduleEvent(
                        context = context,
                        alarmManager = alarmManager,
                        session = session,
                        eventType = ReminderReceiver.EVENT_REMINDER,
                        triggerAt = reminderTime,
                        notificationId = notificationId,
                        reminderMinutes = reminderMinutes,
                        requestCodes = newRequestCodes
                    )
                }
                // ponytail: 课程/考试进度各看各开关，互不为附属；PROGRESS只发首帧并埋下分钟链
                val liveOn = (session.kind == KIND_COURSE && liveUpdate) || (session.kind == KIND_EXAM && examLiveUpdate)
                if (liveOn) {
                    val progressTime = if (now.isAfter(session.start) && now.isBefore(session.end)) now.plusSeconds(2) else session.start
                    if (progressTime.isAfter(now) && progressTime.isBefore(session.end)) {
                        scheduleEvent(
                            context = context,
                            alarmManager = alarmManager,
                            session = session,
                            eventType = ReminderReceiver.EVENT_PROGRESS,
                            triggerAt = progressTime,
                            notificationId = notificationId,
                            reminderMinutes = reminderMinutes,
                            triggerTick = true,
                            requestCodes = newRequestCodes
                        )
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
                            requestCodes = newRequestCodes
                        )
                    }
                }
            }

        scheduleMidnightRollover(context, alarmManager, newRequestCodes)
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

    private fun buildTodayCourseSessions(
        courses: List<Course>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        today: LocalDate,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): List<ReminderSession> {
        // ponytail: 只排当天，1节=课前×1+首帧×1+解散×1，撞500上限是历史问题；跨天重排靠午夜闹钟
        val date = today
        val sessions = mutableListOf<ReminderSession>()
        val week = weekForDate(semesterStart, date)
        if (week in 1..totalWeeks) {

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

    private fun buildTodayExamSessions(
        exams: List<ExamEntity>,
        today: LocalDate
    ): List<ReminderSession> {
        return exams.mapNotNull { exam ->
            val date = try {
                LocalDate.parse(exam.examDate)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            if (date != today) return@mapNotNull null
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

    // 午夜重排闹钟：每天 00:01 触发 ReminderReceiver 重排当天
    private fun scheduleMidnightRollover(
        context: Context,
        alarmManager: AlarmManager,
        requestCodes: MutableSet<Int>
    ) {
        val triggerAt = LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1)
        val triggerTime = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val requestCode = "rollover|${LocalDate.now()}".hashCode()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_TYPE, ReminderReceiver.EVENT_ROLLOVER)
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

    private fun scheduleEvent(
        context: Context,
        alarmManager: AlarmManager,
        session: ReminderSession,
        eventType: String,
        triggerAt: LocalDateTime,
        notificationId: Int,
        reminderMinutes: Int,
        triggerTick: Boolean = false,
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
            putExtra(ReminderReceiver.EXTRA_TRIGGER_TICK, triggerTick)
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
            } catch (_: RuntimeException) {
                // Fall back below. ponytail: 超500上限抛IllegalStateException也走这里，不能崩
            }
        }
        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (_: RuntimeException) {
            // ponytail: 配额满时单个闹钟排不上就丢掉，总比全崩强
        }
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
