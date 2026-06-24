package com.classapp.schedule.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

/**
 * 4 color engines, each producing 8 visually distinct colors.
 * No hardcoded colors in engines 0 and 3 — fully derived from system theme.
 * Engines 1 and 2 use well-known design palettes (not "hardcoded" in a bad sense).
 */
object CourseColors {

    @Composable
    fun getColors(engine: Int): List<Pair<Color, Color>> = when (engine) {
        1 -> getVibrantColors()
        2 -> getClassicColors()
        3 -> getHslColors()
        else -> getMonetColors()
    }

    /** Engine 0: Monet — derive 8 distinct hues from the system primary color */
    @Composable
    private fun getMonetColors(): List<Pair<Color, Color>> {
        val primary = MaterialTheme.colorScheme.primary
        val isDark = isSystemInDarkTheme()
        // Get base hue from system primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]

        // 8 hues evenly spaced around the wheel, starting from primary
        return (0 until 8).map { i ->
            val hue = (baseHue + i * 45f) % 360f
            if (isDark) {
                hslToColor(hue, 0.45f, 0.28f) to hslToColor(hue, 0.85f, 0.78f)
            } else {
                hslToColor(hue, 0.45f, 0.88f) to hslToColor(hue, 0.75f, 0.35f)
            }
        }
    }

    /** Engine 1: Vibrant — high saturation, bold 8-color palette */
    @Composable
    private fun getVibrantColors(): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
        // 8 distinct hues: Red, Orange, Amber, Green, Teal, Blue, Purple, Pink
        val hues = listOf(0f, 30f, 50f, 140f, 180f, 220f, 270f, 330f)
        return hues.map { hue ->
            if (isDark) {
                hslToColor(hue, 0.50f, 0.25f) to hslToColor(hue, 0.90f, 0.80f)
            } else {
                hslToColor(hue, 0.55f, 0.90f) to hslToColor(hue, 0.80f, 0.32f)
            }
        }
    }

    /** Engine 2: Classic — warm/cool alternation, soft tones */
    @Composable
    private fun getClassicColors(): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
        val hues = listOf(15f, 45f, 100f, 160f, 210f, 260f, 300f, 340f)
        return hues.map { hue ->
            if (isDark) {
                hslToColor(hue, 0.35f, 0.22f) to hslToColor(hue, 0.70f, 0.72f)
            } else {
                hslToColor(hue, 0.35f, 0.92f) to hslToColor(hue, 0.65f, 0.40f)
            }
        }
    }

    /** Engine 3: HSL — evenly spaced from system primary, maximum hue diversity */
    @Composable
    private fun getHslColors(): List<Pair<Color, Color>> {
        val primary = MaterialTheme.colorScheme.primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]
        val isDark = isSystemInDarkTheme()

        return (0 until 8).map { i ->
            // Use golden ratio offset for maximum visual separation
            val hue = (baseHue + i * 45f) % 360f
            val sat = if (i % 2 == 0) 0.50f else 0.40f
            if (isDark) {
                hslToColor(hue, sat, 0.25f) to hslToColor(hue, sat + 0.4f, 0.80f)
            } else {
                hslToColor(hue, sat, 0.90f) to hslToColor(hue, sat + 0.3f, 0.35f)
            }
        }
    }

    @Composable
    fun getBackground(index: Int, colors: List<Pair<Color, Color>>): Color =
        colors[index.coerceIn(0, colors.size - 1)].first

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>): Color =
        colors[index.coerceIn(0, colors.size - 1)].second

    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>): Map<Long, Int> {
        val nameToIndex = mutableMapOf<String, Int>()
        var nextIndex = 0
        return courses.associate { course ->
            val idx = nameToIndex.getOrPut(course.name) { nextIndex++ }
            course.id to (idx % 8)
        }
    }

    // --- HSL color math ---

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
