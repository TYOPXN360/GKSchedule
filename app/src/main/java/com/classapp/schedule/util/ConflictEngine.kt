package com.classapp.schedule.util

import com.classapp.schedule.data.Course
import com.classapp.schedule.data.ExamEntity

/**
 * 课程/考试冲突检测引擎
 *
 * 检测同一时段的课程+考试重叠，返回冲突列表。
 * 不删除数据，只标记冲突供 UI 展示 ⚠️ 标识。
 */
object ConflictEngine {

    /** 冲突类型 */
    enum class ConflictType { COURSE_EXAM, EXAM_EXAM, COURSE_COURSE }

    /** 冲突事件对 */
    data class Conflict(
        val type: ConflictType,
        val eventName: String,
        val eventDetails: String,
        val conflictWith: String,
        val conflictDetails: String,
        val dayOfWeek: Int,
        val startPeriod: Int,
        val endPeriod: Int
    )

    /**
     * 检测课程和考试之间的冲突
     * @param courses 本周课程列表
     * @param examCourses 从 ExamEntity 转换来的"假课程"列表（用于时间比较）
     * @return 冲突列表
     */
    fun detectConflicts(
        courses: List<Course>,
        examCourses: List<Course>,
        semesterStart: java.time.LocalDate
    ): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()

        // 课程 vs 考试冲突
        for (course in courses) {
            for (exam in examCourses) {
                if (course.dayOfWeek == exam.dayOfWeek && periodsOverlap(course, exam)) {
                    conflicts.add(
                        Conflict(
                            type = ConflictType.COURSE_EXAM,
                            eventName = course.name,
                            eventDetails = "第${course.startPeriod}-${course.startPeriod + course.periods - 1}节 ${course.classroom}",
                            conflictWith = exam.name,
                            conflictDetails = "第${exam.startPeriod}-${exam.startPeriod + exam.periods - 1}节 ${exam.classroom}",
                            dayOfWeek = course.dayOfWeek,
                            startPeriod = minOf(course.startPeriod, exam.startPeriod),
                            endPeriod = maxOf(course.startPeriod + course.periods, exam.startPeriod + exam.periods)
                        )
                    )
                }
            }
        }

        // 考试 vs 考试冲突
        for (i in examCourses.indices) {
            for (j in i + 1 until examCourses.size) {
                val a = examCourses[i]
                val b = examCourses[j]
                if (a.dayOfWeek == b.dayOfWeek && periodsOverlap(a, b)) {
                    conflicts.add(
                        Conflict(
                            type = ConflictType.EXAM_EXAM,
                            eventName = a.name,
                            eventDetails = "第${a.startPeriod}-${a.startPeriod + a.periods - 1}节 ${a.classroom}",
                            conflictWith = b.name,
                            conflictDetails = "第${b.startPeriod}-${b.startPeriod + b.periods - 1}节 ${b.classroom}",
                            dayOfWeek = a.dayOfWeek,
                            startPeriod = minOf(a.startPeriod, b.startPeriod),
                            endPeriod = maxOf(a.startPeriod + a.periods, b.startPeriod + b.periods)
                        )
                    )
                }
            }
        }

        return conflicts.distinctBy { "${it.eventName}|${it.conflictWith}|${it.dayOfWeek}|${it.startPeriod}" }
    }

    /**
     * 检查两门课的时间段是否重叠
     */
    private fun periodsOverlap(a: Course, b: Course): Boolean {
        val aEnd = a.startPeriod + a.periods
        val bEnd = b.startPeriod + b.periods
        return a.startPeriod < bEnd && b.startPeriod < aEnd
    }

    /**
     * 格式化冲突提示文本
     */
    fun formatConflictHint(conflict: Conflict): String {
        return when (conflict.type) {
            ConflictType.COURSE_EXAM -> "⚠ ${conflict.eventName} 与考试「${conflict.conflictWith}」时间冲突"
            ConflictType.EXAM_EXAM -> "⚠ 两场考试时间冲突：「${conflict.eventName}」vs「${conflict.conflictWith}」"
            ConflictType.COURSE_COURSE -> "⚠ ${conflict.eventName} 与 ${conflict.conflictWith} 时间冲突"
        }
    }
}
