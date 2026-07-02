package com.classapp.schedule.ui.settings

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.classapp.schedule.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    semesterStart: LocalDate,
    totalWeeks: Int,
    periodsPerDay: Int,
    darkMode: String,
    language: String,
    firstDayOfWeek: Int,
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
    reminderMinutes: Int,
    autoSyncOnStart: Boolean,
    autoSyncIntervalValue: Int,
    autoSyncIntervalUnit: String,
    tokenHeartbeat: Boolean,
    showExamSchedule: Boolean,
    examLookaheadWeeks: Int,
    diffColorPerWeek: Boolean,
    onSemesterStartChange: (LocalDate) -> Unit,
    onTotalWeeksChange: (Int) -> Unit,
    onPeriodsPerDayChange: (Int) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onFirstDayOfWeekChange: (Int) -> Unit,
    onGridHeightChange: (Int) -> Unit,
    onGridCornerChange: (Int) -> Unit,
    onGridSpacingChange: (Int) -> Unit,
    onShowPeriodLabelChange: (Boolean) -> Unit,
    onAutoGridHeightChange: (Boolean) -> Unit,
    onMergeConsecutiveChange: (Boolean) -> Unit,
    onShowTimeLabelChange: (Boolean) -> Unit,
    onDetailedSplitChange: (Boolean) -> Unit,
    onColorEngineChange: (Int) -> Unit,
    onColorGroupModeChange: (Int) -> Unit,
    onHideEmptyWeeksChange: (Boolean) -> Unit,
    onShowDateInHeaderChange: (Boolean) -> Unit,
    onReminderMinutesChange: (Int) -> Unit,
    onAutoSyncOnStartChange: (Boolean) -> Unit,
    onAutoSyncIntervalValueChange: (Int) -> Unit,
    onAutoSyncIntervalUnitChange: (String) -> Unit,
    onTokenHeartbeatChange: (Boolean) -> Unit,
    onShowExamScheduleChange: (Boolean) -> Unit,
    onExamLookaheadWeeksChange: (Int) -> Unit,
    onDiffColorPerWeekChange: (Boolean) -> Unit,
    showHiddenCourses: Boolean = false,
    onShowHiddenCoursesChange: (Boolean) -> Unit = {},
    onFetchExam: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportIcs: () -> Unit
) {
    val context = LocalContext.current
    val navController = androidx.navigation.compose.rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "main",
                enterTransition = {
                    slideInHorizontally(tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it } + fadeIn(tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { -it } + fadeOut(tween(150))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -it } + fadeIn(tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { it } + fadeOut(tween(150))
            }
        ) {
            composable("main") {
                SettingsMainPage(
                    onOpenPage = { navController.navigate(it) },
                    onExit = { (context as? android.app.Activity)?.finish() }
                )
            }
            composable("semester") {
                SemesterPage(
                    semesterStart = semesterStart,
                    totalWeeks = totalWeeks,
                    periodsPerDay = periodsPerDay,
                    firstDayOfWeek = firstDayOfWeek,
                    hideEmptyWeeks = hideEmptyWeeks,
                    onSemesterStartChange = onSemesterStartChange,
                    onTotalWeeksChange = onTotalWeeksChange,
                    onPeriodsPerDayChange = onPeriodsPerDayChange,
                    onFirstDayOfWeekChange = onFirstDayOfWeekChange,
                    onHideEmptyWeeksChange = onHideEmptyWeeksChange,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("appearance") {
                AppearancePage(
                    darkMode = darkMode,
                    language = language,
                    onDarkModeChange = onDarkModeChange,
                    onLanguageChange = onLanguageChange,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("schedule_style") {
                ScheduleStylePage(
                    gridHeight = gridHeight,
                    gridCorner = gridCorner,
                    gridSpacing = gridSpacing,
                    showPeriodLabel = showPeriodLabel,
                    autoGridHeight = autoGridHeight,
                    mergeConsecutive = mergeConsecutive,
                    showTimeLabel = showTimeLabel,
                    detailedSplit = detailedSplit,
                    colorEngine = colorEngine,
                    colorGroupMode = colorGroupMode,
                    showDateInHeader = showDateInHeader,
                    onGridHeightChange = onGridHeightChange,
                    onGridCornerChange = onGridCornerChange,
                    onGridSpacingChange = onGridSpacingChange,
                    onShowPeriodLabelChange = onShowPeriodLabelChange,
                    onAutoGridHeightChange = onAutoGridHeightChange,
                    onMergeConsecutiveChange = onMergeConsecutiveChange,
                    onShowTimeLabelChange = onShowTimeLabelChange,
                    onDetailedSplitChange = onDetailedSplitChange,
                    onColorEngineChange = onColorEngineChange,
                    onColorGroupModeChange = onColorGroupModeChange,
                    onShowDateInHeaderChange = onShowDateInHeaderChange,
                    diffColorPerWeek = diffColorPerWeek,
                    onDiffColorPerWeekChange = onDiffColorPerWeekChange,
                    showHiddenCourses = showHiddenCourses,
                    onShowHiddenCoursesChange = onShowHiddenCoursesChange,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("notification") {
                NotificationPage(
                    reminderMinutes = reminderMinutes,
                    onReminderMinutesChange = onReminderMinutesChange,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("sync") {
                SyncPage(
                    autoSyncOnStart = autoSyncOnStart,
                    autoSyncIntervalValue = autoSyncIntervalValue,
                    autoSyncIntervalUnit = autoSyncIntervalUnit,
                    tokenHeartbeat = tokenHeartbeat,
                    showExamSchedule = showExamSchedule,
                    examLookaheadWeeks = examLookaheadWeeks,
                    diffColorPerWeek = diffColorPerWeek,
                    onAutoSyncOnStartChange = onAutoSyncOnStartChange,
                    onAutoSyncIntervalValueChange = onAutoSyncIntervalValueChange,
                    onAutoSyncIntervalUnitChange = onAutoSyncIntervalUnitChange,
                    onTokenHeartbeatChange = onTokenHeartbeatChange,
                    onShowExamScheduleChange = onShowExamScheduleChange,
                    onExamLookaheadWeeksChange = onExamLookaheadWeeksChange,
                    onDiffColorPerWeekChange = onDiffColorPerWeekChange,
                    onFetchExam = onFetchExam,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("data") {
                DataPage(
                    onExportJson = onExportJson,
                    onImportJson = onImportJson,
                    onExportIcs = onExportIcs,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

// === Main page ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainPage(onOpenPage: (String) -> Unit, onExit: () -> Unit) {
    val surf = MaterialTheme.colorScheme.surface
    val surfLow = MaterialTheme.colorScheme.surfaceContainerLow
    val surfCont = MaterialTheme.colorScheme.surfaceContainer
    val surfHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfHighest = MaterialTheme.colorScheme.surfaceContainerHighest
    android.util.Log.d("SettingsColors", "surface=#${Integer.toHexString(surf.hashCode())}, surfaceContainerLow=#${Integer.toHexString(surfLow.hashCode())}, surfaceContainer=#${Integer.toHexString(surfCont.hashCode())}, surfaceContainerHigh=#${Integer.toHexString(surfHigh.hashCode())}, surfaceContainerHighest=#${Integer.toHexString(surfHighest.hashCode())}")
    val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = scaffoldBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg),
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val catPalettes = listOf(
                com.classapp.schedule.ui.theme.BadgeColorPalette.Primary,     // 学期 = 主色蓝
                com.classapp.schedule.ui.theme.BadgeColorPalette.Tertiary,    // 外观 = 第三色粉紫
                com.classapp.schedule.ui.theme.BadgeColorPalette.Secondary,   // 课表样式 = 次色
                com.classapp.schedule.ui.theme.BadgeColorPalette.Tertiary,    // 通知 = 粉紫
                com.classapp.schedule.ui.theme.BadgeColorPalette.Secondary,   // 同步 = 次色
                com.classapp.schedule.ui.theme.BadgeColorPalette.Primary      // 数据 = 主色
            )
            val catIcons = listOf(Icons.Default.CalendarMonth, Icons.Default.Palette, Icons.Default.GridOn, Icons.Default.Notifications, Icons.Default.Sync, Icons.Default.Storage)
            val catTitles = listOf(
                stringResource(R.string.settings_category_semester),
                stringResource(R.string.settings_category_appearance),
                stringResource(R.string.settings_category_schedule),
                stringResource(R.string.settings_category_notification),
                stringResource(R.string.settings_category_sync),
                stringResource(R.string.settings_category_data)
            )
            val catDescs = listOf(
                stringResource(R.string.settings_category_semester_desc),
                stringResource(R.string.settings_category_appearance_desc),
                stringResource(R.string.settings_category_schedule_desc),
                stringResource(R.string.settings_category_notification_desc),
                stringResource(R.string.settings_category_sync_desc),
                stringResource(R.string.settings_category_data_desc)
            )
            val catCallbacks = listOf<() -> Unit>(
                { onOpenPage("semester") },
                { onOpenPage("appearance") },
                { onOpenPage("schedule_style") },
                { onOpenPage("notification") },
                { onOpenPage("sync") },
                { onOpenPage("data") }
            )

            // Group 1: 课程管理
            com.classapp.schedule.ui.theme.Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                variant = com.classapp.schedule.ui.theme.Md3CardVariant.Elevated
            ) {
                Column {
                    listOf(0, 1, 2).forEach { index ->
                        val badgeColor = com.classapp.schedule.util.CourseColors.getSettingsBadgeColor(index)
                        ListItem(
                            headlineContent = { Text(catTitles[index], style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text(catDescs[index], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = {
                                Surface(modifier = Modifier.size(40.dp), shape = MaterialTheme.shapes.small, color = badgeColor.container, contentColor = badgeColor.content) {
                                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = catIcons[index], contentDescription = null, modifier = Modifier.size(22.dp)) }
                                }
                            },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(onClick = catCallbacks[index])
                        )
                        if (index < 2) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Group 2: 系统与数据
            com.classapp.schedule.ui.theme.Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                variant = com.classapp.schedule.ui.theme.Md3CardVariant.Elevated
            ) {
                Column {
                    listOf(3, 4, 5).forEach { index ->
                        val badgeColor = com.classapp.schedule.util.CourseColors.getSettingsBadgeColor(index)
                        ListItem(
                            headlineContent = { Text(catTitles[index], style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { Text(catDescs[index], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingContent = {
                                Surface(modifier = Modifier.size(40.dp), shape = MaterialTheme.shapes.small, color = badgeColor.container, contentColor = badgeColor.content) {
                                    Box(contentAlignment = Alignment.Center) { Icon(imageVector = catIcons[index], contentDescription = null, modifier = Modifier.size(22.dp)) }
                                }
                            },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable(onClick = catCallbacks[index])
                        )
                        if (index < 5) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, badgePalette: com.classapp.schedule.ui.theme.BadgeColorPalette, onClick: () -> Unit) {
    com.classapp.schedule.ui.theme.Md3Card(modifier = Modifier.fillMaxWidth(), variant = com.classapp.schedule.ui.theme.Md3CardVariant.Elevated, shape = MaterialTheme.shapes.small) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
            supportingContent = { Text(subtitle) },
            leadingContent = { com.classapp.schedule.ui.theme.MonetIconBadge(icon = icon, contentDescription = title, badgePalette = badgePalette) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

// === Sub-page wrapper ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = scaffoldBg,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// === Semester ===

@Composable
private fun SemesterPage(
    semesterStart: LocalDate, totalWeeks: Int, periodsPerDay: Int, firstDayOfWeek: Int,
    hideEmptyWeeks: Boolean,
    onSemesterStartChange: (LocalDate) -> Unit, onTotalWeeksChange: (Int) -> Unit,
    onPeriodsPerDayChange: (Int) -> Unit, onFirstDayOfWeekChange: (Int) -> Unit,
    onHideEmptyWeeksChange: (Boolean) -> Unit, onBack: () -> Unit
) {
    SubPage(stringResource(R.string.settings_category_semester), onBack) {
        SettingsCard {
            DropdownItem(Icons.Default.FirstPage, stringResource(R.string.first_day_of_week),
                listOf("1" to stringResource(R.string.first_day_monday), "7" to stringResource(R.string.first_day_sunday)),
                firstDayOfWeek.toString(), onSelect = { onFirstDayOfWeekChange(it.toInt()) })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Visibility, stringResource(R.string.hide_empty_weeks), hideEmptyWeeks, onHideEmptyWeeksChange)
        }
    }
}

// === Appearance ===

@Composable
private fun AppearancePage(
    darkMode: String, language: String,
    onDarkModeChange: (String) -> Unit, onLanguageChange: (String) -> Unit, onBack: () -> Unit
) {
    SubPage(stringResource(R.string.settings_category_appearance), onBack) {
        SettingsCard {
            DropdownItem(Icons.Default.DarkMode, stringResource(R.string.dark_mode),
                listOf("system" to stringResource(R.string.dark_mode_system), "light" to stringResource(R.string.dark_mode_light), "dark" to stringResource(R.string.dark_mode_dark)),
                darkMode, onSelect = onDarkModeChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            DropdownItem(Icons.Default.Language, stringResource(R.string.language),
                listOf("system" to stringResource(R.string.language_system), "en" to stringResource(R.string.language_en), "zh" to stringResource(R.string.language_zh)),
                language, onSelect = onLanguageChange)
        }
    }
}

// === Schedule Style ===

@Composable
private fun ScheduleStylePage(
    gridHeight: Int, gridCorner: Int, gridSpacing: Int, showPeriodLabel: Boolean,
    autoGridHeight: Boolean, mergeConsecutive: Boolean, showTimeLabel: Boolean,
    detailedSplit: Boolean, colorEngine: Int, colorGroupMode: Int, showDateInHeader: Boolean,
    onGridHeightChange: (Int) -> Unit, onGridCornerChange: (Int) -> Unit,
    onGridSpacingChange: (Int) -> Unit, onShowPeriodLabelChange: (Boolean) -> Unit,
    onAutoGridHeightChange: (Boolean) -> Unit, onMergeConsecutiveChange: (Boolean) -> Unit,
    onShowTimeLabelChange: (Boolean) -> Unit, onDetailedSplitChange: (Boolean) -> Unit,
    onColorEngineChange: (Int) -> Unit, onColorGroupModeChange: (Int) -> Unit,
    onShowDateInHeaderChange: (Boolean) -> Unit,
    diffColorPerWeek: Boolean, onDiffColorPerWeekChange: (Boolean) -> Unit,
    showHiddenCourses: Boolean = false, onShowHiddenCoursesChange: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    SubPage(stringResource(R.string.settings_category_schedule), onBack) {
        SettingsCard {
            SwitchItem(Icons.Default.AutoAwesome, stringResource(R.string.auto_grid_height), autoGridHeight, onAutoGridHeightChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            if (!autoGridHeight) {
                StepperItem(Icons.Default.Height, stringResource(R.string.grid_height), gridHeight, 36, 80, onGridHeightChange)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            }
            StepperItem(Icons.Default.RoundedCorner, stringResource(R.string.grid_corner), gridCorner, 0, 20, onGridCornerChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            StepperItem(Icons.Default.SpaceBar, stringResource(R.string.grid_spacing), gridSpacing, 0, 8, onGridSpacingChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.ViewColumn, stringResource(R.string.merge_consecutive), mergeConsecutive, onMergeConsecutiveChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            if (!mergeConsecutive) {
                SwitchItem(Icons.Default.ViewDay, stringResource(R.string.detailed_split), detailedSplit, onDetailedSplitChange)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            }
            SwitchItem(Icons.Default.AccessTime, stringResource(R.string.show_time_label), showTimeLabel, onShowTimeLabelChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.CalendarMonth, stringResource(R.string.show_date_in_header), showDateInHeader, onShowDateInHeaderChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Pin, stringResource(R.string.show_period_label), showPeriodLabel, onShowPeriodLabelChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsCard {
            DropdownItem(Icons.Default.Palette, stringResource(R.string.color_engine),
                listOf("0" to stringResource(R.string.color_engine_monet), "1" to stringResource(R.string.color_engine_vibrant), "2" to stringResource(R.string.color_engine_classic), "3" to stringResource(R.string.color_engine_hsl)),
                colorEngine.toString(), onSelect = { onColorEngineChange(it.toInt()) })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            DropdownItem(Icons.Default.FormatColorFill, stringResource(R.string.color_group_mode),
                listOf("0" to stringResource(R.string.color_group_same), "1" to stringResource(R.string.color_group_same_sat), "2" to stringResource(R.string.color_group_diff)),
                colorGroupMode.toString(), onSelect = { onColorGroupModeChange(it.toInt()) })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Palette, stringResource(R.string.diff_color_per_week), diffColorPerWeek, onDiffColorPerWeekChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.VisibilityOff, "显示已隐藏的课程", showHiddenCourses, onShowHiddenCoursesChange)
        }
    }
}

// === Notification ===

@Composable
private fun NotificationPage(reminderMinutes: Int, onReminderMinutesChange: (Int) -> Unit, onBack: () -> Unit) {
    SubPage(stringResource(R.string.settings_category_notification), onBack) {
        SettingsCard {
            DropdownItem(Icons.Default.Notifications, stringResource(R.string.reminder),
                listOf("0" to stringResource(R.string.reminder_off), "5" to stringResource(R.string.reminder_format, 5), "10" to stringResource(R.string.reminder_format, 10), "15" to stringResource(R.string.reminder_format, 15), "30" to stringResource(R.string.reminder_format, 30)),
                reminderMinutes.toString(), onSelect = { onReminderMinutesChange(it.toInt()) })
        }
    }
}

// === Sync ===

@Composable
private fun SyncPage(
    autoSyncOnStart: Boolean,
    autoSyncIntervalValue: Int,
    autoSyncIntervalUnit: String,
    tokenHeartbeat: Boolean,
    showExamSchedule: Boolean,
    examLookaheadWeeks: Int,
    diffColorPerWeek: Boolean,
    onAutoSyncOnStartChange: (Boolean) -> Unit,
    onAutoSyncIntervalValueChange: (Int) -> Unit,
    onAutoSyncIntervalUnitChange: (String) -> Unit,
    onTokenHeartbeatChange: (Boolean) -> Unit,
    onShowExamScheduleChange: (Boolean) -> Unit,
    onExamLookaheadWeeksChange: (Int) -> Unit,
    onDiffColorPerWeekChange: (Boolean) -> Unit,
    onFetchExam: () -> Unit,
    onBack: () -> Unit
) {
    val unitLabel = when (autoSyncIntervalUnit) {
        "min" -> stringResource(R.string.auto_sync_unit_min)
        "h" -> stringResource(R.string.auto_sync_unit_h)
        "d" -> stringResource(R.string.auto_sync_unit_d)
        else -> ""
    }
    val (minVal, maxVal) = when (autoSyncIntervalUnit) {
        "min" -> 30 to 60
        "h" -> 1 to 24
        "d" -> 1 to 31
        else -> 1 to 60
    }

    SubPage(stringResource(R.string.settings_category_sync), onBack) {
        SettingsCard {
            SwitchItem(Icons.Default.PowerSettingsNew, stringResource(R.string.auto_sync_on_start), autoSyncOnStart) {
                onAutoSyncOnStartChange(it)
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            val syncAlpha = if (autoSyncOnStart) 0.38f else 1f

            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.auto_sync_schedule),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = syncAlpha))
                },
                supportingContent = {
                    Text(stringResource(R.string.auto_sync_schedule_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = syncAlpha))
                },
                leadingContent = {
                    Icon(Icons.Default.Schedule, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = syncAlpha))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Unit selector
            DropdownItem(Icons.Default.Tune, stringResource(R.string.auto_sync_interval),
                listOf(
                    "min" to stringResource(R.string.auto_sync_unit_min),
                    "h" to stringResource(R.string.auto_sync_unit_h),
                    "d" to stringResource(R.string.auto_sync_unit_d)
                ),
                autoSyncIntervalUnit, enabled = !autoSyncOnStart, onSelect = onAutoSyncIntervalUnitChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Value slider with +/- buttons
            ListItem(
                headlineContent = {
                    Text("$autoSyncIntervalValue $unitLabel",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = syncAlpha))
                },
                supportingContent = {
                    Column {
                        Slider(
                            value = autoSyncIntervalValue.toFloat(),
                            onValueChange = { onAutoSyncIntervalValueChange(it.toInt()) },
                            valueRange = minVal.toFloat()..maxVal.toFloat(),
                            steps = maxVal - minVal - 1,
                            enabled = !autoSyncOnStart
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onAutoSyncIntervalValueChange((autoSyncIntervalValue - 1).coerceIn(minVal, maxVal)) },
                                enabled = !autoSyncOnStart && autoSyncIntervalValue > minVal
                            ) {
                                Icon(Icons.Default.Remove, null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = syncAlpha))
                            }
                            Text(
                                "$autoSyncIntervalValue $unitLabel",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = syncAlpha),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(
                                onClick = { onAutoSyncIntervalValueChange((autoSyncIntervalValue + 1).coerceIn(minVal, maxVal)) },
                                enabled = !autoSyncOnStart && autoSyncIntervalValue < maxVal
                            ) {
                                Icon(Icons.Default.Add, null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = syncAlpha))
                            }
                        }
                    }
                },
                leadingContent = { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = syncAlpha)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            var showHeartbeatInfo by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.token_heartbeat)) },
                leadingContent = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showHeartbeatInfo = true }) {
                            Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tokenHeartbeat,
                            onCheckedChange = onTokenHeartbeatChange,
                            thumbContent = if (tokenHeartbeat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            if (showHeartbeatInfo) {
                AlertDialog(
                    onDismissRequest = { showHeartbeatInfo = false },
                    text = { Text(stringResource(R.string.token_heartbeat_desc)) },
                    confirmButton = { TextButton(onClick = { showHeartbeatInfo = false }) { Text("OK") } }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.School, stringResource(R.string.show_exam_schedule), showExamSchedule, onShowExamScheduleChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            StepperItem(
                Icons.Default.Event,
                stringResource(R.string.exam_lookahead_weeks),
                examLookaheadWeeks,
                0,
                20,
                onExamLookaheadWeeksChange
            )
        }
    }
}

// === Data ===

@Composable
private fun DataPage(onExportJson: () -> Unit, onImportJson: () -> Unit, onExportIcs: () -> Unit, onBack: () -> Unit) {
    SubPage(stringResource(R.string.settings_category_data), onBack) {
        SettingsCard {
            SettingsItem(Icons.Default.FileUpload, stringResource(R.string.import_json), onClick = onImportJson)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SettingsItem(Icons.Default.FileDownload, stringResource(R.string.export_json), onClick = onExportJson)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SettingsItem(Icons.Default.CalendarMonth, stringResource(R.string.export_ics), onClick = onExportIcs)
        }
    }
}

// === Shared components ===

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    com.classapp.schedule.ui.theme.Md3Card(
        modifier = Modifier.fillMaxWidth(),
        variant = com.classapp.schedule.ui.theme.Md3CardVariant.Elevated
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun StepperItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text("$value", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SwitchItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DropdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, options: List<Pair<String, String>>, currentKey: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.find { it.first == currentKey }?.second ?: ""
    val alpha = if (enabled) 1f else 0.38f
    Box {
        ListItem(
            headlineContent = { Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)) },
            supportingContent = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) },
            trailingContent = {
                Box {
                    TextButton(onClick = { expanded = true }, enabled = enabled) {
                        Text(label)
                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { (key, text) ->
                            DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(key); expanded = false })
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = if (enabled) Modifier.clickable { expanded = true } else Modifier
        )
    }
}
