package com.ty.gkschedule.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.ty.gkschedule.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = "",
    val size: Long = 0
)

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileSize: Long,
    val isUpdateAvailable: Boolean
)

object UpdateChecker {
    private const val GITHUB_API = "https://api.github.com/repos/TYOPXN360/GKSchedule/releases/latest"
    // ghfast.top 加速前缀
    private const val GHFAST_PREFIX = "https://ghfast.top/"
    private const val CHANNEL_ID = "app_update"
    private const val NOTIFICATION_ID = 1001

    private val json = Json { ignoreUnknownKeys = true }

    // API 请求客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // 下载客户端 - 更长超时
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdate(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty response")

            if (!response.isSuccessful) {
                throw Exception("GitHub API error: ${response.code}")
            }

            val release = json.decodeFromString<GitHubRelease>(body)
            val currentVersion = getCurrentVersion(context)
            val latestVersion = release.tag_name.removePrefix("v")

            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            val rawUrl = apkAsset?.browser_download_url ?: ""
            // 使用 ghfast.top 加速：直接在原始 URL 前加前缀
            val downloadUrl = if (rawUrl.isNotEmpty()) GHFAST_PREFIX + rawUrl else ""

            android.util.Log.d("UpdateChecker", "Raw URL: $rawUrl")
            android.util.Log.d("UpdateChecker", "Download URL: $downloadUrl")

            val isUpdateAvailable = isNewerVersion(currentVersion, latestVersion)

            Result.success(
                UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    releaseName = release.name,
                    releaseNotes = release.body,
                    downloadUrl = downloadUrl,
                    fileSize = apkAsset?.size ?: 0,
                    isUpdateAvailable = isUpdateAvailable
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadApk(context: Context, url: String, fileName: String, version: String): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val file = File(downloadDir, fileName)

        android.util.Log.d("UpdateChecker", "Downloading from: $url")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("正在下载更新")
            .setContentText("GKSchedule v$version")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        val request = Request.Builder()
            .url(url)
            .build()

        val response = downloadClient.newCall(request).execute()
        android.util.Log.d("UpdateChecker", "Download response: ${response.code}")

        if (!response.isSuccessful) {
            notificationManager.cancel(NOTIFICATION_ID)
            throw Exception("Download failed: ${response.code}")
        }

        val body = response.body ?: run {
            notificationManager.cancel(NOTIFICATION_ID)
            throw Exception("Empty response body")
        }

        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        body.byteStream().use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var lastProgressUpdate = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    // Update notification every 500ms
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate > 500 && totalBytes > 0) {
                        lastProgressUpdate = now
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        builder.setProgress(100, progress, false)
                        builder.setContentText("${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}")
                        notificationManager.notify(NOTIFICATION_ID, builder.build())
                    }
                }
            }
        }

        // Download complete
        builder.setContentText("下载完成，点击安装")
            .setStyle(NotificationCompat.BigTextStyle().bigText("GKSchedule v$version 下载完成"))
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, installIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        return file
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "下载应用更新时显示进度"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
}
