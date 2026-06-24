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
import com.classapp.schedule.ui.login.LoginScreen
import com.classapp.schedule.ui.manage.CourseManageScreen
import com.classapp.schedule.ui.settings.SettingsScreen
import com.classapp.schedule.ui.today.TodayScreen
import com.classapp.schedule.ui.weekly.WeeklyScheduleScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Weekly : Screen("weekly")
    data object Courses : Screen("courses")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Login : Screen("login")
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
    val isRefreshing by viewModel.isRefreshing.collectAsState(initial = false)
    val reminderMinutes by viewModel.reminderMinutes.collectAsState(initial = 0)
    val semesterStart by viewModel.semesterStart.collectAsState(initial = java.time.LocalDate.now())
    val darkMode by viewModel.darkMode.collectAsState(initial = "system")
    val language by viewModel.language.collectAsState(initial = "system")
    val loginState by viewModel.loginState.collectAsState()
    val captchaImage by viewModel.captchaImage.collectAsState()

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
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(tween(200))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(200))
            }
        ) {
            composable(Screen.Today.route) {
                TodayScreen(
                    courses = courses, currentWeek = realCurrentWeek,
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) },
                    onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) }
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
                    isRefreshing = isRefreshing,
                    onWeekChange = { viewModel.setWeek(it.coerceIn(1, totalWeeks)) },
                    onCourseClick = { },
                    onCourseLongPress = { navController.navigate(Screen.CourseEdit.createRoute(it.id)) },
                    onAddCourse = { navController.navigate(Screen.CourseEdit.createRoute()) },
                    onRefresh = { viewModel.refreshFromSchool() },
                    getStartTime = { viewModel.getStartTime(it) },
                    getEndTime = { viewModel.getEndTime(it) }
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
                AboutScreen(
                    loginState = loginState,
                    savedStudentId = savedStudentId,
                    savedRealName = savedRealName,
                    savedDeptName = savedDeptName,
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onLogout = { viewModel.logout() },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenAbout = { }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    semesterStart = semesterStart, totalWeeks = totalWeeks,
                    periodsPerDay = periodsPerDay, darkMode = darkMode,
                    language = language, firstDayOfWeek = firstDayOfWeek,
                    gridHeight = gridHeight, gridCorner = gridCorner,
                    gridSpacing = gridSpacing, showPeriodLabel = showPeriodLabel,
                    autoGridHeight = autoGridHeight,
                    mergeConsecutive = mergeConsecutive,
                    showTimeLabel = showTimeLabel,
                    detailedSplit = detailedSplit,
                    reminderMinutes = reminderMinutes,
                    onSemesterStartChange = { viewModel.setSemesterStart(it) },
                    onTotalWeeksChange = { viewModel.setTotalWeeks(it) },
                    onPeriodsPerDayChange = { viewModel.setPeriodsPerDay(it) },
                    onDarkModeChange = { viewModel.setDarkMode(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onFirstDayOfWeekChange = { viewModel.setFirstDayOfWeek(it) },
                    onGridHeightChange = { viewModel.setGridHeight(it) },
                    onGridCornerChange = { viewModel.setGridCorner(it) },
                    onGridSpacingChange = { viewModel.setGridSpacing(it) },
                    onShowPeriodLabelChange = { viewModel.setShowPeriodLabel(it) },
                    onAutoGridHeightChange = { viewModel.setAutoGridHeight(it) },
                    onMergeConsecutiveChange = { viewModel.setMergeConsecutive(it) },
                    onShowTimeLabelChange = { viewModel.setShowTimeLabel(it) },
                    onDetailedSplitChange = { viewModel.setDetailedSplit(it) },
                    onReminderMinutesChange = { viewModel.setReminderMinutes(it) },
                    onExportJson = {
                        scope.launch {
                            val json = com.classapp.schedule.util.JsonImportExport.exportToJson(courses)
                            if (viewModel.saveJsonToDownload(json)) {
                                android.widget.Toast.makeText(context, context.getString(R.string.export_success), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onImportJson = { importLauncher.launch("application/json") },
                    onExportIcs = {
                        scope.launch {
                            val ics = viewModel.exportIcs()
                            if (ics != null && viewModel.saveIcsToDownload(ics)) {
                                android.widget.Toast.makeText(context, context.getString(R.string.export_success), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportImage = {
                        scope.launch {
                            try {
                                val view = android.widget.FrameLayout(context)
                                val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
                                    setContent {
                                        MaterialTheme {
                                            ScheduleGridForExport(
                                                courses = courses,
                                                periodsPerDay = periodsPerDay,
                                                gridHeight = gridHeight,
                                                gridCorner = gridCorner,
                                                gridSpacing = gridSpacing
                                            )
                                        }
                                    }
                                }
                                view.addView(composeView)
                                val spec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.AT_MOST)
                                view.measure(spec, spec)
                                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                                val bitmap = android.graphics.Bitmap.createBitmap(
                                    view.measuredWidth.coerceAtLeast(1),
                                    view.measuredHeight.coerceAtLeast(1),
                                    android.graphics.Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                view.draw(canvas)
                                if (com.classapp.schedule.util.ImageExport.saveBitmapToGallery(context, bitmap)) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.export_success), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    loginState = loginState,
                    captchaImageBase64 = captchaImage,
                    onRefreshCaptcha = { viewModel.refreshCaptcha() },
                    onLogin = { sid, pwd, cap -> viewModel.login(sid, pwd, cap) },
                    onBack = { viewModel.clearLoginError(); navController.popBackStack() }
                )

                // Auto-refresh captcha on first visit
                LaunchedEffect(Unit) {
                    viewModel.clearLoginError()
                    if (captchaImage == null) viewModel.refreshCaptcha()
                }

                // Navigate back on import success
                LaunchedEffect(loginState) {
                    if (loginState is LoginState.ImportResult) {
                        kotlinx.coroutines.delay(1500)
                        navController.popBackStack()
                    }
                }
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
private fun ScheduleGridForExport(
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
