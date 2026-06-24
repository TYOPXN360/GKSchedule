package com.classapp.schedule.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.util.CourseColors
import java.time.LocalDate

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
    onReminderMinutesChange: (Int) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportIcs: () -> Unit,
    onExportImage: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // === Semester ===
        SectionHeader(stringResource(R.string.semester_config))
        SettingsItem(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.semester_start),
            subtitle = semesterStart.toString(),
            onClick = {
                DatePickerDialog(context, { _, y, m, d ->
                    onSemesterStartChange(LocalDate.of(y, m + 1, d))
                }, semesterStart.year, semesterStart.monthValue - 1, semesterStart.dayOfMonth).show()
            }
        )
        StepperItem(Icons.Default.DateRange, stringResource(R.string.total_weeks), totalWeeks, 1, 30, onTotalWeeksChange)
        StepperItem(Icons.Default.AccessTime, stringResource(R.string.periods_per_day), periodsPerDay, 4, 14, onPeriodsPerDayChange)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // === Appearance ===
        SectionHeader(stringResource(R.string.appearance))
        val darkOptions = listOf("system" to stringResource(R.string.dark_mode_system), "light" to stringResource(R.string.dark_mode_light), "dark" to stringResource(R.string.dark_mode_dark))
        DropdownItem(Icons.Default.DarkMode, stringResource(R.string.dark_mode), darkOptions, darkMode, onDarkModeChange)

        val langOptions = listOf("system" to stringResource(R.string.language_system), "en" to stringResource(R.string.language_en), "zh" to stringResource(R.string.language_zh))
        DropdownItem(Icons.Default.Language, stringResource(R.string.language), langOptions, language, onLanguageChange)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // === Schedule Style ===
        SectionHeader(stringResource(R.string.schedule_style))
        SwitchItem(Icons.Default.AutoAwesome, stringResource(R.string.auto_grid_height), autoGridHeight, onAutoGridHeightChange)
        SwitchItem(Icons.Default.ViewColumn, stringResource(R.string.merge_consecutive), mergeConsecutive, onMergeConsecutiveChange)
        if (!mergeConsecutive) {
            SwitchItem(Icons.Default.ViewDay, stringResource(R.string.detailed_split), detailedSplit, onDetailedSplitChange)
        }
        SwitchItem(Icons.Default.AccessTime, stringResource(R.string.show_time_label), showTimeLabel, onShowTimeLabelChange)

        // Color engine
        val colorEngineOptions = listOf(
            "0" to stringResource(R.string.color_engine_monet),
            "1" to stringResource(R.string.color_engine_vibrant),
            "2" to stringResource(R.string.color_engine_classic),
            "3" to stringResource(R.string.color_engine_hsl)
        )
        DropdownItem(Icons.Default.Palette, stringResource(R.string.color_engine), colorEngineOptions, colorEngine.toString(), { onColorEngineChange(it.toInt()) })
        if (!autoGridHeight) {
            StepperItem(Icons.Default.Height, stringResource(R.string.grid_height), gridHeight, 36, 80, onGridHeightChange)
        }
        StepperItem(Icons.Default.RoundedCorner, stringResource(R.string.grid_corner), gridCorner, 0, 20, onGridCornerChange)
        StepperItem(Icons.Default.SpaceBar, stringResource(R.string.grid_spacing), gridSpacing, 0, 8, onGridSpacingChange)
        SwitchItem(Icons.Default.Pin, stringResource(R.string.show_period_label), showPeriodLabel, onShowPeriodLabelChange)

        val fdowOptions = listOf(1 to stringResource(R.string.first_day_monday), 7 to stringResource(R.string.first_day_sunday))
        DropdownItem(Icons.Default.FirstPage, stringResource(R.string.first_day_of_week), fdowOptions.map { it.first.toString() to it.second }, firstDayOfWeek.toString(), { onFirstDayOfWeekChange(it.toInt()) })

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // === Reminder ===
        SectionHeader(stringResource(R.string.reminder))
        val reminderOptions = listOf(
            "0" to stringResource(R.string.reminder_off),
            "5" to stringResource(R.string.reminder_format, 5),
            "10" to stringResource(R.string.reminder_format, 10),
            "15" to stringResource(R.string.reminder_format, 15),
            "30" to stringResource(R.string.reminder_format, 30)
        )
        DropdownItem(Icons.Default.Notifications, stringResource(R.string.reminder), reminderOptions, reminderMinutes.toString(), { onReminderMinutesChange(it.toInt()) })

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // === Data ===
        SectionHeader(stringResource(R.string.data_management))
        SettingsItem(Icons.Default.FileUpload, stringResource(R.string.import_json), onClick = onImportJson)
        SettingsItem(Icons.Default.FileDownload, stringResource(R.string.export_json), onClick = onExportJson)
        SettingsItem(Icons.Default.CalendarMonth, stringResource(R.string.export_ics), onClick = onExportIcs)
        SettingsItem(Icons.Default.Image, stringResource(R.string.export_image), onClick = onExportImage)

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // === About ===
        SectionHeader(stringResource(R.string.about))
        SettingsItem(Icons.AutoMirrored.Filled.Article, stringResource(R.string.app_name), stringResource(R.string.version_format, "1.0.0"), onClick = {})

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable private fun StepperItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
        },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

@Composable private fun SwitchItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable private fun DropdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, options: List<Pair<String, String>>, currentKey: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.find { it.first == currentKey }?.second ?: ""
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(label) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(key); expanded = false })
            }
        }
    }
}
