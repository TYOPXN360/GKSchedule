package com.ty.gkschedule.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
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

    private val json = Json { ignoreUnknownKeys = true }

    // API 请求客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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

    // ponytail: 通知栏自建下载进度/安装逻辑全删，下载交给系统DownloadManager，安装由UpdateInstallReceiver接管
    fun enqueueDownload(context: Context, url: String, fileName: String): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("GKSchedule v${fileName.substringAfter('v').substringBefore(".apk")}")
            .setDescription("正在下载更新")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        UpdateInstallReceiver.pendingFileName = fileName
        return dm.enqueue(request)
    }

    fun openInBrowser(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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
