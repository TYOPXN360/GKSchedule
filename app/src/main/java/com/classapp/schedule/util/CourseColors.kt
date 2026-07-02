package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.google.android.material.color.utilities.Hct
import kotlin.math.abs

/**
 * MD3 Expressive Final Standard — 课程颜色引擎
 *
 * 架构: Seed → HCT Tonal Palette → Semantic Roles → UI
 * - 8 个 Material Design seed colors
 * - 课程名 hash → 稳定 seed 选择（无 jitter）
 * - 完整 tonal system: Tone 0–100 标准映射
 * - Role mapping: primaryContainer / onPrimaryContainer
 * - 暗色/亮色自动适配
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
        // 1. 稳定 seed 选择 — MD3 Seed Palette（无随机）
        val seedKey = when {
            mode == 2 -> if (diffColorPerWeek) "$courseName|$classroom|$week" else "$courseName|$classroom"
            diffColorPerWeek -> "$courseName|$week"
            else -> courseName
        }
        val seedIndex = abs(seedKey.hashCode()) % seeds.size
        val seed = seeds[seedIndex] or 0xFF000000.toInt()

        // 2. HCT base — MD3 核心色彩空间
        val base = Hct.fromInt(seed)

        // 3. MD3 Tonal Palette — 从 seed 派生 primaryContainer / onPrimaryContainer
        // 亮色: Tone 90 primaryContainer / Tone 20 onPrimaryContainer
        // 暗色: Tone 30 primaryContainer / Tone 90 onPrimaryContainer
        // Mode 1 递减: classroomIndex 控制 chroma 递减
        return if (isDark) {
            val chromaShift = if (mode == 1) classroomIndex * 2.0 else 0.0
            val containerChroma = (30.0 - chromaShift).coerceIn(16.0, 36.0)
            CourseColorPair(
                container = hctToColor(base.hue, containerChroma, 30.0),
                content = hctToColor(base.hue, 50.0, 90.0)
            )
        } else {
            val containerChroma = if (mode == 1) (42.0 - classroomIndex * 4.0).coerceIn(22.0, 42.0) else 42.0
            CourseColorPair(
                container = hctToColor(base.hue, containerChroma, 90.0),
                content = hctToColor(base.hue, 65.0, 20.0)
            )
        }
    }

    /** Settings icon badge — 8 色 seed 循环，MD3 表达式风格 */
    @Composable
    fun getSettingsBadgeColor(index: Int): CourseColorPair {
        val isDark = LocalAppIsDark.current
        val seed = seeds[index % seeds.size] or 0xFF000000.toInt()
        val base = Hct.fromInt(seed)
        return if (isDark) {
            CourseColorPair(
                container = hctToColor(base.hue, 45.0, 76.0),
                content = hctToColor(base.hue, 65.0, 15.0)
            )
        } else {
            CourseColorPair(
                container = hctToColor(base.hue, 45.0, 90.0),
                content = hctToColor(base.hue, 65.0, 20.0)
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
                hctToColor(hue, 30.0, 30.0) to hctToColor(hue, 50.0, 90.0)
            } else {
                hctToColor(hue, 42.0, 90.0) to hctToColor(hue, 65.0, 20.0)
            }
        }
    }
}
