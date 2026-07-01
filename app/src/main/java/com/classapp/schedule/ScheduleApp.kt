package com.classapp.schedule

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
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
import com.classapp.schedule.data.Course
import com.classapp.schedule.ui.about.AboutScreen
import com.classapp.schedule.ui.course.CourseEditScreen
import com.classapp.schedule.ui.exam.ExamScreen
import com.classapp.schedule.ui.login.LoginScreen
import com.classapp.schedule.ui.login.WebViewLoginScreen
import com.classapp.schedule.ui.manage.CourseManageScreen
import com.classapp.schedule.ui.settings.SettingsScreen
import com.classapp.schedule.ui.today.TodayScreen
import com.classapp.schedule.ui.weekly.WeeklyScheduleScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Weekly : Screen("weekly")
    data object Courses : Screen("courses")
    data object About : Screen("about")
    data object Login : Screen("login")
    data object WebViewLogin : Screen("webview_login")
    data object Exam : Screen("exam")
    data object CourseEdit : Screen("course_edit?courseId={courseId}") {
        fun createRoute(courseId: Long? = null): String =
            if (courseId != null) "course_edit?courseId=$courseId" else "course_edit"
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
    val reminderMinutes by viewModel.reminderMinutes.collectAsState(initial = 0)
    val semesterStart by viewModel.semesterStart.collectAsState(initial = java.time.LocalDate.now())
    val darkMode by viewModel.darkMode.collectAsState(initial = "system")
    val language by viewModel.language.collectAsState(initial = "system")
    val loginState by viewModel.loginState.collectAsState()
    val captchaImage by viewModel.captchaImage.collectAsState()
    val examLookaheadWeeks by viewModel.examLookaheadWeeks.collectAsState(initial = 2)
    val examList by viewModel.examList.collectAsState()
    val showExamSchedule by viewModel.showExamSchedule.collectAsState(initial = false)

    // Shared color palette — computed once
    val courseColorPalette = com.classapp.schedule.util.CourseColors.getColors(colorEngine, count = 8)

    // Course name|classroom → Color, same iteration logic as WeeklyScheduleScreen's weekBlocks
    val courseColorMap = remember(courses, realCurrentWeek, colorGroupMode, courseColorPalette) {
        val weekCourses = courses.filter { it.isInWeek(realCurrentWeek) }
        val nameToIdx = mutableMapOf<String, Int>()
        val keyToIdx = mutableMapOf<String, Int>()
        val classroomSatMap = mutableMapOf<String, MutableMap<String, Int>>()
        var nextColor = 0
        val result = mutableMapOf<Long, Color>()
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
            val satOffset = if (colorGroupMode == 1) ci % 10 else 0
            result[c.id] = com.classapp.schedule.util.CourseColors.getBackgroundStatic(ci, courseColorPalette, satOffset, realCurrentWeek)
        }
        result
    }

    // Exam kcmc|cdmc → Color. Each exam's color must be computed using the nameToIdx
    // of THE WEEK THAT EXAM IS IN, not the current week. Otherwise the today page's bar
    // (which can show exams from any lookahead week) would use the wrong nameToIdx and
    // mismatch the schedule block.
    val examColorMap = remember(courses, colorGroupMode, courseColorPalette, examList, showExamSchedule, semesterStart) {
        if (!showExamSchedule || examList.isEmpty()) emptyMap<String, Color>()
        else {
            // Find all weeks that contain at least one exam
            val examWeeks = examList.mapNotNull { exam ->
                val d = try { java.time.LocalDate.parse(exam.getExamDate()) } catch (_: Exception) { null }
                if (d == null) null else {
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(semesterStart, d).toInt()
                    if (daysDiff < 0) null else (daysDiff / 7) + 1
                }
            }.distinct().sorted()

            val result = mutableMapOf<String, Color>()
            for (week in examWeeks) {
                val weekStart = semesterStart.plusDays(((week - 1) * 7).toLong())
                val weekEnd = weekStart.plusDays(7)
                val weekExams = examList.filter { exam ->
                    val d = try { java.time.LocalDate.parse(exam.getExamDate()) } catch (_: Exception) { null }
                    d != null && !d.isBefore(weekStart) && d.isBefore(weekEnd)
                }
                val weekCourses = courses.filter { it.isInWeek(week) }
                val nameToIdx = mutableMapOf<String, Int>()
                val keyToIdx = mutableMapOf<String, Int>()
                val classroomSatMap = mutableMapOf<String, MutableMap<String, Int>>()
                var nextColor = 0
                // Build index from regular courses first (same order as scheduleCourses)
                weekCourses.forEach { c ->
                    when (colorGroupMode) {
                        0 -> nameToIdx.getOrPut(c.name) { nextColor++ }
                        1 -> {
                            val baseIdx = nameToIdx.getOrPut(c.name) { nextColor++ }
                            val satMap = classroomSatMap.getOrPut(c.name) { mutableMapOf() }
                            satMap.getOrPut(c.classroom) { satMap.size }
                        }
                        else -> keyToIdx.getOrPut("${c.name}|${c.classroom}") { nextColor++ }
                    }
                }
                // Assign exam indices for exams in this week
                weekExams.forEach { exam ->
                    val ci = when (colorGroupMode) {
                        0 -> nameToIdx.getOrPut(exam.kcmc) { nextColor++ }
                        1 -> {
                            val baseIdx = nameToIdx.getOrPut(exam.kcmc) { nextColor++ }
                            val satMap = classroomSatMap.getOrPut(exam.kcmc) { mutableMapOf() }
                            val satOffset = satMap.getOrPut(exam.cdmc) { satMap.size }
                            baseIdx * 10 + satOffset
                        }
                        else -> keyToIdx.getOrPut("${exam.kcmc}|${exam.cdmc}") { nextColor++ }
                    }
                    val satOffset = if (colorGroupMode == 1) ci % 10 else 0
                    result["${exam.kcmc}|${exam.cdmc}"] =
                        com.classapp.schedule.util.CourseColors.getBackgroundStatic(ci, courseColorPalette, satOffset, week)
                }
            }
            result
        }
    }

    // Show snackbar messages
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    // File picker for JSON import
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

    val bottomBarScreens = listOf(Screen.Today, Screen.Weekly, Screen.Courses, Screen.About)
    val showBottomBar = currentRoute in bottomBarScreens.map { it.route } && currentRoute != Screen.Login.route

    val navView = androidx.compose.ui.platform.LocalView.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(
                        Screen.Today to Triple(Icons.Default.Today, stringResource(R.string.nav_today), "today"),
                        Screen.Weekly to Triple(Icons.Default.DateRange, stringResource(R.string.nav_schedule), "weekly"),
                        Screen.Courses to Triple(Icons.AutoMirrored.Filled.LibraryBooks, stringResource(R.string.nav_courses), "courses"),
                        Screen.About to Triple(Icons.Default.Person, stringResource(R.string.nav_about), "about")
                    ).forEach { (screen, triple) ->
                        NavigationBarItem(
                            icon = { Icon(triple.first, contentDescription = triple.second) },
                            label = { Text(triple.second) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                com.classapp.schedule.util.HapticFeedback.light(navView)
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Today.route) { saveState = true }
                                        launchSingleTop = true; restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Tab index for directional animation
        val tabIndex = mapOf(
            "today" to 0, "weekly" to 1, "courses" to 2, "about" to 3
        )
        fun tabIndexOf(route: String?): Int = tabIndex.entries
            .firstOrNull { route?.startsWith(it.key) == true }?.value ?: 0

        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp)),
            enterTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (to >= from) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + fadeIn(tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + fadeIn(tween(300))
                }
            },
            exitTransition = {
                val from = tabIndexOf(initialState.destination.route)
                val to = tabIndexOf(targetState.destination.route)
                if (to >= from) {
                    slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) + fadeOut(tween(150))
                } else {
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) + fadeOut(tween(150))
                }
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) + fadeOut(tween(150))
            }
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    courses = courses, currentWeek = realCurrentWeek,
                    colorEngine = colorEngine, colorGroupMode = colorGroupMode,
                    exams = examList,
                    showExamSchedule = showExamSchedule,
                    examLookaheadWeeks = examLookaheadWeeks,
                    semesterStart = semesterStart,
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) },
                    courseColorPalette = courseColorPalette,
                    courseColorMap = courseColorMap,
                    examColorMap = examColorMap
                )
            }

            composable(Screen.Weekly.route) {
                WeeklyScheduleScreen(
                    courses = courses, currentWeek = selectedWeek,
                    totalWeeks = totalWeeks, periodsPerDay = periodsPerDay,
                    gridHeight = gridHeight, gridCorner = gridCorner,
                    gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel,
                    autoGridHeight = autoGridHeight,
                    mergeConsecutive = mergeConsecutive,
                    showTimeLabel = showTimeLabel,
                    detailedSplit = detailedSplit,
                    colorEngine = colorEngine,
                    colorGroupMode = colorGroupMode,
                    showDateInHeader = showDateInHeader,
                    hideEmptyWeeks = hideEmptyWeeks,
                    semesterStart = semesterStart,
                    exams = examList,
                    showExamSchedule = showExamSchedule,
                    realCurrentWeek = realCurrentWeek,
                    isRefreshing = isRefreshing,
                    onWeekChange = { viewModel.setWeek(it.coerceIn(1, totalWeeks)) },
                    onCourseClick = { },
                    onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) },
                    onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) },
                    onRefresh = { viewModel.refreshFromSchool() },
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    courseColorPalette = courseColorPalette,
                    courseColorMap = courseColorMap,
                    examColorMap = examColorMap
                )
            }

            composable(Screen.Courses.route) {
                CourseManageScreen(
                    courses = courses,
                    onCourseClick = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) },
                    onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) },
                    onDeleteCourse = { viewModel.deleteCourse(it) },
                    onDeleteAll = { viewModel.deleteAllCourses() }
                )
            }

            composable(Screen.About.route) {
                val savedStudentId by viewModel.savedStudentIdFlow.collectAsState()
                val savedRealName by viewModel.savedRealName.collectAsState(initial = "")
                val savedDeptName by viewModel.savedDeptName.collectAsState(initial = "")
                val totalWeeksVal by viewModel.totalWeeks.collectAsState(initial = 20)
                val periodsPerDayVal by viewModel.periodsPerDay.collectAsState(initial = 10)
                val displayWeeks = if (hideEmptyWeeks && courses.isNotEmpty()) {
                    val weeksWithCourses = courses.flatMap { course -> (1..totalWeeksVal).filter { course.isInWeek(it) } }.toSet()
                    weeksWithCourses.size.coerceAtLeast(1)
                } else totalWeeksVal
                AboutScreen(
                    loginState = loginState,
                    savedStudentId = savedStudentId,
                    savedRealName = savedRealName,
                    savedDeptName = savedDeptName,
                    semesterStart = semesterStart,
                    totalWeeks = displayWeeks,
                    periodsPerDay = periodsPerDayVal,
                    captchaImageBase64 = captchaImage,
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onLogout = { viewModel.logout() },
                    onQuickRelogin = { cap -> viewModel.quickRelogin(cap) },
                    onRefreshCaptcha = { viewModel.refreshCaptcha() },
                    onOpenSettings = {
                        context.startActivity(android.content.Intent(context, com.classapp.schedule.ui.settings.SettingsActivity::class.java))
                    },
                    onOpenAbout = { },
                    onOpenExam = { navController.navigate(Screen.Exam.route) }
                )
            }

            composable(Screen.Login.route) {
                val hasSavedCredentials by viewModel.hasSavedCredentials.collectAsState(initial = false)
                LoginScreen(
                    loginState = loginState,
                    captchaImageBase64 = captchaImage,
                    hasSavedCredentials = hasSavedCredentials,
                    onRefreshCaptcha = { viewModel.refreshCaptcha() },
                    onLogin = { sid, pwd, cap -> viewModel.login(sid, pwd, cap) },
                    onQuickRelogin = { cap -> viewModel.quickRelogin(cap) },
                    onWebViewLogin = { navController.navigate(Screen.WebViewLogin.route) },
                    onBack = { viewModel.clearLoginError(); navController.popBackStack() }
                )

                // Auto-refresh captcha on first visit
                LaunchedEffect(Unit) {
                    viewModel.clearLoginError()
                    if (captchaImage == null) viewModel.refreshCaptcha()
                }

                // Navigate back on login success or import result
                LaunchedEffect(loginState) {
                    if (loginState is LoginState.Success || loginState is LoginState.ImportResult) {
                        kotlinx.coroutines.delay(500)
                        navController.popBackStack()
                    }
                }
            }

            // WebView login route
            composable(Screen.WebViewLogin.route) {
                WebViewLoginScreen(
                    api = viewModel.api,
                    onLoginSuccess = { loginCode ->
                        viewModel.webViewLogin(loginCode)
                    },
                    onBack = { navController.popBackStack() }
                )

                // Auto-navigate back on success
                LaunchedEffect(loginState) {
                    if (loginState is LoginState.Success || loginState is LoginState.ImportResult) {
                        kotlinx.coroutines.delay(500)
                        navController.popBackStack()
                    }
                }
            }

            composable(Screen.Exam.route) {
                val examLoading by viewModel.examLoading.collectAsState()
                val examYear by viewModel.examYear.collectAsState()
                val examSemester by viewModel.examSemester.collectAsState()
                val showExamReloginDialog by viewModel.showExamReloginDialog.collectAsState()
                ExamScreen(
                    exams = examList,
                    isLoading = examLoading,
                    semesterStart = semesterStart,
                    examYear = examYear,
                    examSemester = examSemester,
                    showReloginDialog = showExamReloginDialog,
                    captchaImageBase64 = captchaImage,
                    onYearChange = { viewModel.setExamYear(it) },
                    onSemesterChange = { viewModel.setExamSemester(it) },
                    onRefresh = { viewModel.refreshExamSchedule() },
                    onDismissRelogin = { viewModel.dismissExamReloginDialog() },
                    onRefreshCaptcha = { viewModel.refreshCaptcha() },
                    onQuickRelogin = { cap -> viewModel.quickRelogin(cap) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CourseEdit.route,
                arguments = listOf(navArgument("courseId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getLong("courseId") ?: -1L
                var currentCourse by remember { mutableStateOf(if (courseId > 0) courses.find { it.id == courseId } else null) }

                LaunchedEffect(courseId) {
                    if (courseId > 0 && currentCourse == null) {
                        currentCourse = viewModel.getCourseById(courseId)
                    }
                }

                CourseEditScreen(
                    course = currentCourse, periodsPerDay = periodsPerDay,
                    onSave = { viewModel.saveCourse(it); navController.popBackStack() },
                    onDelete = { viewModel.deleteCourse(it); navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Simplified schedule grid for image export */
@Composable
fun ScheduleGridForExport(
    courses: List<Course>,
    periodsPerDay: Int,
    gridHeight: Int,
    gridCorner: Int,
    gridSpacing: Int
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val courseMap = mutableMapOf<String, Course>()
    courses.forEach { course ->
        for (p in course.startPeriod..course.endPeriod()) {
            courseMap["${course.dayOfWeek}_$p"] = course
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row {
            Box(modifier = Modifier.weight(1f))
            daysOfWeek.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        (1..periodsPerDay).forEach { period ->
            Row {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("$period", style = MaterialTheme.typography.labelSmall)
                }
                for (day in 1..7) {
                    val course = courseMap["${day}_$period"]
                    if (course != null && course.startPeriod == period) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(gridSpacing.dp)
                                .height((gridHeight * course.periods).dp)
                                .clip(RoundedCornerShape(gridCorner.dp))
                                .background(androidx.compose.ui.graphics.Color(0xFFE8F5E9))
                                .padding(4.dp)
                        ) {
                            Text(
                                course.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            )
                        }
                    } else if (course == null) {
                        Spacer(modifier = Modifier.weight(1f).padding(gridSpacing.dp).height(gridHeight.dp))
                    }
                }
            }
        }
    }
}
