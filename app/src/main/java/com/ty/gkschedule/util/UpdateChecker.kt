package com.ty.gkschedule.util

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
    private const val GITHUB_RAW = "https://raw.githubusercontent.com/TYOPXN360/GKSchedule/releases/download"
    // ghfast.top 加速链接
    private const val GHFAST_BASE = "https://ghfast.top/https://github.com/TYOPXN360/GKSchedule/releases/download"

    private val json = Json { ignoreUnknownKeys = true }

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
            val downloadUrl = apkAsset?.browser_download_url?.let { url ->
                // 使用 ghfast.top 加速
                url.replace("https://github.com", GHFAST_BASE)
                    .replace("https://raw.githubusercontent.com", GHFAST_BASE)
            } ?: ""

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

    fun downloadApk(context: Context, url: String, fileName: String): File {
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val file = File(downloadDir, fileName)

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Download failed: ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Empty response body")

        return file
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
}
