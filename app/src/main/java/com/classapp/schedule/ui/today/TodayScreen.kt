package com.classapp.schedule.ui.today

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.data.ScheduleResolver
import com.classapp.schedule.ui.theme.Md3Card
import com.classapp.schedule.ui.theme.Md3CardVariant
import com.classapp.schedule.util.CourseColors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TodayScreen(
    courses: List<Course>,
    currentWeek: Int,
    colorEngine: Int = 0,
    colorGroupMode: Int = 2,
    exams: List<com.classapp.schedule.data.ExamEntity> = emptyList(),
    showExamSchedule: Boolean = false,
    examLookaheadWeeks: Int = 2,
    semesterStart: java.time.LocalDate = java.time.LocalDate.now(),
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String,
    onCourseLongPress: (Course) -> Unit,
    onExamEdit: (com.classapp.schedule.data.ExamEntity) -> Unit = {},
    diffColorPerWeek: Boolean = false
) {
    val today = LocalDate.now()
    val todayDow = today.dayOfWeek.value

    val todayCourses = remember(courses, currentWeek, today) {
        ScheduleResolver.todayCourses(courses, currentWeek, today)
    }

    val todayExams = remember(exams, showExamSchedule, today) {
        ScheduleResolver.todayExams(exams, showExamSchedule, today)
    }

    val allTodayCourses = todayCourses

    val tomorrowCourses = remember(courses, currentWeek, today) {
        ScheduleResolver.tomorrowCourses(courses, currentWeek, today)
    }

    val dayNames = mapOf(
        1 to stringResource(R.string.mon), 2 to stringResource(R.string.tue),
        3 to stringResource(R.string.wed), 4 to stringResource(R.string.thu),
        5 to stringResource(R.string.fri), 6 to stringResource(R.string.sat),
        7 to stringResource(R.string.sun)
    )

    val now = LocalTime.now()
    val currentTimeMinutes = now.hour * 60 + now.minute
    val currentPeriod = findCurrentPeriod(allTodayCourses, getStartTime, getEndTime, currentTimeMinutes)
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    var detailExam by remember { mutableStateOf<com.classapp.schedule.data.ScheduleItem.ExamItem?>(null) }
    // Only trigger animation once per app session, not on course refresh
    var animationPlayed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val maxStagger = (allTodayCourses.size - 1) * 200L
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(allTodayCourses.size) {
        if (!animationPlayed) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it == androidx.lifecycle.Lifecycle.State.RESUMED }
            kotlinx.coroutines.delay(500 + maxStagger + 700)
            animationPlayed = true
        }
    }
    val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
    val upcomingExams = remember(exams, showExamSchedule, today, examLookaheadWeeks) {
        ScheduleResolver.upcomingExams(exams, showExamSchedule, today, examLookaheadWeeks)
    }

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
                style = MaterialTheme.typography.headlineMedium
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
        if (allTodayCourses.isEmpty()) {
            item {
                EmptyCard(stringResource(R.string.no_course_today))
            }
        } else {
            // Pre-compute stagger delays for all courses (by progress, highest first)
            val coursesWithProgress = allTodayCourses.map { course ->
                val startMins = parseTime(course.getActualStartTime(getStartTime))
                val endMins = parseTime(course.getActualEndTime(getEndTime))
                val nowMins = LocalTime.now().hour * 60 + LocalTime.now().minute
                val p = when {
                    nowMins > endMins -> 1f
                    nowMins < startMins -> 0f
                    else -> ((nowMins - startMins).toFloat() / (endMins - startMins)).coerceIn(0f, 1f)
                }
                course.id to p
            }.sortedByDescending { it.second }

            val staggerMap = coursesWithProgress.mapIndexed { index, (id, _) ->
                id to index * 200L
            }.toMap()

            items(allTodayCourses) { course ->
                val isCurrent = currentPeriod in course.startPeriod..course.endPeriod()
                val currentTimeMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
                val courseStartMinutes = parseTime(course.getActualStartTime(getStartTime))
                val courseEndMinutes = parseTime(course.getActualEndTime(getEndTime))
                val isPast = currentTimeMinutes > courseEndMinutes
                val isFuture = currentTimeMinutes < courseStartMinutes
                val firstFuture = allTodayCourses.firstOrNull {
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
                    animDelay = if (animationPlayed) 0L else staggerMap[course.id] ?: 0L,
                    skipAnim = animationPlayed,
                    barColor = CourseColors.getColorSync(colorGroupMode, course.name, course.classroom, week = currentWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark).container,
                    indicatorColor = CourseColors.getColorSync(colorGroupMode, course.name, course.classroom, week = currentWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark).content,
                    onClick = { detailCourse = course }
                )
            }
        }

        // Today's exams (native ExamCard, no Course conversion)
        if (todayExams.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = "今日考试", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("今日考试", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(todayExams) { exam ->
                val examWeek = ScheduleResolver.examWeek(exam, semesterStart, currentWeek)
                val examColor = CourseColors.getColorSync(colorGroupMode, exam.courseName, exam.classroom, week = examWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark)
                ExamCard(
                    exam = exam,
                    examColor = examColor,
                    onClick = { detailExam = ScheduleResolver.examItem(exam, semesterStart, getStartTime, getEndTime) }
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
                            Icons.Default.WbSunny, contentDescription = "明日课程",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tomorrow_title),
                            style = MaterialTheme.typography.titleMedium
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
                    barColor = CourseColors.getColorSync(colorGroupMode, course.name, course.classroom, week = currentWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark).container,
                    indicatorColor = CourseColors.getColorSync(colorGroupMode, course.name, course.classroom, week = currentWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark).content,
                        onClick = { detailCourse = course }
                    )
                }
            }
        }

        // Upcoming exams section
        if (upcomingExams.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.School, contentDescription = "考试",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "近期考试",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(upcomingExams) { exam ->
                val examWeek = ScheduleResolver.examWeek(exam, semesterStart, currentWeek)
                val examColor = CourseColors.getColorSync(colorGroupMode, exam.courseName, exam.classroom, week = examWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark)
                ExamCard(
                    exam = exam,
                    examColor = examColor,
                    onClick = { detailExam = ScheduleResolver.examItem(exam, semesterStart, getStartTime, getEndTime) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Detail sheet
    detailCourse?.let { course ->
        com.classapp.schedule.ui.weekly.ScheduleItemDetailSheet(
            item = com.classapp.schedule.data.ScheduleItem.CourseItem(course),
            getStartTime = getStartTime,
            getEndTime = getEndTime,
            onDismiss = { detailCourse = null },
            onEdit = { detailCourse = null; onCourseLongPress(course) },
            courseColors = CourseColors.getColors(colorEngine, count = 8),
            colorGroupMode = colorGroupMode,
            currentWeek = currentWeek,
            diffColorPerWeek = diffColorPerWeek
        )
    }

    detailExam?.let { item ->
        com.classapp.schedule.ui.weekly.ScheduleItemDetailSheet(
            item = item,
            getStartTime = getStartTime,
            getEndTime = getEndTime,
            onDismiss = { detailExam = null },
            onEdit = {
                detailExam = null
                onExamEdit(item.exam)
            },
            courseColors = CourseColors.getColors(colorEngine, count = 8),
            colorGroupMode = colorGroupMode,
            colorIndex = 0,
            currentWeek = item.weekRange.toIntOrNull() ?: currentWeek,
            diffColorPerWeek = diffColorPerWeek
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
                Icons.Default.Schedule, contentDescription = "暂无课程",
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
    animDelay: Long = 0,
    skipAnim: Boolean = false,
    barColor: Color = MaterialTheme.colorScheme.outline,
    indicatorColor: Color = barColor,
    onClick: () -> Unit
) {
    // Real-time isPast check — only for today's courses, updates every 30 seconds
    var realIsPast by remember { mutableStateOf(isPast) }
    val isTodayCourse = isCurrent || isPast
    LaunchedEffect(isTodayCourse) {
        if (isTodayCourse) {
            while (true) {
                val now = LocalTime.now()
                val endMins = parseTime(endTime)
                val nowMins = now.hour * 60 + now.minute
                realIsPast = nowMins > endMins
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    // Progress calculation
    val progress = when {
        isCurrent -> {
            val now = LocalTime.now()
            val startMins = parseTime(startTime)
            val endMins = parseTime(endTime)
            val nowMins = now.hour * 60 + now.minute
            ((nowMins - startMins).toFloat() / (endMins - startMins)).coerceIn(0f, 1f)
        }
        realIsPast -> 1f
        else -> 0f
    }

    // Animated progress — starts from 0, animates to target after splash screen
    // rememberSaveable keeps state across tab switches
    var startAnimation by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(skipAnim) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(skipAnim) {
        if (!skipAnim) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it == androidx.lifecycle.Lifecycle.State.RESUMED }
            kotlinx.coroutines.delay(animDelay)
            startAnimation = true
        }
    }
    val animSpec = androidx.compose.animation.core.tween<Float>(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) progress else 0f,
        animationSpec = animSpec,
        label = "progress"
    )
    val animDone by remember { derivedStateOf { startAnimation && progress > 0f && (progress - animatedProgress) < 0.01f } }

    Md3Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = Md3CardVariant.Elevated,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(CircleShape).background(barColor)) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(animatedProgress).clip(CircleShape).background(indicatorColor.copy(alpha = 0.2f))) {}
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (isNext) {
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.next_course), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(course.name, style = MaterialTheme.typography.titleMedium)
                    if (realIsPast) {
                        androidx.compose.animation.AnimatedVisibility(visible = animDone, enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn()) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val periodText = if (course.periods > 1) "${course.startPeriod}-${course.endPeriod()}" else "${course.startPeriod}"
                    Text(stringResource(R.string.period_format_short, periodText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    if (course.teacher.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); Text(course.teacher, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (course.classroom.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); Text(course.classroom, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Text("$startTime\n$endTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun ExamCard(exam: com.classapp.schedule.data.ExamEntity, examColor: com.classapp.schedule.util.CourseColors.CourseColorPair, onClick: () -> Unit = {}) {
    val examDate = try { java.time.LocalDate.parse(exam.examDate) } catch (_: Exception) { null }
    val now = java.time.LocalDate.now()
    val today = examDate != null && examDate == now

    // Parse exam time range for time-aware state
    val timeParts = exam.examTimeRange.split("-")
    val startMins = if (timeParts.size == 2) { val p = timeParts[0].trim().split(":"); p.getOrNull(0)?.toIntOrNull()?.let { h -> h * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) } } else null
    val endMins = if (timeParts.size == 2) { val p = timeParts[1].trim().split(":"); p.getOrNull(0)?.toIntOrNull()?.let { h -> h * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) } } else null
    val nowMins = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute

    val isPast = when {
        examDate?.isBefore(now) == true -> true
        today && endMins != null && nowMins > endMins -> true
        else -> false
    }
    val isNow = today && startMins != null && endMins != null && nowMins in startMins..endMins
    val alpha = if (isPast) 0.5f else 1f
    val daysLeft = if (examDate != null && !isPast && !isNow) java.time.temporal.ChronoUnit.DAYS.between(now, examDate) else -1L
    val progress = if (isPast) {
        1f
    } else if (isNow) {
        val start = startMins ?: 0
        val end = endMins ?: start
        val duration = end - start
        if (duration > 0) ((nowMins - start).toFloat() / duration).coerceIn(0f, 1f) else 0f
    } else {
        0f
    }

    com.classapp.schedule.ui.theme.Md3Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        variant = com.classapp.schedule.ui.theme.Md3CardVariant.Elevated,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (isPast) examColor.container.copy(alpha = 0.2f) else examColor.container)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exam.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isPast) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("已结束", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (isNow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = CircleShape, color = examColor.container) {
                                Text("进行中", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = examColor.content)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (exam.examDate.isNotEmpty()) { Text(exam.examDate + " " + exam.examTimeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
                        if (exam.classroom.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); Text(exam.classroom, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
                    }
                    if (exam.examMethod.isNotEmpty()) { Text(exam.examMethod, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)) }
                }
                if (!isPast && !isNow && daysLeft >= 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = if (daysLeft == 0L) MaterialTheme.colorScheme.errorContainer else examColor.container,
                        contentColor = if (daysLeft == 0L) MaterialTheme.colorScheme.onErrorContainer else examColor.content) {
                        Text(if (daysLeft == 0L) "今天" else "剩 ${daysLeft} 天", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            // Progress bar for in-progress or recently finished exams
            if (isNow || (isPast && today)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    color = examColor.container,
                    trackColor = examColor.container.copy(alpha = 0.15f)
                )
            }
        }
    }
}
