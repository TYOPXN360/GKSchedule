package com.ty.gkschedule.ui.settings
import com.ty.gkschedule.ui.theme.GKSwitch

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
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
import com.ty.gkschedule.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    startDestination: String = "main",
    semesterStart: LocalDate,
    totalWeeks: Int,
    periodsPerDay: Int,
    darkMode: String,
    language: String,
    startPage: String,
    blurEffect: Boolean,
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
    reminderMode: String = "notify",
    reminderLiveUpdate: Boolean,
    reminderExamLiveUpdate: Boolean,
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
    onStartPageChange: (String) -> Unit,
    onBlurEffectChange: (Boolean) -> Unit,
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
    onReminderModeChange: (String) -> Unit = {},
    onReminderLiveUpdateChange: (Boolean) -> Unit,
    onReminderExamLiveUpdateChange: (Boolean) -> Unit,
    onAutoSyncOnStartChange: (Boolean) -> Unit,
    onAutoSyncIntervalValueChange: (Int) -> Unit,
    onAutoSyncIntervalUnitChange: (String) -> Unit,
    onTokenHeartbeatChange: (Boolean) -> Unit,
    onShowExamScheduleChange: (Boolean) -> Unit,
    onExamLookaheadWeeksChange: (Int) -> Unit,
    onDiffColorPerWeekChange: (Boolean) -> Unit,
    showHiddenCourses: Boolean = false,
    onShowHiddenCoursesChange: (Boolean) -> Unit = {},
    compactNavBar: Boolean = true,
    onCompactNavBarChange: (Boolean) -> Unit = {},
    pillContentMode: Int = 0,
    onPillContentModeChange: (Int) -> Unit = {},
    onFetchExam: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportIcs: () -> Unit
) {
    val context = LocalContext.current
    val navController = androidx.navigation.compose.rememberNavController()

        NavHost(
            navController = navController,
            // ponytail: startDestination外部可指定——Lineage EXTRA_SHOW_FRAGMENT同款直达
            startDestination = startDestination
            // ponytail: Lineage同款零内容动画——SubSettings transaction.replace()无setCustomAnimations；
            // 预测返回只播系统窗口动画，自播slide+fade会双层错位+seek抽搐
        ) {
            composable("main") {
                SettingsMainPage(
                    // ponytail: Lineage同款真SubSettings——起独立Activity，系统管返回+预测动画
                    onOpenPage = {
                        val cls = when (it) {
                            "semester" -> SubSettingsSemesterActivity::class.java
                            "appearance" -> SubSettingsAppearanceActivity::class.java
                            "schedule_style" -> SubSettingsScheduleStyleActivity::class.java
                            "notification" -> SubSettingsNotificationActivity::class.java
                            "sync" -> SubSettingsSyncActivity::class.java
                            "data" -> SubSettingsDataActivity::class.java
                            else -> null
                        }
                        cls?.let { c -> context.startActivity(android.content.Intent(context, c)) }
                    },
                    onExit = { (context as? android.app.Activity)?.finish() },
                    blurEnabled = blurEffect
                )
            }
        }
    }

// === Main page ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainPage(
    onOpenPage: (String) -> Unit, onExit: () -> Unit,
    blurEnabled: Boolean = true
) {
    val surf = MaterialTheme.colorScheme.surface
    val surfLow = MaterialTheme.colorScheme.surfaceContainerLow
    val surfCont = MaterialTheme.colorScheme.surfaceContainer
    val surfHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfHighest = MaterialTheme.colorScheme.surfaceContainerHighest
    android.util.Log.d("SettingsColors", "surface=#${Integer.toHexString(surf.hashCode())}, surfaceContainerLow=#${Integer.toHexString(surfLow.hashCode())}, surfaceContainer=#${Integer.toHexString(surfCont.hashCode())}, surfaceContainerHigh=#${Integer.toHexString(surfHigh.hashCode())}, surfaceContainerHighest=#${Integer.toHexString(surfHighest.hashCode())}")
    val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = scaffoldBg,
        topBar = {
            com.ty.gkschedule.ui.theme.BlurTopBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                backdrop = backdrop,
                blurEnabled = blurEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(scaffoldBg)
                                // ponytail: 避让走滚动内padding，源纹理全屏录(含顶栏身后)
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
        ) {
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
            com.ty.gkschedule.ui.theme.Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                variant = com.ty.gkschedule.ui.theme.Md3CardVariant.Elevated
            ) {
                Column {
                    listOf(0, 1, 2).forEach { index ->
                        val badgeColor = com.ty.gkschedule.util.CourseColors.getSettingsBadgeColor(index)
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
            com.ty.gkschedule.ui.theme.Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                variant = com.ty.gkschedule.ui.theme.Md3CardVariant.Elevated
            ) {
                Column {
                    listOf(3, 4, 5).forEach { index ->
                        val badgeColor = com.ty.gkschedule.util.CourseColors.getSettingsBadgeColor(index)
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

 // === Sub-page wrapper ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPage(
    title: String, onBack: () -> Unit,
    blurEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    // ponytail: miuix源层
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = scaffoldBg,
        topBar = {
            com.ty.gkschedule.ui.theme.BlurTopBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                backdrop = backdrop,
                blurEnabled = blurEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(scaffoldBg)
                                // ponytail: 避让走滚动内padding，源纹理全屏录(含顶栏身后)
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// === Semester ===

@Composable
internal fun SemesterPage(
    semesterStart: LocalDate, totalWeeks: Int, periodsPerDay: Int, firstDayOfWeek: Int,
    hideEmptyWeeks: Boolean,
    onSemesterStartChange: (LocalDate) -> Unit, onTotalWeeksChange: (Int) -> Unit,
    onPeriodsPerDayChange: (Int) -> Unit, onFirstDayOfWeekChange: (Int) -> Unit,
    onHideEmptyWeeksChange: (Boolean) -> Unit, onBack: () -> Unit,
    blurEnabled: Boolean = true
) {
    SubPage(stringResource(R.string.settings_category_semester), onBack, blurEnabled = blurEnabled) {
        SettingsCard {
            DropdownItem(Icons.Default.FirstPage, stringResource(R.string.first_day_of_week),
                listOf("1" to stringResource(R.string.first_day_monday), "7" to stringResource(R.string.first_day_sunday)),
                firstDayOfWeek.toString(), onSelect = { onFirstDayOfWeekChange(it.toInt()) }, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Visibility, stringResource(R.string.hide_empty_weeks), hideEmptyWeeks, onHideEmptyWeeksChange)
        }
    }
}

// === Appearance ===

@Composable
internal fun AppearancePage(
    darkMode: String, language: String, startPage: String, blurEffect: Boolean,
    compactNavBar: Boolean, pillContentMode: Int,
    colorEngine: Int, colorGroupMode: Int, diffColorPerWeek: Boolean, showHiddenCourses: Boolean,
    onDarkModeChange: (String) -> Unit, onLanguageChange: (String) -> Unit,
    onStartPageChange: (String) -> Unit, onBlurEffectChange: (Boolean) -> Unit,
    onCompactNavBarChange: (Boolean) -> Unit, onPillContentModeChange: (Int) -> Unit,
    onColorEngineChange: (Int) -> Unit, onColorGroupModeChange: (Int) -> Unit,
    onDiffColorPerWeekChange: (Boolean) -> Unit, onShowHiddenCoursesChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    blurEnabled: Boolean = true
) {
    SubPage(stringResource(R.string.settings_category_appearance), onBack, blurEnabled = blurEnabled) {
        SettingsCard {
            DropdownItem(Icons.Default.DarkMode, stringResource(R.string.dark_mode),
                listOf("system" to stringResource(R.string.dark_mode_system), "light" to stringResource(R.string.dark_mode_light), "dark" to stringResource(R.string.dark_mode_dark)),
                darkMode, onSelect = onDarkModeChange, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            DropdownItem(Icons.Default.Language, stringResource(R.string.language),
                listOf("system" to stringResource(R.string.language_system), "en" to stringResource(R.string.language_en), "zh" to stringResource(R.string.language_zh)),
                language, onSelect = onLanguageChange, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            DropdownItem(Icons.Default.Home, stringResource(R.string.start_page),
                listOf("today" to stringResource(R.string.nav_today), "weekly" to stringResource(R.string.nav_schedule)),
                startPage, onSelect = onStartPageChange, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.BlurOn, stringResource(R.string.blur_effect), blurEffect, onBlurEffectChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(stringResource(R.string.style_section_navbar))
        SettingsCard {
            SwitchItem(Icons.Default.Dashboard, stringResource(R.string.compact_nav_bar), compactNavBar, onCompactNavBarChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            // ponytail: 悬浮底栏开关仅抽屉——expand/shrinkVertical上下拉出，同文件其他展开同款
            androidx.compose.animation.AnimatedVisibility(
                visible = compactNavBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    DropdownItem(
                        Icons.Default.Tune, stringResource(R.string.pill_content),
                        listOf(
                            "0" to stringResource(R.string.pill_content_both),
                            "1" to stringResource(R.string.pill_content_icon),
                            "2" to stringResource(R.string.pill_content_text)
                        ),
                        pillContentMode.toString(), onSelect = { onPillContentModeChange(it.toInt()) }, blurEnabled = blurEnabled
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(stringResource(R.string.style_section_color))
        SettingsCard {
            DropdownItem(Icons.Default.Palette, stringResource(R.string.color_engine),
                listOf("0" to stringResource(R.string.color_engine_monet), "1" to stringResource(R.string.color_engine_vibrant), "2" to stringResource(R.string.color_engine_classic), "3" to stringResource(R.string.color_engine_hsl)),
                colorEngine.toString(), onSelect = { onColorEngineChange(it.toInt()) }, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            DropdownItem(Icons.Default.FormatColorFill, stringResource(R.string.color_group_mode),
                listOf("0" to stringResource(R.string.color_group_same), "1" to stringResource(R.string.color_group_same_sat), "2" to stringResource(R.string.color_group_diff)),
                colorGroupMode.toString(), onSelect = { onColorGroupModeChange(it.toInt()) }, blurEnabled = blurEnabled)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Palette, stringResource(R.string.diff_color_per_week), diffColorPerWeek, onDiffColorPerWeekChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.VisibilityOff, "显示已隐藏的课程", showHiddenCourses, onShowHiddenCoursesChange)
        }
    }
}

// === Schedule Style ===

@Composable
internal fun ScheduleStylePage(
    gridHeight: Int, gridCorner: Int, gridSpacing: Int, showPeriodLabel: Boolean,
    autoGridHeight: Boolean, mergeConsecutive: Boolean, showTimeLabel: Boolean,
    detailedSplit: Boolean, showDateInHeader: Boolean,
    onGridHeightChange: (Int) -> Unit, onGridCornerChange: (Int) -> Unit,
    onGridSpacingChange: (Int) -> Unit, onShowPeriodLabelChange: (Boolean) -> Unit,
    onAutoGridHeightChange: (Boolean) -> Unit, onMergeConsecutiveChange: (Boolean) -> Unit,
    onShowTimeLabelChange: (Boolean) -> Unit, onDetailedSplitChange: (Boolean) -> Unit,
    onShowDateInHeaderChange: (Boolean) -> Unit,
    blockMultiline: Boolean = true,
    onBlockMultilineChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    blurEnabled: Boolean = true
) {
    SubPage(stringResource(R.string.settings_category_schedule), onBack, blurEnabled = blurEnabled) {
        // ponytail: 布局/内容/课表块三卡——导航栏+颜色已迁外观页
        SectionHeader(stringResource(R.string.style_section_layout))
        SettingsCard {
            SwitchItem(Icons.Default.AutoAwesome, stringResource(R.string.auto_grid_height), autoGridHeight, onAutoGridHeightChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            AnimatedVisibility(visible = !autoGridHeight, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    StepperItem(Icons.Default.Height, stringResource(R.string.grid_height), gridHeight, 36, 80, onGridHeightChange)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                }
            }
            StepperItem(Icons.Default.RoundedCorner, stringResource(R.string.grid_corner), gridCorner, 0, 20, onGridCornerChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            StepperItem(Icons.Default.SpaceBar, stringResource(R.string.grid_spacing), gridSpacing, 0, 8, onGridSpacingChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.ViewColumn, stringResource(R.string.merge_consecutive), mergeConsecutive, onMergeConsecutiveChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            AnimatedVisibility(visible = !mergeConsecutive, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    SwitchItem(Icons.Default.ViewDay, stringResource(R.string.detailed_split), detailedSplit, onDetailedSplitChange)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(stringResource(R.string.style_section_content))
        SettingsCard {
            SwitchItem(Icons.Default.AccessTime, stringResource(R.string.show_time_label), showTimeLabel, onShowTimeLabelChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.CalendarMonth, stringResource(R.string.show_date_in_header), showDateInHeader, onShowDateInHeaderChange)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            SwitchItem(Icons.Default.Pin, stringResource(R.string.show_period_label), showPeriodLabel, onShowPeriodLabelChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(stringResource(R.string.style_section_block))
        SettingsCard {
            // ponytail: 课表块名多行——关=单行截断，开=最多3行自然折行
            SwitchItem(Icons.Default.WrapText, stringResource(R.string.block_multiline), blockMultiline, onBlockMultilineChange)
        }
    }
}

// === Notification ===

@Composable
internal fun NotificationPage(
    reminderMinutes: Int,
    reminderMode: String = "notify",
    reminderLiveUpdate: Boolean,
    reminderExamLiveUpdate: Boolean,
    onReminderMinutesChange: (Int) -> Unit,
    onReminderModeChange: (String) -> Unit = {},
    onReminderLiveUpdateChange: (Boolean) -> Unit,
    onReminderExamLiveUpdateChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    blurEnabled: Boolean = true
) {
    SubPage(stringResource(R.string.settings_category_notification), onBack, blurEnabled = blurEnabled) {
        // ponytail: 开通知=要后台，先弹系统"允许后台运行吗"，允许才真正打开
        var pendingReminder by remember { mutableStateOf(5) }
        val requestBg = com.ty.gkschedule.util.BackgroundRun.rememberRequester { allowed ->
            if (allowed) onReminderMinutesChange(pendingReminder)
        }
        SettingsCard {
            DropdownItem(Icons.Default.Notifications, stringResource(R.string.reminder),
                listOf("0" to stringResource(R.string.reminder_off), "5" to stringResource(R.string.reminder_format, 5), "10" to stringResource(R.string.reminder_format, 10), "15" to stringResource(R.string.reminder_format, 15), "30" to stringResource(R.string.reminder_format, 30)),
                reminderMinutes.toString(), onSelect = {
                    val v = it.toInt()
                    if (v > 0 && reminderMinutes == 0) { pendingReminder = v; requestBg() }
                    else onReminderMinutesChange(v)
                }, blurEnabled = blurEnabled)
            // ponytail: 开启后展开模式二选一，默认仅通知；简介放i弹窗（心跳同款）
            AnimatedVisibility(visible = reminderMinutes > 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    var showModeInfo by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.reminder_mode)) },
                        leadingContent = { Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showModeInfo = true }) {
                                    Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box {
                                    var modeExpanded by remember { mutableStateOf(false) }
                                    TextButton(onClick = { modeExpanded = true }) {
                                        Text(if (reminderMode == "countdown") stringResource(R.string.reminder_mode_countdown) else stringResource(R.string.reminder_mode_notify))
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                                    }
                                    com.ty.gkschedule.ui.theme.BlurDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }, blurEnabled = blurEnabled) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.reminder_mode_notify)) }, onClick = { onReminderModeChange("notify"); modeExpanded = false })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.reminder_mode_countdown)) }, onClick = { onReminderModeChange("countdown"); modeExpanded = false })
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (showModeInfo) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showModeInfo = false }) {
                            com.ty.gkschedule.ui.theme.BlurCard(
                                enabled = blurEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                radiusDp = 36f,
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                                cornerRadiusDp = 28f
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(stringResource(R.string.reminder_mode), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("${stringResource(R.string.reminder_mode_notify)}：${stringResource(R.string.reminder_mode_notify_desc)}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${stringResource(R.string.reminder_mode_countdown)}：${stringResource(R.string.reminder_mode_countdown_desc)}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showModeInfo = false }) { Text("OK") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    // ponytail: desc改i弹窗（心跳同款BlurCard），行内只留标题+开关
                    var showLiveInfo by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.reminder_live_update)) },
                        leadingContent = { Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showLiveInfo = true }) {
                                    Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GKSwitch(checked = reminderLiveUpdate, onCheckedChange = onReminderLiveUpdateChange)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (showLiveInfo) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showLiveInfo = false }) {
                            com.ty.gkschedule.ui.theme.BlurCard(
                                enabled = blurEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                radiusDp = 36f,
                                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                                cornerRadiusDp = 28f
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(stringResource(R.string.reminder_live_update), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(stringResource(R.string.reminder_live_update_desc), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showLiveInfo = false }) { Text("OK") }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    SwitchItem(Icons.Default.Event, stringResource(R.string.reminder_exam_live_update), reminderExamLiveUpdate, onReminderExamLiveUpdateChange)
                }
            }
        }
    }
}

// === Sync ===

@Composable
internal fun SyncPage(
    autoSyncOnStart: Boolean,
    autoSyncIntervalValue: Int,
    autoSyncIntervalUnit: String,
    tokenHeartbeat: Boolean,
    showExamSchedule: Boolean,
    examLookaheadWeeks: Int,
    diffColorPerWeek: Boolean,
    autoCheckUpdateDaily: Boolean,
    onAutoSyncOnStartChange: (Boolean) -> Unit,
    onAutoSyncIntervalValueChange: (Int) -> Unit,
    onAutoSyncIntervalUnitChange: (String) -> Unit,
    onTokenHeartbeatChange: (Boolean) -> Unit,
    onShowExamScheduleChange: (Boolean) -> Unit,
    onExamLookaheadWeeksChange: (Int) -> Unit,
    onDiffColorPerWeekChange: (Boolean) -> Unit,
    onAutoCheckUpdateDailyChange: (Boolean) -> Unit,
    onFetchExam: () -> Unit,
    onBack: () -> Unit,
    blurEnabled: Boolean = true
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

    SubPage(stringResource(R.string.settings_category_sync), onBack, blurEnabled = blurEnabled) {
        SettingsCard {
            SwitchItem(Icons.Default.PowerSettingsNew, stringResource(R.string.auto_sync_on_start), autoSyncOnStart) {
                onAutoSyncOnStartChange(it)
            }
            // ponytail: 关启动同步才展定时项——开则收起，同课表样式抽屉同款
            androidx.compose.animation.AnimatedVisibility(
                visible = !autoSyncOnStart,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.auto_sync_schedule))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.auto_sync_schedule_desc))
                        },
                        leadingContent = {
                            Icon(Icons.Default.Schedule, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        autoSyncIntervalUnit, onSelect = onAutoSyncIntervalUnitChange, blurEnabled = blurEnabled)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    // Value slider with +/- buttons
                    ListItem(
                        headlineContent = {
                            Text("$autoSyncIntervalValue $unitLabel")
                        },
                        supportingContent = {
                            Column {
                                Slider(
                                    value = autoSyncIntervalValue.toFloat(),
                                    onValueChange = { onAutoSyncIntervalValueChange(it.toInt()) },
                                    valueRange = minVal.toFloat()..maxVal.toFloat(),
                                    steps = maxVal - minVal - 1
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onAutoSyncIntervalValueChange((autoSyncIntervalValue - 1).coerceIn(minVal, maxVal)) },
                                        enabled = autoSyncIntervalValue > minVal
                                    ) {
                                        Icon(Icons.Default.Remove, null)
                                    }
                                    Text(
                                        "$autoSyncIntervalValue $unitLabel",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(
                                        onClick = { onAutoSyncIntervalValueChange((autoSyncIntervalValue + 1).coerceIn(minVal, maxVal)) },
                                        enabled = autoSyncIntervalValue < maxVal
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsCard {
            // ponytail: 开心跳=要后台，先弹系统页，允许才真正打开；默认关闭
            val requestBgHb = com.ty.gkschedule.util.BackgroundRun.rememberRequester { allowed ->
                if (allowed) onTokenHeartbeatChange(true)
            }
            var showHeartbeatInfo by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.token_heartbeat)) },
                leadingContent = { Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showHeartbeatInfo = true }) {
                            Icon(Icons.Default.Info, "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        GKSwitch(
                            checked = tokenHeartbeat,
                            onCheckedChange = { v ->
                                if (v && !tokenHeartbeat) requestBgHb()
                                else onTokenHeartbeatChange(v)
                            }
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            if (showHeartbeatInfo) {
                // ponytail: 重登录同款BlurCard毛玻璃Dialog（dim 12%+底48%+半径36）
                androidx.compose.ui.window.Dialog(onDismissRequest = { showHeartbeatInfo = false }) {
                    com.ty.gkschedule.ui.theme.BlurCard(
                        enabled = blurEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        radiusDp = 36f,
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f),
                        cornerRadiusDp = 28f
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(stringResource(R.string.token_heartbeat), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.token_heartbeat_desc), style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showHeartbeatInfo = false }) { Text("OK") }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            // showExamSchedule moved to ExamScreen
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ponytail: 查更新归自动同步页——同应用级后台行为
        SectionHeader(stringResource(R.string.settings_category_update))
        SettingsCard {
            SwitchItem(Icons.Default.SystemUpdate, stringResource(R.string.auto_check_update_daily), autoCheckUpdateDaily, onAutoCheckUpdateDailyChange)
        }
    }
}

// === Data ===

@Composable
internal fun DataPage(onExportJson: () -> Unit, onImportJson: () -> Unit, onExportIcs: () -> Unit, onBack: () -> Unit, blurEnabled: Boolean = true) {
    SubPage(stringResource(R.string.settings_category_data), onBack, blurEnabled = blurEnabled) {
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    com.ty.gkschedule.ui.theme.Md3Card(
        modifier = Modifier.fillMaxWidth(),
        variant = com.ty.gkschedule.ui.theme.Md3CardVariant.Elevated
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
            GKSwitch(
                checked = checked,
                onCheckedChange = onChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DropdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, options: List<Pair<String, String>>, currentKey: String, enabled: Boolean = true, onSelect: (String) -> Unit, blurEnabled: Boolean = true) {
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
                    com.ty.gkschedule.ui.theme.BlurDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, blurEnabled = blurEnabled) {
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
