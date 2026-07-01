package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 课程颜色引擎 - 统一架构
 *
 * 设计原则:
 * 1. 颜色对 (container, content) 遵循 M3 语义配对
 * 2. 深/浅色模式下 container 亮度固定，内容色亮度固定
 * 3. 三种模式仅改变色相计算方式，饱和度统一
 * 4. 不使用两套颜色系统 (getColors vs getColorInternal)
 */
object CourseColors {

    /** 黄金角度用于 mode 2 的色相分配 */
    private const val GOLDEN_ANGLE = 137.508f

    /** 8 色色相步进 (360° / 8) */
    private const val HUE_STEP_8 = 45f

    /** 色相分组数 */
    private const val HUE_COUNT = 8

    /** 颜色对: container=背景/容器, content=文字/内容 */
    data class CourseColorPair(val container: Color, val content: Color)

    // ============================================================================
    // 深浅色统一亮度参数 (M3 语义色对)
    // ============================================================================
    // container: 深色用低亮度(暗背景)，浅色用高亮度(亮背景)
    // content: 深色用高亮度(亮文字)，浅色用低亮度(暗文字)
    // sat: 统一饱和度，深色 0.78，浅色 0.75

    private const val DARK_BG_LIGHTNESS = 0.32f
    private const val DARK_TXT_LIGHTNESS = 0.96f
    private const val LIGHT_BG_LIGHTNESS = 0.90f
    private const val LIGHT_TXT_LIGHTNESS = 0.15f
    private const val DARK_SATURATION = 0.78f
    private const val LIGHT_SATURATION = 0.75f

    // Mode 1 饱和度梯度 (教室数 1-4 的饱和度递减)
    private const val MODE1_MAX_SAT_DARK = 0.88f
    private const val MODE1_MAX_SAT_LIGHT = 0.85f
    private const val MODE1_SAT_STEP = 0.12f
    private const val MODE1_MIN_SAT = 0.45f

    // ============================================================================
    // 公开 API
    // ============================================================================

    @Composable
    fun getColor(
        mode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0
    ): CourseColorPair {
        return getColorSync(mode, courseName, classroom, classroomIndex, LocalAppIsDark.current)
    }

    fun getColorSync(
        mode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0,
        isDark: Boolean = false
    ): CourseColorPair {
        return getColorInternal(mode, courseName, classroom, classroomIndex, isDark)
    }

    // ============================================================================
    // 统一颜色计算入口
    // ============================================================================

    private fun getColorInternal(
        mode: Int,
        courseName: String,
        classroom: String,
        classroomIndex: Int,
        isDark: Boolean
    ): CourseColorPair {
        // 统一亮度参数
        val bgL = if (isDark) DARK_BG_LIGHTNESS else LIGHT_BG_LIGHTNESS
        val txtL = if (isDark) DARK_TXT_LIGHTNESS else LIGHT_TXT_LIGHTNESS

        // 计算色相 (三种模式不同)
        val hue = computeHue(mode, courseName, classroom, isDark)

        // 计算饱和度 (Mode 1 有梯度变化)
        val sat = computeSaturation(mode, isDark, classroomIndex)

        return CourseColorPair(
            container = hslToColor(hue, sat, bgL),
            content = hslToColor(hue, sat, txtL)
        )
    }

    // ============================================================================
    // 色相计算 (三种模式核心差异)
    // ============================================================================

    /**
     * Mode 0: 同名同色
     * - 仅用课程名 hash 取模 8
     * - 8 种基础色相同色相环
     */
    private fun computeHue(mode: Int, courseName: String, classroom: String, isDark: Boolean): Float {
        val nameHash = abs(courseName.hashCode())

        return when (mode) {
            0 -> {
                // 同名同色: hash % 8 * 45°
                (nameHash % HUE_COUNT) * HUE_STEP_8
            }
            1 -> {
                // 同名不同饱和度: 色相与 mode 0 相同
                (nameHash % HUE_COUNT) * HUE_STEP_8
            }
            else -> {
                // 完全不同色: 课程名+教室名 黄金角度分配
                // 乘数用绝对值确保正数，%360 取余数
                val combinedHash = abs("$courseName|$classroom".hashCode())
                (combinedHash.toFloat() * GOLDEN_ANGLE).let {
                    val normalized = (it / 360f).let { div -> div - div.roundToInt() }
                    normalized * 360f
                }
            }
        }
    }

    // ============================================================================
    // 饱和度计算
    // ============================================================================

    /**
     * Mode 0/2: 固定饱和度
     * Mode 1: 饱和度梯度 (同课程不同教室用不同饱和度)
     */
    private fun computeSaturation(mode: Int, isDark: Boolean, classroomIndex: Int): Float {
        val baseSat = if (isDark) DARK_SATURATION else LIGHT_SATURATION

        return when (mode) {
            1 -> {
                // 同教室递减饱和度: max - step * index，最低 0.45
                val maxSat = if (isDark) MODE1_MAX_SAT_DARK else MODE1_MAX_SAT_LIGHT
                (maxSat - classroomIndex * MODE1_SAT_STEP).coerceAtLeast(MODE1_MIN_SAT)
            }
            else -> baseSat
        }
    }

    // ============================================================================
    // 索引分配 (用于需要预计算颜色的场景)
    // ============================================================================

    /**
     * 为课程列表分配颜色索引
     * - Mode 0: courseName.hash % 8
     * - Mode 1: courseName.hash % 8 * 10 + classroomIdx (含饱和度偏移编码)
     * - Mode 2: (name|classroom).hash % 64
     */
    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>, groupMode: Int): Map<Long, Int> {
        return courses.associate { course ->
            val idx = when (groupMode) {
                0 -> abs(course.name.hashCode()) % HUE_COUNT
                1 -> {
                    val baseIdx = abs(course.name.hashCode()) % HUE_COUNT
                    val sameNameCourses = courses.filter { it.name == course.name }
                    val classroomIdx = sameNameCourses.indexOf(course)
                    baseIdx * 10 + classroomIdx
                }
                else -> abs("${course.name}|${course.classroom}".hashCode()) % 64
            }
            course.id to idx
        }
    }

    // ============================================================================
    // 静态调色板 (用于需要预生成颜色列表的场景)
    // ============================================================================

    /**
     * 获取静态颜色调色板
     * 统一使用 getColorInternal 的参数逻辑:
     * - 深色: sat=0.78, bgL=0.32, txtL=0.96
     * - 浅色: sat=0.75, bgL=0.90, txtL=0.15
     */
    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val primaryHue = rgbToHsl(primary.red, primary.green, primary.blue)[0]
        val step = 360f / count

        return (0 until count).map { i ->
            val hue = when (engine) {
                // Monet: 基于 primary 色相，步进分配
                1 -> (primaryHue + i * step) % 360f
                // Vibrant: 彩虹色相环
                2 -> i * step
                // Classic: 彩虹色相环 (与 Vibrant 相同)
                3 -> i * step
                // 默认 Monet: 基于 primary
                else -> (primaryHue + i * step) % 360f
            }
            // 统一使用 DARK/LIGHT 常量
            if (isDark) {
                hslToColor(hue, DARK_SATURATION, DARK_BG_LIGHTNESS) to
                hslToColor(hue, DARK_SATURATION, DARK_TXT_LIGHTNESS)
            } else {
                hslToColor(hue, LIGHT_SATURATION, LIGHT_BG_LIGHTNESS) to
                hslToColor(hue, LIGHT_SATURATION, LIGHT_TXT_LIGHTNESS)
            }
        }
    }

    // ============================================================================
    // 静态调色板辅助函数 (保留兼容性)
    // ============================================================================

    /**
     * 根据索引获取背景色 (用于静态调色板)
     */
    fun getBackgroundStatic(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].first
        return if (satOffset > 0) adjustSaturation(base, satOffset) else base
    }

    /**
     * 根据索引获取文字色 (用于静态调色板)
     */
    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].second
        return if (satOffset > 0) adjustSaturation(base, satOffset) else base
    }

    /**
     * 调整饱和度 (用于 mode 1 的饱和度梯度)
     */
    private fun adjustSaturation(color: Color, offset: Int): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newSat = (hsl[1] + offset * MODE1_SAT_STEP).coerceIn(0f, 1f)
        return hslToColor(hsl[0], newSat, hsl[2])
    }

    // ============================================================================
    // HSL 色彩转换
    // ============================================================================

    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (max == min) return floatArrayOf(0f, 0f, l)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> ((g - b) / d + (if (g < b) 6 else 0)) * 60f
            g -> ((b - r) / d + 2) * 60f
            else -> ((r - g) / d + 4) * 60f
        }
        return floatArrayOf(h, if (s.isNaN()) 0f else s, l)
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60  -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else    -> Triple(c, 0f, x)
        }
        return Color(
            (r + m).coerceIn(0f, 1f),
            (g + m).coerceIn(0f, 1f),
            (b + m).coerceIn(0f, 1f)
        )
    }
}