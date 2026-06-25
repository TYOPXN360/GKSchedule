package com.classapp.schedule.ui.settings

import android.app.Activity
import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportIcs: () -> Unit,
    onExportImage: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf("main") }

    // Predictive back: sub-pages go back to main
    if (currentPage != "main") {
        androidx.activity.compose.PredictiveBackHandler { progress ->
            // Just commit the back on release
            currentPage = "main"
        }
    }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            slideInHorizontally(tween(250)) { it } + fadeIn(tween(200)) togetherWith
                slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(150))
        },
        label = "settingsPage"
    ) { page ->
        when (page) {
            "main" -> SettingsMainPage(
                onOpenPage = { currentPage = it },
                onExit = { (context as? Activity)?.finish() }
            )
            "semester" -> SemesterPage(
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
                onBack = { currentPage = "main" }
            )
            "appearance" -> AppearancePage(
                darkMode = darkMode,
                language = language,
                onDarkModeChange = onDarkModeChange,
                onLanguageChange = onLanguageChange,
                onBack = { currentPage = "main" }
            )
            "schedule_style" -> ScheduleStylePage(
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
                onBack = { currentPage = "main" }
            )
            "notification" -> NotificationPage(
                reminderMinutes = reminderMinutes,
                onReminderMinutesChange = onReminderMinutesChange,
                onBack = { currentPage = "main" }
            )
            "data" -> DataPage(
                onExportJson = onExportJson,
                onImportJson = onImportJson,
                onExportIcs = onExportIcs,
                onExportImage = onExportImage,
                onBack = { currentPage = "main" }
            )
        }
    }
}

// === Main page ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainPage(onOpenPage: (String) -> Unit, onExit: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            CategoryItem(Icons.Default.CalendarMonth, stringResource(R.string.settings_category_semester), stringResource(R.string.settings_category_semester_desc)) { onOpenPage("semester") }
            CategoryItem(Icons.Default.Palette, stringResource(R.string.settings_category_appearance), stringResource(R.string.settings_category_appearance_desc)) { onOpenPage("appearance") }
            CategoryItem(Icons.Default.GridOn, stringResource(R.string.settings_category_schedule), stringResource(R.string.settings_category_schedule_desc)) { onOpenPage("schedule_style") }
            CategoryItem(Icons.Default.Notifications, stringResource(R.string.settings_category_notification), stringResource(R.string.settings_category_notification_desc)) { onOpenPage("notification") }
            CategoryItem(Icons.Default.Storage, stringResource(R.string.settings_category_data), stringResource(R.string.settings_category_data_desc)) { onOpenPage("data") }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// === Sub-page wrapper ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .verticalScroll(rememberScrollState())
        ) {
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
    val context = LocalContext.current
    SubPage(stringResource(R.string.settings_category_semester), onBack) {
        SettingsItem(Icons.Default.CalendarMonth, stringResource(R.string.semester_start), semesterStart.toString()) {
            DatePickerDialog(context, { _, y, m, d ->
                onSemesterStartChange(LocalDate.of(y, m + 1, d))
            }, semesterStart.year, semesterStart.monthValue - 1, semesterStart.dayOfMonth).show()
        }
        StepperItem(Icons.Default.DateRange, stringResource(R.string.total_weeks), totalWeeks, 1, 30, onTotalWeeksChange)
        StepperItem(Icons.Default.AccessTime, stringResource(R.string.periods_per_day), periodsPerDay, 4, 14, onPeriodsPerDayChange)
        DropdownItem(Icons.Default.FirstPage, stringResource(R.string.first_day_of_week),
            listOf("1" to stringResource(R.string.first_day_monday), "7" to stringResource(R.string.first_day_sunday)),
            firstDayOfWeek.toString()) { onFirstDayOfWeekChange(it.toInt()) }
        SwitchItem(Icons.Default.Visibility, stringResource(R.string.hide_empty_weeks), hideEmptyWeeks, onHideEmptyWeeksChange)
    }
}

// === Appearance ===

@Composable
private fun AppearancePage(
    darkMode: String, language: String,
    onDarkModeChange: (String) -> Unit, onLanguageChange: (String) -> Unit, onBack: () -> Unit
) {
    SubPage(stringResource(R.string.settings_category_appearance), onBack) {
        DropdownItem(Icons.Default.DarkMode, stringResource(R.string.dark_mode),
            listOf("system" to stringResource(R.string.dark_mode_system), "light" to stringResource(R.string.dark_mode_light), "dark" to stringResource(R.string.dark_mode_dark)),
            darkMode, onDarkModeChange)
        DropdownItem(Icons.Default.Language, stringResource(R.string.language),
            listOf("system" to stringResource(R.string.language_system), "en" to stringResource(R.string.language_en), "zh" to stringResource(R.string.language_zh)),
            language, onLanguageChange)
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
    onShowDateInHeaderChange: (Boolean) -> Unit, onBack: () -> Unit
) {
    SubPage(stringResource(R.string.settings_category_schedule), onBack) {
        SwitchItem(Icons.Default.AutoAwesome, stringResource(R.string.auto_grid_height), autoGridHeight, onAutoGridHeightChange)
        if (!autoGridHeight) StepperItem(Icons.Default.Height, stringResource(R.string.grid_height), gridHeight, 36, 80, onGridHeightChange)
        StepperItem(Icons.Default.RoundedCorner, stringResource(R.string.grid_corner), gridCorner, 0, 20, onGridCornerChange)
        StepperItem(Icons.Default.SpaceBar, stringResource(R.string.grid_spacing), gridSpacing, 0, 8, onGridSpacingChange)
        SwitchItem(Icons.Default.ViewColumn, stringResource(R.string.merge_consecutive), mergeConsecutive, onMergeConsecutiveChange)
        if (!mergeConsecutive) SwitchItem(Icons.Default.ViewDay, stringResource(R.string.detailed_split), detailedSplit, onDetailedSplitChange)
        SwitchItem(Icons.Default.AccessTime, stringResource(R.string.show_time_label), showTimeLabel, onShowTimeLabelChange)
        SwitchItem(Icons.Default.CalendarMonth, stringResource(R.string.show_date_in_header), showDateInHeader, onShowDateInHeaderChange)
        SwitchItem(Icons.Default.Pin, stringResource(R.string.show_period_label), showPeriodLabel, onShowPeriodLabelChange)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        DropdownItem(Icons.Default.Palette, stringResource(R.string.color_engine),
            listOf("0" to stringResource(R.string.color_engine_monet), "1" to stringResource(R.string.color_engine_vibrant), "2" to stringResource(R.string.color_engine_classic), "3" to stringResource(R.string.color_engine_hsl)),
            colorEngine.toString()) { onColorEngineChange(it.toInt()) }
        DropdownItem(Icons.Default.FormatColorFill, stringResource(R.string.color_group_mode),
            listOf("0" to stringResource(R.string.color_group_same), "1" to stringResource(R.string.color_group_same_sat), "2" to stringResource(R.string.color_group_diff)),
            colorGroupMode.toString()) { onColorGroupModeChange(it.toInt()) }
    }
}

// === Notification ===

@Composable
private fun NotificationPage(reminderMinutes: Int, onReminderMinutesChange: (Int) -> Unit, onBack: () -> Unit) {
    SubPage(stringResource(R.string.settings_category_notification), onBack) {
        DropdownItem(Icons.Default.Notifications, stringResource(R.string.reminder),
            listOf("0" to stringResource(R.string.reminder_off), "5" to stringResource(R.string.reminder_format, 5), "10" to stringResource(R.string.reminder_format, 10), "15" to stringResource(R.string.reminder_format, 15), "30" to stringResource(R.string.reminder_format, 30)),
            reminderMinutes.toString()) { onReminderMinutesChange(it.toInt()) }
    }
}

// === Data ===

@Composable
private fun DataPage(onExportJson: () -> Unit, onImportJson: () -> Unit, onExportIcs: () -> Unit, onExportImage: () -> Unit, onBack: () -> Unit) {
    SubPage(stringResource(R.string.settings_category_data), onBack) {
        SettingsItem(Icons.Default.FileUpload, stringResource(R.string.import_json), onClick = onImportJson)
        SettingsItem(Icons.Default.FileDownload, stringResource(R.string.export_json), onClick = onExportJson)
        SettingsItem(Icons.Default.CalendarMonth, stringResource(R.string.export_ics), onClick = onExportIcs)
        SettingsItem(Icons.Default.Image, stringResource(R.string.export_image), onClick = onExportImage)
    }
}

// === Shared components ===

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

@Composable
private fun SwitchItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable
private fun DropdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, options: List<Pair<String, String>>, currentKey: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.find { it.first == currentKey }?.second ?: ""
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(label) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingContent = {
                Box {
                    TextButton(onClick = { expanded = true }) {
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
            modifier = Modifier.clickable { expanded = true }
        )
    }
}
