package com.ty.gkschedule.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ty.gkschedule.R
import com.ty.gkschedule.ui.about.AboutActivity

// ponytail: 每日更新通知——点通知/更新键进关于页自动弹窗，取消靠滑动关闭
object UpdateNotificationHelper {
    const val CHANNEL_ID = "app_update"
    const val NOTIFICATION_ID = 9001

    const val EXTRA_SHOW_UPDATE_DIALOG = "show_update_dialog"
    const val EXTRA_UPDATE_LATEST_VERSION = "update_latest_version"
    const val EXTRA_UPDATE_DOWNLOAD_URL = "update_download_url"
    const val EXTRA_UPDATE_NOTES = "update_notes"
    const val EXTRA_UPDATE_FILE_SIZE = "update_file_size"

    fun showUpdateNotification(context: Context, info: UpdateInfo) {
        createChannel(context)
        val openIntent = Intent(context, AboutActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_UPDATE_DIALOG, true)
            putExtra(EXTRA_UPDATE_LATEST_VERSION, info.latestVersion)
            putExtra(EXTRA_UPDATE_DOWNLOAD_URL, info.downloadUrl)
            putExtra(EXTRA_UPDATE_NOTES, info.releaseNotes)
            putExtra(EXTRA_UPDATE_FILE_SIZE, info.fileSize)
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_class)
            .setContentTitle(context.getString(R.string.update_found_fmt, info.latestVersion))
            .setContentText(context.getString(R.string.update_tap_detail_fmt, info.currentVersion))
            .setStyle(NotificationCompat.BigTextStyle().bigText(info.releaseNotes.ifEmpty { context.getString(R.string.update_available_note) }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_notif_class, context.getString(R.string.update_action), openPending)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.update_channel_desc)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
