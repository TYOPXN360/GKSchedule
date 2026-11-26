package com.ty.gkschedule.util

import com.ty.gkschedule.data.Course
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ponytail: 3 起往返无损——补上隐藏/手动编辑标记和超星绑定；老文件缺字段走默认值继续兼容
private const val EXPORT_VERSION = 3

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
    val customEndTime: String = "",
    // v3 新增：老文件没有这些键，靠默认值补齐，导入不会报错
    val isManuallyEdited: Boolean = false,
    val isHidden: Boolean = false,
    val chaoxingClassId: Int = 0,
    val chaoxingCourseId: Long = 0L,
    val chaoxingFid: Int = 0
) {
    fun toCourse(): Course = Course(
        name = name, teacher = teacher, classroom = classroom,
        dayOfWeek = dayOfWeek, startPeriod = startPeriod,
        periods = periods, colorIndex = colorIndex, weekRange = weekRange,
        remark = remark, isCustomTime = isCustomTime,
        customStartTime = customStartTime, customEndTime = customEndTime,
        isManuallyEdited = isManuallyEdited,
        isHidden = isHidden,
        chaoxingClassId = chaoxingClassId,
        chaoxingCourseId = chaoxingCourseId,
        chaoxingFid = chaoxingFid
    )
}

@Serializable
data class ScheduleExport(
    val version: Int = EXPORT_VERSION,
    val courses: List<CourseExportData>
)

object JsonImportExport {
    const val CURRENT_VERSION = EXPORT_VERSION

    // encodeDefaults: version=3 这类与默认值相同的字段也要写出来，否则文件里看不到版本号
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    sealed interface ImportResult {
        data class Success(val courses: List<Course>) : ImportResult
        data class Failure(val reason: Reason, val fileVersion: Int = 0) : ImportResult
        enum class Reason { EMPTY_FILE, MALFORMED, NEWER_VERSION, NO_COURSES, INVALID_COURSES }
    }

    fun exportToJson(courses: List<Course>): String {
        val exportData = ScheduleExport(
            version = CURRENT_VERSION,
            courses = courses.map { c ->
                CourseExportData(
                    name = c.name, teacher = c.teacher, classroom = c.classroom,
                    dayOfWeek = c.dayOfWeek, startPeriod = c.startPeriod,
                    periods = c.periods, colorIndex = c.colorIndex, weekRange = c.weekRange,
                    remark = c.remark, isCustomTime = c.isCustomTime,
                    customStartTime = c.customStartTime, customEndTime = c.customEndTime,
                    isManuallyEdited = c.isManuallyEdited,
                    isHidden = c.isHidden,
                    chaoxingClassId = c.chaoxingClassId,
                    chaoxingCourseId = c.chaoxingCourseId,
                    chaoxingFid = c.chaoxingFid
                )
            }
        )
        return json.encodeToString(exportData)
    }

    fun importFromJson(jsonString: String): ImportResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return ImportResult.Failure(ImportResult.Reason.EMPTY_FILE)
        return try {
            val courses = if (trimmed.startsWith("[")) {
                // Bare array format (SchedU compatible)
                json.decodeFromString<List<CourseExportData>>(trimmed)
            } else {
                val export = json.decodeFromString<ScheduleExport>(trimmed)
                if (export.version > CURRENT_VERSION) {
                    return ImportResult.Failure(ImportResult.Reason.NEWER_VERSION, export.version)
                }
                export.courses
            }
            when {
                courses.isEmpty() -> ImportResult.Failure(ImportResult.Reason.NO_COURSES)
                courses.any { !it.isValid() } -> ImportResult.Failure(ImportResult.Reason.INVALID_COURSES)
                else -> ImportResult.Success(courses.map { it.toCourse() })
            }
        } catch (_: Exception) {
            ImportResult.Failure(ImportResult.Reason.MALFORMED)
        }
    }

    private fun CourseExportData.isValid(): Boolean =
        name.isNotBlank() && dayOfWeek in 1..7 && startPeriod >= 1 && periods >= 1
}

/** 导出课程为 ICS 日历格式（RFC 5545） */
object IcsExport {
    private val dtStampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun exportToIcs(
        courses: List<Course>,
        semesterStart: LocalDate,
        totalWeeks: Int,
        getStartTime: (Int) -> String,
        getEndTime: (Int) -> String
    ): String {
        val sb = StringBuilder()
        fun line(value: String) {
            sb.append(fold(value)).append("\r\n")
        }

        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//GK-Schedule//Schedule//EN")
        line("CALSCALE:GREGORIAN")
        line("METHOD:PUBLISH")
        val dtStamp = ZonedDateTime.now(ZoneOffset.UTC).format(dtStampFormatter)

        // 隐藏课程不进日历；JSON 仍然全量导出，保留隐藏状态
        courses.filter { !it.isHidden }.forEach { course ->
            val weeks = course.getWeekList()
                .ifEmpty { (1..totalWeeks).toList() }
                .filter { it in 1..totalWeeks }
                .distinct()
                .sorted()
            val startTime = icsTime(course.getActualStartTime(getStartTime)) ?: return@forEach
            val endTime = icsTime(course.getActualEndTime(getEndTime)) ?: return@forEach

            weeks.forEach { week ->
                val date = semesterStart.plusDays(weekDateOffset(week, course.dayOfWeek).toLong())
                val uidSeed = "${course.name}|${course.dayOfWeek}|${course.startPeriod}|$semesterStart|$week"
                line("BEGIN:VEVENT")
                line("UID:${Integer.toHexString(uidSeed.hashCode())}-$week@gkschedule.local")
                line("DTSTAMP:$dtStamp")
                line("DTSTART:${date.format(dateFormatter)}T$startTime")
                line("DTEND:${date.format(dateFormatter)}T$endTime")
                line("SUMMARY:${escapeText(course.name)}")
                if (course.classroom.isNotEmpty()) line("LOCATION:${escapeText(course.classroom)}")
                val description = listOfNotNull(
                    course.teacher.takeIf { it.isNotEmpty() }?.let { "Teacher: $it" },
                    course.remark.takeIf { it.isNotEmpty() }
                ).joinToString("\n")
                if (description.isNotEmpty()) line("DESCRIPTION:${escapeText(description)}")
                line("END:VEVENT")
            }
        }

        line("END:VCALENDAR")
        return sb.toString()
    }

    /** 第 week 周、周 dayOfWeek 距学期开始日的天数（学期开始日按该周周一算） */
    private fun weekDateOffset(week: Int, dayOfWeek: Int): Int = (week - 1) * 7 + (dayOfWeek - 1)

    /** "08:30" → "083000"；解析不出来就跳过这条事件，别写进无效时间 */
    private fun icsTime(hhmm: String): String? {
        val parts = hhmm.trim().split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        // 固定 Locale.US：部分区域设置会输出非 ASCII 数字，日历客户端解析不了
        return java.lang.String.format(java.util.Locale.US, "%02d%02d00", hour, minute)
    }

    /** RFC 5545 TEXT 转义：反斜杠、分号、逗号、换行 */
    private fun escapeText(text: String): String = buildString {
        text.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(c)
            }
        }
    }

    /** 按 75 字节折行，续行以单个空格开头；不能把多字节字符劈开 */
    private fun fold(value: String): String {
        if (value.toByteArray(Charsets.UTF_8).size <= 75) return value
        val out = StringBuilder()
        var width = 0
        var firstLine = true
        var i = 0
        while (i < value.length) {
            val codePoint = value.codePointAt(i)
            val size = utf8Size(codePoint)
            val capacity = if (firstLine) 75 else 74 // 续行开头的空格也占 1 字节
            if (width + size > capacity) {
                out.append("\r\n ")
                width = 0
                firstLine = false
            }
            out.appendCodePoint(codePoint)
            width += size
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    private fun utf8Size(codePoint: Int): Int = when {
        codePoint < 0x80 -> 1
        codePoint < 0x800 -> 2
        codePoint < 0x10000 -> 3
        else -> 4
    }
}
