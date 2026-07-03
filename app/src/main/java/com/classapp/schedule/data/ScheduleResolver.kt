package com.classapp.schedule.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class ScheduleRenderBlock(
    val item: ScheduleItem,
    val day: Int,
    val start: Int,
    val span: Int,
    val colorIdx: Int,
    val startLine: Float,
    val endLine: Float
)

object ScheduleResolver {
    fun buildItems(
        courses: List<Course>,
        exams: List<ExamEntity>,
        showExamSchedule: Boolean,
        semesterStart: LocalDate,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): List<ScheduleItem> {
        val courseItems = courses.map { ScheduleItem.CourseItem(it) }
        if (!showExamSchedule) return courseItems

        return courseItems + exams.mapNotNull { exam ->
            ScheduleItem.fromExam(exam, semesterStart, getStartTime, getEndTime)
        }
    }

    fun itemsForWeek(items: List<ScheduleItem>, week: Int): List<ScheduleItem> =
        items.filter { it.isInWeek(week) }

    fun visibleWeeks(
        items: List<ScheduleItem>,
        totalWeeks: Int,
        hideEmptyWeeks: Boolean,
        currentWeek: Int,
        realCurrentWeek: Int
    ): List<Int> {
        if (!hideEmptyWeeks) return (1..totalWeeks).toList()

        val nonEmpty = (1..totalWeeks).filter { week ->
            items.any { it.isInWeek(week) }
        }.toMutableList()
        if (!nonEmpty.contains(currentWeek) && currentWeek in 1..totalWeeks) nonEmpty.add(currentWeek)
        if (!nonEmpty.contains(realCurrentWeek) && realCurrentWeek in 1..totalWeeks) nonEmpty.add(realCurrentWeek)

        return nonEmpty.distinct().sorted().ifEmpty { listOf(1) }
    }

    fun buildRenderBlocks(
        items: List<ScheduleItem>,
        week: Int,
        colorGroupMode: Int,
        mergeConsecutive: Boolean,
        detailedSplit: Boolean,
        periodsPerDay: Int,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): List<ScheduleRenderBlock> {
        val blocks = mutableListOf<ScheduleRenderBlock>()
        val classroomCounters = mutableMapOf<String, Int>()

        itemsForWeek(items, week).forEach { item ->
            val colorIdx = when (colorGroupMode) {
                0 -> abs(item.name.hashCode()) % 8
                1 -> {
                    val baseIdx = abs(item.name.hashCode()) % 8
                    val classIdx = classroomCounters.getOrPut(item.name) { 0 }
                    classroomCounters[item.name] = classIdx + 1
                    baseIdx * 10 + classIdx
                }
                else -> abs("${item.name}|${item.classroom}".hashCode()) % 64
            }

            fun addBlock(start: Int, span: Int) {
                val startLine = if (item.isExam) {
                    timeToGridLine(item.customStartTime, getStartTime, getEndTime, periodsPerDay)
                } else {
                    (start - 1).toFloat()
                }
                val endLine = if (item.isExam) {
                    timeToGridLine(item.customEndTime, getStartTime, getEndTime, periodsPerDay)
                        .coerceAtLeast(startLine + 0.25f)
                } else {
                    startLine + span
                }
                blocks.add(
                    ScheduleRenderBlock(
                        item = item,
                        day = item.dayOfWeek,
                        start = start,
                        span = span,
                        colorIdx = colorIdx,
                        startLine = startLine,
                        endLine = endLine
                    )
                )
            }

            if (item.isExam || mergeConsecutive) {
                addBlock(item.startPeriod, item.periods)
            } else if (detailedSplit) {
                for (period in item.startPeriod..item.endPeriod()) addBlock(period, 1)
            } else {
                var period = item.startPeriod
                while (period <= item.endPeriod()) {
                    val pairEnd = minOf(period + 1, item.endPeriod())
                    addBlock(period, pairEnd - period + 1)
                    period = pairEnd + 1
                }
            }
        }

        return blocks
    }

    fun todayCourses(courses: List<Course>, currentWeek: Int, today: LocalDate): List<Course> =
        courses.filter { it.dayOfWeek == today.dayOfWeek.value && it.isInWeek(currentWeek) }
            .sortedBy { it.startPeriod }

    fun tomorrowCourses(courses: List<Course>, currentWeek: Int, today: LocalDate): List<Course> {
        val tomorrow = today.plusDays(1)
        val tomorrowWeek = if (today.dayOfWeek.value == 7) currentWeek + 1 else currentWeek
        return courses
            .filter { it.dayOfWeek == tomorrow.dayOfWeek.value && tomorrowWeek in 1..52 && it.isInWeek(tomorrowWeek) }
            .sortedBy { it.startPeriod }
    }

    fun todayExams(exams: List<ExamEntity>, showExamSchedule: Boolean, today: LocalDate): List<ExamEntity> {
        if (!showExamSchedule) return emptyList()
        return exams.filter { exam -> parseDateOrNull(exam.examDate) == today }
    }

    fun upcomingExams(
        exams: List<ExamEntity>,
        showExamSchedule: Boolean,
        today: LocalDate,
        lookaheadWeeks: Int,
        limit: Int = 5
    ): List<ExamEntity> {
        if (!showExamSchedule) return emptyList()
        val latestExamDate = today.plusWeeks(lookaheadWeeks.toLong())
        return exams.filter { exam ->
            val examDate = parseDateOrNull(exam.examDate)
            examDate != null && !examDate.isBefore(today) && !examDate.isAfter(latestExamDate) && examDate != today
        }.sortedBy { it.examDate }.take(limit)
    }

    fun examWeek(exam: ExamEntity, semesterStart: LocalDate, fallbackWeek: Int): Int {
        val examDate = parseDateOrNull(exam.examDate) ?: return fallbackWeek
        return (ChronoUnit.DAYS.between(semesterStart, examDate).toInt() / 7) + 1
    }

    fun examItem(
        exam: ExamEntity,
        semesterStart: LocalDate,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): ScheduleItem.ExamItem? = ScheduleItem.fromExam(exam, semesterStart, getStartTime, getEndTime)

    private fun timeToGridLine(
        time: String,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String,
        periodsPerDay: Int
    ): Float {
        val target = parseMinutes(time) ?: return 0f
        for (period in 1..periodsPerDay) {
            val start = parseMinutes(getStartTime(period)) ?: continue
            val end = parseMinutes(getEndTime(period)) ?: continue
            if (target in start..end) {
                val duration = (end - start).coerceAtLeast(1)
                return (period - 1) + (target - start).toFloat() / duration.toFloat()
            }
            val nextStart = parseMinutes(getStartTime(period + 1))
            if (nextStart != null && target >= end && target <= nextStart) return period.toFloat()
        }
        val firstStart = parseMinutes(getStartTime(1)) ?: return 0f
        val lastEnd = parseMinutes(getEndTime(periodsPerDay)) ?: return periodsPerDay.toFloat()
        return when {
            target <= firstStart -> 0f
            target >= lastEnd -> periodsPerDay.toFloat()
            else -> timeToPeriod(time, getStartTime).let { if (it > 0) (it - 1).toFloat() else 0f }
        }
    }

    private fun timeToPeriod(time: String, timeProvider: (Int) -> String): Int {
        val targetMins = parseMinutes(time) ?: return 0
        var bestPeriod = 0
        var bestDiff = Int.MAX_VALUE
        for (period in 1..14) {
            val periodMins = parseMinutes(timeProvider(period)) ?: continue
            val diff = abs(targetMins - periodMins)
            if (diff < bestDiff) {
                bestDiff = diff
                bestPeriod = period
            }
        }
        return bestPeriod
    }

    private fun parseMinutes(time: String): Int? {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun parseDateOrNull(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: Exception) {
        null
    }
}
