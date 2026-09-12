package com.ty.gkschedule

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.ui.about.AboutScreen
import com.ty.gkschedule.ui.course.CourseEditScreen
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
    data object CourseEdit : Screen("course_edit?courseId={courseId}&isExam={isExam}") {
        fun createRoute(courseId: Long? = null, isExam: Boolean = false): String =
            "course_edit?courseId=${courseId ?: -1L}&isExam=$isExam"
    }
}

@Composable
private fun navItemList(): List<Pair<Screen, Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>> =
    listOf(
        Screen.Today to Triple(Icons.Default.Today, "今日", "today"),
        Screen.Weekly to Triple(Icons.Default.DateRange, "课表", "weekly"),
        Screen.Courses to Triple(Icons.AutoMirrored.Filled.LibraryBooks, "课程", "courses"),
        Screen.About to Triple(Icons.Default.Person, "我的", "about")
    )

// 悬浮药丸底栏：展开居中底部；收起缩到屏幕左边只剩箭头书签
// ponytail: 收/展是两套AnimatedVisibility左右对滑，不是同一条Row的if/else，位置变化才有动画
@Composable
private fun FloatingPillNavBar(
    currentRoute: String?,
    pillContentMode: Int,
    onNavigate: (Screen) -> Unit
) {
    var collapsed by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !collapsed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.fadeInSpec()),
            exit = slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.fadeOutSpec())
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .animateContentSize(animationSpec = tween(300))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItemList().forEach { (screen, triple) ->
                    var expanded by remember(screen.route) { mutableStateOf(true) }
                    val selected = currentRoute == screen.route
                    // ponytail: 设置项定默认显示；点选中项切换展开/收起；选中项在仅图标/仅名字模式下补全另一半
                    val showBoth = pillContentMode == 0
                    val showText = if (showBoth) true else pillContentMode == 2
                    val showIcon = if (showBoth) true else pillContentMode == 1
                    val visibleText = (showText || selected) && expanded
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
                            .padding(horizontal = if (visibleText && visibleIcon) 12.dp else 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (visibleIcon) Icon(triple.first, contentDescription = triple.second, tint = fg, modifier = Modifier.size(20.dp))
                        androidx.compose.animation.AnimatedVisibility(visible = visibleText && visibleIcon) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(triple.second, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(visible = visibleText && !visibleIcon) {
                            Text(triple.second, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
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
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, contentDescription = "收起",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        // 书签：贴屏幕左边，只露箭头
        androidx.compose.animation.AnimatedVisibility(
            visible = collapsed,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 48.dp),
            enter = slideInHorizontally(initialOffsetX = { -it / 2 }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()) + fadeIn(com.ty.gkschedule.ui.theme.M3Motion.fadeInSpec()),
            exit = slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec()) + fadeOut(com.ty.gkschedule.ui.theme.M3Motion.fadeOutSpec())
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 16.dp, bottomEnd = 16.dp
                        )
                    )
                    .clip(
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 0.dp, bottomStart = 0.dp,
                            topEnd = 16.dp, bottomEnd = 16.dp
                        )
                    )
                    .clickable(onClick = { collapsed = false })
                    .padding(start = 4.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChevronRight, contentDescription = "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
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
            composable(Screen.Today.route) { TodayScreen(courses = displayCourses, colorCourses = courses, currentWeek = realCurrentWeek, colorEngine = colorEngine, colorGroupMode = colorGroupMode, exams = examList, showExamSchedule = showExamSchedule, examLookaheadWeeks = examLookaheadWeeks, semesterStart = semesterStart, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, diffColorPerWeek = diffColorPerWeek) }
            composable(Screen.Weekly.route) { WeeklyScheduleScreen(courses = displayCourses, colorCourses = courses, currentWeek = selectedWeek, totalWeeks = totalWeeks, periodsPerDay = periodsPerDay, gridHeight = gridHeight, gridCorner = gridCorner, gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel, autoGridHeight = autoGridHeight, firstDayOfWeek = firstDayOfWeek, mergeConsecutive = mergeConsecutive, showTimeLabel = showTimeLabel, detailedSplit = detailedSplit, colorEngine = colorEngine, colorGroupMode = colorGroupMode, showDateInHeader = showDateInHeader, hideEmptyWeeks = hideEmptyWeeks, semesterStart = semesterStart, exams = examList, showExamSchedule = showExamSchedule, realCurrentWeek = realCurrentWeek, isRefreshing = isRefreshing, onWeekChange = { viewModel.setWeek(it.coerceIn(1, totalWeeks)) }, onCourseClick = { }, onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) }, onExamEdit = { context.startActivity(Intent(context, com.ty.gkschedule.ui.exam.ExamActivity::class.java).apply { putExtra("examId", it.id) }) }, onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) }, onRefresh = { viewModel.refreshFromSchool() }, getStartTime = { viewModel.getStartTime(it) }, getEndTime = { viewModel.getEndTime(it) }, diffColorPerWeek = diffColorPerWeek) }
            composable(Screen.Courses.route) { CourseManageScreen(courses = courses, colorEngine = colorEngine, colorGroupMode = colorGroupMode, onCourseClick = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) }, onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) }, onDeleteCourse = { viewModel.deleteCourse(it) }, onDeleteAll = { viewModel.deleteAllCourses() }) }
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
            composable(route = Screen.CourseEdit.route, arguments = listOf(navArgument("courseId") { type = NavType.LongType; defaultValue = -1L }, navArgument("isExam") { type = NavType.BoolType; defaultValue = false })) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getLong("courseId") ?: -1L
                val isExam = backStackEntry.arguments?.getBoolean("isExam") ?: false
                var currentCourse by remember { mutableStateOf(if (courseId > 0) courses.find { it.id == courseId } else null) }
                LaunchedEffect(courseId) { if (courseId > 0 && currentCourse == null) { currentCourse = viewModel.getCourseById(courseId) } }
                CourseEditScreen(course = currentCourse, allCourses = courses, periodsPerDay = periodsPerDay, onSave = { savedCourse, hiddenScopeName -> viewModel.saveCourse(savedCourse, hiddenScopeName); navController.popBackStack() }, onDelete = { viewModel.deleteCourse(it); navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
        }
        // 悬浮pill：跟随tab显隐做位移，内部收/展另有自己的左右对滑
        if (compactNavBar) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    modifier = Modifier.padding(bottom = 24.dp),
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideInSpec()),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = com.ty.gkschedule.ui.theme.M3Motion.tabSlideOutSpec())
                ) {
                    FloatingPillNavBar(currentRoute = currentRoute, pillContentMode = pillContentMode) { screen ->
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
