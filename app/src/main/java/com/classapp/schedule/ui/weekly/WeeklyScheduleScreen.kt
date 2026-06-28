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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
    exams: List<com.classapp.schedule.api.ExamInfo> = emptyList(),
    showExamSchedule: Boolean = false,
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

    val examCourses = remember(exams, showExamSchedule, semesterStart, getStartTime, getEndTime) {
        if (!showExamSchedule) emptyList() else exams.mapNotNull { exam ->
            exam.toScheduleCourse(semesterStart, getStartTime, getEndTime)
        }
    }
    val scheduleCourses = remember(courses, examCourses) { courses + examCourses }

    // Build render blocks
    // Build render blocks with dynamic color assignment
    val renderBlocks = remember(courses, currentWeek, mergeConsecutive, detailedSplit, colorGroupMode) {
        val weekCourses = scheduleCourses.filter { it.isInWeek(currentWeek) }
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
    val visibleWeeks = remember(scheduleCourses, totalWeeks, hideEmptyWeeks) {
        if (!hideEmptyWeeks) (1..totalWeeks).toList()
        else {
            val nonEmpty = (1..totalWeeks).filter { week ->
                scheduleCourses.any { it.isInWeek(week) }
            }
            val allNonEmpty = nonEmpty.distinct().sorted()
            if (allNonEmpty.isEmpty()) listOf(1) else allNonEmpty
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
        val coroutineScope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current
        val rootView = androidx.compose.ui.platform.LocalView.current
        var cropTopPx by remember { mutableIntStateOf(0) }
        var cropBottomPx by remember { mutableIntStateOf(0) }
        var hideFabs by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Week selector — track top edge in pixels
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showWeekPicker = true }
                    .onGloballyPositioned { cropTopPx = it.positionInRoot().y.toInt() },
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
            val uniqueCourseCount = remember(scheduleCourses) { scheduleCourses.map { it.name }.distinct().size }
            val monetColors = CourseColors.getColors(colorEngine, count = uniqueCourseCount.coerceAtLeast(8))
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
                    .onGloballyPositioned { cropBottomPx = it.positionInRoot().y.toInt() + it.size.height }
            ) { page ->
                val week = visibleWeeks.getOrElse(page) { currentWeek }
                val weekBlocks = remember(week, colorGroupMode, scheduleCourses, mergeConsecutive, detailedSplit) {
                    val weekCourses = scheduleCourses.filter { it.isInWeek(week) }
                    data class B(
                        val course: Course,
                        val day: Int,
                        val start: Int,
                        val span: Int,
                        val colorIdx: Int,
                        val startLine: Float,
                        val endLine: Float
                    )
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
                        fun addBlock(course: Course, start: Int, span: Int, colorIdx: Int) {
                            val startLine = if (course.isExamCourse()) {
                                timeToGridLine(course.customStartTime, getStartTime, getEndTime, periodsPerDay)
                            } else {
                                (start - 1).toFloat()
                            }
                            val endLine = if (course.isExamCourse()) {
                                timeToGridLine(course.customEndTime, getStartTime, getEndTime, periodsPerDay)
                                    .coerceAtLeast(startLine + 0.25f)
                            } else {
                                startLine + span
                            }
                            blocks.add(B(course, course.dayOfWeek, start, span, colorIdx, startLine, endLine))
                        }
                        if (c.isExamCourse() || mergeConsecutive) {
                            addBlock(c, c.startPeriod, c.periods, ci)
                        } else if (detailedSplit) {
                            for (p in c.startPeriod..c.endPeriod()) addBlock(c, p, 1, ci)
                        } else {
                            var p = c.startPeriod
                            while (p <= c.endPeriod()) {
                                val pairEnd = minOf(p + 1, c.endPeriod())
                                addBlock(c, p, pairEnd - p + 1, ci)
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

                    // Pre-compute base colors
                    val blockBaseColors = remember(weekBlocks, monetColors, colorGroupMode) {
                        weekBlocks.map { block ->
                            val satOffset = if (colorGroupMode == 1) block.colorIdx % 10 else 0
                            CourseColors.getBackgroundStatic(block.colorIdx, monetColors, satOffset)
                        }
                    }

                    // Draw backgrounds: base colors + overlapping regions blended
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val ch = size.height
                        val rowH2 = ch / periodsPerDay
                        val cellW2 = (size.width - labelWidthDp.toPx()) / 7
                        val spacing = gridSpacing.dp.toPx()
                        val corner = gridCorner.dp.toPx()

                        // Draw all blocks with original colors
                        weekBlocks.forEachIndexed { idx, block ->
                            val x = labelWidthDp.toPx() + cellW2 * (block.day - 1) + spacing
                            val y = rowH2 * block.startLine + spacing
                            val bw = cellW2 - spacing * 2
                            val bh = rowH2 * (block.endLine - block.startLine) - spacing * 2
                            drawRoundRect(
                                color = blockBaseColors[idx],
                                topLeft = Offset(x, y),
                                size = Size(bw.coerceAtLeast(24.dp.toPx()), bh.coerceAtLeast(24.dp.toPx())),
                                cornerRadius = CornerRadius(corner)
                            )
                        }

                        // Draw blended overlay on overlapping regions
                        for (i in weekBlocks.indices) {
                            for (j in i + 1 until weekBlocks.size) {
                                val a = weekBlocks[i]; val b = weekBlocks[j]
                                if (a.day == b.day && a.startLine < b.endLine && a.endLine > b.startLine) {
                                    val overlapStart = maxOf(a.startLine, b.startLine)
                                    val overlapEnd = minOf(a.endLine, b.endLine)
                                    val ox = labelWidthDp.toPx() + cellW2 * (a.day - 1) + spacing
                                    val oy = rowH2 * overlapStart + spacing
                                    val ow = cellW2 - spacing * 2
                                    val oh = rowH2 * (overlapEnd - overlapStart) - spacing * 2
                                    val blend = Color(
                                        (blockBaseColors[i].red + blockBaseColors[j].red) / 2f,
                                        (blockBaseColors[i].green + blockBaseColors[j].green) / 2f,
                                        (blockBaseColors[i].blue + blockBaseColors[j].blue) / 2f,
                                        (blockBaseColors[i].alpha + blockBaseColors[j].alpha) / 2f
                                    )
                                    drawRoundRect(
                                        color = blend,
                                        topLeft = Offset(ox, oy),
                                        size = Size(ow, oh),
                                        cornerRadius = CornerRadius(corner)
                                    )
                                }
                            }
                        }
                    }

                    // Overlay: course text
                    weekBlocks.forEach { block ->
                        val x = labelWidthDp + cellW * (block.day - 1) + gridSpacing.dp
                        val y = rowH * block.startLine + gridSpacing.dp
                        val w = cellW - gridSpacing.dp * 2
                        val h = rowH * (block.endLine - block.startLine) - gridSpacing.dp * 2
                        val satOffset = if (colorGroupMode == 1) block.colorIdx % 10 else 0

                        Box(
                            modifier = Modifier.offset(x = x, y = y)
                                .size(width = w.coerceAtLeast(24.dp), height = h.coerceAtLeast(24.dp))
                                .clickable {
                                com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                                detailCourse = block.course
                            }
                                .padding(4.dp)
                        ) {
                            val textColor = CourseColors.getTextColor(block.colorIdx, monetColors, satOffset)
                            Column {
                                if (block.course.isExamCourse()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 2.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(textColor.copy(alpha = 0.92f))
                                            .padding(horizontal = 3.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "考试",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CourseColors.getBackgroundStatic(block.colorIdx, monetColors, satOffset),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    block.course.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (block.course.classroom.isNotEmpty()) {
                                    Text(block.course.classroom, style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } // HorizontalPager
        }

        // FABs — hidden during screenshot capture
        if (!hideFabs) {
        var fabExpanded by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            // Collapse/Expand toggle — use FloatingActionButton for identical shape
            FloatingActionButton(
                onClick = {
                    com.classapp.schedule.util.HapticFeedback.light(hapticView)
                    fabExpanded = !fabExpanded
                },
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    if (fabExpanded) "—" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(20.dp)
                )
            }
            // Expanded FABs — stacked above the toggle button
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 68.dp),
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
            // Screenshot button
            AnimatedVisibility(
                visible = fabExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                        coroutineScope.launch {
                            try {
                                hideFabs = true
                                kotlinx.coroutines.delay(100) // wait for recomposition
                                val fullBitmap = android.graphics.Bitmap.createBitmap(rootView.width, rootView.height, android.graphics.Bitmap.Config.ARGB_8888)
                                rootView.draw(android.graphics.Canvas(fullBitmap))
                                hideFabs = false
                                val left = 0
                                val top = cropTopPx.coerceIn(0, fullBitmap.height)
                                val right = fullBitmap.width
                                val bottom = cropBottomPx.coerceIn(top, fullBitmap.height)
                                val cropped = android.graphics.Bitmap.createBitmap(fullBitmap, left, top, right - left, bottom - top)
                                val saved = com.classapp.schedule.util.ImageExport.saveBitmapToGallery(
                                    context, cropped, "Pictures/Screenshots/schedule_${System.currentTimeMillis()}.png"
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    if (saved) "已保存到 Pictures/Screenshots" else "保存失败",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "截图失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.CameraAlt, "Screenshot")
                }
            }
            } // Column
        } // Box
        } // if (!hideFabs)
    } // PullToRefreshBox

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
            if (course.id >= 0) {
                Button(onClick = {
                    com.classapp.schedule.util.HapticFeedback.heavy(detailView)
                    onEdit()
                }, modifier = Modifier.fillMaxWidth()) { Text("编辑课程") }
            }
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

private fun Course.isExamCourse(): Boolean = id < 0

private fun com.classapp.schedule.api.ExamInfo.toScheduleCourse(
    semesterStart: java.time.LocalDate,
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String
): Course? {
    return try {
        val examDate = java.time.LocalDate.parse(getExamDate())
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(semesterStart, examDate).toInt()
        val week = (daysDiff / 7) + 1
        if (week <= 0) return null

        val timeParts = getExamTimeRange().split("-")
        if (timeParts.size != 2) return null
        val startPeriod = timeToPeriod(timeParts[0].trim(), getStartTime)
        val endPeriod = timeToPeriod(timeParts[1].trim(), getEndTime)
        if (startPeriod <= 0 || endPeriod < startPeriod) return null

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
    } catch (_: Exception) {
        null
    }
}

private fun timeToGridLine(
    time: String,
    getStartTime: (Int) -> String,
    getEndTime: (Int) -> String,
    periodsPerDay: Int
): Float {
    val target = parseMinutes(time) ?: return 0f
    for (period in 1..periodsPerDay) {
        val start = parseMinutes(getStartTime(period)) ?: continue
        val end = parseMinutes(getEndTime(period)) ?: continue
        if (target in start..end) {
            val duration = (end - start).coerceAtLeast(1)
            return (period - 1) + (target - start).toFloat() / duration.toFloat()
        }
        val nextStart = parseMinutes(getStartTime(period + 1))
        if (nextStart != null && target > end && target < nextStart) {
            return period.toFloat()
        }
    }
    val firstStart = parseMinutes(getStartTime(1)) ?: return 0f
    val lastEnd = parseMinutes(getEndTime(periodsPerDay)) ?: return periodsPerDay.toFloat()
    return when {
        target <= firstStart -> 0f
        target >= lastEnd -> periodsPerDay.toFloat()
        else -> timeToPeriod(time, getStartTime).let { if (it > 0) (it - 1).toFloat() else 0f }
    }
}

private fun parseMinutes(time: String): Int? {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return hour * 60 + minute
}

/** Map a time string like "14:20" to the closest period number. */
private fun timeToPeriod(time: String, timeProvider: (Int) -> String): Int {
    val targetMins = parseMinutes(time) ?: return 0
    var bestPeriod = 0
    var bestDiff = Int.MAX_VALUE
    for (p in 1..14) {
        val pMins = parseMinutes(timeProvider(p)) ?: continue
        val diff = kotlin.math.abs(targetMins - pMins)
        if (diff < bestDiff) {
            bestDiff = diff
            bestPeriod = p
        }
    }
    return bestPeriod
}
