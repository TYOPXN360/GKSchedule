package com.ty.gkschedule.api

import com.ty.gkschedule.data.Course

/**
 * Converts remote API courses to local Course entities.
 * Groups by (courseName, dayWeek, teacher, classroomName) — ignores singleOrDoubleWeek.
 * Merges consecutive periods into one course block.
 */
object CourseImporter {

    fun convertRemoteCourses(remoteCourses: List<RemoteCourse>): List<Course> {
        // Group by course identity INCLUDING time slot
        val groups = remoteCourses.groupBy { r ->
            "${r.courseName}|${r.dayWeek}|${r.teacher}|${r.classroomName}|${r.whichSection}"
        }

        val semesterMaxWeek = remoteCourses.maxOfOrNull { it.week } ?: 0
        return groups.mapNotNull { (_, entries) ->
            if (entries.isEmpty()) return@mapNotNull null

            val first = entries.first()
            // Each group has the same section, collect all weeks
            val allWeeks = entries.map { it.week }.filter { it > 0 }.distinct().sorted()
            // ponytail: 精确周次优先，singleOrDoubleWeek 只兜底——它会把"1,5,17"这类子集泛化成整学期单周，
            // 于是同一时段另一教室的同名课周次被抹平，两条在课表页重叠
            val weekRange = if (allWeeks.isEmpty()) {
                when (entries.firstNotNullOfOrNull { it.singleOrDoubleWeek?.trim() }) {
                    "单" -> "odd"
                    "双" -> "even"
                    else -> "all"
                }
            } else {
                buildWeekRange(allWeeks, semesterMaxWeek)
            }

            Course(
                name = first.courseName,
                teacher = first.teacher,
                classroom = first.classroomName,
                dayOfWeek = first.dayWeek,
                startPeriod = first.whichSection,
                periods = 1, // Will be merged below
                colorIndex = 0, // Assigned dynamically by CourseColors
                weekRange = weekRange
            )
        }.let { courses ->
            // Merge consecutive periods with same weekRange into one block
            mergeConsecutiveCourses(courses)
        }
    }

    private fun mergeConsecutiveCourses(courses: List<Course>): List<Course> {
        // Group by (name, day, teacher, classroom, weekRange)
        val groups = courses.groupBy { "${it.name}|${it.dayOfWeek}|${it.teacher}|${it.classroom}|${it.weekRange}" }
        return groups.flatMap { (_, courseList) ->
            val sorted = courseList.sortedBy { it.startPeriod }
            val merged = mutableListOf<Course>()
            var current = sorted.first()
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (next.startPeriod == current.startPeriod + current.periods) {
                    // Consecutive, merge
                    current = current.copy(periods = current.periods + 1)
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            merged
        }
    }

    private fun buildWeekRange(weeks: List<Int>, semesterMaxWeek: Int): String {
        if (weeks.isEmpty()) return "all"
        if (semesterMaxWeek > 0) {
            if (weeks == (1..semesterMaxWeek).toList()) return "all"
            // ponytail: 只有覆盖整学期的完整单/双周才配预设；子集必须逐周保留，
            // 否则 {1,5,17} 会被写成 odd，与另一教室的同名课在同一周叠在一起
            if (weeks == (1..semesterMaxWeek step 2).toList()) return "odd"
            if (weeks == (2..semesterMaxWeek step 2).toList()) return "even"
        }
        return buildCompactRange(weeks)
    }

    private fun buildCompactRange(sorted: List<Int>): String {
        if (sorted.isEmpty()) return "all"
        val ranges = mutableListOf<String>()
        var start = sorted[0]
        var end = sorted[0]
        for (i in 1 until sorted.size) {
            if (sorted[i] == end + 1) {
                end = sorted[i]
            } else {
                ranges.add(if (start == end) "$start" else "$start-$end")
                start = sorted[i]
                end = sorted[i]
            }
        }
        ranges.add(if (start == end) "$start" else "$start-$end")
        return ranges.joinToString(",")
    }
}
