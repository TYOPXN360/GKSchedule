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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(eventType == EVENT_PROGRESS)

        if (eventType == EVENT_PROGRESS) {
            val percent = progressPercent(startEpoch, endEpoch)
            val titlePrefix = if (itemType == "exam") "正在考试" else "正在上课"

            // ponytail: Live Update规范——状态栏chip取自smallIcon，闹钟换百分比位图
            builder.setSmallIcon(percentSmallIcon(context, percent))

            // 尝试使用 ProgressStyle (Live Update API)
            try {
                val progressStyle = NotificationCompat.ProgressStyle()
                    .setProgress(percent)
                builder
                    .setContentTitle("$titlePrefix：$courseName")
                    .setContentText("${percent}% · ${body.ifEmpty { "进行中" }}")
                    .setStyle(progressStyle)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setRequestPromotedOngoing(true)
            } catch (_: Throwable) {
                // Fallback: 标准进度条（ponytail: Error如NoClassDefFoundError也得接住，否则一响就崩）
                builder
                    .setContentTitle("$titlePrefix：$courseName")
                    .setContentText("${percent}% · ${body.ifEmpty { "进行中" }}")
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
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
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

    // ponytail: 百分比画进smallIcon位图——状态栏只认单色alpha，文字白画剩透明，系统自动套色
    // ponytail: 字号按位数自适应撑满48dp安全框，1位0.55/2位0.42/3位0.32，超宽再缩到贴边
    private fun percentSmallIcon(context: Context, percent: Int): androidx.core.graphics.drawable.IconCompat {
        val p = percent.coerceIn(0, 100)
        val text = "$p"
        val density = context.resources.displayMetrics.density
        val size = (24 * density).toInt().coerceAtLeast(48)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * (if (text.length >= 3) 0.32f else if (text.length >= 2) 0.42f else 0.55f)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        // ponytail: 实测宽度贴边再缩，保证1~100都不裁边
        val w = paint.measureText(text)
        if (w > size * 0.92f) paint.textSize *= (size * 0.92f / w)
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, size / 2f, y, paint)
        return androidx.core.graphics.drawable.IconCompat.createWithBitmap(bitmap)
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
