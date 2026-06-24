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
    fun getColors(engine: Int, count: Int = 16): List<Pair<Color, Color>> = when (engine) {
        1 -> getVibrantColors(count)
        2 -> getClassicColors(count)
        3 -> getHslColors(count)
        else -> getMonetColors(count)
    }

    /** Engine 0: Monet — derive N distinct hues from the system primary color */
    @Composable
    private fun getMonetColors(count: Int): List<Pair<Color, Color>> {
        val primary = MaterialTheme.colorScheme.primary
        val isDark = isSystemInDarkTheme()
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

    /** Engine 1: Vibrant — high saturation, N colors evenly spaced */
    @Composable
    private fun getVibrantColors(count: Int): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
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

    /** Engine 2: Classic — soft tones, N colors evenly spaced */
    @Composable
    private fun getClassicColors(count: Int): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
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

    /** Engine 3: HSL — from system primary, N colors with alternating saturation */
    @Composable
    private fun getHslColors(count: Int): List<Pair<Color, Color>> {
        val primary = MaterialTheme.colorScheme.primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]
        val isDark = isSystemInDarkTheme()
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

    @Composable
    fun getBackground(index: Int, colors: List<Pair<Color, Color>>): Color =
        colors[index.coerceIn(0, colors.size - 1)].first

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>): Color =
        colors[index.coerceIn(0, colors.size - 1)].second

    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>): Map<Long, Int> {
        // Each course entry gets its own color based on position
        return courses.mapIndexed { index, course -> course.id to index }.toMap()
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
