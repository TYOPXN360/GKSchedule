package com.ty.gkschedule.captcha

import android.content.Context
import android.util.Log
import com.ty.gkschedule.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * ponytail: 验证码识别模型的按需下载管理。
 *
 * 模型不再打进 APK，首次需要时从 GitHub Release 拉单文件（约 5.2MB），也支持镜像前缀回退。
 * 下载完必须过 SHA-256 才会被 [CaptchaOcr] 使用——从网络拿到的 ONNX 直接喂给解析器是不安全的，
 * 顺带也能挡住代理劫持、HTML 错误页、半截文件。
 *
 * 状态真相：filesDir 里的模型文件 + DataStore 的「已校验」标记。
 * 标记只是快路径（避免每次启动都算 5MB 哈希），文件长度对不上时仍会重新校验。
 */
sealed interface CaptchaModelState {
    data object NotDownloaded : CaptchaModelState
    data class Downloading(val progress: Float, val receivedBytes: Long, val totalBytes: Long) : CaptchaModelState
    data object Ready : CaptchaModelState
    data class Failed(val message: String) : CaptchaModelState
}

object CaptchaModel {

    private const val TAG = "CaptchaModel"

    const val FILE_NAME = "dddocr_captcha.onnx"
    const val EXPECTED_SIZE = 5_230_913L
    private const val EXPECTED_SHA256 = "5d187a185229958f714e20852dbd406afa90e7a136887bd038505a8070fbe964"

    /**
     * 模型的固定地址：打在本仓库 tag `captcha-model-v1` 上，tag 不可变所以内容永远一致。
     * GitHub Release 的 asset 上传通道（uploads.github.com）在慢速上行下会 408，
     * 因此这里直接用 tag 固定的 raw / jsDelivr，两者都已核对 SHA-256。
     */
    private const val RAW_URL =
        "https://raw.githubusercontent.com/TYOPXN360/GKSchedule/captcha-model-v1/app/src/main/assets/ddddocr_captcha.onnx"
    private const val JSDELIVR_URL =
        "https://cdn.jsdelivr.net/gh/TYOPXN360/GKSchedule@captcha-model-v1/app/src/main/assets/ddddocr_captcha.onnx"

    /** 按顺序尝试；jsDelivr 在国内通常最快，直连其次，最后走镜像前缀。 */
    private val SOURCES = listOf(
        JSDELIVR_URL,
        RAW_URL,
        "https://ghfast.top/$RAW_URL",
        "https://gh-proxy.com/$RAW_URL",
        "https://ghproxy.net/$RAW_URL",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<CaptchaModelState>(CaptchaModelState.NotDownloaded)
    val state: StateFlow<CaptchaModelState> = _state.asStateFlow()

    private val lock = Any()
    private var downloadJob: kotlinx.coroutines.Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun file(context: Context): File = File(File(context.filesDir, "captcha"), FILE_NAME)

    private fun store(context: Context) = SettingsDataStore(context.applicationContext)

    /** 模型是否可用（已存在且校验通过）。会顺手把状态刷成 Ready/NotDownloaded。 */
    suspend fun isReady(context: Context): Boolean = withContext(Dispatchers.IO) {
        val f = file(context)
        if (!f.isFile || f.length() != EXPECTED_SIZE) {
            if (_state.value == CaptchaModelState.Ready) {
                _state.value = CaptchaModelState.NotDownloaded
            }
            return@withContext false
        }
        val marked = runCatching { store(context).captchaModelReady.first() }.getOrDefault(false)
        if (marked) {
            if (_state.value !is CaptchaModelState.Ready) _state.value = CaptchaModelState.Ready
            return@withContext true
        }
        // 标记丢了（升级/清理）但文件在，重新算一次哈希补齐标记
        if (verify(f)) {
            runCatching { store(context).setCaptchaModelReady(true) }
            _state.value = CaptchaModelState.Ready
            true
        } else {
            f.delete()
            _state.value = CaptchaModelState.NotDownloaded
            false
        }
    }

    /** 设置页进入时调用，恢复上次状态。下载中/失败不会被覆盖。 */
    suspend fun refresh(context: Context) {
        val current = _state.value
        if (current is CaptchaModelState.Downloading) return
        val ok = isReady(context)
        if (ok) {
            _state.value = CaptchaModelState.Ready
        } else if (current !is CaptchaModelState.Failed) {
            _state.value = CaptchaModelState.NotDownloaded
        }
    }

    fun download(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            if (downloadJob?.isActive == true) return
            downloadJob = scope.launch {
                _state.value = CaptchaModelState.Downloading(0f, 0L, EXPECTED_SIZE)
                var lastError = app.getString(com.ty.gkschedule.R.string.captcha_model_fail_generic)
                for (url in SOURCES) {
                    val result = runCatching { fetch(app, url) }
                    if (result.isSuccess) {
                        runCatching { store(app).setCaptchaModelReady(true) }
                        CaptchaOcr.reset()
                        _state.value = CaptchaModelState.Ready
                        Log.d(TAG, "model downloaded from $url")
                        return@launch
                    }
                    val e = result.exceptionOrNull()
                    lastError = e?.message ?: lastError
                    Log.w(TAG, "download failed via $url: $lastError")
                }
                if (!isReady(app)) {
                    _state.value = CaptchaModelState.Failed(lastError)
                }
            }
        }
    }

    suspend fun delete(context: Context) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            downloadJob?.cancel()
            downloadJob = null
        }
        runCatching { file(context).delete() }
        runCatching { store(context).setCaptchaModelReady(false) }
        CaptchaOcr.reset()
        _state.value = CaptchaModelState.NotDownloaded
    }

    /** 设置页展示用文案：已验证的大小，未下载时给预期体积。 */
    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        file(context).takeIf { it.isFile }?.length()?.takeIf { it > 0 } ?: EXPECTED_SIZE
    }

    private fun fetch(app: Context, url: String) {
        val dest = file(app)
        val dir = dest.parentFile ?: throw IOException("no parent dir")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("mkdirs failed")
        val tmp = File(dir, "$FILE_NAME.download")
        tmp.delete()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/octet-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 } ?: EXPECTED_SIZE
            var received = 0L
            var lastPct = -1
            body.byteStream().use { input ->
                FileOutputStream(tmp).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        received += read
                        if (received > EXPECTED_SIZE * 2) throw IOException("too large")
                        val pct = ((received * 100) / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            _state.value = CaptchaModelState.Downloading(
                                (received.toFloat() / total).coerceIn(0f, 1f),
                                received,
                                total
                            )
                        }
                    }
                    out.flush()
                    out.fd.sync()
                }
            }
        }

        if (tmp.length() != EXPECTED_SIZE) {
            tmp.delete()
            throw IOException("size ${tmp.length()} != $EXPECTED_SIZE")
        }
        if (!verify(tmp)) {
            tmp.delete()
            throw IOException("checksum mismatch")
        }
        dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.delete()
            throw IOException("rename failed")
        }
    }

    private fun verify(file: File): Boolean = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) } == EXPECTED_SHA256
    }.getOrDefault(false)
}
