package com.ty.gdust_schedule

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
import com.ty.gdust_schedule.data.Course
import com.ty.gdust_schedule.ui.about.AboutScreen
import com.ty.gdust_schedule.ui.course.CourseEditScreen
import com.ty.gdust_schedule.ui.exam.ExamScreen
import com.ty.gdust_schedule.ui.login.LoginScreen
import com.ty.gdust_schedule.ui.login.WebViewLoginScreen
import com.ty.gdust_schedule.ui.manage.CourseManageScreen
import com.ty.gdust_schedule.ui.settings.SettingsScreen
import com.ty.gdust_schedule.ui.today.TodayScreen
import com.ty.gdust_schedule.ui.weekly.WeeklyScheduleScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Weekly : Screen("weekly")
    data object Courses : Screen("courses")
    data object About : Screen("about")
    data object AboutDetail : Screen("about_detail")
    data object Login : Screen("login")
    data object WebViewLogin : Screen("webview_login")
    data object Exam : Screen("exam")
    data object CourseEdit : Screen("course_edit?courseId={courseId}&isExam={isExam}") {
        fun createRoute(courseId: Long? = null, isExam: Boolean = false): String =
            "course_edit?courseId=${courseId ?: -1L}&isExam=$isExam"
    }
    data object ExamEdit : Screen("exam_edit?examId={examId}") {
        fun createRoute(examId: Long? = null): String =
            "exam_edit?examId=${examId ?: -1L}"
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
    // Gemini Fix: displayCourses 在 NavHost 外面，作为活的 State 随数据变化实时更新
    val displayCourses = if (showHiddenCourses) courses else courses.filter { !it.isHidden }
    val examList by viewModel.examList.collectAsState(initial = emptyList())
    val showExamSchedule by viewModel.showExamSchedule.collectAsState(initial = false)

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

    val mainScaffoldBg = if (com.ty.gdust_schedule.ui.theme.LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    Scaffold(
        containerColor = mainScaffoldBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small
                )
            }
        },
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
                                com.ty.gdust_schedule.util.HapticFeedback.light(navView)
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
                    courses = displayCourses, colorCourses = courses, currentWeek = realCurrentWeek,
                    colorEngine = colorEngine, colorGroupMode = colorGroupMode,
                    exams = examList,
                    showExamSchedule = showExamSchedule,
                    examLookaheadWeeks = examLookaheadWeeks,
                    semesterStart = semesterStart,
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) },
                    onExamEdit = { navController.navigate(Screen.ExamEdit.createRoute(it.id)) },
                    diffColorPerWeek = diffColorPerWeek
                )
            }

            composable(Screen.Weekly.route) {
                WeeklyScheduleScreen(
                    courses = displayCourses, colorCourses = courses, currentWeek = selectedWeek,
                    totalWeeks = totalWeeks, periodsPerDay = periodsPerDay,
                    gridHeight = gridHeight, gridCorner = gridCorner,
                    gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel,
                    autoGridHeight = autoGridHeight, firstDayOfWeek = firstDayOfWeek,
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
                    onExamEdit = { navController.navigate(Screen.ExamEdit.createRoute(it.id)) },
                    onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) },
                    onRefresh = { viewModel.refreshFromSchool() },
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    diffColorPerWeek = diffColorPerWeek
                )
            }

            composable(Screen.Courses.route) {
                CourseManageScreen(
                    courses = courses,
                    colorEngine = colorEngine,
                    colorGroupMode = colorGroupMode,
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
                        context.startActivity(android.content.Intent(context, com.ty.gdust_schedule.ui.settings.SettingsActivity::class.java))
                    },
                    onOpenAbout = { navController.navigate(Screen.AboutDetail.route) },
                    onOpenExam = { navController.navigate(Screen.Exam.route) }
                )
            }

            composable(Screen.AboutDetail.route) {
                com.ty.gdust_schedule.ui.about.AboutDetailPage(
                    onBack = { navController.popBackStack() }
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
                    colorCourses = courses,
                    customExams = emptyList(),
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
                    colorEngine = colorEngine,
                    colorGroupMode = colorGroupMode,
                    examLookaheadWeeks = examLookaheadWeeks,
                    onExamLookaheadWeeksChange = { viewModel.setExamLookaheadWeeks(it) },
                    showExamSchedule = showExamSchedule,
                    onShowExamScheduleChange = { viewModel.setShowExamSchedule(it) },
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    currentWeek = selectedWeek,
                    diffColorPerWeek = diffColorPerWeek,
                    onAddExam = { navController.navigate(Screen.ExamEdit.createRoute()) },
                    onEditExam = { navController.navigate(Screen.ExamEdit.createRoute(it.id)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CourseEdit.route,
                arguments = listOf(
                    navArgument("courseId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("isExam") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getLong("courseId") ?: -1L
                val isExam = backStackEntry.arguments?.getBoolean("isExam") ?: false
                var currentCourse by remember { mutableStateOf(if (courseId > 0) courses.find { it.id == courseId } else null) }

                LaunchedEffect(courseId) {
                    if (courseId > 0 && currentCourse == null) {
                        currentCourse = viewModel.getCourseById(courseId)
                    }
                }

                CourseEditScreen(
                    course = currentCourse, allCourses = courses, periodsPerDay = periodsPerDay,
                    onSave = { savedCourse, hiddenScopeName -> viewModel.saveCourse(savedCourse, hiddenScopeName); navController.popBackStack() },
                    onDelete = { viewModel.deleteCourse(it); navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ExamEdit.route,
                arguments = listOf(navArgument("examId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getLong("examId") ?: -1L
                val currentExam = remember(examId, examList) {
                    if (examId != -1L) examList.find { it.id == examId } else null
                }
                com.ty.gdust_schedule.ui.exam.ExamEditScreen(
                    exam = currentExam,
                    semesterStart = semesterStart,
                    onSave = { examEntities ->
                        viewModel.saveExams(examEntities)
                        android.widget.Toast.makeText(context, "成功导入 ${examEntities.size} 场考试！", android.widget.Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onDelete = { entity ->
                        viewModel.deleteExamById(entity.id)
                        navController.popBackStack()
                    },
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
