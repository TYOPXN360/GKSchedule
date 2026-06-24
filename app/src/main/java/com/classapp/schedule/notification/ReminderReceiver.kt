package com.classapp.schedule.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "course_reminder"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_CLASSROOM = "classroom"
        const val EXTRA_TEACHER = "teacher"
        const val EXTRA_START_TIME = "start_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
        val classroom = intent.getStringExtra(EXTRA_CLASSROOM) ?: ""
        val teacher = intent.getStringExtra(EXTRA_TEACHER) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""

        val body = buildString {
            if (startTime.isNotEmpty()) append("$startTime · ")
            if (classroom.isNotEmpty()) append(classroom)
            if (teacher.isNotEmpty()) append(" · $teacher")
        }.trimStart('·', ' ').trimEnd('·', ' ')

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(courseName)
            .setContentText(body.ifEmpty { "即将上课" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
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
