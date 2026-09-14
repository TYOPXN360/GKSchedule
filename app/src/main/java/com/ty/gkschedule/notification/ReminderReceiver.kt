package com.ty.gkschedule.notification

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
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (eventType == EVENT_END) {
            // ponytail: 倒计时→进度接力靠startEpoch>0区分；真正下课才解散
            val isHandoff = startEpoch > 0L && System.currentTimeMillis() < endEpoch
            if (isHandoff) {
                val pending = goAsync()
                kotlin.concurrent.thread {
                    try {
                        ReminderScheduler.scheduleTodayFromStore(context)
                    } catch (_: Exception) {
                    } finally {
                        pending.finish()
                    }
                }
            } else {
                nm.cancel(notificationId)
            }
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
            .setOnlyAlertOnce(eventType == EVENT_PROGRESS)

        if (eventType == EVENT_PROGRESS || eventType == EVENT_COUNTDOWN) {
            val percent = progressPercent(startEpoch, endEpoch)
            val isCountdown = eventType == EVENT_COUNTDOWN
            // ponytail: 倒计时chip走剩余分钟，进度走百分比；同Live Update一套
            val chipText = if (isCountdown) countdownChipText(startEpoch) else "$percent%"
            val titlePrefix = when {
                isCountdown && itemType == "exam" -> "考试倒计时"
                isCountdown -> "上课倒计时"
                itemType == "exam" -> "正在考试"
                else -> "正在上课"
            }

            // ponytail: Live smallIcon用favicon白模剪影（状态栏只取alpha），largeIcon用彩色favicon
            builder.setSmallIcon(com.ty.gkschedule.R.drawable.ic_notif_live)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, com.ty.gkschedule.R.drawable.ic_notif_live_large))
                .setShortCriticalText(chipText)

            // 尝试使用 ProgressStyle (Live Update API)
            try {
                val progressStyle = NotificationCompat.ProgressStyle()
                    .setProgress(percent)
                // ponytail: 倒计时正文走剩余时间，进度正文走百分比
                val contentText = if (isCountdown) "${countdownBodyText(startEpoch)} · ${body.ifEmpty { "即将开始" }}"
                else "${percent}% · ${body.ifEmpty { "进行中" }}"
                builder
                    .setContentTitle("$titlePrefix：$courseName")
                    .setContentText(contentText)
                    .setStyle(progressStyle)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setRequestPromotedOngoing(true)
            } catch (_: Throwable) {
                // Fallback: 标准进度条（ponytail: Error如NoClassDefFoundError也得接住，否则一响就崩）
                val contentText = if (isCountdown) "${countdownBodyText(startEpoch)} · ${body.ifEmpty { "即将开始" }}"
                else "${percent}% · ${body.ifEmpty { "进行中" }}"
                builder
                    .setContentTitle("$titlePrefix：$courseName")
                    .setContentText(contentText)
                    .setProgress(100, percent, false)
                    .setOngoing(true)
                    .setAutoCancel(false)
            }
        } else {
            val titlePrefix = if (itemType == "exam") "考前提醒" else "课前提醒"
            val fallback = if (itemType == "exam") "即将考试" else "即将上课"
            val detail = body.ifEmpty { fallback }
            val contentText = if (reminderMinutes > 0) "${reminderMinutes}分钟后 · $detail" else detail
            builder
                .setSmallIcon(com.ty.gkschedule.R.drawable.ic_notif_class)
                .setContentTitle("$titlePrefix：$courseName")
                .setContentText(contentText)
                .setAutoCancel(true)
        }

        nm.notify(notificationId, builder.build())

        // ponytail: Live分钟链——PROGRESS首帧带tick标记，每分钟自排下一跳；重启才更新就是链断在这里
        if ((eventType == EVENT_PROGRESS || eventType == EVENT_COUNTDOWN) && intent.getBooleanExtra(EXTRA_TRIGGER_TICK, false)) {
            val nextMinute = ((System.currentTimeMillis() / 60000L) + 1) * 60000L
            if (nextMinute < endEpoch) {
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
                }
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val pi = android.app.PendingIntent.getBroadcast(
                    context,
                    "$itemType|$courseName|$startEpoch|$eventType|tick|$nextMinute".hashCode(),
                    next,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextMinute, pi)
                    } else {
                        am.set(android.app.AlarmManager.RTC_WAKEUP, nextMinute, pi)
                    }
                }
            }
        }
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

    // ponytail: 倒计时chip/正文——剩余分钟，<1分钟显示秒
    private fun countdownChipText(startEpoch: Long): String {
        val remainMs = (startEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        val mins = (remainMs / 60000L).toInt()
        return if (mins >= 1) "${mins}分" else "${(remainMs / 1000L).toInt()}秒"
    }

    private fun countdownBodyText(startEpoch: Long): String {
        val remainMs = (startEpoch - System.currentTimeMillis()).coerceAtLeast(0L)
        val mins = (remainMs / 60000L).toInt()
        return if (mins >= 1) "还有${mins}分钟" else "还有${(remainMs / 1000L).toInt()}秒"
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "课前提醒通知"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
