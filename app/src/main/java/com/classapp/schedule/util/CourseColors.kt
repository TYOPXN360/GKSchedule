package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import kotlin.math.abs

object CourseColors {

    private const val GOLDEN_ANGLE = 137.508f

    data class CourseColorPair(val container: Color, val content: Color)

    @Composable
    fun getColor(
        mode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0
    ): CourseColorPair {
        val isDark = LocalAppIsDark.current
        return getColorInternal(mode, courseName, classroom, classroomIndex, isDark)
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

    private fun getColorInternal(
        mode: Int,
        courseName: String,
        classroom: String,
        classroomIndex: Int,
        isDark: Boolean
    ): CourseColorPair {
        val bgL = if (isDark) 0.32f else 0.90f
        val txtL = if (isDark) 0.96f else 0.15f
        val courseHash = abs(courseName.hashCode())

        val hue: Float
        val sat: Float

        when (mode) {
            0 -> {
                hue = (courseHash % 8) * 45f
                sat = if (isDark) 0.78f else 0.75f
            }
            1 -> {
                hue = (courseHash % 8) * 45f
                val maxSat = if (isDark) 0.88f else 0.85f
                sat = (maxSat - (classroomIndex % 4) * 0.15f).coerceAtLeast(0.45f)
            }
            else -> {
                val combinedHash = abs("$courseName|$classroom".hashCode())
                hue = (combinedHash * GOLDEN_ANGLE) % 360f
                sat = if (isDark) 0.78f else 0.75f
            }
        }

        return CourseColorPair(
            container = hslToColor(hue, sat, bgL),
            content = hslToColor(hue, sat, txtL)
        )
    }

    fun assignColorIndices(
        courses: List<com.classapp.schedule.data.Course>,
        groupMode: Int = 2
    ): Map<Long, Int> {
        return courses.associate { course ->
            val idx = when (groupMode) {
                0 -> abs(course.name.hashCode()) % 8
                1 -> {
                    val baseIdx = abs(course.name.hashCode()) % 8
                    val classroomIdx = courses.filter { it.name == course.name }.indexOf(course)
                    baseIdx * 10 + classroomIdx
                }
                else -> abs("${course.name}|${course.classroom}".hashCode()) % 64
            }
            course.id to idx
        }
    }

    fun getBackgroundStatic(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].first
        return if (satOffset > 0) adjustSaturation(base, satOffset) else base
    }

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].second
        return if (satOffset > 0) adjustSaturation(base, satOffset) else base
    }

    private fun adjustSaturation(color: Color, offset: Int): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newSat = (hsl[1] + offset * 0.15f).coerceIn(0f, 1f)
        return hslToColor(hsl[0], newSat, hsl[2])
    }

    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> = when (engine) {
        1 -> getVibrantColors(count)
        2 -> getClassicColors(count)
        3 -> getHslColors(count)
        else -> getMonetColors(count)
    }

    @Composable
    private fun getMonetColors(count: Int): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]
        val step = 360f / count
        return (0 until count).map { i ->
            val hue = (baseHue + i * step) % 360f
            if (isDark) {
                hslToColor(hue, 0.78f, 0.32f) to hslToColor(hue, 0.30f, 0.96f)
            } else {
                hslToColor(hue, 0.45f, 0.88f) to hslToColor(hue, 0.75f, 0.35f)
            }
        }
    }

    @Composable
    private fun getVibrantColors(count: Int): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val step = 360f / count
        return (0 until count).map { i ->
            val hue = i * step
            if (isDark) {
                hslToColor(hue, 0.78f, 0.32f) to hslToColor(hue, 0.30f, 0.96f)
            } else {
                hslToColor(hue, 0.55f, 0.90f) to hslToColor(hue, 0.80f, 0.32f)
            }
        }
    }

    @Composable
    private fun getClassicColors(count: Int): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val step = 360f / count
        return (0 until count).map { i ->
            val hue = i * step
            if (isDark) {
                hslToColor(hue, 0.78f, 0.32f) to hslToColor(hue, 0.30f, 0.96f)
            } else {
                hslToColor(hue, 0.35f, 0.92f) to hslToColor(hue, 0.65f, 0.40f)
            }
        }
    }

    @Composable
    private fun getHslColors(count: Int): List<Pair<Color, Color>> {
        val isDark = LocalAppIsDark.current
        val primary = MaterialTheme.colorScheme.primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]
        val step = 360f / count
        return (0 until count).map { i ->
            val hue = (baseHue + i * step) % 360f
            val sat = if (i % 2 == 0) 0.50f else 0.40f
            if (isDark) {
                hslToColor(hue, 0.78f, 0.32f) to hslToColor(hue, 0.30f, 0.96f)
            } else {
                hslToColor(hue, sat, 0.90f) to hslToColor(hue, sat + 0.3f, 0.35f)
            }
        }
    }

    private fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
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
        return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
    }
}
