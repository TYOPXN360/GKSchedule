package com.ty.gkschedule.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

// 下载完成即调安装器；失败原因打log由调用方toast
class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        @Volatile var pendingFileName: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id == -1L) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(id))
        cursor?.use {
            if (!it.moveToFirst()) return
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                android.util.Log.e("UpdateChecker", "DownloadManager failed: $reason")
                return
            }
        }
        val name = pendingFileName ?: return
        pendingFileName = null
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)
        if (file.exists()) UpdateChecker.installApk(context, file)
        else android.util.Log.e("UpdateChecker", "Downloaded file missing: $name")
    }
}
