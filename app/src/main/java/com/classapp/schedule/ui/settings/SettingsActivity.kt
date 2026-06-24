package com.classapp.schedule.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.ScheduleViewModel
import com.classapp.schedule.ui.theme.ClassAppTheme
import java.time.LocalDate

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")

            ClassAppTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsContent(vm = vm, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(vm: ScheduleViewModel, onBack: () -> Unit) {
    val semesterStart by vm.semesterStart.collectAsState(initial = LocalDate.now())
    val totalWeeks by vm.totalWeeks.collectAsState(initial = 20)
    val periodsPerDay by vm.periodsPerDay.collectAsState(initial = 10)
    val darkMode by vm.darkMode.collectAsState(initial = "system")
    val language by vm.language.collectAsState(initial = "system")
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
    val reminderMinutes by vm.reminderMinutes.collectAsState(initial = 0)

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
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
                reminderMinutes = reminderMinutes,
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
                onReminderMinutesChange = { vm.setReminderMinutes(it) },
                onExportJson = {},
                onImportJson = {},
                onExportIcs = {},
                onExportImage = {}
            )
        }
    }
}
