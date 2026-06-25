package com.classapp.schedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.util.CourseColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(
    courses: List<Course>,
    currentWeek: Int,
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String,
    onCourseLongPress: (Course) -> Unit
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayDow = today.dayOfWeek.value
    val tomorrowDow = tomorrow.dayOfWeek.value

    // Calculate tomorrow's week (might be next week if today is Sunday)
    val tomorrowWeek = if (todayDow == 7) currentWeek + 1 else currentWeek

    val todayCourses = courses
        .filter { it.dayOfWeek == todayDow && it.isInWeek(currentWeek) }
        .sortedBy { it.startPeriod }

    val tomorrowCourses = courses
        .filter { it.dayOfWeek == tomorrowDow && tomorrowWeek in 1..52 && it.isInWeek(tomorrowWeek) }
        .sortedBy { it.startPeriod }

    val dayNames = mapOf(
        1 to stringResource(R.string.mon), 2 to stringResource(R.string.tue),
        3 to stringResource(R.string.wed), 4 to stringResource(R.string.thu),
        5 to stringResource(R.string.fri), 6 to stringResource(R.string.sat),
        7 to stringResource(R.string.sun)
    )

    val now = LocalTime.now()
    val currentTimeMinutes = now.hour * 60 + now.minute
    val currentPeriod = findCurrentPeriod(todayCourses, getStartTime, getEndTime, currentTimeMinutes)
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    val uniqueCourseCount = remember(courses) { courses.map { it.name }.distinct().size }
    val monetColors = com.classapp.schedule.util.CourseColors.getColors(0, count = uniqueCourseCount.coerceAtLeast(8))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.today_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.date_format,
                    dayNames[todayDow] ?: "", today.monthValue, today.dayOfMonth),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.week_format, currentWeek),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Today's courses
        if (todayCourses.isEmpty()) {
            item {
                EmptyCard(stringResource(R.string.no_course_today))
            }
        } else {
            items(todayCourses) { course ->
                val isCurrent = currentPeriod in course.startPeriod..course.endPeriod()
                // "Next" = first future course (start time is after current time)
                val currentTimeMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
                val courseStartMinutes = parseTime(course.getActualStartTime(getStartTime))
                val courseEndMinutes = parseTime(course.getActualEndTime(getEndTime))
                val isPast = currentTimeMinutes > courseEndMinutes
                val isFuture = currentTimeMinutes < courseStartMinutes
                // Find the first future course
                val firstFuture = todayCourses.firstOrNull {
                    parseTime(it.getActualStartTime(getStartTime)) > currentTimeMinutes
                }
                val isNext = !isCurrent && !isPast && isFuture && firstFuture?.id == course.id

                CourseCard(
                    course = course,
                    startTime = course.getActualStartTime(getStartTime),
                    endTime = course.getActualEndTime(getEndTime),
                    isCurrent = isCurrent,
                    isNext = isNext,
                    isPast = isPast,
                    onClick = { detailCourse = course }
                )
            }
        }

        // Tomorrow preview — only show between 16:00 and 23:59
        if (currentTimeMinutes >= 960) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WbSunny, contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tomorrow_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (tomorrowCourses.isEmpty()) {
                item {
                    EmptyCard(stringResource(R.string.no_course_tomorrow))
                }
            } else {
                items(tomorrowCourses) { course ->
                    CourseCard(
                        course = course,
                        startTime = course.getActualStartTime(getStartTime),
                        endTime = course.getActualEndTime(getEndTime),
                        isCurrent = false,
                        isNext = false,
                        onClick = { detailCourse = course }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Detail sheet
    detailCourse?.let { course ->
        com.classapp.schedule.ui.weekly.CourseDetailSheet(
            course = course,
            getStartTime = getStartTime,
            getEndTime = getEndTime,
            onDismiss = { detailCourse = null },
            onEdit = { detailCourse = null; onCourseLongPress(course) }
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Schedule, contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    startTime: String,
    endTime: String,
    isCurrent: Boolean,
    isNext: Boolean,
    isPast: Boolean = false,
    onClick: () -> Unit
) {
    // Progress calculation for current course
    val progress = if (isCurrent) {
        val now = LocalTime.now()
        val startMins = parseTime(startTime)
        val endMins = parseTime(endTime)
        val nowMins = now.hour * 60 + now.minute
        ((nowMins - startMins).toFloat() / (endMins - startMins)).coerceIn(0f, 1f)
    } else 0f

    // Animated progress
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        if (isCurrent) {
            // Animate from 0 to current progress on first display
            kotlinx.coroutines.delay(300)
            animatedProgress = progress
        }
    }

    val colors = CourseColors.getColors(0, count = 32)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Progress overlay for current course
            if (isCurrent && animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress)
                        .matchParentSize()
                        .background(
                            CourseColors.getBackground(course.colorIndex, colors).copy(alpha = 0.3f)
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CourseColors.getTextColor(course.colorIndex, colors))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall) }
                            )
                        } else if (isNext) {
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.next_course), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        if (isPast) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val periodText = if (course.periods > 1)
                            "${course.startPeriod}-${course.endPeriod()}"
                        else "${course.startPeriod}"
                        Text(
                            text = stringResource(R.string.period_format_short, periodText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        if (course.teacher.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(course.teacher, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (course.classroom.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(course.classroom, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Time
                Text(
                    text = "$startTime\n$endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun findCurrentPeriod(
    courses: List<Course>,
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String,
    currentTimeMinutes: Int
): Int {
    for (course in courses) {
        val start = parseTime(course.getActualStartTime(getStartTime))
        val end = parseTime(course.getActualEndTime(getEndTime))
        if (currentTimeMinutes in start..end) return course.startPeriod
    }
    return 0
}

private fun parseTime(time: String): Int {
    val parts = time.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}
