package com.classapp.schedule.ui.weekly

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CropFree
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.data.ScheduleItem
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
    exams: List<com.classapp.schedule.data.ExamEntity> = emptyList(),
    showExamSchedule: Boolean = false,
    onWeekChange: (Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    onCourseLongPress: (Course) -> Unit,
    onAddCourse: () -> Unit,
    onRefresh: () -> Unit,
    realCurrentWeek: Int = currentWeek,
    firstDayOfWeek: Int = 1,
    diffColorPerWeek: Boolean = false,
    getStartTime: (Int) -> String = { "" },
    getEndTime: (Int) -> String = { "" }
) {
    // Reorder days based on firstDayOfWeek setting (1=Monday, 7=Sunday)
    val startDay = firstDayOfWeek.coerceIn(1, 7)
    val allDays = listOf(R.string.mon, R.string.tue, R.string.wed, R.string.thu, R.string.fri, R.string.sat, R.string.sun)
    val daysOfWeek = allDays.drop(startDay - 1) + allDays.take(startDay - 1)
    // Map course dayOfWeek (1=Mon..7=Sun) to visual column position
    fun dayToColumn(courseDay: Int): Int {
        return ((courseDay - startDay + 7) % 7) + 1
    }
    var showWeekPicker by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<ScheduleItem?>(null) }
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticView = androidx.compose.ui.platform.LocalView.current
    val labelWidthDp = if (showPeriodLabel) { if (showTimeLabel) 64.dp else 36.dp } else 0.dp

    // Build unified schedule items (replaces id<0 hack)
    val scheduleItems = remember(courses, exams, showExamSchedule, semesterStart, getStartTime, getEndTime) {
        val courseItems = courses.map { ScheduleItem.CourseItem(it) }
        if (!showExamSchedule) courseItems
        else courseItems + exams.mapNotNull { exam ->
            ScheduleItem.fromExam(exam, semesterStart, getStartTime, getEndTime)
        }
    }

    // Build render blocks with dynamic color assignment
    val renderBlocks = remember(courses, currentWeek, mergeConsecutive, detailedSplit, colorGroupMode, scheduleItems) {
        val weekItems = scheduleItems.filter { it.isInWeek(currentWeek) }
        data class Block(val item: ScheduleItem, val day: Int, val start: Int, val span: Int, val colorIdx: Int)
        val nameToIdx = mutableMapOf<String, Int>()
        val keyToIdx = mutableMapOf<String, Int>()
        var nextColor = 0
        val blocks = mutableListOf<Block>()
        weekItems.forEach { item ->
            val ci = when (colorGroupMode) {
                0 -> nameToIdx.getOrPut(item.name) { nextColor++ }
                1 -> nameToIdx.getOrPut(item.name) { nextColor++ }
                else -> keyToIdx.getOrPut("${item.name}|${item.classroom}") { nextColor++ }
            }
            if (item.isExam || mergeConsecutive) {
                blocks.add(Block(item, item.dayOfWeek, item.startPeriod, item.periods, ci))
            } else if (detailedSplit) {
                for (p in item.startPeriod..item.endPeriod()) {
                    blocks.add(Block(item, item.dayOfWeek, p, 1, ci))
                }
            } else {
                var p = item.startPeriod
                while (p <= item.endPeriod()) {
                    val pairEnd = minOf(p + 1, item.endPeriod())
                    blocks.add(Block(item, item.dayOfWeek, p, pairEnd - p + 1, ci))
                    p = pairEnd + 1
                }
            }
        }
        blocks
    }

    // Compute visible weeks (skip empty weeks if hideEmptyWeeks is on)
    // 🔥 防死锁：强制包含 currentWeek 和 realCurrentWeek，防止 indexOf 返回 -1
    val visibleWeeks = remember(scheduleItems, totalWeeks, hideEmptyWeeks, currentWeek, realCurrentWeek) {
        if (!hideEmptyWeeks) (1..totalWeeks).toList()
        else {
            val nonEmpty = (1..totalWeeks).filter { week ->
                scheduleItems.any { it.isInWeek(week) }
            }.toMutableList()
            if (!nonEmpty.contains(currentWeek) && currentWeek in 1..totalWeeks) nonEmpty.add(currentWeek)
            if (!nonEmpty.contains(realCurrentWeek) && realCurrentWeek in 1..totalWeeks) nonEmpty.add(realCurrentWeek)
            val finalWeeks = nonEmpty.distinct().sorted()
            if (finalWeeks.isEmpty()) listOf(1) else finalWeeks
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
    // 🔥 防死锁：直接在 LaunchedEffect 作用域内挂起，不再嵌套 coroutineScope.launch
    LaunchedEffect(currentWeek, visibleWeeks) {
        val targetPage = visibleWeeks.indexOf(currentWeek).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
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
                        else if (currentWeek > 1) onWeekChange(currentWeek - 1) // 兜底抗沉没
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
                                    color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Text(stringResource(R.string.week_format, currentWeek),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = {
                        com.classapp.schedule.util.HapticFeedback.light(hapticView)
                        val idx = visibleWeeks.indexOf(currentWeek)
                        if (idx >= 0 && idx < visibleWeeks.size - 1) onWeekChange(visibleWeeks[idx + 1])
                        else if (currentWeek < totalWeeks) onWeekChange(currentWeek + 1) // 兜底抗沉没
                    }) {
                        Icon(Icons.Default.ChevronRight, "Next", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Header: unified day+date grid with today highlight
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showPeriodLabel) Spacer(modifier = Modifier.width(labelWidthDp))
                val todayDate = java.time.LocalDate.now()
                val todayDow = todayDate.dayOfWeek.value
                for (index in 0 until 7) {
                    val dayRes = daysOfWeek[index]
                    val courseDay = ((startDay - 1 + index) % 7) + 1
                    val isToday = courseDay == todayDow && currentWeek == realCurrentWeek
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(dayRes), style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (showDateInHeader) {
                            val date = semesterStart.plusDays(((currentWeek - 1) * 7 + (courseDay - 1)).toLong())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${date.monthValue}/${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // 🔥 Fix: 使用 LocalAppIsDark 统一暗色模式判定源头（提到 Pager 外部供 detail sheet 使用）
            val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .onGloballyPositioned { cropBottomPx = it.positionInRoot().y.toInt() + it.size.height }
            ) { page ->
                val week = visibleWeeks.getOrElse(page) { currentWeek }
                val weekBlocks = remember(week, colorGroupMode, scheduleItems, mergeConsecutive, detailedSplit) {
                    val weekItems = scheduleItems.filter { it.isInWeek(week) }
                    data class B(
                        val item: ScheduleItem,
                        val day: Int,
                        val start: Int,
                        val span: Int,
                        val colorIdx: Int,
                        val startLine: Float,
                        val endLine: Float
                    )
                    val blocks = mutableListOf<B>()
                    val classroomCounters = mutableMapOf<String, Int>()
                    weekItems.forEach { item ->
                        val ci = when (colorGroupMode) {
                            0 -> abs(item.name.hashCode()) % 8
                            1 -> {
                                val baseIdx = abs(item.name.hashCode()) % 8
                                val classIdx = classroomCounters.getOrPut(item.name) { 0 }
                                classroomCounters[item.name] = classIdx + 1
                                baseIdx * 10 + classIdx
                            }
                            else -> abs("${item.name}|${item.classroom}".hashCode()) % 64
                        }
                        fun addBlock(si: ScheduleItem, start: Int, span: Int, colorIdx: Int) {
                            val startLine = if (si.isExam) {
                                timeToGridLine(si.customStartTime, getStartTime, getEndTime, periodsPerDay)
                            } else {
                                (start - 1).toFloat()
                            }
                            val endLine = if (si.isExam) {
                                timeToGridLine(si.customEndTime, getStartTime, getEndTime, periodsPerDay)
                                    .coerceAtLeast(startLine + 0.25f)
                            } else {
                                startLine + span
                            }
                            blocks.add(B(si, si.dayOfWeek, start, span, colorIdx, startLine, endLine))
                        }
                        if (item.isExam || mergeConsecutive) {
                            addBlock(item, item.startPeriod, item.periods, ci)
                        } else if (detailedSplit) {
                            for (p in item.startPeriod..item.endPeriod()) addBlock(item, p, 1, ci)
                        } else {
                            var p = item.startPeriod
                            while (p <= item.endPeriod()) {
                                val pairEnd = minOf(p + 1, item.endPeriod())
                                addBlock(item, p, pairEnd - p + 1, ci)
                                p = pairEnd + 1
                            }
                        }
                    }
                    blocks
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                ) {
                    // 🎯 智能多轨高度计算，动态恢复 autoGridHeight 功能
                    val rowH = if (autoGridHeight) maxHeight / periodsPerDay else gridHeight.dp
                    val totalGridHeight = rowH * periodsPerDay
                    val cellW = (maxWidth - labelWidthDp) / 7

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Layer 1: Grid background
                        Column(modifier = Modifier.fillMaxWidth().height(totalGridHeight)) {
                            for (period in 1..periodsPerDay) {
                                Row(modifier = Modifier.fillMaxWidth().height(rowH)) {
                                    if (showPeriodLabel) {
                                        Box(modifier = Modifier.width(labelWidthDp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("$period", style = MaterialTheme.typography.labelMedium,
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

                        // Layer 2: Canvas for course block colors
                        val blockBaseColors = remember(weekBlocks, colorGroupMode, isDark) {
                            weekBlocks.map { block ->
                                val realClassroomIdx = if (colorGroupMode == 1) block.colorIdx % 10 else 0
                                com.classapp.schedule.util.CourseColors.getColorSync(
                                    colorGroupMode, block.item.name, block.item.classroom, realClassroomIdx, week = week, diffColorPerWeek = diffColorPerWeek, isDark = isDark
                                ).container
                            }
                        }

                        val density = LocalDensity.current
                        val rowHPx = with(density) { rowH.toPx() }
                        val labelWidthPx = with(density) { labelWidthDp.toPx() }

                        Canvas(modifier = Modifier.fillMaxWidth().height(totalGridHeight)) {
                            val cellW2 = (size.width - labelWidthPx) / 7
                            val spacing = gridSpacing.dp.toPx()
                            val corner = gridCorner.dp.toPx()

                            weekBlocks.forEachIndexed { idx, block ->
                                val x = labelWidthPx + cellW2 * (dayToColumn(block.day) - 1) + spacing
                                val y = rowHPx * block.startLine + spacing
                                val bw = cellW2 - spacing * 2
                                val bh = rowHPx * (block.endLine - block.startLine) - spacing * 2
                                drawRoundRect(
                                    color = blockBaseColors[idx],
                                    topLeft = Offset(x, y),
                                    size = Size(bw.coerceAtLeast(24.dp.toPx()), bh.coerceAtLeast(24.dp.toPx())),
                                    cornerRadius = CornerRadius(corner)
                                )
                            }

                            for (i in weekBlocks.indices) {
                                for (j in i + 1 until weekBlocks.size) {
                                    val a = weekBlocks[i]; val b = weekBlocks[j]
                                    if (a.day == b.day && a.startLine < b.endLine && a.endLine > b.startLine) {
                                        val overlapStart = maxOf(a.startLine, b.startLine)
                                        val overlapEnd = minOf(a.endLine, b.endLine)
                                        val ox = labelWidthPx + cellW2 * (dayToColumn(a.day) - 1) + spacing
                                        val oy = rowHPx * overlapStart + spacing
                                        val ow = cellW2 - spacing * 2
                                        val oh = rowHPx * (overlapEnd - overlapStart) - spacing * 2
                                        val blend = Color(
                                            (blockBaseColors[i].red + blockBaseColors[j].red) / 2f,
                                            (blockBaseColors[i].green + blockBaseColors[j].green) / 2f,
                                            (blockBaseColors[i].blue + blockBaseColors[j].blue) / 2f,
                                            (blockBaseColors[i].alpha + blockBaseColors[j].alpha) / 2f
                                        )
                                        drawRoundRect(color = blend, topLeft = Offset(ox, oy), size = Size(ow, oh), cornerRadius = CornerRadius(corner))
                                    }
                                }
                            }
                        }

                        // Layer 3: Course text overlay
                        weekBlocks.forEach { block ->
                            val x = labelWidthDp + cellW * (dayToColumn(block.day) - 1) + gridSpacing.dp
                            val y = rowH * block.startLine + gridSpacing.dp
                            val w = cellW - gridSpacing.dp * 2
                            val h = rowH * (block.endLine - block.startLine) - gridSpacing.dp * 2

                            Box(
                                modifier = Modifier.offset(x = x, y = y)
                                    .size(width = w.coerceAtLeast(24.dp), height = h.coerceAtLeast(24.dp))
                                    .clickable {
                                        com.classapp.schedule.util.HapticFeedback.medium(hapticView)
                                        detailItem = block.item
                                    }
                                    .semantics { contentDescription = block.item.name }
                                    .padding(4.dp)
                            ) {
                                val textColor = remember(block, colorGroupMode, isDark) {
                                    val realClassroomIdx = if (colorGroupMode == 1) block.colorIdx % 10 else 0
                                    com.classapp.schedule.util.CourseColors.getColorSync(
                                        colorGroupMode, block.item.name, block.item.classroom, realClassroomIdx, week = week, diffColorPerWeek = diffColorPerWeek, isDark = isDark
                                    ).content
                                }
                                Column {
                                    if (block.item.isExam) {
                                        Box(
                                            modifier = Modifier.padding(bottom = 2.dp).clip(RoundedCornerShape(4.dp)).background(textColor.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("考试", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                                        }
                                    }
                                    val isHidden = block.item.let { it is ScheduleItem.CourseItem && it.course.isHidden }
                                    if (isHidden) {
                                        Box(
                                            modifier = Modifier.padding(bottom = 2.dp).clip(RoundedCornerShape(4.dp)).background(textColor.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("隐藏", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                                        }
                                    }
                                    Text(block.item.name, style = MaterialTheme.typography.labelMedium, color = textColor, overflow = TextOverflow.Ellipsis)
                                    if (block.item.classroom.isNotEmpty()) {
                                        Text(block.item.classroom, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f), overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    } // scrollable Box
                } // BoxWithConstraints
            } // HorizontalPager
        }

        // FABs
        if (!hideFabs) {
        var fabExpanded by remember { mutableStateOf(true) }
        val expandHeight by animateDpAsState(
            targetValue = if (fabExpanded) (3 * 56 + 3 * 12).dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
            label = "expandHeight"
        )
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            // Toggle — bottom right
            FloatingActionButton(
                onClick = { com.classapp.schedule.util.HapticFeedback.light(hapticView); fabExpanded = !fabExpanded },
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) { Text(if (fabExpanded) "—" else "+", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp)) }

            // Column: expandable buttons only (declared first so back-to-week draws on top)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 68.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = fabExpanded, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    FloatingActionButton(onClick = { com.classapp.schedule.util.HapticFeedback.medium(hapticView); onRefresh() }, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
                AnimatedVisibility(visible = fabExpanded, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    FloatingActionButton(onClick = { com.classapp.schedule.util.HapticFeedback.medium(hapticView); onAddCourse() }, containerColor = MaterialTheme.colorScheme.primary) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_course))
                    }
                }
                AnimatedVisibility(visible = fabExpanded, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    FloatingActionButton(onClick = { com.classapp.schedule.util.HapticFeedback.medium(hapticView); coroutineScope.launch { try { hideFabs = true; kotlinx.coroutines.delay(100); val fb = android.graphics.Bitmap.createBitmap(rootView.width, rootView.height, android.graphics.Bitmap.Config.ARGB_8888); rootView.draw(android.graphics.Canvas(fb)); hideFabs = false; val c = android.graphics.Bitmap.createBitmap(fb, 0, cropTopPx.coerceIn(0, fb.height), fb.width, cropBottomPx.coerceIn(cropTopPx.coerceIn(0, fb.height), fb.height) - cropTopPx.coerceIn(0, fb.height)); val s = com.classapp.schedule.util.ImageExport.saveBitmapToGallery(context, c, "Pictures/Screenshots/schedule_${System.currentTimeMillis()}.png"); android.widget.Toast.makeText(context, if (s) "已保存到 Pictures/Screenshots" else "保存失败", android.widget.Toast.LENGTH_SHORT).show() } catch (e: Exception) { android.widget.Toast.makeText(context, "截图失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show() } }
                    }, containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) { Icon(Icons.Default.CropFree, "Screenshot") }
                    }
            } // HorizontalPager

            // Back to current week — horizontal slide + fade, no clip bounds
            AnimatedVisibility(
                visible = currentWeek != realCurrentWeek,
                enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 68.dp).offset(y = -expandHeight)
            ) {
                FloatingActionButton(
                    onClick = { com.classapp.schedule.util.HapticFeedback.medium(hapticView); onWeekChange(realCurrentWeek) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) { Icon(if (currentWeek > realCurrentWeek) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, stringResource(R.string.back_to_current_week)) }
            }
        } // Box
        } // if (!hideFabs)
    } // PullToRefreshBox

    // Detail sheet — 🔥 归一：无论是否考试课，统一走 HCT 动态分配
    detailItem?.let { item ->
        val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
        val targetWeek = if (item.isExam) item.weekRange.toIntOrNull() ?: currentWeek else currentWeek
        ScheduleItemDetailSheet(item = item, getStartTime = getStartTime, getEndTime = getEndTime,
            onDismiss = { detailItem = null }, onEdit = {
                detailItem = null
                val course = (item as? ScheduleItem.CourseItem)?.course ?: return@ScheduleItemDetailSheet
                onCourseLongPress(course)
            },
            courseColors = CourseColors.getColors(colorEngine, count = 8),
            colorGroupMode = colorGroupMode,
            colorIndex = item.colorIndex,
            currentWeek = targetWeek,
            diffColorPerWeek = diffColorPerWeek)
    }

    if (showWeekPicker) {
        WeekPickerSheet(totalWeeks, currentWeek, { onWeekChange(it); showWeekPicker = false }, { showWeekPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleItemDetailSheet(item: ScheduleItem, getStartTime: (Int) -> String, getEndTime: (Int) -> String, onDismiss: () -> Unit, onEdit: () -> Unit, courseColors: List<Pair<Color, Color>> = CourseColors.getColors(0, count = 8), colorGroupMode: Int = 0, colorIndex: Int = item.colorIndex, dotColor: Color? = null, currentWeek: Int = 0, diffColorPerWeek: Boolean = false) {
    val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
    val hctColors = remember(item, colorGroupMode, currentWeek, diffColorPerWeek, isDark) {
        val realClassroomIdx = if (colorGroupMode == 1) item.colorIndex % 10 else 0
        val targetWeek = if (item.isExam) item.weekRange.toIntOrNull() ?: currentWeek else currentWeek
        com.classapp.schedule.util.CourseColors.getColorSync(colorGroupMode, item.name, item.classroom, classroomIndex = realClassroomIdx, week = targetWeek, diffColorPerWeek = diffColorPerWeek, isDark = isDark)
    }

    // Smart remark cleaner: filter time lines + strip "考试" text
    val cleanedRemark = remember(item.remark, item.isExam) {
        if (!item.isExam) item.remark
        else item.remark.split("\n")
            .filter { line -> !line.contains(":") && !line.contains("时间") && line.isNotBlank() }
            .map { it.replace("考试", "").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val detailDotColor = dotColor ?: hctColors.container
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(detailDotColor))
                Spacer(modifier = Modifier.width(12.dp))
                if (item.isExam) {
                    Box(
                        modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(4.dp)).background(hctColors.content.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("考试", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = hctColors.content, maxLines = 1)
                    }
                }
                Text(item.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(16.dp))
            val dayNames = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
            DetailRow("星期", dayNames.getOrElse(item.dayOfWeek) { "" })
            if (item.isCustomTime) {
                DetailRow("时间", "${item.customStartTime} - ${item.customEndTime}")
            } else {
                DetailRow("节次", "${item.startPeriod}-${item.endPeriod()}节")
                DetailRow("时间", "${getStartTime(item.startPeriod)} - ${getEndTime(item.endPeriod())}")
            }
            if (item.teacher.isNotEmpty()) DetailRow("教师", item.teacher)
            if (item.classroom.isNotEmpty()) DetailRow("教室", item.classroom)
            DetailRow("周次", item.weekRange)
            if (cleanedRemark.isNotEmpty()) DetailRow("备注", cleanedRemark)
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
            Text(stringResource(R.string.select_week), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
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
        if (nextStart != null && target >= end && target <= nextStart) {
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
