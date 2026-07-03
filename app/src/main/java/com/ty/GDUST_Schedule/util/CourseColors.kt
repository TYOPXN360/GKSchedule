package com.ty.GDUST_Schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ty.GDUST_Schedule.ui.theme.LocalAppIsDark
import com.google.android.material.color.utilities.Hct

object CourseColors {
    data class CourseColorPair(val container: Color, val content: Color)

    private const val GOLDEN_ANGLE = 137.50776405
    private const val DEFAULT_THEME_HUE = 270.0

    private val classicHues = doubleArrayOf(
        8.0, 32.0, 54.0, 92.0, 136.0, 176.0, 212.0, 248.0, 286.0, 326.0
    )

    @Composable
    fun currentThemeHue(): Double = Hct.fromInt(MaterialTheme.colorScheme.primary.toArgb()).hue

    @Composable
    fun getColor(
        engine: Int,
        groupMode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0,
        colorIndex: Int? = null,
        week: Int = 0,
        diffColorPerWeek: Boolean = false
    ): CourseColorPair {
        val isDark = LocalAppIsDark.current
        val themeHue = currentThemeHue()
        return getColorSync(
            engine = engine,
            groupMode = groupMode,
            courseName = courseName,
            classroom = classroom,
            classroomIndex = classroomIndex,
            colorIndex = colorIndex,
            week = week,
            diffColorPerWeek = diffColorPerWeek,
            isDark = isDark,
            themeHue = themeHue
        )
    }

    fun getColorSync(
        engine: Int,
        groupMode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0,
        colorIndex: Int? = null,
        week: Int = 0,
        diffColorPerWeek: Boolean = false,
        isDark: Boolean = false,
        themeHue: Double = DEFAULT_THEME_HUE
    ): CourseColorPair {
        val normalizedGroupMode = groupMode.coerceIn(0, 2)
        val normalizedEngine = engine.coerceIn(0, 3)
        val hueKey = hueKey(courseName, classroom, week, normalizedGroupMode, diffColorPerWeek)
        val slot = colorIndex ?: stableSlot(hueKey)
        val variant = variantIndex(classroom, classroomIndex, normalizedGroupMode)
        val hue = hueForEngine(normalizedEngine, hueKey, slot, themeHue)

        return CourseColorPair(
            container = tonalColor(hue, normalizedEngine, isDark, Role.Container, variant),
            content = tonalColor(hue, normalizedEngine, isDark, Role.Content, variant)
        )
    }

    @Composable
    fun getSettingsBadgeColor(index: Int): CourseColorPair {
        val isDark = LocalAppIsDark.current
        val themeHue = Hct.fromInt(MaterialTheme.colorScheme.primary.toArgb()).hue
        val hue = hueForEngine(engine = 0, key = "settings|$index", slot = index, themeHue = themeHue)
        return CourseColorPair(
            container = tonalColor(hue, engine = 0, isDark = isDark, role = Role.Container, variant = 0),
            content = tonalColor(hue, engine = 0, isDark = isDark, role = Role.Content, variant = 0)
        )
    }

    fun colorIdentityKey(
        groupMode: Int,
        courseName: String,
        classroom: String,
        week: Int,
        diffColorPerWeek: Boolean
    ): String = hueKey(courseName, classroom, week, groupMode.coerceIn(0, 2), diffColorPerWeek)

    fun stableColorIndex(key: String): Int = stableSlot(key)

    private fun hueKey(
        courseName: String,
        classroom: String,
        week: Int,
        groupMode: Int,
        diffColorPerWeek: Boolean
    ): String {
        val base = when (groupMode) {
            2 -> "$courseName|$classroom"
            else -> courseName
        }
        return if (diffColorPerWeek) "$base|week:$week" else base
    }

    private fun variantIndex(classroom: String, classroomIndex: Int, groupMode: Int): Int {
        if (groupMode != 1) return 0
        if (classroom.isBlank()) return 0
        if (classroomIndex > 0) return classroomIndex.coerceAtMost(5)
        return (positiveHash(classroom) % 5).toInt() + 1
    }

    private fun stableSlot(key: String): Int = (positiveHash(key) % 4096).toInt()

    private fun hueForEngine(engine: Int, key: String, slot: Int, themeHue: Double): Double {
        val safeSlot = if (slot < 0) -slot.toLong() else slot.toLong()
        return when (engine) {
            0 -> normalizeHue(themeHue + safeSlot * GOLDEN_ANGLE)
            1 -> normalizeHue(18.0 + safeSlot * GOLDEN_ANGLE)
            2 -> {
                val baseIndex = (safeSlot % classicHues.size).toInt()
                val cycle = safeSlot / classicHues.size
                normalizeHue(classicHues[baseIndex] + cycle * 11.0)
            }
            else -> stableHue(key)
        }
    }

    private fun stableHue(key: String): Double =
        normalizeHue(positiveHash(key) * GOLDEN_ANGLE)

    private fun positiveHash(key: String): Long = key.hashCode().toLong() and 0xFFFFFFFFL

    private fun normalizeHue(hue: Double): Double {
        val normalized = hue % 360.0
        return if (normalized < 0) normalized + 360.0 else normalized
    }

    private enum class Role { Container, Content }

    private fun tonalColor(
        hue: Double,
        engine: Int,
        isDark: Boolean,
        role: Role,
        variant: Int
    ): Color {
        val variantBoost = variant.coerceIn(0, 5).toDouble()
        val (baseChroma, baseTone) = when (engine) {
            1 -> when (role) {
                Role.Container -> if (isDark) 44.0 to 30.0 else 58.0 to 86.0
                Role.Content -> if (isDark) 78.0 to 90.0 else 70.0 to 20.0
            }
            2 -> when (role) {
                Role.Container -> if (isDark) 24.0 to 32.0 else 30.0 to 91.0
                Role.Content -> if (isDark) 50.0 to 88.0 else 48.0 to 24.0
            }
            3 -> when (role) {
                Role.Container -> if (isDark) 36.0 to 30.0 else 46.0 to 88.0
                Role.Content -> if (isDark) 68.0 to 89.0 else 62.0 to 21.0
            }
            else -> when (role) {
                Role.Container -> if (isDark) 30.0 to 30.0 else 42.0 to 90.0
                Role.Content -> if (isDark) 64.0 to 88.0 else 58.0 to 20.0
            }
        }

        val chroma = when (role) {
            Role.Container -> baseChroma + variantBoost * 7.0
            Role.Content -> baseChroma + variantBoost * 5.0
        }.coerceIn(16.0, 88.0)
        val tone = when {
            variant == 0 -> baseTone
            isDark && role == Role.Container -> baseTone + variantBoost * 3.0
            !isDark && role == Role.Container -> baseTone - variantBoost * 2.5
            else -> baseTone
        }.coerceIn(18.0, 92.0)

        return Color(Hct.from(hue, chroma, tone).toInt())
    }
}
