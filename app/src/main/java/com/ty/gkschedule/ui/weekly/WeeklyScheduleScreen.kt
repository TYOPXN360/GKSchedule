package com.ty.gkschedule.ui.weekly

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import com.ty.gkschedule.ui.theme.BlurCard
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.R
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.data.ScheduleResolver
import com.ty.gkschedule.data.ScheduleItem
import com.ty.gkschedule.util.CourseColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    courses: List<Course>,
    colorCourses: List<Course> = courses,
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
    exams: List<com.ty.gkschedule.data.ExamEntity> = emptyList(),
    showExamSchedule: Boolean = false,
    onWeekChange: (Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    onCourseLongPress: (Course) -> Unit,
    onExamEdit: (com.ty.gkschedule.data.ExamEntity) -> Unit = {},
    onAddCourse: () -> Unit,
    onRefresh: () -> Unit,
    onScreenshotHidePill: () -> Unit = {},
    onScreenshotRestorePill: () -> Unit = {},
    realCurrentWeek: Int = currentWeek,
    firstDayOfWeek: Int = 1,
    diffColorPerWeek: Boolean = false,
    blurEnabled: Boolean = true,
    // ponytail: 课表块名多行——开=最多3行折行，关=单行截断
    blockMultiline: Boolean = true,
    // ponytail: 默认底栏避让开关——悬浮pill不占位，传false不留白
    applyBottomBarInset: Boolean = true,
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
    var detailColorIndex by remember { mutableStateOf<Int?>(null) }
    var detailClassroomColorIndex by remember { mutableIntStateOf(0) }
    // ponytail: miuix源层，FAB组drawBackdrop吃糊
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticView = androidx.compose.ui.platform.LocalView.current
    val labelWidthDp = if (showPeriodLabel) { if (showTimeLabel) 64.dp else 36.dp } else 0.dp

    // Build unified schedule items (replaces id<0 hack)
    val scheduleItems = remember(courses, exams, showExamSchedule, semesterStart, getStartTime, getEndTime) {
        ScheduleResolver.buildItems(courses, exams, showExamSchedule, semesterStart, getStartTime, getEndTime)
    }
    val colorScheduleItems = remember(colorCourses, exams, semesterStart, getStartTime, getEndTime) {
        ScheduleResolver.buildItems(colorCourses, exams, true, semesterStart, getStartTime, getEndTime)
    }

    // Compute visible weeks (skip empty weeks if hideEmptyWeeks is on)
    // 🔥 防死锁：强制包含 currentWeek 和 realCurrentWeek，防止 indexOf 返回 -1
    val visibleWeeks = remember(scheduleItems, totalWeeks, hideEmptyWeeks, currentWeek, realCurrentWeek) {
        ScheduleResolver.visibleWeeks(scheduleItems, totalWeeks, hideEmptyWeeks, currentWeek, realCurrentWeek)
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

    val ptrState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            com.ty.gkschedule.util.HapticFeedback.medium(hapticView)
            onRefresh()
        },
        modifier = Modifier.fillMaxSize(),
        state = ptrState,
        // ponytail: 下拉头自组毛玻璃球——位移公式与尺寸复刻官方 IndicatorBox，花团用官方 ContainedLoadingIndicator
        indicator = {
            val hlDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
            val hlStroke = if (hlDark) {
                top.yukonga.miuix.kmp.blur.highlight.Highlight.GlassStrokeSmallDark
            } else {
                top.yukonga.miuix.kmp.blur.highlight.Highlight.GlassStrokeSmallLight
            }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxDistancePx = with(density) { 160.dp.toPx() }
            @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // 官方同款：fraction*maxDistance - 自身高 ⇒ 起始完全藏在顶边之上，下拉才从屏幕顶边探出
                    .graphicsLayer {
                        translationY = ptrState.distanceFraction * maxDistancePx - size.height
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { androidx.compose.foundation.shape.CircleShape },
                        effects = { blur(28.dp.toPx()) },
                        highlight = { hlStroke },
                    )
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.40f),
                        androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // ponytail: 官方同款分支——刷新中不能喂 progress，否则 distanceFraction 停在 1f，花团被钉死在满进度那一帧
                androidx.compose.animation.Crossfade(targetState = isRefreshing) { refreshing ->
                    if (refreshing) {
                        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                        androidx.compose.material3.ContainedLoadingIndicator(
                            modifier = Modifier.size(
                                width = androidx.compose.material3.LoadingIndicatorDefaults.ContainerWidth,
                                height = androidx.compose.material3.LoadingIndicatorDefaults.ContainerHeight,
                            ),
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                        androidx.compose.material3.ContainedLoadingIndicator(
                            progress = { ptrState.distanceFraction },
                            modifier = Modifier
                                .size(
                                    width = androidx.compose.material3.LoadingIndicatorDefaults.ContainerWidth,
                                    height = androidx.compose.material3.LoadingIndicatorDefaults.ContainerHeight,
                                )
                                // 官方同款：超过 1 之后整颗连续自转，避免跳变
                                .graphicsLayer {
                                    val f = ptrState.distanceFraction
                                    if (f > 1f) rotationZ = -(f - 1f) * 180f
                                },
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    ) {
        val coroutineScope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current
        val rootView = androidx.compose.ui.platform.LocalView.current
        var cropTopPx by remember { mutableIntStateOf(0) }
        var cropBottomPx by remember { mutableIntStateOf(0) }
        var hideFabs by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // ponytail: 底色与今日/我的统一——暗surface/亮surfaceContainer（ScheduleApp主源同值）
                .layerBackdrop(backdrop)
                .background(if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer)
                .statusBarsPadding()
                // ponytail: 停底栏上（仅默认底栏；悬浮pill不占位不留白）
                .padding(
                    bottom = (if (applyBottomBarInset) 80.dp else 0.dp) +
                        (if (applyBottomBarInset) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp)
                )
        ) {
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
                        com.ty.gkschedule.util.HapticFeedback.light(hapticView)
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
                        com.ty.gkschedule.util.HapticFeedback.light(hapticView)
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
            val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
            val themeHue = CourseColors.currentThemeHue()
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .onGloballyPositioned { cropBottomPx = it.positionInRoot().y.toInt() + it.size.height }
            ) { page ->
                val week = visibleWeeks.getOrElse(page) { currentWeek }
                val weekBlocks = remember(week, colorGroupMode, diffColorPerWeek, scheduleItems, colorScheduleItems, mergeConsecutive, detailedSplit, periodsPerDay, getStartTime, getEndTime) {
                    ScheduleResolver.buildRenderBlocks(
                        items = scheduleItems,
                        week = week,
                        colorGroupMode = colorGroupMode,
                        diffColorPerWeek = diffColorPerWeek,
                        mergeConsecutive = mergeConsecutive,
                        detailedSplit = detailedSplit,
                        periodsPerDay = periodsPerDay,
                        getStartTime = getStartTime,
                        getEndTime = getEndTime,
                        colorItems = colorScheduleItems
                    )
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
                        val blockBaseColors = remember(weekBlocks, colorEngine, colorGroupMode, diffColorPerWeek, isDark, themeHue) {
                            weekBlocks.map { block ->
                                CourseColors.getColorSync(
                                    engine = colorEngine,
                                    groupMode = colorGroupMode,
                                    courseName = block.item.name,
                                    classroom = block.item.classroom,
                                    classroomIndex = block.classroomColorIdx,
                                    colorIndex = block.colorIdx,
                                    week = week,
                                    diffColorPerWeek = diffColorPerWeek,
                                    isDark = isDark,
                                    themeHue = themeHue
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
                                        com.ty.gkschedule.util.HapticFeedback.medium(hapticView)
                                        detailItem = block.item
                                        detailColorIndex = block.colorIdx
                                        detailClassroomColorIndex = block.classroomColorIdx
                                    }
                                    .semantics { contentDescription = block.item.name }
                                    .padding(4.dp)
                            ) {
                                val textColor = remember(block, colorEngine, colorGroupMode, diffColorPerWeek, isDark, themeHue) {
                                    CourseColors.getColorSync(
                                        engine = colorEngine,
                                        groupMode = colorGroupMode,
                                        courseName = block.item.name,
                                        classroom = block.item.classroom,
                                        classroomIndex = block.classroomColorIdx,
                                        colorIndex = block.colorIdx,
                                        week = week,
                                        diffColorPerWeek = diffColorPerWeek,
                                        isDark = isDark,
                                        themeHue = themeHue
                                    ).content
                                }
                                // ponytail: 块内名+地点共享剩余高——名先吃(多行开不限行)，吃完还剩才给地点，溢出才省略
                                Column(modifier = Modifier.fillMaxSize()) {
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
                                    // ponytail: 名上地点下——名先吃(多行开不限行)，地点吃剩高，溢出才省略
                                    if (blockMultiline) {
                                        Text(block.item.name, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                                        if (block.item.classroom.isNotEmpty()) {
                                            androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                                                if (maxHeight > 18.dp) {
                                                    Text(block.item.classroom, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f), maxLines = Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(block.item.name, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (block.item.classroom.isNotEmpty()) {
                                            Text(block.item.classroom, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
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
            targetValue = if (fabExpanded) (2 * 56 + 2 * 12).dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
            label = "expandHeight"
        )
        // ponytail: FAB抬升只给默认底栏——悬浮pill不占位不抬
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = if (applyBottomBarInset) 80.dp else 0.dp)) {
            // Toggle — bottom right
            // ponytail: FAB糊——miuix drawBackdrop吃课表源，关模糊开关时回退纯色；blurEffect开关透传
            // ponytail: clip shape与按钮外轮廓同源——不一致必漏角
            val fabShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            FloatingActionButton(
                onClick = { com.ty.gkschedule.util.HapticFeedback.light(hapticView); fabExpanded = !fabExpanded },
                shape = fabShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                // ponytail: 关开关纯色——糊开才75%透
                modifier = if (blurEnabled) Modifier
                    .align(Alignment.BottomEnd)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { fabShape },
                        effects = { blur(28.dp.toPx()) }
                    ) else Modifier.align(Alignment.BottomEnd),
                containerColor = if (blurEnabled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) { Text(if (fabExpanded) "—" else "+", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp)) }

            // Column: expandable buttons only (declared first so back-to-week draws on top)
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 68.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = fabExpanded, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    FloatingActionButton(
                        onClick = { com.ty.gkschedule.util.HapticFeedback.medium(hapticView); onRefresh() },
                shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        // ponytail: 关开关回纯色——drawBackdrop无视开关会漏糊
                        modifier = if (blurEnabled) Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { fabShape },
                            effects = { blur(28.dp.toPx()) }
                        ) else Modifier,
                        containerColor = if (blurEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        // ponytail: 官方LoadingIndicator(M3 1.5.0-alpha27 pin版，BOM不管)
                        if (isRefreshing) {
                            @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
                            androidx.compose.material3.LoadingIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(28.dp))
                        }
                    }
                }
                // ponytail: 加号已删（与课程管理重复），剩刷新+截图
                AnimatedVisibility(visible = fabExpanded, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
                    FloatingActionButton(onClick = {
                        com.ty.gkschedule.util.HapticFeedback.medium(hapticView)
                        // ponytail: 截图前藏FAB+悬浮pill，截完恢复；pill藏显走ViewModel，跨组件
                        coroutineScope.launch {
                            try {
                                hideFabs = true
                                onScreenshotHidePill()
                                kotlinx.coroutines.delay(300)
                                val fb = android.graphics.Bitmap.createBitmap(rootView.width, rootView.height, android.graphics.Bitmap.Config.ARGB_8888)
                                rootView.draw(android.graphics.Canvas(fb))
                                hideFabs = false
                                onScreenshotRestorePill()
                                val c = android.graphics.Bitmap.createBitmap(fb, 0, cropTopPx.coerceIn(0, fb.height), fb.width, cropBottomPx.coerceIn(cropTopPx.coerceIn(0, fb.height), fb.height) - cropTopPx.coerceIn(0, fb.height))
                                val s = com.ty.gkschedule.util.ImageExport.saveBitmapToGallery(context, c, "Pictures/Screenshots/schedule_${System.currentTimeMillis()}.png")
                                android.widget.Toast.makeText(context, if (s) "已保存到 Pictures/Screenshots" else "保存失败", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                hideFabs = false
                                onScreenshotRestorePill()
                                android.widget.Toast.makeText(context, "截图失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, shape = fabShape, elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        // ponytail: 关开关回纯色——drawBackdrop无视开关会漏糊
                        modifier = if (blurEnabled) Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { fabShape },
                            effects = { blur(28.dp.toPx()) }
                        ) else Modifier,
                        containerColor = if (blurEnabled) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) { Icon(Icons.Default.CropFree, "Screenshot") }
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
                    onClick = { com.ty.gkschedule.util.HapticFeedback.medium(hapticView); onWeekChange(realCurrentWeek) },
                shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    // ponytail: 关开关回纯色——drawBackdrop无视开关会漏糊
                    modifier = if (blurEnabled) Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { fabShape },
                        effects = { blur(28.dp.toPx()) }
                    ) else Modifier,
                    containerColor = if (blurEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) { Icon(if (currentWeek > realCurrentWeek) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, stringResource(R.string.back_to_current_week)) }
            }
        } // Box
        } // if (!hideFabs)
    } // PullToRefreshBox

    // Detail sheet — 🔥 归一：无论是否考试课，统一走 HCT 动态分配
    detailItem?.let { item ->
        val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
        val targetWeek = if (item.isExam) item.weekRange.toIntOrNull() ?: currentWeek else currentWeek
        ScheduleItemDetailSheet(item = item, getStartTime = getStartTime, getEndTime = getEndTime,
            onDismiss = { detailItem = null }, onEdit = {
                detailItem = null
                when (item) {
                    is ScheduleItem.CourseItem -> onCourseLongPress(item.course)
                    is ScheduleItem.ExamItem -> onExamEdit(item.exam)
                }
            },
            colorEngine = colorEngine,
            colorGroupMode = colorGroupMode,
            colorIndex = detailColorIndex,
            classroomColorIndex = detailClassroomColorIndex,
            currentWeek = targetWeek,
            diffColorPerWeek = diffColorPerWeek, blurEnabled = blurEnabled)
    }

    if (showWeekPicker) {
        WeekPickerSheet(totalWeeks, currentWeek, { onWeekChange(it); showWeekPicker = false }, { showWeekPicker = false }, blurEnabled = blurEnabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleItemDetailSheet(item: ScheduleItem, getStartTime: (Int) -> String, getEndTime: (Int) -> String, onDismiss: () -> Unit, onEdit: () -> Unit, colorEngine: Int = 0, colorGroupMode: Int = 0, colorIndex: Int? = null, classroomColorIndex: Int = 0, dotColor: Color? = null, currentWeek: Int = 0, diffColorPerWeek: Boolean = false, blurEnabled: Boolean = true) {
    val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
    val themeHue = CourseColors.currentThemeHue()
    val hctColors = remember(item, colorEngine, colorGroupMode, colorIndex, classroomColorIndex, currentWeek, diffColorPerWeek, isDark, themeHue) {
        val targetWeek = if (item.isExam) item.weekRange.toIntOrNull() ?: currentWeek else currentWeek
        CourseColors.getColorSync(
            engine = colorEngine,
            groupMode = colorGroupMode,
            courseName = item.name,
            classroom = item.classroom,
            classroomIndex = classroomColorIndex,
            colorIndex = colorIndex,
            week = targetWeek,
            diffColorPerWeek = diffColorPerWeek,
            isDark = isDark,
            themeHue = themeHue
        )
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // ponytail: 只糊卡片不糊全屏——BlurCard载体等大+LayerDrawable合成；handle自画进覆盖区
        containerColor = Color.Transparent,
        scrimColor = Color.Transparent,
        dragHandle = { },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        BlurCard(
            enabled = blurEnabled,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // ponytail: 自画把手进BlurCard覆盖区，与卡片同底色
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.width(32.dp).height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
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
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("编辑")
            }
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
private fun WeekPickerSheet(totalWeeks: Int, currentWeek: Int, onWeekSelected: (Int) -> Unit, onDismiss: () -> Unit, blurEnabled: Boolean = true) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = Color.Transparent,
        dragHandle = { },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        BlurCard(
            enabled = blurEnabled,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.width(32.dp).height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
            Text(stringResource(R.string.select_week), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
            for (row in 0 until (totalWeeks + 4) / 5) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 5) {
                        val week = row * 5 + col + 1
                        if (week <= totalWeeks) {
                            val pickerView = androidx.compose.ui.platform.LocalView.current
                            FilledTonalButton(onClick = {
                                com.ty.gkschedule.util.HapticFeedback.light(pickerView)
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
}
