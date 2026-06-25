package com.classapp.schedule.ui.settings

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.R
import com.classapp.schedule.ScheduleViewModel
import com.classapp.schedule.ui.theme.ClassAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val scope = rememberCoroutineScope()
            val context = this
            val darkMode by vm.darkMode.collectAsState(initial = "system")
            val language by vm.language.collectAsState(initial = "system")

            // Apply language change — recreate activity when language changes
            var lastLanguage by remember { mutableStateOf(language) }
            LaunchedEffect(language) {
                if (language != lastLanguage) {
                    lastLanguage = language
                    val locales = when (language) {
                        "en" -> androidx.core.os.LocaleListCompat.forLanguageTags("en")
                        "zh" -> androidx.core.os.LocaleListCompat.forLanguageTags("zh")
                        else -> androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                    }
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
                }
            }

            val importJsonLauncher = rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        try {
                            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            if (json != null) vm.importJson(json)
                        } catch (_: Exception) {}
                    }
                }
            }

            ClassAppTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Collect all states
                    val semesterStart by vm.semesterStart.collectAsState(initial = java.time.LocalDate.now())
                    val totalWeeks by vm.totalWeeks.collectAsState(initial = 20)
                    val periodsPerDay by vm.periodsPerDay.collectAsState(initial = 10)
                    val firstDayOfWeek by vm.firstDayOfWeek.collectAsState(initial = 1)
                    val gridHeight by vm.gridHeight.collectAsState(initial = 52)
                    val gridCorner by vm.gridCorner.collectAsState(initial = 8)
                    val gridSpacing by vm.gridSpacing.collectAsState(initial = 2)
                    val showPeriodLabel by vm.showPeriodLabel.collectAsState(initial = true)
                    val autoGridHeight by vm.autoGridHeight.collectAsState(initial = true)
                    val mergeConsecutive by vm.mergeConsecutive.collectAsState(initial = true)
                    val showTimeLabel by vm.showTimeLabel.collectAsState(initial = true)
                    val detailedSplit by vm.detailedSplit.collectAsState(initial = false)
                    val colorEngine by vm.colorEngine.collectAsState(initial = 0)
                    val colorGroupMode by vm.colorGroupMode.collectAsState(initial = 2)
                    val hideEmptyWeeks by vm.hideEmptyWeeks.collectAsState(initial = false)
                    val showDateInHeader by vm.showDateInHeader.collectAsState(initial = false)
                    val reminderMinutes by vm.reminderMinutes.collectAsState(initial = 0)
                    val autoSyncOnStart by vm.autoSyncOnStart.collectAsState(initial = true)
                    val autoSyncIntervalValue by vm.autoSyncIntervalValue.collectAsState(initial = 1)
                    val autoSyncIntervalUnit by vm.autoSyncIntervalUnit.collectAsState(initial = "d")

                    SettingsScreen(
                        semesterStart = semesterStart,
                        totalWeeks = totalWeeks,
                        periodsPerDay = periodsPerDay,
                        darkMode = darkMode,
                        language = language,
                        firstDayOfWeek = firstDayOfWeek,
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
                        hideEmptyWeeks = hideEmptyWeeks,
                        showDateInHeader = showDateInHeader,
                        reminderMinutes = reminderMinutes,
                        autoSyncOnStart = autoSyncOnStart,
                        autoSyncIntervalValue = autoSyncIntervalValue,
                        autoSyncIntervalUnit = autoSyncIntervalUnit,
                        onSemesterStartChange = { vm.setSemesterStart(it) },
                        onTotalWeeksChange = { vm.setTotalWeeks(it) },
                        onPeriodsPerDayChange = { vm.setPeriodsPerDay(it) },
                        onDarkModeChange = { vm.setDarkMode(it) },
                        onLanguageChange = { vm.setLanguage(it) },
                        onFirstDayOfWeekChange = { vm.setFirstDayOfWeek(it) },
                        onGridHeightChange = { vm.setGridHeight(it) },
                        onGridCornerChange = { vm.setGridCorner(it) },
                        onGridSpacingChange = { vm.setGridSpacing(it) },
                        onShowPeriodLabelChange = { vm.setShowPeriodLabel(it) },
                        onAutoGridHeightChange = { vm.setAutoGridHeight(it) },
                        onMergeConsecutiveChange = { vm.setMergeConsecutive(it) },
                        onShowTimeLabelChange = { vm.setShowTimeLabel(it) },
                        onDetailedSplitChange = { vm.setDetailedSplit(it) },
                        onColorEngineChange = { vm.setColorEngine(it) },
                        onColorGroupModeChange = { vm.setColorGroupMode(it) },
                        onHideEmptyWeeksChange = { vm.setHideEmptyWeeks(it) },
                        onShowDateInHeaderChange = { vm.setShowDateInHeader(it) },
                        onReminderMinutesChange = { vm.setReminderMinutes(it) },
                        onAutoSyncOnStartChange = { vm.setAutoSyncOnStart(it) },
                        onAutoSyncIntervalValueChange = { vm.setAutoSyncIntervalValue(it) },
                        onAutoSyncIntervalUnitChange = { vm.setAutoSyncIntervalUnit(it) },
                        onExportJson = {
                            scope.launch {
                                val json = vm.exportJson()
                                if (json != null) {
                                    if (vm.saveJsonToDownload(json)) {
                                        android.widget.Toast.makeText(context, "已导出到 Downloads/schedule_export.json", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.import_failed), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onImportJson = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                type = "application/json"
                            }
                            importJsonLauncher.launch(intent)
                        },
                        onExportIcs = {
                            scope.launch {
                                val ics = vm.exportIcs()
                                if (ics != null) {
                                    if (vm.saveIcsToDownload(ics)) {
                                        android.widget.Toast.makeText(context, "已导出到 Downloads/schedule_export.ics", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onExportImage = {}
                    )
                }
            }
        }
    }
}
