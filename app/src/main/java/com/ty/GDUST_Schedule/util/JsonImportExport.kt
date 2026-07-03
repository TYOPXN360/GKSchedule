package com.ty.GDUST_Schedule.util

import com.ty.GDUST_Schedule.data.Course
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class CourseExportData(
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periods: Int = 1,
    val colorIndex: Int = 0,
    val weekRange: String = "all",
    val remark: String = "",
    val isCustomTime: Boolean = false,
    val customStartTime: String = "",
    val customEndTime: String = ""
) {
    fun toCourse(): Course = Course(
        name = name, teacher = teacher, classroom = classroom,
        dayOfWeek = dayOfWeek, startPeriod = startPeriod,
        periods = periods, colorIndex = colorIndex, weekRange = weekRange,
        remark = remark, isCustomTime = isCustomTime,
        customStartTime = customStartTime, customEndTime = customEndTime
    )
}

@Serializable
data class ScheduleExport(
    val version: Int = 2,
    val courses: List<CourseExportData>
)

object JsonImportExport {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun exportToJson(courses: List<Course>): String {
        val exportData = ScheduleExport(
            courses = courses.map { c ->
                CourseExportData(
                    name = c.name, teacher = c.teacher, classroom = c.classroom,
                    dayOfWeek = c.dayOfWeek, startPeriod = c.startPeriod,
                    periods = c.periods, colorIndex = c.colorIndex, weekRange = c.weekRange,
                    remark = c.remark, isCustomTime = c.isCustomTime,
                    customStartTime = c.customStartTime, customEndTime = c.customEndTime
                )
            }
        )
        return json.encodeToString(exportData)
    }

    fun importFromJson(jsonString: String): List<Course> {
        val trimmed = jsonString.trim()
        return if (trimmed.startsWith("[")) {
            // Bare array format (SchedU compatible)
            json.decodeFromString<List<CourseExportData>>(trimmed).map { it.toCourse() }
        } else {
            // Wrapped format
            json.decodeFromString<ScheduleExport>(trimmed).courses.map { it.toCourse() }
        }
    }
}

/** 导出课程为 ICS 日历格式 */
object IcsExport {
    fun exportToIcs(
        courses: List<Course>,
        semesterStart: LocalDate,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//GDUST-Schedule//Schedule//EN")
        sb.appendLine("CALSCALE:GREGORIAN")

        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        courses.forEach { course ->
            val weekList = course.getWeekList().ifEmpty { (1..20).toList() }
            weekList.forEach { week ->
                val dayOffset = (week - 1) * 7 + (course.dayOfWeek - 1)
                val date = semesterStart.plusDays(dayOffset.toLong())

                val startTimeStr = course.getActualStartTime(getStartTime)
                val endTimeStr = course.getActualEndTime(getEndTime)

                val dtStart = "${date.format(dateFormatter)}T${startTimeStr.replace(":", "")}00"
                val dtEnd = "${date.format(dateFormatter)}T${endTimeStr.replace(":", "")}00"

                sb.appendLine("BEGIN:VEVENT")
                sb.appendLine("DTSTART:$dtStart")
                sb.appendLine("DTEND:$dtEnd")
                sb.appendLine("SUMMARY:${course.name}")
                if (course.classroom.isNotEmpty()) sb.appendLine("LOCATION:${course.classroom}")
                if (course.teacher.isNotEmpty()) sb.appendLine("DESCRIPTION:Teacher: ${course.teacher}")
                sb.appendLine("END:VEVENT")
            }
        }

        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }
}
