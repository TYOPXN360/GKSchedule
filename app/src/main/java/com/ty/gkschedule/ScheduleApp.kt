package com.ty.gkschedule

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.ui.about.AboutScreen
import com.ty.gkschedule.ui.login.LoginScreen
import com.ty.gkschedule.ui.login.WebViewLoginScreen
import com.ty.gkschedule.ui.manage.CourseManageScreen
import com.ty.gkschedule.ui.settings.SettingsScreen
import com.ty.gkschedule.ui.today.TodayScreen
import com.ty.gkschedule.ui.weekly.WeeklyScheduleScreen
import kotlinx.coroutines.launch

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
    onNavigate: (Screen) -> Unit
) {
    var collapsed by remember { mutableStateOf(false) }
    // ponytail: 一份进度+一份spring，两个视图时序物理上不错开
    val p = remember { Animatable(if (collapsed) 1f else 0f) }
    LaunchedEffect(collapsed) {
        p.animateTo(
            if (collapsed) 1f else 0f,
            spring(dampingRatio = 0.9f, stiffness = 380f)
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
        // ponytail: 系统RenderEffect backdrop blur，底色只给60%透明度让课表透上来取色
        val pillBg = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (blurEnabled) 0.6f else 1f
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barBottom)
                .height(barH)
                .graphicsLayer {
                    translationX = -p.value * (swPx / 2f + size.width / 2f)
                }
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color = pillBg)
                .then(if (blurEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier)
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
                val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
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
                    .clickable(onClick = { collapsed = true })
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
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = barBottom)
                .height(barH)
                .graphicsLayer {
                    translationX = -(1f - p.value) * size.width
                }
                .clip(
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 0.dp, bottomStart = 0.dp,
                        topEnd = barH / 2, bottomEnd = barH / 2
                    )
                )
                .background(color = pillBg)
                .then(if (blurEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) Modifier.blur(24.dp) else Modifier)
                .clickable(enabled = p.value > 0.5f) { collapsed = false }
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
    val scope = rememberCoroutineScope()
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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                if (json != null) viewModel.importJson(json)
            } catch (_: Exception) {}
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarScreens = listOf("today", "weekly", "courses", "about")
    val showBottomBar = currentRoute in bottomBarScreens
    val navView = androidx.compose.ui.platform.LocalView.current
    val mainScaffoldBg = if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    val tabIndex = mapOf("today" to 0, "weekly" to 1, "courses" to 2, "about" to 3)
    // ponytail: 子页(login/webview/exam/edit…)无tab序号 → tabIndexOf回0；导航靠isTabRoute分支而非序号比较
    fun tabIndexOf(route: String?): Int = tabIndex.entries.firstOrNull { route?.startsWith(it.key) == true }?.value ?: 0
    fun isTabRoute(route: String?): Boolean = route != null && tabIndex.keys.any { route.startsWith(it) }

    // Simple approach: NavHost with conditional bottom bar
    Scaffold(
        containerColor = mainScaffoldBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(snackbarData = snackbarData, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface, shape = MaterialTheme.shapes.small)
            }
        },
        bottomBar = {
            // ponytail: 普通底栏走Scaffold槽位常驻位移；悬浮pill走内容区Box覆盖层，两套互斥
            if (!compactNavBar) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec())
                ) {
                    NavigationBar {
                        navItemList().forEach { (screen, triple) ->
                            NavigationBarItem(
                                icon = { Icon(triple.first, contentDescription = triple.second) },
                                label = { Text(triple.second) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    com.ty.gkschedule.util.HapticFeedback.light(navView)
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(startPage) { saveState = true }
                                            launchSingleTop = true; restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startPage,
                // ponytail: 悬浮pill覆盖不占位，内容吃满；普通底栏槽位常驻padding恒定
                modifier = Modifier.fillMaxSize(),
                // ponytail: tab↔tab按左右方向滑；进子页统一右进；返回统一镜像左出（预测返回手势方向）
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (isTabRoute(from) && isTabRoute(to)) {
                    if (tabIndexOf(to) >= tabIndexOf(from)) {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.fadeInSpec())
                    } else {
                        slideInHorizontally(initialOffsetX = { -it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.fadeInSpec())
                    }
                } else {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.pageEnterSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.subPageEnterSpec())
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (isTabRoute(from) && isTabRoute(to)) {
                    if (tabIndexOf(to) >= tabIndexOf(from)) {
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.fadeOutSpec())
                    } else {
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.fadeOutSpec())
                    }
                } else {
                    slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.pageExitSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.subPageExitSpec())
                }
            },
            // ponytail: 系统级预测返回只播popExit+popEnter；seek跟手要求这两者是纯位移+透明度，scale(0.9f)会让预览抽搐
            popEnterTransition = {
                fadeIn(animationSpec = tween(150))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { (it * 0.15f).toInt() }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(150))
            }
        ) {
            composable(Screen.Today.route) { TodayScreen(courses = displayCourses, colorCourses = courses, currentWeek = realCurrentWeek, colorEngine = colorEngine, colorGroupMode = colorGroupMode, exams = examList, showExamSchedule = showExamSchedule, examLookaheadWeeks = examLookaheadWeeks, semesterStart = semesterStart, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, onCourseLongPress = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, diffColorPerWeek = diffColorPerWeek) }
            composable(Screen.Weekly.route) { WeeklyScheduleScreen(courses = displayCourses, colorCourses = courses, currentWeek = selectedWeek, totalWeeks = totalWeeks, periodsPerDay = periodsPerDay, gridHeight = gridHeight, gridCorner = gridCorner, gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel, autoGridHeight = autoGridHeight, firstDayOfWeek = firstDayOfWeek, mergeConsecutive = mergeConsecutive, showTimeLabel = showTimeLabel, detailedSplit = detailedSplit, colorEngine = colorEngine, colorGroupMode = colorGroupMode, showDateInHeader = showDateInHeader, hideEmptyWeeks = hideEmptyWeeks, semesterStart = semesterStart, exams = examList, showExamSchedule = showExamSchedule, realCurrentWeek = realCurrentWeek, isRefreshing = isRefreshing, onWeekChange = { viewModel.setWeek(it.coerceIn(1, totalWeeks)) }, onCourseClick = { }, onCourseLongPress = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, onAddCourse = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java)) }, onRefresh = { viewModel.refreshFromSchool() }, onScreenshotHidePill = { viewModel.setPillHidden(true) }, onScreenshotRestorePill = { viewModel.setPillHidden(false) }, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, diffColorPerWeek = diffColorPerWeek) }
            composable(Screen.Courses.route) { CourseManageScreen(courses = courses, colorEngine = colorEngine, colorGroupMode = colorGroupMode, onCourseClick = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java).apply { putExtra("courseId", it.id) }) }, onAddCourse = { context.startActivity(Intent(context, com.ty.gkschedule.ui.course.CourseEditActivity::class.java)) }, onDeleteCourse = { viewModel.deleteCourse(it) }, onDeleteAll = { viewModel.deleteAllCourses() }, onScrollHidePill = { viewModel.setPillHidden(it) }) }
            composable(Screen.About.route) {
                val savedStudentId by viewModel.savedStudentIdFlow.collectAsState()
                val savedRealName by viewModel.savedRealName.collectAsState(initial = "")
                val savedDeptName by viewModel.savedDeptName.collectAsState(initial = "")
                val totalWeeksVal by viewModel.totalWeeks.collectAsState(initial = 20)
                val periodsPerDayVal by viewModel.periodsPerDay.collectAsState(initial = 10)
                val displayWeeks = if (hideEmptyWeeks && courses.isNotEmpty()) { val weeksWithCourses = courses.flatMap { course -> (1..totalWeeksVal).filter { course.isInWeek(it) } }.toSet(); weeksWithCourses.size.coerceAtLeast(1) } else totalWeeksVal
                AboutScreen(loginState = loginState, savedStudentId = savedStudentId, savedRealName = savedRealName, savedDeptName = savedDeptName, semesterStart = semesterStart, totalWeeks = displayWeeks, periodsPerDay = periodsPerDayVal, captchaImageBase64 = captchaImage, onLogin = { navController.navigate(Screen.Login.route) }, onLogout = { viewModel.logout() }, onQuickRelogin = { cap -> viewModel.quickRelogin(cap) }, onRefreshCaptcha = { viewModel.refreshCaptcha() }, onOpenSettings = { context.startActivity(Intent(context, com.ty.gkschedule.ui.settings.SettingsActivity::class.java)) }, onOpenAbout = { context.startActivity(Intent(context, com.ty.gkschedule.ui.about.AboutActivity::class.java)) }, onOpenExam = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java)) })
            }
            composable(Screen.Login.route) {
                val hasSavedCredentials by viewModel.hasSavedCredentials.collectAsState(initial = false)
                LoginScreen(loginState = loginState, captchaImageBase64 = captchaImage, hasSavedCredentials = hasSavedCredentials, onRefreshCaptcha = { viewModel.refreshCaptcha() }, onLogin = { sid, pwd, cap -> viewModel.login(sid, pwd, cap) }, onQuickRelogin = { cap -> viewModel.quickRelogin(cap) }, onWebViewLogin = { navController.navigate(Screen.WebViewLogin.route) }, onBack = { viewModel.clearLoginError(); navController.popBackStack() })
                LaunchedEffect(Unit) { viewModel.clearLoginError(); if (captchaImage == null) viewModel.refreshCaptcha() }
                LaunchedEffect(loginState) { if (loginState is LoginState.Success || loginState is LoginState.ImportResult) { kotlinx.coroutines.delay(500); navController.popBackStack() } }
            }
            composable(Screen.WebViewLogin.route) { WebViewLoginScreen(loginState = loginState, api = viewModel.api, onLoginSuccess = { loginCode -> viewModel.webViewLogin(loginCode) }, onBack = { navController.popBackStack() })
                // ponytail: 扫码成功直接回我的页，跳过中间账号密码页
                LaunchedEffect(loginState) { if (loginState is LoginState.Success || loginState is LoginState.ImportResult) { kotlinx.coroutines.delay(1200); navController.popBackStack(Screen.Login.route, inclusive = true) } }
            }
        }
        // 悬浮pill：跟随tab显隐做位移，内部收/展另有自己的左右对滑；
        // ponytail: 课程管理下滑时pillHidden=true，向下淡出隐藏
        if (compactNavBar) {
            val pillHidden by viewModel.pillHidden.collectAsState(initial = false)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar && !pillHidden,
                    modifier = Modifier.padding(bottom = 24.dp),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.fadeInSpec()),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.fadeOutSpec())
                ) {
                    FloatingPillNavBar(currentRoute = currentRoute, pillContentMode = pillContentMode, screenshotHidden = pillHidden, blurEnabled = blurEffect) { screen ->
                        com.ty.gkschedule.util.HapticFeedback.light(navView)
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(startPage) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }
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
