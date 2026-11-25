package com.ty.gkschedule.notification

import com.ty.gkschedule.R
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ty.gkschedule.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "course_reminder"
        const val EVENT_REMINDER = "reminder"
        const val EVENT_PROGRESS = "progress"
        const val EVENT_COUNTDOWN = "countdown"
        const val EVENT_END = "end"
        const val EVENT_ROLLOVER = "rollover"
        const val EXTRA_EVENT_TYPE = "event_type"
        const val EXTRA_ITEM_TYPE = "item_type"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_CLASSROOM = "classroom"
        const val EXTRA_TEACHER = "teacher"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_START_EPOCH_MILLIS = "start_epoch_millis"
        const val EXTRA_END_EPOCH_MILLIS = "end_epoch_millis"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_REMINDER_MINUTES = "reminder_minutes"
        const val EXTRA_TRIGGER_TICK = "trigger_tick"
        const val EXTRA_BREAK_EPOCHS = "break_epochs"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: EVENT_REMINDER
        // 午夜重排：不检查通知权限、不读notification extras，直接重排当天后返回
        if (eventType == EVENT_ROLLOVER) {
            val pending = goAsync()
            kotlin.concurrent.thread {
                try {
                    ReminderScheduler.scheduleTodayFromStore(context)
                } catch (_: Exception) {
                } finally {
                    pending.finish()
                }
            }
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createNotificationChannel(context)

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE) ?: "course"
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
        val teacher = intent.getStringExtra(EXTRA_TEACHER) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
        val startEpoch = intent.getLongExtra(EXTRA_START_EPOCH_MILLIS, 0L)
        val endEpoch = intent.getLongExtra(EXTRA_END_EPOCH_MILLIS, 0L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, stableNotificationId(itemType, courseName, startEpoch))
        val reminderMinutes = intent.getIntExtra(EXTRA_REMINDER_MINUTES, 0)
        val breakEpochs = intent.getLongArrayExtra(EXTRA_BREAK_EPOCHS) ?: longArrayOf()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (eventType == EVENT_END) {
            val isHandoff = startEpoch > 0L && System.currentTimeMillis() < endEpoch
            if (isHandoff) handoffOrCancel(context, itemType, notificationId)
            else nm.cancel(notificationId)
            return
        }

        // 兼容旧版本留下的倒计时闹钟：过了上课时间后只允许进度链接管。
        if (eventType == EVENT_COUNTDOWN && System.currentTimeMillis() >= startEpoch) {
            handoffOrCancel(context, itemType, notificationId)
            return
        }

        val body = buildString {
            if (startTime.isNotEmpty() && endTime.isNotEmpty()) append("$startTime-$endTime · ")
            else if (startTime.isNotEmpty()) append("$startTime · ")
            if (classroom.isNotEmpty()) append(classroom)
            if (teacher.isNotEmpty()) append(" · $teacher")
        }.trimStart('·', ' ').trimEnd('·', ' ')

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(eventType == EVENT_PROGRESS || eventType == EVENT_COUNTDOWN)

        if (eventType == EVENT_PROGRESS || eventType == EVENT_COUNTDOWN) {
            val isCountdown = eventType == EVENT_COUNTDOWN
            val percent = if (isCountdown) {
                countdownProgressPercent(startEpoch, reminderMinutes)
            } else {
                progressPercent(startEpoch, endEpoch)
            }
            val chipText = if (isCountdown) countdownChipText(context, startEpoch) else "$percent%"
            val titlePrefix = when {
                isCountdown && itemType == "exam" -> context.getString(R.string.notif_exam_countdown)
                isCountdown -> context.getString(R.string.notif_class_countdown)
                itemType == "exam" -> context.getString(R.string.notif_in_exam)
                else -> context.getString(R.string.notif_in_class)
            }

            // ponytail: Live smallIcon用favicon白模剪影（状态栏只取alpha），largeIcon用彩色favicon
            builder.setSmallIcon(com.ty.gkschedule.R.drawable.ic_notif_live)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, com.ty.gkschedule.R.drawable.ic_notif_live_large))
                .setShortCriticalText(chipText)

            val contentText = if (isCountdown) "${countdownBodyText(context, startEpoch, itemType == "exam")} · ${body.ifEmpty { context.getString(R.string.notif_upcoming) }}"
            else "$percent% · ${inClassRemainText(context, endEpoch)} · ${body.ifEmpty { context.getString(R.string.notif_in_progress) }}"
            builder
                .setContentTitle("$titlePrefix：$courseName")
                .setContentText(contentText)
                .setOngoing(true)
                .setAutoCancel(false)
                .setRequestPromotedOngoing(true)
            if (Build.VERSION.SDK_INT >= 36) {
                // ponytail: 官方 ProgressStyle——Segment 长度与 setProgress 共用同一把标尺（这里用秒）。
                // 每个小节的时长是一段，相邻段交替配色，段边界正好落在“第一大节里第一小节下课”的时间点。
                val style = NotificationCompat.ProgressStyle()
                if (isCountdown) {
                    style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(100))
                    style.setProgress(percent)
                } else if (startEpoch > 0L && endEpoch > startEpoch) {
                    val bounds = buildList {
                        add(startEpoch)
                        breakEpochs.filter { it > startEpoch && it < endEpoch }.sorted().forEach { add(it) }
                        add(endEpoch)
                    }
                    val accent = runCatching { context.getColor(android.R.color.system_accent1_500) }
                        .getOrDefault(0xFF4C8DFF.toInt())
                    val accentSoft = runCatching { context.getColor(android.R.color.system_accent1_200) }
                        .getOrDefault(0xFF9CBBFF.toInt())
                    var totalSec = 0
                    for (i in 0 until bounds.size - 1) {
                        val len = ((bounds[i + 1] - bounds[i]) / 1000L).toInt().coerceAtLeast(1)
                        totalSec += len
                        style.addProgressSegment(
                            NotificationCompat.ProgressStyle.Segment(len)
                                .setColor(if (i % 2 == 0) accent else accentSoft)
                        )
                    }
                    val elapsedSec = ((System.currentTimeMillis() - startEpoch) / 1000L)
                        .coerceIn(0L, totalSec.toLong()).toInt()
                    style.setProgress(elapsedSec)
                } else {
                    style.addProgressSegment(NotificationCompat.ProgressStyle.Segment(100))
                    style.setProgress(percent)
                }
                builder.setStyle(style)
            } else {
                builder.setProgress(100, percent, false)
            }
            // ponytail: 通知已不在活跃列表（被划掉/系统清掉后重建）时静默补发——
            // setOnlyAlertOnce 只对"存在中的通知"生效，重建照样会响
            if (runCatching { nm.activeNotifications.none { it.id == notificationId } }.getOrDefault(false)) {
                builder.setSilent(true)
            }
        } else {
            val titlePrefix = if (itemType == "exam") context.getString(R.string.notif_exam_reminder) else context.getString(R.string.notif_class_reminder)
            val fallback = if (itemType == "exam") context.getString(R.string.notif_about_to_exam) else context.getString(R.string.notif_about_to_class)
            val detail = body.ifEmpty { fallback }
            val contentText = if (reminderMinutes > 0) context.getString(R.string.notif_minutes_ago, reminderMinutes, detail) else detail
            builder
                .setSmallIcon(com.ty.gkschedule.R.drawable.ic_notif_class)
                .setContentTitle("$titlePrefix：$courseName")
                .setContentText(contentText)
                .setAutoCancel(true)
        }

        nm.notify(notificationId, builder.build())

        if ((eventType == EVENT_PROGRESS || eventType == EVENT_COUNTDOWN) && intent.getBooleanExtra(EXTRA_TRIGGER_TICK, false)) {
            val nextMinute = ((System.currentTimeMillis() / 60000L) + 1) * 60000L
            val tickEnd = if (eventType == EVENT_COUNTDOWN) startEpoch else endEpoch
            if (nextMinute < tickEnd) {
                val next = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(EXTRA_EVENT_TYPE, eventType)
                    putExtra(EXTRA_ITEM_TYPE, itemType)
                    putExtra(EXTRA_COURSE_NAME, courseName)
                    putExtra(EXTRA_CLASSROOM, classroom)
                    putExtra(EXTRA_TEACHER, teacher)
                    putExtra(EXTRA_START_TIME, startTime)
                    putExtra(EXTRA_END_TIME, endTime)
                    putExtra(EXTRA_START_EPOCH_MILLIS, startEpoch)
                    putExtra(EXTRA_END_EPOCH_MILLIS, endEpoch)
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(EXTRA_REMINDER_MINUTES, reminderMinutes)
                    putExtra(EXTRA_TRIGGER_TICK, true)
                    if (breakEpochs.isNotEmpty()) putExtra(EXTRA_BREAK_EPOCHS, breakEpochs)
                }
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val pi = android.app.PendingIntent.getBroadcast(
                    context,
                    "$itemType|$courseName|$startEpoch|$eventType|tick".hashCode(),
                    next,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    // ponytail: setAlarmClock 是 doze 下唯一不被限流的精确闹钟（setExactAndAllowWhileIdle
                    // 待机时会被压到几分钟一次，倒计时/进度看上去就是"冻住了"）
                    if (am.canScheduleExactAlarms()) {
                        am.setAlarmClock(android.app.AlarmManager.AlarmClockInfo(nextMinute, pi), pi)
                    } else {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextMinute, pi)
                    }
                }
            }
        }
    }

    private fun handoffOrCancel(context: Context, itemType: String, notificationId: Int) {
        val pending = goAsync()
        kotlin.concurrent.thread {
            try {
                val settings = SettingsDataStore(context)
                val liveOn = runBlocking {
                    if (itemType == "exam") settings.reminderExamLiveUpdate.first()
                    else settings.reminderLiveUpdate.first()
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (liveOn) ReminderScheduler.scheduleTodayFromStore(context)
                else nm.cancel(notificationId)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private fun countdownProgressPercent(startEpoch: Long, reminderMinutes: Int): Int {
        val totalMs = reminderMinutes.coerceAtLeast(1) * 60_000L
        val remainingMs = (startEpoch - System.currentTimeMillis()).coerceIn(0L, totalMs)
        return ((remainingMs * 100L) / totalMs).toInt().coerceIn(0, 100)
    }

    private fun progressPercent(startEpoch: Long, endEpoch: Long): Int {
        if (startEpoch <= 0L || endEpoch <= startEpoch) return 0
        val now = System.currentTimeMillis().coerceIn(startEpoch, endEpoch)
        return (((now - startEpoch).toDouble() / (endEpoch - startEpoch).toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun stableNotificationId(itemType: String, name: String, startEpoch: Long): Int =
        "$itemType|$name|$startEpoch".hashCode()

    // ponytail: 倒计时chip——只到分钟，不足1分钟直接“即将开始”（不再逐秒跳，通知本来也只按分钟刷新）
    private fun countdownChipText(context: Context, startEpoch: Long): String {
        val remainMs = (startEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        val mins = (remainMs / 60000L).toInt()
        return if (mins >= 1) context.getString(R.string.notif_remain_min, mins)
        else context.getString(R.string.notif_upcoming)
    }

    // ponytail: 上课中的正文加"还剩N分钟"——ProgressStyle 与 setProgress fallback 共用同一行文案
    private fun inClassRemainText(context: Context, endEpoch: Long): String {
        if (endEpoch <= 0L) return context.getString(R.string.notif_in_progress)
        val remainMs = (endEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        val mins = ((remainMs + 30000L) / 60000L).toInt()  // 30s 向上取整，避免刚开始就显示 0 分钟
        return if (mins >= 1) context.getString(R.string.notif_remaining_min, mins)
        else context.getString(R.string.notif_in_progress)
    }

    // ponytail: 倒计时正文同样只到分钟，不足1分钟显示“即将开始”，不再出现秒级倒数
    private fun countdownBodyText(context: Context, startEpoch: Long, isExam: Boolean): String {
        val remainMs = (startEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        val mins = (remainMs / 60000L).toInt()
        if (mins < 1) return context.getString(R.string.notif_upcoming)
        return if (isExam) context.getString(R.string.notif_remaining_min, mins)
        else context.getString(R.string.notif_class_remaining_min, mins)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_course_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_course_channel_desc)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
