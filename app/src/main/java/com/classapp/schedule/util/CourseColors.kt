package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.google.android.material.color.utilities.Hct
import kotlin.math.abs

/**
 * MD3 Expressive 课程颜色引擎
 *
 * 架构: Seed Palette → HCT Tonal Palette → Role Mapping → UI
 * - 8 个 Material Design seed colors（非随机）
 * - 课程名 hash → 稳定 seed 选择（无 jitter）
 * - 暗色/亮色固定 tone 映射（MD3 标准）
 * - container / content / accent 三角色体系
 */
object CourseColors {

    /** MD3 Expressive Seed Palette — 8 个 Google Material 风格种子色 */
    private val seeds = intArrayOf(
        0x6750A4,  // primary purple
        0x006A6A,  // teal
        0x386A20,  // green
        0x8C4A2F,  // brown
        0x7D5260,  // rose
        0x525F79,  // blue grey
        0x984061,  // magenta
        0x7C4DFF   // deep purple
    )

    /** 颜色对: container=背景色  content=文字色 */
    data class CourseColorPair(val container: Color, val content: Color)

    @Composable
    fun getColor(mode: Int, courseName: String, classroom: String = "", classroomIndex: Int = 0): CourseColorPair {
        return getColorSync(mode, courseName, classroom, classroomIndex, isDark = LocalAppIsDark.current)
    }

    fun getColorSync(mode: Int, courseName: String, classroom: String = "", classroomIndex: Int = 0, week: Int = 0, diffColorPerWeek: Boolean = false, isDark: Boolean = false): CourseColorPair {
        return makeColor(mode, courseName, classroom, classroomIndex, week, diffColorPerWeek, isDark)
    }

    private fun makeColor(mode: Int, courseName: String, classroom: String, classroomIndex: Int, week: Int, diffColorPerWeek: Boolean, isDark: Boolean): CourseColorPair {
        // 1. 稳定 seed 选择（NO random jitter）
        val seedKey = when {
            mode == 2 -> if (diffColorPerWeek) "$courseName|$classroom|$week" else "$courseName|$classroom"
            diffColorPerWeek -> "$courseName|$week"
            else -> courseName
        }
        val seedIndex = abs(seedKey.hashCode()) % seeds.size
        val seed = seeds[seedIndex] or 0xFF000000.toInt()

        // 2. HCT base
        val base = Hct.fromInt(seed)

        // 3. MD3 Expressive tone mapping — 暗色/亮色固定 tone
        return if (isDark) {
            // 暗色: container=Tone 25 + mode衰减, content=Tone 90
            val modeShift = if (mode == 1) classroomIndex * 2.0 else 0.0
            val containerTone = (25.0 - modeShift).coerceIn(18.0, 30.0)
            CourseColorPair(
                container = hctToColor(base.hue, 30.0, containerTone),
                content = hctToColor(base.hue, 40.0, 90.0)
            )
        } else {
            // 亮色: container=Tone 90, content=Tone 40 (MD3 standard)
            CourseColorPair(
                container = hctToColor(base.hue, 48.0, 90.0),
                content = hctToColor(base.hue, 65.0, 40.0)
            )
        }
    }

    /** Settings icon badge — 8 色 seed 循环，MD3 表达式风格 */
    @Composable
    fun getSettingsBadgeColor(index: Int): CourseColorPair {
        val isDark = LocalAppIsDark.current
        val seed = seeds[index % seeds.size]
        val base = Hct.fromInt(seed)
        return if (isDark) {
            CourseColorPair(
                container = hctToColor(base.hue, 45.0, 76.0),
                content = hctToColor(base.hue, 60.0, 15.0)
            )
        } else {
            CourseColorPair(
                container = hctToColor(base.hue, 45.0, 90.0),
                content = hctToColor(base.hue, 60.0, 22.0)
            )
        }
    }

    private fun hctToColor(hue: Double, chroma: Double, tone: Double): Color {
        return Color(Hct.from(hue, chroma, tone).toInt())
    }

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
            if (isDark) {
                hctToColor(hue, 30.0, 25.0) to hctToColor(hue, 40.0, 90.0)
            } else {
                hctToColor(hue, 48.0, 90.0) to hctToColor(hue, 65.0, 40.0)
            }
        }
    }
}
