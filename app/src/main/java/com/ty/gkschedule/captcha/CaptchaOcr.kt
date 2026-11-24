package com.ty.gkschedule.captcha

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * ponytail: 登录验证码离线识别。
 *
 * 模型来自 ddddocr（MIT）的 common_old.onnx，已把末层 1024x8210 的 int8 分类头裁成
 * blank + [A-Za-z0-9] 共 63 类，体积从 13.6MB 降到 5.2MB；裁剪前后对 30 张真实验证码
 * 输出完全一致。
 *
 * 预处理必须与 ddddocr 一致，否则准确率会掉：
 * 1. 等比缩放到高 64（宽度按比例截断取整）—— 用 Lanczos3，不能用 Bitmap 的双线性
 *    （实测双线性在 30 张里有 2 张会识别错）
 * 2. 缩放在 RGB 上做，之后才转灰度（ITU-R 601-2：L = (R*299 + G*587 + B*114) / 1000）
 * 3. 像素 /255 归一化，输入形状 [1, 1, 64, W]
 * 4. 输出 (T, 1, 63) 逐帧 argmax，再做 CTC 去重并跳过 index 0（blank）
 */
object CaptchaOcr {

    private const val MODEL_ASSET = "dddocr_captcha.onnx"
    private const val INPUT_NAME = "input1"
    private const val INPUT_HEIGHT = 64

    /** 模型第 0 类为 blank，其余按此顺序对应输出 index 1..62。 */
    private const val CHARS = "2F7DMCrYbcJIfvilBEu9ksPZn1GmKzAWpTXOHdV46jNeSQyLx0o58waRUq3tgh"

    private val lock = Any()
    @Volatile private var session: OrtSession? = null
    @Volatile private var inputName: String = INPUT_NAME

    /** 只对 arm64 打包了原生库，其它 ABI 直接跳过识别。 */
    val isSupported: Boolean
        get() = android.os.Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a"

    /**
     * 识别 jpeg/png 字节。失败返回 null，调用方保留手动输入。
     */
    fun recognize(context: Context, imageBytes: ByteArray): String? = runCatching {
        if (!isSupported) return null
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null
        val input = preprocess(bitmap)
        val width = input.width
        val data = input.data
        if (width <= 0 || data.isEmpty()) return null

        val sess = session(context)
        val env = OrtEnvironment.getEnvironment()
        OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(data),
            longArrayOf(1L, 1L, INPUT_HEIGHT.toLong(), width.toLong())
        ).use { tensor ->
            sess.run(mapOf(inputName to tensor)).use { result ->
                val buffer = (result[0] as OnnxTensor).floatBuffer
                val classes = CHARS.length + 1
                var last = -1
                val sb = StringBuilder()
                var offset = 0
                // 输出是 (T, 1, classes) 的连续 float，每个时间步取 argmax
                while (offset + classes <= buffer.limit()) {
                    var best = 0
                    var bestScore = buffer.get(offset)
                    for (c in 1 until classes) {
                        val v = buffer.get(offset + c)
                        if (v > bestScore) {
                            bestScore = v
                            best = c
                        }
                    }
                    if (best != last && best != 0) sb.append(CHARS[best - 1])
                    last = best
                    offset += classes
                }
                sb.toString().ifEmpty { null }
            }
        }
    }.getOrNull()

    private fun session(context: Context): OrtSession = session ?: synchronized(lock) {
        session ?: run {
            val env = OrtEnvironment.getEnvironment()
            val created = OrtSession.SessionOptions().use { options ->
                options.setIntraOpNumThreads(2)
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                // 直接从 assets 读进内存建会话，避免在磁盘上留一份 5MB 模型
                val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
                env.createSession(bytes, options)
            }
            inputName = created.inputNames.firstOrNull() ?: INPUT_NAME
            session = created
            created
        }
    }

    // ---- 预处理 ----

    private class Planes(val width: Int, val height: Int, val data: FloatArray)

    private fun preprocess(bitmap: Bitmap): Planes {
        val srcW = bitmap.width
        val srcH = bitmap.height
        require(srcW > 0 && srcH > 0)
        val dstH = INPUT_HEIGHT
        val dstW = max(1, (srcW * (dstH.toDouble() / srcH)).toInt())

        val pixels = IntArray(srcW * srcH)
        bitmap.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)
        // 先按 RGB 通道做 Lanczos3 缩放，再转灰度，顺序与 ddddocr 一致
        val r = FloatArray(srcW * srcH)
        val g = FloatArray(srcW * srcH)
        val b = FloatArray(srcW * srcH)
        for (i in pixels.indices) {
            val p = pixels[i]
            r[i] = ((p shr 16) and 0xFF).toFloat()
            g[i] = ((p shr 8) and 0xFF).toFloat()
            b[i] = (p and 0xFF).toFloat()
        }
        val rr = resample(r, srcW, srcH, dstW, dstH)
        val gg = resample(g, srcW, srcH, dstW, dstH)
        val bb = resample(b, srcW, srcH, dstW, dstH)
        val out = FloatArray(dstW * dstH)
        for (i in out.indices) {
            val luma = (rr[i].toInt() * 299 + gg[i].toInt() * 587 + bb[i].toInt() * 114) / 1000
            out[i] = luma / 255.0f
        }
        return Planes(dstW, dstH, out)
    }

    /** 与 PIL Image.resize(..., LANCZOS) 对齐的可分离 Lanczos3 重采样。 */
    private fun resample(src: FloatArray, sw: Int, sh: Int, dw: Int, dh: Int): FloatArray {
        if (sw == dw && sh == dh) return src.copyOf()
        val horizontal = FloatArray(dw * sh)
        val scaleX = sw.toDouble() / dw
        val fscaleX = if (scaleX < 1.0) 1.0 / scaleX else 1.0
        val supportX = 3.0 * fscaleX
        for (y in 0 until sh) {
            val rowBase = y * sw
            for (xx in 0 until dw) {
                val center = (xx + 0.5) * scaleX
                val xmin = max(0, floor(center - supportX + 0.5).toInt())
                val xmax = min(sw, ceil(center + supportX + 0.5).toInt())
                var sum = 0.0
                var wsum = 0.0
                for (x in xmin until xmax) {
                    val w = lanczos((x - center + 0.5) / fscaleX)
                    sum += src[rowBase + x] * w
                    wsum += w
                }
                horizontal[y * dw + xx] = clip8(if (wsum != 0.0) sum / wsum else 0.0)
            }
        }
        val out = FloatArray(dw * dh)
        val scaleY = sh.toDouble() / dh
        val fscaleY = if (scaleY < 1.0) 1.0 / scaleY else 1.0
        val supportY = 3.0 * fscaleY
        for (yy in 0 until dh) {
            val center = (yy + 0.5) * scaleY
            val ymin = max(0, floor(center - supportY + 0.5).toInt())
            val ymax = min(sh, ceil(center + supportY + 0.5).toInt())
            for (xx in 0 until dw) {
                var sum = 0.0
                var wsum = 0.0
                for (y in ymin until ymax) {
                    val w = lanczos((y - center + 0.5) / fscaleY)
                    sum += horizontal[y * dw + xx] * w
                    wsum += w
                }
                out[yy * dw + xx] = clip8(if (wsum != 0.0) sum / wsum else 0.0)
            }
        }
        return out
    }

    private fun lanczos(x: Double): Double {
        val ax = abs(x)
        if (ax < 1e-8) return 1.0
        if (ax >= 3.0) return 0.0
        val pix = Math.PI * x
        return (sin(pix) / pix) * (sin(pix / 3.0) / (pix / 3.0))
    }

    private fun clip8(v: Double): Float {
        val rounded = Math.round(v).toFloat()
        return rounded.coerceIn(0f, 255f)
    }
}
