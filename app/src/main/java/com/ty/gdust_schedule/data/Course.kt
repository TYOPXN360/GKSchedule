package com.ty.gdust_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int,        // 1=Mon ... 7=Sun
    val startPeriod: Int,      // 起始节次 (1-based)
    val periods: Int = 1,      // 持续节数
    val colorIndex: Int = 0,   // 颜色索引 0-7
    val weekRange: String = "all", // "all", "odd", "even", "1-16,18"
    val remark: String = "",   // 备注
    val isCustomTime: Boolean = false, // 自定义时间
    val customStartTime: String = "",  // HH:mm
    val customEndTime: String = "",    // HH:mm
    val isManuallyEdited: Boolean = false, // 手动编辑过，同步时不会被覆盖
    val isHidden: Boolean = false // 在课表中隐藏
) {
    fun endPeriod(): Int = startPeriod + periods - 1

    fun isInWeek(week: Int): Boolean = when (weekRange) {
        "all" -> true
        "odd" -> week % 2 == 1
        "even" -> week % 2 == 0
        else -> parseWeekList().contains(week)
    }

    fun getWeekList(): List<Int> = when (weekRange) {
        "all" -> emptyList() // means all
        "odd" -> (1..30 step 2).toList()
        "even" -> (2..30 step 2).toList()
        else -> parseWeekList()
    }

    private fun parseWeekList(): List<Int> {
        val result = mutableListOf<Int>()
        weekRange.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            when {
                part.contains("-") -> {
                    val (start, end) = part.split("-").map { it.trim().toIntOrNull() ?: 0 }
                    result.addAll(start..end)
                }
                else -> part.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    /** 获取实际开始时间（考虑自定义时间） */
    fun getActualStartTime(getter: (Int) -> String): String =
        if (isCustomTime && customStartTime.isNotEmpty()) customStartTime else getter(startPeriod)

    /** 获取实际结束时间（考虑自定义时间） */
    fun getActualEndTime(getter: (Int) -> String): String =
        if (isCustomTime && customEndTime.isNotEmpty()) customEndTime else getter(endPeriod())

    /** 检测是否与另一门课冲突 */
    fun conflictsWith(other: Course): Boolean {
        if (dayOfWeek != other.dayOfWeek) return false
        if (!hasOverlapWeek(other)) return false
        return startPeriod <= other.endPeriod() && endPeriod() >= other.startPeriod
    }

    private fun hasOverlapWeek(other: Course): Boolean {
        val myWeeks = getWeekList().ifEmpty { (1..30).toList() }
        val otherWeeks = other.getWeekList().ifEmpty { (1..30).toList() }
        return myWeeks.intersect(otherWeeks.toSet()).isNotEmpty()
    }
}
