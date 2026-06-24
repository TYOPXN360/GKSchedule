package com.classapp.schedule.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

/**
 * Color engines:
 * 0 = Monet Dynamic — from MaterialTheme.colorScheme container colors
 * 1 = Vibrant Container — high saturation container colors
 * 2 = Classic Pastel — fixed soft pastel palette
 * 3 = HSL Rotation — evenly spaced hues from primary
 */
object CourseColors {

    @Composable
    fun getColors(engine: Int): List<Pair<Color, Color>> = when (engine) {
        1 -> getVibrantColors()
        2 -> getClassicColors()
        3 -> getHslColors()
        else -> getMonetColors()
    }

    /** Engine 0: Monet — MD3E container colors from system wallpaper */
    @Composable
    private fun getMonetColors(): List<Pair<Color, Color>> {
        val s = MaterialTheme.colorScheme
        return listOf(
            s.primaryContainer to s.onPrimaryContainer,
            s.secondaryContainer to s.onSecondaryContainer,
            s.tertiaryContainer to s.onTertiaryContainer,
            s.errorContainer to s.onErrorContainer,
            s.primary.copy(alpha = 0.25f) to s.primary,
            s.secondary.copy(alpha = 0.25f) to s.secondary,
            s.tertiary.copy(alpha = 0.25f) to s.tertiary,
            s.inversePrimary to s.inverseSurface,
        )
    }

    /** Engine 1: Vibrant — high saturation, bold colors */
    @Composable
    private fun getVibrantColors(): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
        return if (isDark) listOf(
            Color(0xFF442B2B) to Color(0xFFFFB4AB),   // Red
            Color(0xFF1A2B44) to Color(0xFF9ECAFF),   // Blue
            Color(0xFF2B1A3D) to Color(0xFFDDB8FF),   // Purple
            Color(0xFF1A3A2A) to Color(0xFF8FD8A0),   // Green
            Color(0xFF3D2B1A) to Color(0xFFFFCC80),   // Orange
            Color(0xFF1A3A3A) to Color(0xFF80D4D3),   // Teal
            Color(0xFF3D3A1A) to Color(0xFFD8C88F),   // Amber
            Color(0xFF3D1A2B) to Color(0xFFFFB0CE),   // Pink
        ) else listOf(
            Color(0xFFFFEBEE) to Color(0xFFC62828),   // Red
            Color(0xFFE3F2FD) to Color(0xFF1565C0),   // Blue
            Color(0xFFF3E5F5) to Color(0xFF6A1B9A),   // Purple
            Color(0xFFE8F5E9) to Color(0xFF2E7D32),   // Green
            Color(0xFFFFF3E0) to Color(0xFFE65100),   // Orange
            Color(0xFFE0F7FA) to Color(0xFF00838F),   // Teal
            Color(0xFFFFF8E1) to Color(0xFFEF6C00),   // Amber
            Color(0xFFFCE4EC) to Color(0xFFAD1457),   // Pink
        )
    }

    /** Engine 2: Classic — fixed pastel palette */
    @Composable
    private fun getClassicColors(): List<Pair<Color, Color>> {
        val isDark = isSystemInDarkTheme()
        return if (isDark) listOf(
            Color(0xFF3A2A1A) to Color(0xFFFFD9B3),   // Warm
            Color(0xFF1A2A3A) to Color(0xFFB3D9FF),   // Cool
            Color(0xFF2A1A3A) to Color(0xFFD9B3FF),   // Violet
            Color(0xFF1A3A1A) to Color(0xFFB3FFB3),   // Mint
            Color(0xFF3A1A1A) to Color(0xFFFFB3B3),   // Rose
            Color(0xFF1A3A2A) to Color(0xFFB3FFD9),   // Sea
            Color(0xFF3A3A1A) to Color(0xFFFFFFB3),   // Lemon
            Color(0xFF2A1A1A) to Color(0xFFFFB3D9),   // Coral
        ) else listOf(
            Color(0xFFFFF0E0) to Color(0xFF8B5E3C),   // Warm
            Color(0xFFE0F0FF) to Color(0xFF3C5E8B),   // Cool
            Color(0xFFF0E0FF) to Color(0xFF5E3C8B),   // Violet
            Color(0xFFE0FFE0) to Color(0xFF3C8B3C),   // Mint
            Color(0xFFFFE0E0) to Color(0xFF8B3C3C),   // Rose
            Color(0xFFE0FFF0) to Color(0xFF3C8B5E),   // Sea
            Color(0xFFFFFFE0) to Color(0xFF8B8B3C),   // Lemon
            Color(0xFFFFE0F0) to Color(0xFF8B3C5E),   // Coral
        )
    }

    /** Engine 3: HSL — evenly spaced hues derived from primary */
    @Composable
    private fun getHslColors(): List<Pair<Color, Color>> {
        val primary = MaterialTheme.colorScheme.primary
        val hsl = rgbToHsl(primary.red, primary.green, primary.blue)
        val baseHue = hsl[0]
        val isDark = isSystemInDarkTheme()

        return (0 until 8).map { i ->
            val hue = (baseHue + i * 45f) % 360f
            if (isDark) {
                val bg = hslToColor(hue, 0.4f, 0.25f)
                val fg = hslToColor(hue, 0.8f, 0.75f)
                bg to fg
            } else {
                val bg = hslToColor(hue, 0.3f, 0.9f)
                val fg = hslToColor(hue, 0.7f, 0.35f)
                bg to fg
            }
        }
    }

    @Composable
    fun getBackground(index: Int, colors: List<Pair<Color, Color>>): Color {
        return colors[index.coerceIn(0, colors.size - 1)].first
    }

    fun getTextColor(index: Int, colors: List<Pair<Color, Color>>): Color {
        return colors[index.coerceIn(0, colors.size - 1)].second
    }

    fun assignColorIndices(courses: List<com.classapp.schedule.data.Course>): Map<Long, Int> {
        val nameToIndex = mutableMapOf<String, Int>()
        var nextIndex = 0
        return courses.associate { course ->
            val idx = nameToIndex.getOrPut(course.name) { nextIndex++ }
            course.id to (idx % 8)
        }
    }

    // HSL helpers
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
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
    }
}
