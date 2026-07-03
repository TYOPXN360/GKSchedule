package com.classapp.schedule.notification

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
        const val EVENT_END = "end"
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createNotificationChannel(context)

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
        val eventType = intent.getStringExtra(EXTRA_EVENT_TYPE) ?: EVENT_REMINDER
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
            nm.cancel(notificationId)
            return
        }

        val body = buildString {
            if (startTime.isNotEmpty() && endTime.isNotEmpty()) append("$startTime-$endTime · ")
            else if (startTime.isNotEmpty()) append("$startTime · ")
            if (classroom.isNotEmpty()) append(classroom)
            if (teacher.isNotEmpty()) append(" · $teacher")
        }.trimStart('·', ' ').trimEnd('·', ' ')

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(eventType == EVENT_PROGRESS)

        if (eventType == EVENT_PROGRESS) {
            val percent = progressPercent(startEpoch, endEpoch)
            val titlePrefix = if (itemType == "exam") "正在考试" else "正在上课"
            builder
                .setContentTitle("$titlePrefix：$courseName")
                .setContentText("${percent}% · ${body.ifEmpty { "进行中" }}")
                .setProgress(100, percent, false)
                .setOngoing(true)
                .setAutoCancel(false)
        } else {
            val titlePrefix = if (itemType == "exam") "考前提醒" else "课前提醒"
            val fallback = if (itemType == "exam") "即将考试" else "即将上课"
            val detail = body.ifEmpty { fallback }
            val contentText = if (reminderMinutes > 0) "${reminderMinutes}分钟后 · $detail" else detail
            builder
                .setContentTitle("$titlePrefix：$courseName")
                .setContentText(contentText)
                .setAutoCancel(true)
        }

        nm.notify(notificationId, builder.build())
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
