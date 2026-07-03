package com.classapp.schedule.data

/**
 * 课程 & 考试冲突检测引擎
 *
 * 架构: ScheduleEvent → Conflict Detection → Classification → UI Hint
 * - 只标记 + 提示，不删除数据
 * - 优先级: Exam > Manual Exam > Course > Optional Course
 */
object ConflictEngine {

    /** 统一事件模型 */
    sealed class ScheduleEvent {
        abstract val id: Long
        abstract val name: String
        abstract val dayOfWeek: Int
        abstract val startPeriod: Int
        abstract val endPeriod: Int
        abstract val isManuallyAdded: Boolean

        data class CourseEvent(
            override val id: Long,
            override val name: String,
            override val dayOfWeek: Int,
            override val startPeriod: Int,
            override val endPeriod: Int,
            val classroom: String,
            val teacher: String,
            override val isManuallyAdded: Boolean
        ) : ScheduleEvent()

        data class ExamEvent(
            override val id: Long,
            override val name: String,
            override val dayOfWeek: Int,
            override val startPeriod: Int,
            override val endPeriod: Int,
            val classroom: String,
            override val isManuallyAdded: Boolean
        ) : ScheduleEvent()
    }

    /** 冲突类型 */
    enum class ConflictType { COURSE_COURSE, COURSE_EXAM, EXAM_EXAM }

    /** 冲突结构 */
    data class Conflict(
        val eventA: ScheduleEvent,
        val eventB: ScheduleEvent,
        val type: ConflictType,
        val week: Int
    )

    /**
     * 从 Course 列表构建 ScheduleEvent
     */
    fun Course.toScheduleEvent(): ScheduleEvent.CourseEvent {
        return ScheduleEvent.CourseEvent(
            id = id,
            name = name,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            endPeriod = endPeriod(),
            classroom = classroom,
            teacher = teacher,
            isManuallyAdded = isManuallyEdited
        )
    }

    fun ExamEntity.toScheduleEvent(): ScheduleEvent.ExamEvent {
        // Calculate period from examTimeRange
        val parts = examTimeRange.split("-")
        val startPeriod = if (parts.size == 2) timeToPeriod(parts[0].trim()) else 1
        val endPeriod = if (parts.size == 2) timeToPeriod(parts[1].trim()) else startPeriod + 2
        return ScheduleEvent.ExamEvent(
            id = id,
            name = courseName,
            dayOfWeek = try { java.time.LocalDate.parse(examDate).dayOfWeek.value } catch (_: Exception) { 0 },
            startPeriod = startPeriod,
            endPeriod = endPeriod,
            classroom = classroom,
            isManuallyAdded = isLocal
        )
    }

    /**
     * 冲突检测: O(n²) 但课程数量通常 < 100
     */
    fun detect(events: List<ScheduleEvent>): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()
        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val a = events[i]
                val b = events[j]
                // 同一天 + 时间重叠 = 冲突
                if (a.dayOfWeek == b.dayOfWeek && a.dayOfWeek > 0 &&
                    a.startPeriod < b.endPeriod && b.startPeriod < a.endPeriod
                ) {
                    val type = when {
                        a is ScheduleEvent.CourseEvent && b is ScheduleEvent.CourseEvent -> ConflictType.COURSE_COURSE
                        a is ScheduleEvent.ExamEvent && b is ScheduleEvent.ExamEvent -> ConflictType.EXAM_EXAM
                        else -> ConflictType.COURSE_EXAM
                    }
                    conflicts.add(Conflict(a, b, type, 0))
                }
            }
        }
        return conflicts
    }

    /**
     * 按周次检测冲突
     */
    fun detectForWeek(
        courses: List<Course>,
        exams: List<ExamEntity>,
        week: Int,
        semesterStart: java.time.LocalDate
    ): List<Conflict> {
        val weekCourses = courses.filter { it.isInWeek(week) }.map { it.toScheduleEvent() }
        val weekExams = exams.filter { exam ->
            try {
                val examDate = java.time.LocalDate.parse(exam.examDate)
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(semesterStart, examDate).toInt()
                val examWeek = (daysDiff / 7) + 1
                examWeek == week
            } catch (_: Exception) { false }
        }.map { it.toScheduleEvent() }

        val allEvents = weekCourses + weekExams
        return detect(allEvents).map { it.copy(week = week) }
    }

    private fun timeToPeriod(time: String): Int {
        val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: 9
        return when {
            hour < 10 -> 1
            hour < 12 -> 3
            hour < 14 -> 5
            hour < 16 -> 7
            hour < 18 -> 9
            hour < 20 -> 11
            else -> 13
        }
    }
}
