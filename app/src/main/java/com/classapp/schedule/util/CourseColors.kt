package com.classapp.schedule.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.google.android.material.color.utilities.Hct

/**
 * MD3 Expressive Industrial — 课程颜色引擎 v2
 *
 * 架构: Course Identity → Stable Hue Engine (360° + Golden Angle) → MD3 Tonal Palette (HCT) → Semantic Roles
 * - 360° 连续色相空间（彻底消除 8-seed 鸽巢撞色）
 * - 黄金角 137.508° 分布（视觉均匀，防聚类）
 * - MD3 tonal roles: container / content
 * - 多维稳定 key（course / classroom / week）
 * - 暗色/亮色自适应
 */
object CourseColors {

    /** MD3 Expressive Seed Palette — 仅用于 Settings badge 等固定场景 */
    private val seeds = intArrayOf(
        0x6750A4, 0x006A6A, 0x386A20, 0x8C4A2F,
        0x7D5260, 0x525F79, 0x984061, 0x7C4DFF
    )

    /** 颜色对: container=背景色  content=文字色 */
    data class CourseColorPair(val container: Color, val content: Color)

    // ─── Public API ──────────────────────────────────────────────

    @Composable
    fun getColor(mode: Int, courseName: String, classroom: String = "", classroomIndex: Int = 0): CourseColorPair {
        return getColorSync(mode, courseName, classroom, classroomIndex, isDark = LocalAppIsDark.current)
    }

    fun getColorSync(
        mode: Int,
        courseName: String,
        classroom: String = "",
        classroomIndex: Int = 0,
        week: Int = 0,
        diffColorPerWeek: Boolean = false,
        isDark: Boolean = false
    ): CourseColorPair {
        val key = buildKey(courseName, classroom, week, mode, classroomIndex, diffColorPerWeek)
        val hue = stableHue(key)
        // accent hue 偏移 18°，保持同色系但有区分度
        return CourseColorPair(
            container = tonalColor(hue, isDark, Role.Container),
            content = tonalColor(hue, isDark, Role.Content)
        )
    }

    @Composable
    fun getSettingsBadgeColor(index: Int): CourseColorPair {
        val isDark = LocalAppIsDark.current
        // 固定 8 色 seed 循环，但用黄金角偏移色相避免聚集
        val hue = (index * 137.50776405) % 360.0
        return CourseColorPair(
            container = tonalColor(hue, isDark, Role.Container),
            content = tonalColor(hue, isDark, Role.Content)
        )
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
            tonalColor(hue, isDark, Role.Container) to tonalColor(hue, isDark, Role.Content)
        }
    }

    // ─── Core Engine ─────────────────────────────────────────────

    /**
     * 多维稳定 key — 根据 colorGroupMode 构建不同粒度的身份标识
     * mode 0/1: 课程名（或 + classroom）
     * mode 2:   课程名 + 教室（+ week if diffColorPerWeek）
     */
    private fun buildKey(
        courseName: String,
        classroom: String,
        week: Int,
        mode: Int,
        classroomIndex: Int,
        diffColorPerWeek: Boolean
    ): String {
        return when (mode) {
            1 -> "$courseName|$classroom|$classroomIndex"
            2 -> if (diffColorPerWeek) "$courseName|$classroom|$week"
                 else "$courseName|$classroom"
            else -> if (diffColorPerWeek) "$courseName|$week" else courseName
        }
    }

    /**
     * 黄金角 hue engine — 360° 连续空间，消除鸽巢原理撞色
     *
     * 黄金角 137.508° 是自然界最均匀的分布角度（向日葵种子排列），
     * 即使 hash 值相近，也能保证色相差异最大化。
     * 规范化: (hash × goldenAngle) % 360 纯黄金角映射。
     */
    private fun stableHue(key: String): Double {
        val hash = key.hashCode().toLong() and 0xFFFFFFFFL
        return (hash * 137.50776405) % 360.0
    }

    private enum class Role { Container, Content }

    /**
     * MD3 Tonal Color — HCT 色彩空间标准角色映射
     *
     * Light: container=Tone90/Chroma42  content=Tone20/Chroma60
     * Dark:  container=Tone30/Chroma30  content=Tone88/Chroma70（AMOLED 稳定）
     */
    private fun tonalColor(hue: Double, isDark: Boolean, role: Role): Color {
        val (chroma, tone) = when (role) {
            Role.Container -> if (isDark) 30.0 to 30.0 else 42.0 to 90.0
            Role.Content   -> if (isDark) 70.0 to 88.0 else 60.0 to 20.0
        }
        return Color(Hct.from(hue, chroma, tone).toInt())
    }
}
