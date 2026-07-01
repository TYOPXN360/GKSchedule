package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import kotlin.math.abs

/**
 * 课程颜色引擎 - 彻底统一架构
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  M3 Tonal Pair: container / content                        │
 * │  Light:  container=tone90(L=0.90)  content=tone10(L=0.15)  │
 * │  Dark:   container=tone30(L=0.32)  content=tone90(L=0.96)  │
 * │  Sat:    Dark=0.78  Light=0.75  (统一)                      │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  三模式统一公式: hue = hashFn() % steps * step              │
 * │  ┌─────────┬──────────────┬───────────────┬────────────┐ │
 * │  │ Mode 0   │ 同色分组       │ name%8        │ sat=固定    │ │
 * │  │ Mode 1   │ 同色+饱和梯度  │ name%8        │ sat=梯度    │ │
 * │  │ Mode 2   │ 完全不同色     │ (n|c)*黄金角  │ sat=固定    │ │
 * │  └─────────┴──────────────┴───────────────┴────────────┘ │
 * └─────────────────────────────────────────────────────────────┘
 */
object CourseColors {

    // =========================================================================
    // 常量
    // =========================================================================

    /** 黄金角度 (Mode 2 专用) */
    private const val GOLDEN = 137.508f

    /** Mode 0/1 色相分组数 */
    private const val HUE_SLOTS = 8

    /** Mode 0/1 色相步进 */
    private const val HUE_STEP = 45f

    /** Mode 2 额外分组 (用于索引映射) */
    private const val MODE2_SLOTS = 64

    /** 颜色对: container=容器色  content=内容色 */
    data class CourseColorPair(val container: Color, val content: Color)

    // =========================================================================
    // M3 Tonal Pair 参数 (Light / Dark 统一)
    // =========================================================================
    // M3 色对语义:
    // - Light:  亮容器(tone90≈L0.90) + 暗文字(tone10≈L0.15) → 对比度最大化
    // - Dark:   暗容器(tone30≈L0.32) + 亮文字(tone90≈L0.96) → 对比度最大化
    // - Sat:    饱和度固定 0.78(Dark) / 0.75(Light)

    private const val L_LIGHT = 0.90f   // container tone 90
    private const val L_DARK  = 0.32f   // container tone 30
    private const val T_LIGHT = 0.15f  // content  tone 10
    private const val T_DARK  = 0.96f   // content  tone 90

    private const val S_LIGHT = 0.75f   // 饱和度 统一
    private const val S_DARK  = 0.78f   // 饱和度 统一

    // Mode 1 饱和度梯度 (同课程名不同教室)
    // maxSat - index * step，最低 clamp
    private const val M1_MAX_S = 0.88f
    private const val M1_MAX_L = 0.85f
    private const val M1_STEP  = 0.12f
    private const val M1_MIN   = 0.45f

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
    // 统一取色入口
    // =========================================================================

    private fun makeColor(mode: Int, courseName: String, classroom: String, classroomIndex: Int, isDark: Boolean): CourseColorPair {
        // 亮度 + 饱和度 (三模式共用，仅 mode1 有梯度)
        val (sat, _) = getTonalPair(isDark, mode, classroomIndex)
        val containerL = if (isDark) L_DARK else L_LIGHT
        val contentL   = if (isDark) T_DARK else T_LIGHT

        // 色相 (唯一差异点)
        val hue = computeHue(mode, courseName, classroom)

        return CourseColorPair(
            container = hsl(hue, sat, containerL),
            content   = hsl(hue, sat, contentL)
        )
    }

    // =========================================================================
    // 三模式统一色相公式
    // =========================================================================

    /**
     * 统一色相计算:
     * - Mode 0/1: courseName * 黄金角 % 360° (连续分布，64 槽)
     * - Mode 2:    courseName|classroom * 黄金角 % 360°
     *
     * 三模式共用同一结构: hue = hash(name) * GOLDEN % 360°
     * Mode 1 的差异在饱和度，不在色相
     */
    private fun computeHue(mode: Int, courseName: String, classroom: String): Float {
        return when (mode) {
            // Mode 0 & 1: 课程名 hash 黄金角度连续分配 (64 槽)
            0, 1 -> {
                val hash = abs(courseName.hashCode()).toFloat()
                (hash * GOLDEN).wrapAngle()
            }
            // Mode 2: 课程名+教室名 黄金角度
            else -> {
                val hash = abs("$courseName|$classroom".hashCode()).toFloat()
                (hash * GOLDEN).wrapAngle()
            }
        }
    }

    // =========================================================================
    // 饱和度 (Mode 1 有梯度)
    // =========================================================================

    /**
     * 统一饱和度:
     * - Mode 0/2: 固定 (Dark=0.78 / Light=0.75)
     * - Mode 1:   梯度递减 (同课程名不同教室递减 0.12，最低 0.45)
     */
    private fun getTonalPair(isDark: Boolean, mode: Int, classroomIndex: Int): Pair<Float, Float> {
        return when (mode) {
            1 -> {
                // Mode 1: 梯度饱和度
                val maxSat = if (isDark) M1_MAX_S else M1_MAX_L
                val sat = (maxSat - classroomIndex * M1_STEP).coerceAtLeast(M1_MIN)
                sat to sat  // container/content 同 sat (仅亮度不同)
            }
            else -> {
                // Mode 0/2: 固定饱和度
                val sat = if (isDark) S_DARK else S_LIGHT
                sat to sat
            }
        }
    }

    // =========================================================================
    // 色相归一化 (0-360)
    // =========================================================================

    private fun Float.wrapAngle(): Float {
        val wrapped = this % 360f
        return if (wrapped < 0) wrapped + 360f else wrapped
    }

    // =========================================================================
    // HSL → Color
    // =========================================================================

    private fun hsl(h: Float, s: Float, l: Float): Color {
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

    // =========================================================================
    // 索引分配 (兼容性保留)
    // =========================================================================

    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>, groupMode: Int): Map<Long, Int> {
        return courses.associate { course ->
            val idx = when (groupMode) {
                // Mode 0/1: courseName hash * GOLDEN 连续分配 (64 槽)
                0, 1 -> {
                    val hash = abs(course.name.hashCode())
                    // 与 computeHue 保持一致: hash * GOLDEN % 360，映射到 64 槽
                    (((hash.toFloat() * GOLDEN) % 360f) / 360f * 64).toInt().coerceIn(0, 63)
                }
                // Mode 2: courseName|classroom hash * GOLDEN
                else -> {
                    val hash = abs("${course.name}|${course.classroom}".hashCode())
                    (((hash.toFloat() * GOLDEN) % 360f) / 360f * 64).toInt().coerceIn(0, 63)
                }
            }
            course.id to idx
        }
    }

    // =========================================================================
    // 静态调色板 (生成均匀色相环)
    // =========================================================================

    /**
     * 静态调色板生成
     * 与 makeColor 共用同一亮度/饱和度常量
     * 仅用于: 预生成调色板列表的场景 (如 TodayScreen fallback)
     */
    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val primaryHue = rgbHsl(primary)[0]
        val step = 360f / count

        return (0 until count).map { i ->
            // 色相分配策略 (engine 参数)
            val hue = when (engine) {
                // Engine 1: Monet — 基于 primary 色相，均匀步进
                1 -> (primaryHue + i * step).wrapAngle()
                // Engine 2: Vibrant — 彩虹色环 (0°, 22.5°, 45°...)
                2 -> i * step
                // Engine 3: Classic — 同 Vibrant
                3 -> i * step
                // Engine 0: 默认 Monet
                else -> (primaryHue + i * step).wrapAngle()
            }

            // 统一亮度对
            val containerL = if (isDark) L_DARK else L_LIGHT
            val contentL   = if (isDark) T_DARK else T_LIGHT
            // 统一饱和度 (Mode 0/2 固定值)
            val sat = if (isDark) S_DARK else S_LIGHT

            hsl(hue, sat, containerL) to hsl(hue, sat, contentL)
        }
    }

    // =========================================================================
    // 静态调色板辅助 (兼容性保留)
    // =========================================================================

    fun getBackgroundStatic(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val base = colors[index % colors.size].first
        return if (satOffset > 0) adjSat(base, satOffset) else base
    }

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val base = colors[index % colors.size].second
        return if (satOffset > 0) adjSat(base, satOffset) else base
    }

    private fun adjSat(color: Color, offset: Int): Color {
        val hsl = rgbHsl(color.red, color.green, color.blue)
        val newSat = (hsl[1] + offset * M1_STEP).coerceIn(0f, 1f)
        return hsl(hsl[0], newSat, hsl[2])
    }

    // =========================================================================
    // RGB ↔ HSL
    // =========================================================================

    private fun rgbHsl(r: Float, g: Float, b: Float): FloatArray {
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
        return floatArrayOf(h, s, l)
    }

    private fun rgbHsl(color: Color): FloatArray {
        return rgbHsl(color.red, color.green, color.blue)
    }
}