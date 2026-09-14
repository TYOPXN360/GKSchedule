package com.ty.gkschedule

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import com.ty.gkschedule.ui.theme.LocalAppIsDark
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.ui.about.AboutScreen
import com.ty.gkschedule.ui.login.LoginScreen
import com.ty.gkschedule.ui.login.WebViewLoginScreen
import com.ty.gkschedule.ui.manage.CourseManageScreen
import com.ty.gkschedule.ui.settings.SettingsScreen
import com.ty.gkschedule.ui.today.TodayScreen
import com.ty.gkschedule.ui.weekly.WeeklyScheduleScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Weekly : Screen("weekly")
    data object Courses : Screen("courses")
    data object About : Screen("about")
    data object Login : Screen("login")
    data object WebViewLogin : Screen("webview_login")
}

@Composable
private fun navItemList(): List<Pair<Screen, Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>> =
    listOf(
        Screen.Today to Triple(Icons.Default.Today, "今日", "today"),
        Screen.Weekly to Triple(Icons.Default.DateRange, "课表", "weekly"),
        Screen.Courses to Triple(Icons.AutoMirrored.Filled.LibraryBooks, "课程", "courses"),
        Screen.About to Triple(Icons.Default.Person, "我的", "about")
    )

// 悬浮药丸底栏：展开居中底部；收起整条左滑，只剩左边半胶囊书签
// ponytail: 单Animatable进度p驱动双graphicsLayer位移（方案B）；选中补全用静态if，无涟漪抖动
@Composable
private fun FloatingPillNavBar(
    currentRoute: String?,
    pillContentMode: Int,
    screenshotHidden: Boolean = false,
    blurEnabled: Boolean = true,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    collapsed: Boolean,
    onCollapsedChange: (Boolean) -> Unit,
    visible: Boolean,
    onNavigate: (Screen) -> Unit
) {
    // ponytail: 一份进度+一份spring，两个视图时序物理上不错开
    val p = remember { Animatable(if (collapsed) 1f else 0f) }
    LaunchedEffect(collapsed) {
        p.animateTo(
            if (collapsed) 1f else 0f,
            spring(dampingRatio = 0.9f, stiffness = 380f)
        )
    }
    // ponytail: 滚动隐藏走位移不断组合，collapsed/p保住不断动画
    // ponytail: 位移挂Row自身(size=barH)，挂全屏Box会位移整屏高=瞬间消失
    // ponytail: 280ms FastOutSlowIn向下淡出，位移自身高+底边距确保完全滑出
    val slide = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        slide.animateTo(
            targetValue = if (visible) 0f else 1f,
            animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // ponytail: 截屏瞬间整条gone，比alpha=0少一帧合成，rootView.draw抓不到残影
        if (screenshotHidden) return@BoxWithConstraints
        val screenW = maxWidth
        // 尺寸档：按屏宽分三档，小屏不再硬塞，全部等比缩小
        val iconSize = when {
            screenW < 340.dp -> 18.dp
            screenW < 400.dp -> 20.dp
            else -> 22.dp
        }
        val barBottom = 24.dp
        val pillHPad = when {
            screenW < 340.dp -> 5.dp
            screenW < 400.dp -> 6.dp
            else -> 7.dp
        }
        val itemHPadBoth = when {
            screenW < 340.dp -> 9.dp
            screenW < 400.dp -> 10.dp
            else -> 12.dp
        }
        val itemHPadSingle = when {
            screenW < 340.dp -> 8.dp
            screenW < 400.dp -> 9.dp
            else -> 10.dp
        }
        val itemVPad = when {
            screenW < 340.dp -> 8.dp
            else -> 9.dp
        }
        val textStyle = MaterialTheme.typography.labelMedium
        val gapW = when {
            screenW < 340.dp -> 3.dp
            else -> 4.dp
        }
        // ponytail: 高度锁死同一barH，显式等高=同行，不靠padding凑
        val barH = iconSize + itemVPad * 2 + pillHPad * 2
        val swPx = with(LocalDensity.current) { screenW.toPx() }
        // 药丸：BottomCenter，p=1时右边缘越过x=0整条出左屏
        // ponytail: miuix同窗口backdrop糊（窗口级API只糊别家窗口，同窗口必须走这条）
        // ponytail: 纯色背景Blur(C)=C无反差——shadow+Highest色阶+1dp描边接管轮廓，糊只管彩色区
        val pillShape = androidx.compose.foundation.shape.CircleShape
        val isDark = LocalAppIsDark.current
        val pillBg = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
            alpha = if (blurEnabled) 0.65f else 1f
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barBottom)
                .height(barH)
                .graphicsLayer {
                    translationX = -p.value * (swPx / 2f + size.width / 2f)
                    translationY = slide.value * (size.height + barBottom.toPx())
                    alpha = (1f - slide.value).coerceIn(0f, 1f)
                }
                .shadow(
                    elevation = 6.dp,
                    shape = pillShape,
                    spotColor = Color.Black.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(pillShape)
                .then(
                    if (blurEnabled) Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = { blur(28.dp.toPx()) }
                    ) else Modifier.background(pillBg)
                )
                .background(pillBg)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                    shape = pillShape
                )
                .padding(horizontal = pillHPad, vertical = pillHPad),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItemList().forEach { (screen, triple) ->
                var expanded by remember(screen.route) { mutableStateOf(true) }
                val selected = currentRoute == screen.route
                val showBoth = pillContentMode == 0
                val showText = if (showBoth) true else pillContentMode == 2
                val showIcon = if (showBoth) true else pillContentMode == 1
                // 选中项强制补全另一半；expanded只管收起，不参与补全
                val visibleText = (showText || selected) && (expanded || selected)
                val visibleIcon = showIcon || selected || !visibleText
                // ponytail: 选中底只给55%透明，透出药丸糊层，不盖糊
                val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f) else Color.Transparent
                val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(bg)
                        .clickable(onClick = {
                            if (selected) expanded = !expanded
                            else onNavigate(screen)
                        })
                        .padding(horizontal = if (visibleText && visibleIcon) itemHPadBoth else itemHPadSingle, vertical = itemVPad),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (visibleIcon) Icon(triple.first, contentDescription = triple.second, tint = fg, modifier = Modifier.size(iconSize))
                    if (visibleText) {
                        if (visibleIcon) Spacer(modifier = Modifier.width(gapW))
                        Text(triple.second, style = textStyle, color = fg, maxLines = 1)
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(onClick = { onCollapsedChange(true) })
                    .padding(horizontal = itemHPadSingle, vertical = itemVPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChevronLeft, contentDescription = "收起",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        // 书签：BottomStart静止位即贴边；p=0时藏到屏外
        val bookmarkShape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 0.dp, bottomStart = 0.dp,
            topEnd = barH / 2, bottomEnd = barH / 2
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = barBottom)
                .height(barH)
                .graphicsLayer {
                    translationX = -(1f - p.value) * size.width
                    translationY = slide.value * (size.height + barBottom.toPx())
                    alpha = (1f - slide.value).coerceIn(0f, 1f)
                }
                .shadow(
                    elevation = 6.dp,
                    shape = bookmarkShape,
                    spotColor = Color.Black.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(bookmarkShape)
                .then(
                    if (blurEnabled) Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { bookmarkShape },
                        effects = { blur(28.dp.toPx()) }
                    ) else Modifier.background(pillBg)
                )
                .background(pillBg)
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                    shape = bookmarkShape
                )
                .clickable(enabled = p.value > 0.5f) { onCollapsedChange(false) }
                .padding(start = 6.dp, end = 12.dp, top = itemVPad, bottom = itemVPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChevronRight, contentDescription = "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun ScheduleApp(
    viewModel: ScheduleViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val courses by viewModel.courses.collectAsState(initial = emptyList())
    val selectedWeek by viewModel.selectedWeek.collectAsState()
    val realCurrentWeek by viewModel.currentWeek.collectAsState(initial = 1)
    val totalWeeks by viewModel.totalWeeks.collectAsState(initial = 20)
    val periodsPerDay by viewModel.periodsPerDay.collectAsState(initial = 10)
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState(initial = 1)
    val gridHeight by viewModel.gridHeight.collectAsState(initial = 52)
    val gridCorner by viewModel.gridCorner.collectAsState(initial = 8)
    val gridSpacing by viewModel.gridSpacing.collectAsState(initial = 2)
    val showPeriodLabel by viewModel.showPeriodLabel.collectAsState(initial = true)
    val autoGridHeight by viewModel.autoGridHeight.collectAsState(initial = true)
    val mergeConsecutive by viewModel.mergeConsecutive.collectAsState(initial = true)
    val showTimeLabel by viewModel.showTimeLabel.collectAsState(initial = true)
    val detailedSplit by viewModel.detailedSplit.collectAsState(initial = false)
    val colorEngine by viewModel.colorEngine.collectAsState(initial = 0)
    val colorGroupMode by viewModel.colorGroupMode.collectAsState(initial = 2)
    val showDateInHeader by viewModel.showDateInHeader.collectAsState(initial = false)
    val hideEmptyWeeks by viewModel.hideEmptyWeeks.collectAsState(initial = false)
    val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
    val semesterStart by viewModel.semesterStart.collectAsState(initial = java.time.LocalDate.now())
    val darkMode by viewModel.darkMode.collectAsState(initial = "system")
    val language by viewModel.language.collectAsState(initial = "system")
    val loginState by viewModel.loginState.collectAsState()
    val captchaImage by viewModel.captchaImage.collectAsState()
    val examLookaheadWeeks by viewModel.examLookaheadWeeks.collectAsState(initial = 1)
    val diffColorPerWeek by viewModel.diffColorPerWeek.collectAsState(initial = false)
    val showHiddenCourses by viewModel.showHiddenCourses.collectAsState(initial = false)
    val compactNavBar by viewModel.compactNavBar.collectAsState(initial = true)
    val pillContentMode by viewModel.pillContentMode.collectAsState(initial = 0)
    val blurEffect by viewModel.blurEffect.collectAsState(initial = true)
    val startPage by viewModel.startPage.collectAsState(initial = "today")
    val displayCourses = if (showHiddenCourses) courses else courses.filter { !it.isHidden }
    val examList by viewModel.examList.collectAsState(initial = emptyList())
    val showExamSchedule by viewModel.showExamSchedule.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // 专门给截图用的无动画拔除状态，严禁与日常滚动 pillHidden 混用！
    var screenshotHidden by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                if (json != null) viewModel.importJson(json)
            } catch (_: Exception) {}
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // ponytail: 官方规范——选中走hierarchy判（嵌套图/参数路由不漏），切换pop到graph.findStartDestination
    val bottomBarScreens = listOf("today", "weekly", "courses", "about")
    val currentRoute = currentDestination?.route
    // ponytail: ReSukiSU同款——4 tab常驻HorizontalPager（NavHost只留子页），底栏切=animateScrollToPage横滑
    val tabRoutes = listOf("today", "weekly", "courses", "about")
    val tabIndexMap = mapOf("today" to 0, "weekly" to 1, "courses" to 2, "about" to 3)
    val scope = rememberCoroutineScope()
    val startTabIndex = (tabIndexMap[startPage] ?: 0).coerceIn(0, 3)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = startTabIndex,
        pageCount = { tabRoutes.size }
    )
    var uiSelectedPage by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(startTabIndex) }
    var pagerAnimating by remember { mutableStateOf(false) }
    // ponytail: 启动页可切——startPage变化且pager还没动过时对齐
    LaunchedEffect(startPage) {
        val target = (tabIndexMap[startPage] ?: 0).coerceIn(0, 3)
        if (pagerState.currentPage == startTabIndex && target != startTabIndex) {
            pagerState.scrollToPage(target)
            uiSelectedPage = target
        }
    }
    val handlePageChange: (Int) -> Unit = remember(pagerState, scope) {
        { page ->
            uiSelectedPage = page
            if (page != pagerState.currentPage) {
                scope.launch {
                    pagerAnimating = true
                    try {
                        pagerState.animateScrollToPage(page)
                    } finally {
                        pagerAnimating = false
                    }
                }
            }
        }
    }
    LaunchedEffect(pagerState) {
        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!pagerAnimating) uiSelectedPage = page
        }
    }
    // ponytail: NavHost只剩tabs+子页——tabs常驻时route=tabs，子页时route=子页名
    val showBottomBar = currentRoute == null || currentRoute == "tabs"
    fun navigateTab(route: String) {
        (tabIndexMap[route])?.let { handlePageChange(it) }
    }
    // ponytail: BackHandler必须在NavHost之后注册才优先（后加先调），放函数末尾；tab页吞预测秒回，首页放行回桌面
    val navView = androidx.compose.ui.platform.LocalView.current
    val mainScaffoldBg = if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    val tabIndex = mapOf("today" to 0, "weekly" to 1, "courses" to 2, "about" to 3)
    // ponytail: 子页(login/webview/exam/edit…)无tab序号 → tabIndexOf回0；导航靠isTabRoute分支而非序号比较
    fun tabIndexOf(route: String?): Int = tabIndex.entries.firstOrNull { route?.startsWith(it.key) == true }?.value ?: 0
    fun isTabRoute(route: String?): Boolean = route != null && tabIndex.keys.any { route.startsWith(it) }

    // Simple approach: NavHost with conditional bottom bar
    // ponytail: snackbar糊要吃主源——backdrop提Scaffold外，内容层与snackbar同源
    val backdrop = rememberLayerBackdrop()
    Scaffold(
        containerColor = mainScaffoldBg,
        // ponytail: 默认底栏snackbar回Scaffold默认槽（底栏占位自动顶起）；悬浮pill才自挂贴边
        snackbarHost = {
            if (!compactNavBar) SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            // ponytail: 普通底栏走Scaffold槽位常驻位移；悬浮pill走内容区Box覆盖层，两套互斥
            // ponytail: 底栏糊——drawBackdrop吃主源（与pill/snackbar同源），容器透明+onDrawSurface单层底
            if (!compactNavBar) {
                // ponytail: 红底验证通过=采样链路通——问题在85%盖太厚+糊挂AnimatedVisibility外层整块红；糊回NavigationBar内层，底降55%
                val barBg = MaterialTheme.colorScheme.surfaceContainer
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec())
                ) {
                    NavigationBar(
                        modifier = if (blurEffect) Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { androidx.compose.foundation.shape.RoundedCornerShape(0.dp) },
                            effects = { blur(28.dp.toPx()) },
                            onDrawSurface = { drawRect(barBg.copy(alpha = 0.55f)) }
                        ) else Modifier,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        navItemList().forEach { (screen, triple) ->
                            NavigationBarItem(
                                icon = { Icon(triple.first, contentDescription = triple.second) },
                                label = { Text(triple.second) },
                                selected = uiSelectedPage == (tabIndexMap[screen.route] ?: 0),
                                onClick = {
                                    com.ty.gkschedule.util.HapticFeedback.light(navView)
                                    navigateTab(screen.route)
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // ponytail: miuix源层——内容标layerBackdrop吃糊；药丸挂兄弟层(环=RenderThread栈溢出，见08c190d)
        // ponytail: 外层不垫状态栏，各Tab自己吃（今日/课表无顶栏挂statusBarsPadding，管理页顶栏自己吃）
        // ponytail: 源层必须全屏不被底栏截断，否则默认底栏只能糊到靠上一小条；避让下移到NavHost
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(mainScaffoldBg)
        ) {
            NavHost(
                navController = navController,
                startDestination = "tabs",
                // ponytail: tabs页常驻Pager（ReSukiSU同款横滑），子页走Top level快淡
                enterTransition = {
                    fadeIn(animationSpec = tween(200, easing = LinearEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200, easing = LinearEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(200, easing = LinearEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200, easing = LinearEasing))
                },
                modifier = Modifier.fillMaxSize()
            ) {
            composable("tabs") {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.ty.gkschedule.ui.util.LocalPagerState provides pagerState,
                    com.ty.gkschedule.ui.util.LocalSelectedPage provides uiSelectedPage,
                    com.ty.gkschedule.ui.util.LocalHandlePageChange provides handlePageChange
                ) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // ponytail: 课表页内嵌周HorizontalPager——外层禁手滑，只许点底栏横滑，防手势打架
                        userScrollEnabled = false,
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.ty.gkschedule.ui.util.LocalPagerPage provides pageIndex
                        ) {
                            when (tabRoutes[pageIndex]) {
                                "today" -> TodayScreen(courses = displayCourses, colorCourses = courses, currentWeek = realCurrentWeek, colorEngine = colorEngine, colorGroupMode = colorGroupMode, exams = examList, showExamSchedule = showExamSchedule, examLookaheadWeeks = examLookaheadWeeks, semesterStart = semesterStart, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, onCourseLongPress = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, diffColorPerWeek = diffColorPerWeek)
                                "weekly" -> WeeklyScheduleScreen(courses = displayCourses, colorCourses = courses, currentWeek = selectedWeek, totalWeeks = totalWeeks, periodsPerDay = periodsPerDay, gridHeight = gridHeight, gridCorner = gridCorner, gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel, autoGridHeight = autoGridHeight, firstDayOfWeek = firstDayOfWeek, mergeConsecutive = mergeConsecutive, showTimeLabel = showTimeLabel, detailedSplit = detailedSplit, colorEngine = colorEngine, colorGroupMode = colorGroupMode, showDateInHeader = showDateInHeader, hideEmptyWeeks = hideEmptyWeeks, semesterStart = semesterStart, exams = examList, showExamSchedule = showExamSchedule, realCurrentWeek = realCurrentWeek, isRefreshing = isRefreshing, onWeekChange = { viewModel.setWeek(it.coerceIn(1, totalWeeks)) }, onCourseClick = { }, onCourseLongPress = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, onAddCourse = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java)) }, onRefresh = { viewModel.refreshFromSchool() }, onScreenshotHidePill = { screenshotHidden = true }, onScreenshotRestorePill = { screenshotHidden = false }, blurEnabled = blurEffect, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, diffColorPerWeek = diffColorPerWeek)
                                "courses" -> CourseManageScreen(courses = courses, blurEnabled = blurEffect, colorEngine = colorEngine, colorGroupMode = colorGroupMode, onCourseClick = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onAddCourse = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java)) }, onDeleteCourse = { viewModel.deleteCourse(it) }, onDeleteAll = { viewModel.deleteAllCourses() }, onScrollHidePill = { viewModel.setPillHidden(it) })
                                else -> {
                                    val savedStudentId by viewModel.savedStudentIdFlow.collectAsState()
                                    val savedRealName by viewModel.savedRealName.collectAsState(initial = "")
                                    val savedDeptName by viewModel.savedDeptName.collectAsState(initial = "")
                                    val totalWeeksVal by viewModel.totalWeeks.collectAsState(initial = 20)
                                    val periodsPerDayVal by viewModel.periodsPerDay.collectAsState(initial = 10)
                                    val displayWeeks = if (hideEmptyWeeks && courses.isNotEmpty()) { val weeksWithCourses = courses.flatMap { course -> (1..totalWeeksVal).filter { course.isInWeek(it) } }.toSet(); weeksWithCourses.size.coerceAtLeast(1) } else totalWeeksVal
                                    AboutScreen(loginState = loginState, savedStudentId = savedStudentId, savedRealName = savedRealName, savedDeptName = savedDeptName, semesterStart = semesterStart, totalWeeks = displayWeeks, periodsPerDay = periodsPerDayVal, captchaImageBase64 = captchaImage, onLogin = { navController.navigate(Screen.Login.route) }, onLogout = { viewModel.logout() }, onQuickRelogin = { cap -> viewModel.quickRelogin(cap) }, onRefreshCaptcha = { viewModel.refreshCaptcha() }, onOpenSettings = { context.startActivity(Intent(context, com.ty.gkschedule.ui.settings.SettingsActivity::class.java)) }, onOpenAbout = { context.startActivity(Intent(context, com.ty.gkschedule.ui.about.AboutActivity::class.java)) }, onOpenExam = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java)) })
                                }
                            }
                        }
                    }
                }
            }
            composable(Screen.Login.route) {
                val hasSavedCredentials by viewModel.hasSavedCredentials.collectAsState(initial = false)
                LoginScreen(loginState = loginState, captchaImageBase64 = captchaImage, hasSavedCredentials = hasSavedCredentials, onRefreshCaptcha = { viewModel.refreshCaptcha() }, onLogin = { sid, pwd, cap -> viewModel.login(sid, pwd, cap) }, onQuickRelogin = { cap -> viewModel.quickRelogin(cap) }, onWebViewLogin = { navController.navigate(Screen.WebViewLogin.route) }, onBack = { viewModel.clearLoginError(); navController.popBackStack() }, blurEnabled = blurEffect)
                LaunchedEffect(Unit) { viewModel.clearLoginError(); if (captchaImage == null) viewModel.refreshCaptcha() }
                LaunchedEffect(loginState) { if (loginState is LoginState.Success || loginState is LoginState.ImportResult) { kotlinx.coroutines.delay(500); navController.popBackStack() } }
            }
            composable(Screen.WebViewLogin.route) { WebViewLoginScreen(loginState = loginState, api = viewModel.api, onLoginSuccess = { loginCode -> viewModel.webViewLogin(loginCode) }, onBack = { navController.popBackStack() }, blurEnabled = blurEffect)
                // ponytail: 扫码成功直接回我的页，跳过中间账号密码页
                LaunchedEffect(loginState) { if (loginState is LoginState.Success || loginState is LoginState.ImportResult) { kotlinx.coroutines.delay(1200); navController.popBackStack(Screen.Login.route, inclusive = true) } }
            }
        }
        } // 源层Box只含NavHost
        // 悬浮pill：跟随tab显隐做位移，内部收/展另有自己的左右对滑；
        // ponytail: 源层兄弟节点(断环)；滚动隐藏走位移不断组合，收起态常驻
        if (compactNavBar) {
            val pillHidden by viewModel.pillHidden.collectAsState(initial = false)
            val pillCollapsed by viewModel.pillCollapsed.collectAsState(initial = false)
            Box(Modifier.fillMaxSize().padding(bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
                FloatingPillNavBar(
                    currentRoute = tabRoutes.getOrElse(uiSelectedPage) { "today" }, pillContentMode = pillContentMode, screenshotHidden = screenshotHidden,
                    blurEnabled = blurEffect, backdrop = backdrop,
                    collapsed = pillCollapsed, onCollapsedChange = { viewModel.setPillCollapsed(it) },
                    visible = showBottomBar && !(pillHidden && !pillCollapsed)
                ) { screen ->
                    com.ty.gkschedule.util.HapticFeedback.light(navView)
                    navigateTab(screen.route)
                }
            }
            // ponytail: snackbar贴pill上——pill高barH+底边24，snack底=24+barH+8贴上沿
            if (showBottomBar) {
                androidx.compose.foundation.layout.BoxWithConstraints(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
                ) {
                    // ponytail: barH三档屏宽22/20/18图标——snack底垫同步三档，否则小屏错位
                    val icon = if (maxWidth < 340.dp) 18.dp else if (maxWidth < 400.dp) 20.dp else 22.dp
                    val vPad = if (maxWidth < 340.dp) 8.dp else 9.dp
                    val hPad = if (maxWidth < 340.dp) 5.dp else if (maxWidth < 400.dp) 6.dp else 7.dp
                    val snackBottom = 24.dp + icon + vPad * 2 + hPad * 2 + 8.dp
                    Box(Modifier.fillMaxSize().padding(bottom = snackBottom), contentAlignment = Alignment.BottomCenter) {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                }
            }
        } else {
            // ponytail: 默认底栏snackbar已回Scaffold默认槽，这里不再自挂
        } // pill兄弟层
    }
    // ponytail: Pager即栈——返回=回第0页（ReSukiSU同款普通BackHandler）；首页放行回桌面
    androidx.activity.compose.BackHandler(enabled = showBottomBar && uiSelectedPage != 0) {
        handlePageChange(0)
    }
}

@Composable
fun ScheduleGridForExport(courses: List<Course>, periodsPerDay: Int, gridHeight: Int, gridCorner: Int, gridSpacing: Int) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val courseMap = mutableMapOf<String, Course>()
    courses.forEach { course -> for (p in course.startPeriod..course.endPeriod()) { courseMap["${course.dayOfWeek}_$p"] = course } }
    Column(modifier = Modifier.padding(8.dp)) {
        Row { Box(modifier = Modifier.weight(1f)); daysOfWeek.forEach { day -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(day, style = MaterialTheme.typography.labelSmall) } } }
        (1..periodsPerDay).forEach { period -> Row { Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("$period", style = MaterialTheme.typography.labelSmall) }; for (day in 1..7) { val course = courseMap["${day}_$period"]; if (course != null && course.startPeriod == period) { Box(modifier = Modifier.weight(1f).padding(gridSpacing.dp).height((gridHeight * course.periods).dp).clip(RoundedCornerShape(gridCorner.dp)).background(androidx.compose.ui.graphics.Color(0xFFE8F5E9)).padding(4.dp)) { Text(course.name, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF2E7D32)) } } else if (course == null) { Spacer(modifier = Modifier.weight(1f).padding(gridSpacing.dp).height(gridHeight.dp)) } } } }
    }
}
