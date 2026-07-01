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
 * - Mode 0/1: 课程名 hash → 8 槽色相环 (同课同名)
 * - Mode 2:    课程名|教室 hash → 黄金角度连续分配 (完全不同色)
 *
 * 色相分配: hue = hash * 137.508° % 360
 */
object CourseColors {

    /** 黄金角度 */
    private const val GOLDEN = 137.508

    /** Mode 0/1 色相分组数 */
    private const val HUE_SLOTS = 8

    /** Mode 0/1 色相步进 */
    private const val HUE_STEP = 45.0

    /** HCT 色度 (Chroma) - 70.0 = 浓郁不暗淡的彩色 */
    private const val CHROMA = 70.0

    /** Mode 1 最低色度 */
    private const val CHROMA_MIN = 35.0

    /** Mode 1 色度递减步进 */
    private const val CHROMA_STEP = 8.0

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
        // 计算色相 (三模式核心差异)
        val hue = computeHue(mode, courseName, classroom)

        // 计算色度 (Mode 1 有梯度)
        val chroma = computeChroma(mode, classroomIndex)

        // M3 标准 Tone 档位
        val containerTone = if (isDark) 30.0 else 90.0
        val contentTone = if (isDark) 95.0 else 10.0

        // HCT → Compose Color
        val container = hctToColor(hue, chroma, containerTone)
        val content = hctToColor(hue, chroma, contentTone)

        return CourseColorPair(container, content)
    }

    // =========================================================================
    // 色相计算
    // =========================================================================

    /**
     * Mode 0/1: 课程名 hash 映射到 8 槽色相环
     * Mode 2:    课程名|教室 hash 黄金角度连续分配
     */
    private fun computeHue(mode: Int, courseName: String, classroom: String): Double {
        return when (mode) {
            0, 1 -> {
                val slot = abs(courseName.hashCode()) % HUE_SLOTS
                slot * HUE_STEP
            }
            else -> {
                val hash = abs("$courseName|$classroom".hashCode()).toDouble()
                (hash * GOLDEN).wrapAngle()
            }
        }
    }

    // =========================================================================
    // 色度计算 (Mode 1 梯度)
    // =========================================================================

    private fun computeChroma(mode: Int, classroomIndex: Int): Double {
        return when (mode) {
            1 -> (CHROMA - classroomIndex * CHROMA_STEP).coerceAtLeast(CHROMA_MIN)
            else -> CHROMA
        }
    }

    // =========================================================================
    // HCT → Compose Color
    // =========================================================================

    private fun hctToColor(hue: Double, chroma: Double, tone: Double): Color {
        val hct = Hct.from(hue, chroma, tone)
        return Color(hct.toInt())
    }

    // =========================================================================
    // TonalPalette (生成调色板)
    // =========================================================================

    private fun createPalette(hue: Double, chroma: Double): TonalPalette {
        return TonalPalette.fromHueAndChroma(hue, chroma)
    }

    // =========================================================================
    // 色相归一化
    // =========================================================================

    private fun Double.wrapAngle(): Double {
        val wrapped = this % 360.0
        return if (wrapped < 0) wrapped + 360.0 else wrapped
    }

    // =========================================================================
    // 静态调色板 (兼容旧代码)
    // =========================================================================

    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val primaryHue = Hct.fromInt(primary.value.toInt()).hue
        val step = 360.0 / count

        return (0 until count).map { i ->
            val hue = when (engine) {
                1 -> (primaryHue + i * step).wrapAngle()  // Monet
                else -> i * step  // Vibrant/Classic
            }
            val containerTone = if (isDark) 30.0 else 90.0
            val contentTone = if (isDark) 95.0 else 10.0
            hctToColor(hue, CHROMA, containerTone) to hctToColor(hue, CHROMA, contentTone)
        }
    }

    // =========================================================================
    // 兼容旧代码
    // =========================================================================

    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>, groupMode: Int): Map<Long, Int> {
        return courses.associate { course ->
            val idx = when (groupMode) {
                0 -> abs(course.name.hashCode()) % HUE_SLOTS
                1 -> {
                    val base = abs(course.name.hashCode()) % HUE_SLOTS
                    val sameNameCourses = courses.filter { it.name == course.name }
                    val classIdx = sameNameCourses.indexOf(course)
                    base * 10 + classIdx
                }
                else -> abs("${course.name}|${course.classroom}".hashCode()) % 64
            }
            course.id to idx
        }
    }

    fun getBackgroundStatic(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        return colors[index % colors.size].first
    }

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        return colors[index % colors.size].second
    }
}
