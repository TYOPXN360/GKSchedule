package com.ty.gkschedule.data

import com.ty.gkschedule.util.CourseColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

data class ScheduleRenderBlock(
    val item: ScheduleItem,
    val day: Int,
    val start: Int,
    val span: Int,
    val colorIdx: Int?,
    val classroomColorIdx: Int,
    val startLine: Float,
    val endLine: Float
)

data class ScheduleColorAssignment(
    val colorIndex: Int?,
    val classroomColorIndex: Int
)

data class ScheduleColorAssignments(
    val colorSlots: Map<String, Int>,
    val classroomSlots: Map<String, Map<String, Int>>
) {
    fun get(
        courseName: String,
        classroom: String,
        week: Int,
        colorGroupMode: Int,
        diffColorPerWeek: Boolean
    ): ScheduleColorAssignment {
        val normalizedGroupMode = colorGroupMode.coerceIn(0, 2)
        val colorKey = CourseColors.colorIdentityKey(
            groupMode = normalizedGroupMode,
            courseName = courseName,
            classroom = classroom,
            week = week,
            diffColorPerWeek = diffColorPerWeek
        )
        val classroomIndex = if (normalizedGroupMode == 1 && classroom.isNotBlank()) {
            classroomSlots[courseName]?.get(classroom) ?: 0
        } else {
            0
        }
        return ScheduleColorAssignment(
            colorIndex = colorSlots[colorKey] ?: CourseColors.stableColorIndex(colorKey),
            classroomColorIndex = classroomIndex
        )
    }

    fun get(
        item: ScheduleItem,
        week: Int,
        colorGroupMode: Int,
        diffColorPerWeek: Boolean
    ): ScheduleColorAssignment = get(
        courseName = item.name,
        classroom = item.classroom,
        week = week,
        colorGroupMode = colorGroupMode,
        diffColorPerWeek = diffColorPerWeek
    )
}

object ScheduleResolver {
    private const val COLOR_WEEK_LIMIT = 52

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

    fun buildColorAssignments(
        items: List<ScheduleItem>,
        colorGroupMode: Int,
        diffColorPerWeek: Boolean
    ): ScheduleColorAssignments {
        val normalizedGroupMode = colorGroupMode.coerceIn(0, 2)
        val colorSlots = items
            .flatMap { item ->
                colorWeeks(item, diffColorPerWeek).map { week ->
                    CourseColors.colorIdentityKey(
                        groupMode = normalizedGroupMode,
                        courseName = item.name,
                        classroom = item.classroom,
                        week = week,
                        diffColorPerWeek = diffColorPerWeek
                    )
                }
            }
            .distinct()
            .sorted()
            .mapIndexed { index, key -> key to index }
            .toMap()

        val classroomSlots = if (normalizedGroupMode == 1) {
            items.groupBy { it.name }
                .mapValues { (_, subjectItems) ->
                    subjectItems.map { it.classroom }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                        .mapIndexed { index, classroom -> classroom to index + 1 }
                        .toMap()
                }
        } else {
            emptyMap()
        }

        return ScheduleColorAssignments(
            colorSlots = colorSlots,
            classroomSlots = classroomSlots
        )
    }

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
        diffColorPerWeek: Boolean,
        mergeConsecutive: Boolean,
        detailedSplit: Boolean,
        periodsPerDay: Int,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String,
        colorItems: List<ScheduleItem> = items
    ): List<ScheduleRenderBlock> {
        val blocks = mutableListOf<ScheduleRenderBlock>()
        val weekItems = itemsForWeek(items, week)
        val colorAssignments = buildColorAssignments(colorItems, colorGroupMode, diffColorPerWeek)

        weekItems.forEach { item ->
            val colorAssignment = colorAssignments.get(item, week, colorGroupMode, diffColorPerWeek)

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
                        colorIdx = colorAssignment.colorIndex,
                        classroomColorIdx = colorAssignment.classroomColorIndex,
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

    private fun colorWeeks(item: ScheduleItem, diffColorPerWeek: Boolean): List<Int> {
        if (!diffColorPerWeek) return listOf(0)
        return when (item.weekRange) {
            "all" -> (1..COLOR_WEEK_LIMIT).toList()
            "odd" -> (1..COLOR_WEEK_LIMIT step 2).toList()
            "even" -> (2..COLOR_WEEK_LIMIT step 2).toList()
            else -> item.getWeekList().filter { it > 0 }.ifEmpty { listOf(0) }
        }
    }
}
