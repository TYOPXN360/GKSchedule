package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import kotlin.math.abs

/**
 * 课程颜色引擎 - Google HCT 官方算法
 *
 * 三模式统一用 HCT 色相分配:
 * - Mode 0/1: 课程名 hash → 黄金角度散射 (同课同名，异名绝对错开)
 * - Mode 2:    课程名|教室 hash → 黄金角度散射 (完全差异化)
 *
 * 色相分配: hue = hash * 137.508° % 360
 * 容器 Chroma 限制在 42 防止暗色模式 sRGB 色域溢出
 */
object CourseColors {

    /** 黄金角度 - 全圆周绝对均匀发散 */
    private const val GOLDEN = 137.508

    /** 容器色度 - 42.0 防止暗色低明度下 sRGB 色域溢出产生硬色 */
    private const val CHROMA_CONTAINER = 42.0

    /** 文字色度 - 65.0 保持高对比度可读性 */
    private const val CHROMA_CONTENT = 65.0

    /** Mode 1 最低色度 */
    private const val CHROMA_MIN = 22.0

    /** Mode 1 色度递减步进 */
    private const val CHROMA_STEP = 6.0

    /** 颜色对: container=背景色  content=文字色 */
    data class CourseColorPair(val container: Color, val content: Color)

    // =========================================================================
    // 公开 API
    // =========================================================================

    @Composable
    fun getColor(mode: Int, courseName: String, classroom: String = "", classroomIndex: Int = 0): CourseColorPair {
        return getColorSync(mode, courseName, classroom, classroomIndex, LocalAppIsDark.current)
    }

    fun getColorSync(mode: Int, courseName: String, classroom: String = "", classroomIndex: Int = 0, isDark: Boolean = false): CourseColorPair {
        return makeColor(mode, courseName, classroom, classroomIndex, isDark)
    }

    // =========================================================================
    // HCT 官方取色
    // =========================================================================

    private fun makeColor(mode: Int, courseName: String, classroom: String, classroomIndex: Int, isDark: Boolean): CourseColorPair {
        val hue = computeHue(mode, courseName, classroom)

        // 容器用低色度防止暗色模式色域溢出，文字用高色度保持对比
        val containerChroma = computeChroma(mode, classroomIndex, CHROMA_CONTAINER)
        val contentChroma = CHROMA_CONTENT

        val containerTone = if (isDark) 34.0 else 92.0
        val contentTone = if (isDark) 96.0 else 15.0

        return CourseColorPair(
            container = hctToColor(hue, containerChroma, containerTone),
            content = hctToColor(hue, contentChroma, contentTone)
        )
    }

    // =========================================================================
    // 色相计算 - 全模式统一黄金角度散射
    // =========================================================================

    private fun computeHue(mode: Int, courseName: String, classroom: String): Double {
        return when (mode) {
            0, 1 -> {
                // 取消 % 8 限制，黄金角度直接散射课程名 Hash
                val hash = abs(courseName.hashCode()).toDouble()
                (hash * GOLDEN) % 360.0
            }
            else -> {
                val hash = abs("$courseName|$classroom".hashCode()).toDouble()
                (hash * GOLDEN) % 360.0
            }
        }
    }

    // =========================================================================
    // 色度计算 (Mode 1 梯度)
    // =========================================================================

    private fun computeChroma(mode: Int, classroomIndex: Int, baseChroma: Double): Double {
        return when (mode) {
            1 -> (baseChroma - classroomIndex * CHROMA_STEP).coerceAtLeast(CHROMA_MIN)
            else -> baseChroma
        }
    }

    // =========================================================================
    // HCT → Compose Color
    // =========================================================================

    private fun hctToColor(hue: Double, chroma: Double, tone: Double): Color {
        val hct = Hct.from(hue, chroma, tone)
        return Color(hct.toInt())
    }

    /**
     * 静态调色板 - 供需要 List<Pair<Color,Color>> 的旧接口使用
     */
    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val primaryHue = Hct.fromInt(primary.value.toInt()).hue
        val step = 360.0 / count

        return (0 until count).map { i ->
            val hue = when (engine) {
                1 -> (primaryHue + i * step) % 360.0
                else -> i * step
            }
            val containerTone = if (isDark) 34.0 else 92.0
            val contentTone = if (isDark) 96.0 else 15.0
            hctToColor(hue, CHROMA_CONTAINER, containerTone) to hctToColor(hue, CHROMA_CONTENT, contentTone)
        }
    }
}
