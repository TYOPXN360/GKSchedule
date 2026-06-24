package com.classapp.schedule.api

import com.classapp.schedule.data.Course

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

        return groups.mapNotNull { (_, entries) ->
            if (entries.isEmpty()) return@mapNotNull null

            val first = entries.first()
            // Each group has the same section, collect all weeks
            val allWeeks = entries.map { it.week }.distinct().sorted()
            val weekRange = buildWeekRange(allWeeks)

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

    private fun mergeConsecutive(sorted: List<Int>): List<List<Int>> {
        if (sorted.isEmpty()) return emptyList()
        val result = mutableListOf<MutableList<Int>>()
        var current = mutableListOf(sorted[0])
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current.add(sorted[i])
            } else {
                result.add(current)
                current = mutableListOf(sorted[i])
            }
        }
        result.add(current)
        return result
    }

    private fun buildWeekRange(weeks: List<Int>): String {
        if (weeks.isEmpty()) return "all"
        // Check if ALL weeks are consecutive odd or consecutive even
        val allOdd = weeks.all { it % 2 == 1 }
        val allEven = weeks.all { it % 2 == 0 }
        // Only use odd/even if the range covers a significant portion
        // Otherwise just build the range
        if (allOdd && weeks.size > 8) return "odd"
        if (allEven && weeks.size > 8) return "even"
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
