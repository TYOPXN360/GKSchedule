package com.classapp.schedule.ui.weekly

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.util.CourseColors
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    courses: List<Course>,
    currentWeek: Int,
    totalWeeks: Int,
    periodsPerDay: Int,
    gridHeight: Int,
    gridCorner: Int,
    gridSpacing: Int,
    showPeriodLabel: Boolean,
    autoGridHeight: Boolean,
    mergeConsecutive: Boolean,
    showTimeLabel: Boolean,
    detailedSplit: Boolean,
    colorEngine: Int,
    colorGroupMode: Int,
    hideEmptyWeeks: Boolean,
    showDateInHeader: Boolean,
    semesterStart: java.time.LocalDate,
    isRefreshing: Boolean,
    onWeekChange: (Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    onCourseLongPress: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onRefresh: () -> Unit,
    realCurrentWeek: Int = currentWeek,
    getStartTime: (Int) -> String = { "" },
    getEndTime: (Int) -> String = { "" }
) {
    val daysOfWeek = listOf(R.string.mon, R.string.tue, R.string.wed, R.string.thu, R.string.fri, R.string.sat, R.string.sun)
    var showWeekPicker by remember { mutableStateOf(false) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticView = androidx.compose.ui.platform.LocalView.current
    val labelWidthDp = if (showPeriodLabel) { if (showTimeLabel) 64.dp else 36.dp } else 0.dp

    // Build render blocks
    // Build render blocks with dynamic color assignment
    val renderBlocks = remember(courses, currentWeek, mergeConsecutive, detailedSplit, colorGroupMode) {
        val weekCourses = courses.filter { it.isInWeek(currentWeek) }
        data class Block(val course: Course, val day: Int, val start: Int, val span: Int, val colorIdx: Int)
        val nameToIdx = mutableMapOf<String, Int>()
        val keyToIdx = mutableMapOf<String, Int>()
        var nextColor = 0
        val blocks = mutableListOf<Block>()
        weekCourses.forEach { c ->
            val ci = when (colorGroupMode) {
                0 -> nameToIdx.getOrPut(c.name) { nextColor++ }
                1 -> nameToIdx.getOrPut(c.name) { nextColor++ }
                else -> keyToIdx.getOrPut("${c.name}|${c.classroom}") { nextColor++ }
            }
            if (mergeConsecutive) {
                blocks.add(Block(c, c.dayOfWeek, c.startPeriod, c.periods, ci))
            } else if (detailedSplit) {
                for (p in c.startPeriod..c.endPeriod()) {
                    blocks.add(Block(c, c.dayOfWeek, p, 1, ci))
                }
            } else {
                var p = c.startPeriod
                while (p <= c.endPeriod()) {
                    val pairEnd = minOf(p + 1, c.endPeriod())
                    blocks.add(Block(c, c.dayOfWeek, p, pairEnd - p + 1, ci))
                    p = pairEnd + 1
                }
            }
        }
        blocks
    }

    // Compute visible weeks (skip empty weeks if hideEmptyWeeks is on)
    val visibleWeeks = remember(courses, totalWeeks, hideEmptyWeeks) {
        if (!hideEmptyWeeks) (1..totalWeeks).toList()
        else {
            val weeksWithCourses = courses.mapNotNull { course ->
                (1..totalWeeks).filter { course.isInWeek(it) }
            }.flatten().toSet()
            val nonEmpty = (1..totalWeeks).filter { week ->
                courses.any { it.isInWeek(week) }
            }
            if (nonEmpty.isEmpty()) listOf(1) else nonEmpty
        }
    }

    // Pager: maps visible index → actual week number
    val pagerState = rememberPagerState(
        initialPage = visibleWeeks.indexOf(currentWeek).coerceAtLeast(0),
        pageCount = { visibleWeeks.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val realWeek = remember { mutableIntStateOf(currentWeek) }

    // Sync pager → week
    LaunchedEffect(pagerState.settledPage) {
        val week = visibleWeeks.getOrElse(pagerState.settledPage) { currentWeek }
        if (week != currentWeek) onWeekChange(week)
    }
    // Sync week → pager (from arrow buttons / picker)
    LaunchedEffect(currentWeek) {
        val page = visibleWeeks.indexOf(currentWeek).coerceAtLeast(0)
        if (pagerState.currentPage != page) {
            coroutineScope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    // Predictive back
    BackHandler(enabled = currentWeek != realWeek.intValue) {
        onWeekChange(realWeek.intValue)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            com.classapp.schedule.util.HapticFeedback.medium(hapticView)
            onRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Week selector with refresh
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showWeekPicker = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        com.classapp.schedule.util.HapticFeedback.light(hapticView)
                        val idx = visibleWeeks.indexOf(currentWeek)
                        if (idx > 0) onWeekChange(visibleWeeks[idx - 1])
                    }) {
                        Icon(Icons.Default.ChevronLeft, "Prev", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentWeek == realCurrentWeek) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("今", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Text(stringResource(R.string.week_format, currentWeek),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = {
                        com.classapp.schedule.util.HapticFeedback.light(hapticView)
                        val idx = visibleWeeks.indexOf(currentWeek)
                        if (idx < visibleWeeks.size - 1) onWeekChange(visibleWeeks[idx + 1])
                    }) {
                        Icon(Icons.Default.ChevronRight, "Next", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                if (showPeriodLabel) Spacer(modifier = Modifier.width(labelWidthDp))
                daysOfWeek.forEach { dayRes ->
                    Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(dayRes), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Date row (optional)
            if (showDateInHeader) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    if (showPeriodLabel) Spacer(modifier = Modifier.width(labelWidthDp))
                    for (dow in 1..7) {
                        val date = semesterStart.plusDays(((currentWeek - 1) * 7 + (dow - 1)).toLong())
                        Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${date.monthValue}/${date.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Grid — HorizontalPager for native swipe
            val uniqueCourseCount = remember(courses) { courses.map { it.name }.distinct().size }
            val monetColors = CourseColors.getColors(colorEngine, count = uniqueCourseCount.coerceAtLeast(8))
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val week = page + 1
                val weekBlocks = remember(week, colorGroupMode) {
                    val weekCourses = courses.filter { it.isInWeek(week) }
                    data class B(val course: Course, val day: Int, val start: Int, val span: Int, val colorIdx: Int)
                    val nameToIdx = mutableMapOf<String, Int>()
                    val keyToIdx = mutableMapOf<String, Int>()
                    var nextColor = 0
                    val blocks = mutableListOf<B>()
                    val classroomSatMap = mutableMapOf<String, MutableMap<String, Int>>()
                    weekCourses.forEach { c ->
                        val ci = when (colorGroupMode) {
                            0 -> nameToIdx.getOrPut(c.name) { nextColor++ }
                            1 -> {
                                val baseIdx = nameToIdx.getOrPut(c.name) { nextColor++ }
                                val satMap = classroomSatMap.getOrPut(c.name) { mutableMapOf() }
                                val satOffset = satMap.getOrPut(c.classroom) { satMap.size }
                                baseIdx * 10 + satOffset
                            }
                            else -> keyToIdx.getOrPut("${c.name}|${c.classroom}") { nextColor++ }
                        }
                        if (mergeConsecutive) {
                            blocks.add(B(c, c.dayOfWeek, c.startPeriod, c.periods, ci))
                        } else if (detailedSplit) {
                            for (p in c.startPeriod..c.endPeriod()) blocks.add(B(c, c.dayOfWeek, p, 1, ci))
                        } else {
                            var p = c.startPeriod
                            while (p <= c.endPeriod()) {
                                val pairEnd = minOf(p + 1, c.endPeriod())
                                blocks.add(B(c, c.dayOfWeek, p, pairEnd - p + 1, ci))
                                p = pairEnd + 1
                            }
                        }
                    }
                    blocks
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    val totalH = maxHeight
                    val totalW = maxWidth
                    val rowH = totalH / periodsPerDay
                    val cellW = (totalW - labelWidthDp) / 7

                    // Background: empty grid + period labels
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        for (period in 1..periodsPerDay) {
                            Row(modifier = Modifier.fillMaxWidth().height(rowH)) {
                                if (showPeriodLabel) {
                                    Box(modifier = Modifier.width(labelWidthDp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$period", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (showTimeLabel) {
                                                Text("${getStartTime(period)}\n${getEndTime(period)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                for (day in 1..7) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(gridSpacing.dp)
                                        .clip(RoundedCornerShape(gridCorner.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)))
                                }
                            }
                        }
                    }

                    // Overlay: course blocks
                    weekBlocks.forEach { block ->
                        val x = labelWidthDp + cellW * (block.day - 1) + gridSpacing.dp
                        val y = rowH * (block.start - 1) + gridSpacing.dp
                        val w = cellW - gridSpacing.dp * 2
                        val h = rowH * block.span - gridSpacing.dp * 2
                        val satOffset = if (colorGroupMode == 1) block.colorIdx % 10 else 0

                        Box(
                            modifier = Modifier.offset(x = x, y = y)
                                .size(width = w.coerceAtLeast(24.dp), height = h.coerceAtLeast(24.dp))
                                .clip(RoundedCornerShape(gridCorner.dp))
                                .background(CourseColors.getBackground(block.colorIdx, monetColors, satOffset))
                                .clickable {
                                com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                                detailCourse = block.course
                            }
                                .padding(4.dp)
                        ) {
                            Column {
                                Text(block.course.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                    color = CourseColors.getTextColor(block.colorIdx, monetColors, satOffset),
                                    overflow = TextOverflow.Ellipsis)
                                if (block.course.classroom.isNotEmpty()) {
                                    Text(block.course.classroom, style = MaterialTheme.typography.labelSmall,
                                        color = CourseColors.getTextColor(block.colorIdx, monetColors, satOffset).copy(alpha = 0.7f),
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } // HorizontalPager
        }

        // FABs
        var fabExpanded by remember { mutableStateOf(true) }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back to current week — only visible when not on current week
            AnimatedVisibility(
                visible = currentWeek != realCurrentWeek && fabExpanded,
                enter = if (currentWeek > realCurrentWeek) slideInHorizontally(initialOffsetX = { -it }) + fadeIn() else slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = if (currentWeek > realCurrentWeek) slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() else slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                        onWeekChange(realCurrentWeek)
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        if (currentWeek > realCurrentWeek) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        stringResource(R.string.back_to_current_week)
                    )
                }
            }
            // Refresh button
            AnimatedVisibility(
                visible = fabExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                        onRefresh()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            }
            // Add course button
            AnimatedVisibility(
                visible = fabExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                        onAddCourse()
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_course))
                }
            }
            // Collapse/Expand toggle
            SmallFloatingActionButton(
                onClick = {
                    com.classapp.schedule.util.HapticFeedback.light(hapticView)
                    fabExpanded = !fabExpanded
                },
                modifier = Modifier.size(40.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = if (fabExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = if (fabExpanded) 90f else -90f }
                )
            }
        }
    }

    // Detail sheet
    detailCourse?.let { course ->
        CourseDetailSheet(course = course, getStartTime = getStartTime, getEndTime = getEndTime,
            onDismiss = { detailCourse = null }, onEdit = { detailCourse = null; onCourseLongPress(course) })
    }

    if (showWeekPicker) {
        WeekPickerSheet(totalWeeks, currentWeek, { onWeekChange(it); showWeekPicker = false }, { showWeekPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(course: Course, getStartTime: (Int) -> String, getEndTime: (Int) -> String, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val detailColors = CourseColors.getColors(0, count = 32)
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(CourseColors.getTextColor(course.colorIndex, detailColors)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(course.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            val dayNames = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
            DetailRow("星期", dayNames.getOrElse(course.dayOfWeek) { "" })
            DetailRow("节次", "${course.startPeriod}-${course.endPeriod()}节")
            DetailRow("时间", "${getStartTime(course.startPeriod)} - ${getEndTime(course.endPeriod())}")
            if (course.teacher.isNotEmpty()) DetailRow("教师", course.teacher)
            if (course.classroom.isNotEmpty()) DetailRow("教室", course.classroom)
            DetailRow("周次", course.weekRange)
            if (course.remark.isNotEmpty()) DetailRow("备注", course.remark)
            Spacer(modifier = Modifier.height(24.dp))
            val detailView = androidx.compose.ui.platform.LocalView.current
            OutlinedButton(onClick = {
                com.classapp.schedule.util.HapticFeedback.heavy(detailView)
                onEdit()
            }, modifier = Modifier.fillMaxWidth()) { Text("编辑课程") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekPickerSheet(totalWeeks: Int, currentWeek: Int, onWeekSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.select_week), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            for (row in 0 until (totalWeeks + 4) / 5) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 5) {
                        val week = row * 5 + col + 1
                        if (week <= totalWeeks) {
                            val pickerView = androidx.compose.ui.platform.LocalView.current
                            FilledTonalButton(onClick = {
                                com.classapp.schedule.util.HapticFeedback.light(pickerView)
                                onWeekSelected(week)
                            }, modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                colors = if (week == currentWeek) ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.filledTonalButtonColors()) {
                                Text("$week", color = if (week == currentWeek) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        } else Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
