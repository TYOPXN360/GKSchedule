package com.ty.gkschedule.data

/**
 * 实训周判定。
 *
 * 规则：第 10 周以后（>= 11 周），同一门课程（按课程名归并，不区分教室）在一周内
 * 有 3 天及以上，每天都能凑出 4 节及以上连续课程，则该周为实训周。
 * 这 3 天不需要相邻；同一天被拆成多个相邻课块（如 1-2 节 + 3-4 节）会先合并再判断。
 */
object TrainingWeekDetector {
    private const val MIN_WEEK = 11
    private const val MIN_DAYS = 3
    private const val MIN_CONSECUTIVE_PERIODS = 4

    fun isTrainingWeek(courses: List<Course>, week: Int): Boolean {
        if (week < MIN_WEEK) return false
        return courses
            .filter { !it.isHidden && it.name.isNotBlank() && it.isInWeek(week) }
            .groupBy { it.name.trim() }
            .any { (_, sameCourse) ->
                sameCourse.groupBy { it.dayOfWeek }
                    .count { (_, dayCourses) -> longestConsecutiveRun(dayCourses) >= MIN_CONSECUTIVE_PERIODS } >= MIN_DAYS
            }
    }

    /** 合并同一天同一门课的相邻节次（不区分教室），返回最长连续节数。 */
    private fun longestConsecutiveRun(dayCourses: List<Course>): Int {
        val intervals = dayCourses.map { it.startPeriod to it.endPeriod() }.sortedBy { it.first }
        var best = 0
        var runStart = -1
        var runEnd = -1
        for ((start, end) in intervals) {
            when {
                runStart < 0 -> {
                    runStart = start
                    runEnd = end
                }
                start <= runEnd + 1 -> {
                    if (end > runEnd) runEnd = end
                }
                else -> {
                    best = maxOf(best, runEnd - runStart + 1)
                    runStart = start
                    runEnd = end
                }
            }
        }
        if (runStart >= 0) best = maxOf(best, runEnd - runStart + 1)
        return best
    }
}
