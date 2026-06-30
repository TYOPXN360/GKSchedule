package com.classapp.schedule.ui.today
import com.classapp.schedule.ui.theme.Md3Card

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import androidx.compose.ui.graphics.Color
import com.classapp.schedule.util.CourseColors
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(
    courses: List<Course>,
    currentWeek: Int,
    colorEngine: Int = 0,
    colorGroupMode: Int = 2,
    exams: List<com.classapp.schedule.api.ExamInfo> = emptyList(),
    showExamSchedule: Boolean = false,
    examLookaheadWeeks: Int = 2,
    semesterStart: java.time.LocalDate = java.time.LocalDate.now(),
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String,
    onCourseLongPress: (Course) -> Unit,
    courseColorPalette: List<Pair<Color, Color>> = CourseColors.getColors(0, count = 8),
    courseColorMap: Map<Long, Color> = emptyMap(),
    examColorMap: Map<String, Color> = emptyMap()
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

    // Convert today's exams to Course objects and merge into today's list
    val todayExamCourses = if (showExamSchedule) {
        exams.mapNotNull { exam ->
            try {
                val examDate = java.time.LocalDate.parse(exam.getExamDate())
                if (examDate == today) exam.toTodayCourse(semesterStart, getStartTime, getEndTime) else null
            } catch (_: Exception) { null }
        }
    } else emptyList()
    val allTodayCourses = (todayCourses + todayExamCourses).sortedBy { it.startPeriod }

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
    val currentPeriod = findCurrentPeriod(allTodayCourses, getStartTime, getEndTime, currentTimeMinutes)
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    // Only trigger animation once per app session, not on course refresh
    var animationPlayed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val maxStagger = (allTodayCourses.size - 1) * 200L
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(allTodayCourses.size) {
        if (!animationPlayed) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it == androidx.lifecycle.Lifecycle.State.RESUMED }
            kotlinx.coroutines.delay(500 + maxStagger + 700)
            animationPlayed = true
        }
    }
    val monetColors = courseColorPalette

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
                    barColor = courseColorMap[course.id] ?: monetColors.getOrElse(course.colorIndex.coerceIn(0, monetColors.size - 1)) { monetColors.first() }.first,
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
                        barColor = courseColorMap[course.id] ?: monetColors.getOrElse(course.colorIndex.coerceIn(0, monetColors.size - 1)) { monetColors.first() }.first,
                        onClick = { detailCourse = course }
                    )
                }
            }
        }

        // Upcoming exams section
        if (showExamSchedule && exams.isNotEmpty()) {
            val todayDate = LocalDate.now()
            val latestExamDate = todayDate.plusWeeks(examLookaheadWeeks.toLong())
            val upcomingExams = exams.filter { exam ->
                try {
                    val examDate = java.time.LocalDate.parse(exam.getExamDate())
                    !examDate.isBefore(todayDate) && !examDate.isAfter(latestExamDate) && examDate != todayDate
                } catch (_: Exception) { false }
            }.sortedBy { it.getExamDate() }.take(5)

            if (upcomingExams.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School, contentDescription = null,
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
                    ExamCard(exam = exam, barColor = examColorMap["${exam.kcmc}|${exam.cdmc}"] ?: monetColors.getOrElse((exam.kcmc.hashCode().and(0x7fffffff) % monetColors.size).coerceIn(0, monetColors.size - 1)) { monetColors.first() }.first)
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
            onEdit = { detailCourse = null; onCourseLongPress(course) },
            courseColors = monetColors,
            colorGroupMode = colorGroupMode,
            dotColor = courseColorMap[course.id]
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
    animDelay: Long = 0L,
    skipAnim: Boolean = false,
    barColor: Color = Color.Gray,
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
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
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


    // Wave animation: during loading for all, or always for current courses
    val showWave = isCurrent || !animDone
    val waveOffset = if (showWave) {
        val transition = rememberInfiniteTransition(label = "wave")
        transition.animateFloat(
            initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "wavePhase"
        ).value
    } else 0f

    Md3Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (animatedProgress > 0f) {
                val fillColor = barColor.copy(alpha = if (realIsPast) 0.3f else 0.55f)
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val progressX = w * animatedProgress
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        if (showWave && waveOffset != 0f) {
                            val amp = 6.dp.toPx()
                            val freq = 3f
                            lineTo(progressX, 0f)
                            for (y in 0..h.toInt()) {
                                val yF = y.toFloat()
                                val wave = amp * sin(freq * yF / h * 2f * Math.PI.toFloat() + waveOffset).toFloat()
                                lineTo(progressX + wave, yF)
                            }
                        } else {
                            lineTo(progressX, 0f)
                            lineTo(progressX, h)
                        }
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path, color = fillColor)
                }
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
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (course.id < 0) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "考试",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (isCurrent) {
                            val pctBg = com.classapp.schedule.ui.theme.monetCardColor(barColor)
                            val pctText = com.classapp.schedule.ui.theme.MonetIconBadgeTextColor(barColor)
                            Box(
                                modifier = Modifier
                                    .background(pctBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = pctText
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (isNext) {
                            val tagBg = com.classapp.schedule.ui.theme.monetCardColor(barColor)
                            val tagText = com.classapp.schedule.ui.theme.MonetIconBadgeTextColor(barColor)
                            Box(
                                modifier = Modifier
                                    .background(tagBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.next_course),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tagText
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (realIsPast) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = animDone,
                                enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn()
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = barColor
                                )
                            }
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

private fun com.classapp.schedule.api.ExamInfo.toTodayCourse(
    semesterStart: java.time.LocalDate,
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String
): Course? {
    return try {
        val examDate = java.time.LocalDate.parse(getExamDate())
        val timeRange = getExamTimeRange()
        android.util.Log.d("TodayExam", "kssj=$kssj, timeRange=$timeRange")
        val timeParts = timeRange.split("-")
        if (timeParts.size != 2) return null
        val startPeriod = timeToPeriod(timeParts[0].trim(), getStartTime)
        val endPeriod = timeToPeriod(timeParts[1].trim(), getEndTime)
        if (startPeriod <= 0 || endPeriod < startPeriod) return null
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(semesterStart, examDate).toInt()
        val week = (daysDiff / 7) + 1
        Course(
            id = -((kch.ifEmpty { "$kcmc|$kssj|$cdmc" }).hashCode().toLong().let { kotlin.math.abs(it) } + 1L),
            name = kcmc,
            teacher = jsxx,
            classroom = cdmc,
            dayOfWeek = examDate.dayOfWeek.value,
            startPeriod = startPeriod,
            periods = endPeriod - startPeriod + 1,
            weekRange = week.toString(),
            remark = listOf(kssj, ksfs, khfs).filter { it.isNotEmpty() }.joinToString("\n"),
            isCustomTime = true,
            customStartTime = timeParts[0].trim(),
            customEndTime = timeParts[1].trim()
        )
    } catch (_: Exception) { null }
}

private fun timeToPeriod(time: String, timeProvider: (Int) -> String): Int {
    val targetMins = parseTime(time)
    var bestPeriod = 0
    var bestDiff = Int.MAX_VALUE
    for (p in 1..14) {
        val pMins = parseTime(timeProvider(p)) ?: continue
        val diff = kotlin.math.abs(targetMins - pMins)
        if (diff < bestDiff) {
            bestDiff = diff
            bestPeriod = p
        }
    }
    return bestPeriod
}

@Composable
private fun ExamCard(exam: com.classapp.schedule.api.ExamInfo, barColor: Color = Color.Gray) {

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.kcmc,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exam.kssj.isNotEmpty()) {
                        Text(
                            text = exam.kssj,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (exam.cdmc.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = exam.cdmc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (exam.ksfs.isNotEmpty()) {
                    Text(
                        text = exam.ksfs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
