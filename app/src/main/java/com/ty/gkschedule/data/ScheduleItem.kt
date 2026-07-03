package com.ty.gkschedule.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Unified display model for schedule items (courses + exams).
 * Replaces the id<0 hack where ExamEntity was converted to Course with negative id.
 */
sealed class ScheduleItem {
    abstract val id: Long
    abstract val name: String
    abstract val teacher: String
    abstract val classroom: String
    abstract val dayOfWeek: Int
    abstract val startPeriod: Int
    abstract val periods: Int
    abstract val colorIndex: Int
    abstract val weekRange: String
    abstract val remark: String
    abstract val isCustomTime: Boolean
    abstract val customStartTime: String
    abstract val customEndTime: String
    abstract val isExam: Boolean

    fun endPeriod(): Int = startPeriod + periods - 1

    fun isInWeek(week: Int): Boolean = when (weekRange) {
        "all" -> true
        "odd" -> week % 2 == 1
        "even" -> week % 2 == 0
        else -> parseWeekList().contains(week)
    }

    fun getWeekList(): List<Int> = when (weekRange) {
        "all" -> emptyList()
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

    /** Get actual start time (considering custom time) */
    fun getActualStartTime(getter: (Int) -> String): String =
        if (isCustomTime && customStartTime.isNotEmpty()) customStartTime else getter(startPeriod)

    /** Get actual end time (considering custom time) */
    fun getActualEndTime(getter: (Int) -> String): String =
        if (isCustomTime && customEndTime.isNotEmpty()) customEndTime else getter(endPeriod())

    /** Wrap a Course as ScheduleItem */
    data class CourseItem(val course: Course) : ScheduleItem() {
        override val id get() = course.id
        override val name get() = course.name
        override val teacher get() = course.teacher
        override val classroom get() = course.classroom
        override val dayOfWeek get() = course.dayOfWeek
        override val startPeriod get() = course.startPeriod
        override val periods get() = course.periods
        override val colorIndex get() = course.colorIndex
        override val weekRange get() = course.weekRange
        override val remark get() = course.remark
        override val isCustomTime get() = course.isCustomTime
        override val customStartTime get() = course.customStartTime
        override val customEndTime get() = course.customEndTime
        override val isExam get() = false
    }

    /** Wrap an ExamEntity as ScheduleItem */
    data class ExamItem(
        val exam: ExamEntity,
        override val dayOfWeek: Int,
        override val startPeriod: Int,
        override val periods: Int,
        override val weekRange: String,
        override val customStartTime: String,
        override val customEndTime: String
    ) : ScheduleItem() {
        override val id get() = exam.id
        override val name get() = exam.courseName
        override val teacher get() = exam.teacherInfo
        override val classroom get() = exam.classroom
        override val colorIndex get() = 7 // exams get a fixed color slot
        override val remark get() = exam.examMethod
        override val isCustomTime get() = true
        override val isExam get() = true

        /** Original exam fields for detail display */
        val courseCode get() = exam.courseCode
        val campus get() = exam.campus
        val credits get() = exam.credits
        val yearName get() = exam.yearName
        val semesterName get() = exam.semesterName
        val examDate get() = exam.examDate
        val examTimeRange get() = exam.examTimeRange
    }

    companion object {
        /** Convert ExamEntity to ScheduleItem (returns null if date/time parsing fails) */
        fun fromExam(
            exam: ExamEntity,
            semesterStart: LocalDate,
            getStartTime: (Int) -> String,
            getEndTime: (Int) -> String
        ): ExamItem? {
            return try {
                val examDate = LocalDate.parse(exam.examDate)
                val daysDiff = ChronoUnit.DAYS.between(semesterStart, examDate).toInt()
                val week = (daysDiff / 7) + 1
                if (week <= 0) return null

                val timeParts = exam.examTimeRange.split("-")
                if (timeParts.size != 2) return null
                val startPeriod = timeToPeriod(timeParts[0].trim(), getStartTime)
                val endPeriod = timeToPeriod(timeParts[1].trim(), getEndTime)
                if (startPeriod <= 0 || endPeriod < startPeriod) return null

                ExamItem(
                    exam = exam,
                    dayOfWeek = examDate.dayOfWeek.value,
                    startPeriod = startPeriod,
                    periods = endPeriod - startPeriod + 1,
                    weekRange = week.toString(),
                    customStartTime = timeParts[0].trim(),
                    customEndTime = timeParts[1].trim()
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun timeToPeriod(time: String, timeProvider: (Int) -> String): Int {
            val parts = time.split(":")
            val targetMins = ((parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60) + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
            var bestPeriod = 0
            var bestDiff = Int.MAX_VALUE
            for (p in 1..14) {
                val pTime = timeProvider(p)
                if (pTime.isEmpty()) continue
                val pParts = pTime.split(":")
                val pMins = ((pParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60) + (pParts.getOrNull(1)?.toIntOrNull() ?: 0)
                val diff = kotlin.math.abs(targetMins - pMins)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestPeriod = p
                }
            }
            return bestPeriod
        }
    }
}
