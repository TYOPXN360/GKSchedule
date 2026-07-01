package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import java.security.MessageDigest
import kotlin.math.abs

object CourseColors {

    private const val GOLDEN_ANGLE = 137.508f

    @Composable
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> = when (engine) {
        1 -> getVibrantColors(count)
        2 -> getClassicColors(count)
        3 -> getHslColors(count)
        else -> getMonetColors(count)
    }

    @Composable
    fun computeColor(
        courseIndex: Int,
        classroomIndex: Int,
        colorGroupMode: Int,
        engine: Int = 0
    ): Pair<Color, Color> {
        val isDark = LocalAppIsDark.current
        return computeColorInternal(courseIndex, classroomIndex, colorGroupMode, isDark)
    }

    fun computeColorSync(
        courseIndex: Int,
        classroomIndex: Int,
        colorGroupMode: Int,
        isDark: Boolean = false
    ): Pair<Color, Color> {
        return computeColorInternal(courseIndex, classroomIndex, colorGroupMode, isDark)
    }

    private fun computeColorInternal(
        courseIndex: Int,
        classroomIndex: Int,
        colorGroupMode: Int,
        isDark: Boolean
    ): Pair<Color, Color> {
        val bgL = if (isDark) 0.25f else 0.90f
        val txtL = if (isDark) 0.92f else 0.15f

        val hue: Float
        val sat: Float

        when (colorGroupMode) {
            0 -> {
                hue = (courseIndex % 8) * 45f
                sat = if (isDark) 0.65f else 0.75f
            }
            1 -> {
                hue = (courseIndex % 8) * 45f
                val maxSat = if (isDark) 0.80f else 0.85f
                sat = (maxSat - (classroomIndex % 4) * 0.15f).coerceAtLeast(0.40f)
            }
            else -> {
                hue = abs((courseIndex * 8 + classroomIndex) * GOLDEN_ANGLE) % 360f
                sat = if (isDark) 0.65f else 0.75f
            }
        }

        return hslToColor(hue, sat, bgL) to hslToColor(hue, sat, txtL)
    }

    @Composable
    fun getBackground(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0, weekOffset: Int = 0): Color {
        val base = colors[index % colors.size].first
        val rotated = if (weekOffset != 0) rotateHueByHash(base, index, weekOffset) else base
        return if (satOffset > 0) adjustSaturation(rotated, satOffset) else rotated
    }

    fun getBackgroundStatic(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0, weekOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].first
        val rotated = if (weekOffset != 0) rotateHueByHash(base, baseIdx, weekOffset) else base
        return if (satOffset > 0) adjustSaturation(rotated, satOffset) else rotated
    }

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>, satOffset: Int = 0, weekOffset: Int = 0): Color {
        val baseIdx = index % colors.size
        val base = colors[baseIdx].second
        val rotated = if (weekOffset != 0) rotateHueByHash(base, baseIdx, weekOffset) else base
        return if (satOffset > 0) adjustSaturation(rotated, satOffset) else rotated
    }

    private fun adjustSaturation(color: Color, offset: Int): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newSat = (hsl[1] + offset * 0.15f).coerceIn(0f, 1f)
        return hslToColor(hsl[0], newSat, hsl[2])
    }

    private fun rotateHue(color: Color, degrees: Float): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val newHue = (hsl[0] + degrees) % 360f
        return hslToColor(if (newHue < 0) newHue + 360f else newHue, hsl[1], hsl[2])
    }

    private fun rotateHueByHash(color: Color, courseIndex: Int, week: Int): Color {
        val hsl = rgbToHsl(color.red, color.green, color.blue)
        val md5 = MessageDigest.getInstance("MD5").digest("$courseIndex:$week".toByteArray())
        val hashDeg = ((md5[0].toInt() and 0xFF) * 360f / 256f)
        val newHue = (hsl[0] + hashDeg) % 360f
        return hslToColor(if (newHue < 0) newHue + 360f else newHue, hsl[1], hsl[2])
    }

    fun assignColorIndices(
        courses: List<com.classapp.schedule.data.Course>,
        groupMode: Int = 2
    ): Map<Long, Int> {
        val nameToIndex = mutableMapOf<String, Int>()
        val keyToIndex = mutableMapOf<String, Int>()
        var nextColor = 0
        return courses.associate { course ->
            val idx = when (groupMode) {
                0 -> nameToIndex.getOrPut(course.name) { nextColor++ }
                1 -> {
                    val baseIdx = nameToIndex.getOrPut(course.name) { nextColor++ }
                    val classroomCount = courses.count { it.name == course.name }
                    val classroomIdx = courses.filter { it.name == course.name }.indexOf(course)
                    baseIdx * 10 + classroomIdx
                }
                else -> keyToIndex.getOrPut("${course.name}|${course.classroom}") { nextColor++ }
            }
            course.id to idx
        }
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
                hslToColor(hue, 0.45f, 0.28f) to hslToColor(hue, 0.85f, 0.78f)
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
                hslToColor(hue, 0.50f, 0.25f) to hslToColor(hue, 0.90f, 0.80f)
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
                hslToColor(hue, 0.35f, 0.22f) to hslToColor(hue, 0.70f, 0.72f)
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
                hslToColor(hue, sat, 0.25f) to hslToColor(hue, sat + 0.4f, 0.80f)
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
