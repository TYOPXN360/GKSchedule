package com.classapp.schedule.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CourseColors {
    // Text colors — vibrant, high contrast
    val textColors = listOf(
        Color(0xFFC62828), // Red
        Color(0xFF1565C0), // Blue
        Color(0xFF6A1B9A), // Purple
        Color(0xFF2E7D32), // Green
        Color(0xFFE65100), // Orange
        Color(0xFF00838F), // Teal
        Color(0xFFEF6C00), // Amber
        Color(0xFFAD1457), // Pink
    )

    // Light mode backgrounds — soft pastel
    val lightBgs = listOf(
        Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFF3E5F5),
        Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFE0F7FA),
        Color(0xFFFFF8E1), Color(0xFFFCE4EC),
    )

    // Dark mode backgrounds — muted deep tones
    val darkBgs = listOf(
        Color(0xFF3E1A1A), Color(0xFF1A2A4A), Color(0xFF2E1A3E),
        Color(0xFF1A3A1A), Color(0xFF3E2A1A), Color(0xFF1A3A3A),
        Color(0xFF3E3A1A), Color(0xFF3E1A2A),
    )

    fun getColor(index: Int): Color = textColors[index.coerceIn(0, textColors.size - 1)]
    fun getLightBackground(index: Int): Color = lightBgs[index.coerceIn(0, lightBgs.size - 1)]
    fun getDarkBackground(index: Int): Color = darkBgs[index.coerceIn(0, darkBgs.size - 1)]

    @Composable
    fun getBackground(index: Int): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) getDarkBackground(index) else getLightBackground(index)
    }

    fun getBackground(index: Int, isDark: Boolean): Color =
        if (isDark) getDarkBackground(index) else getLightBackground(index)
}
