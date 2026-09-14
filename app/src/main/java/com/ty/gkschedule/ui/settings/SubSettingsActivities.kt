package com.ty.gkschedule.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.ui.theme.GKScheduleTheme
import kotlinx.coroutines.launch

// ponytail: Lineage同款真SubSettings——1页1Activity，系统管返回栈+预测返回动画
abstract class SubSettingsBaseActivity : AppCompatActivity() {
    @Composable
    abstract fun SubContent(vm: ScheduleViewModel, finish: () -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ScheduleViewModel = viewModel()
            val scope = rememberCoroutineScope()
            val darkMode by vm.darkMode.collectAsState(initial = "system")
            GKScheduleTheme(darkTheme = darkMode) {
                SubContent(vm) { finish() }
            }
        }
    }
}

class SubSettingsSemesterActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
        val semesterStart by vm.semesterStart.collectAsState(initial = java.time.LocalDate.now())
        val totalWeeks by vm.totalWeeks.collectAsState(initial = 20)
        val periodsPerDay by vm.periodsPerDay.collectAsState(initial = 10)
        val firstDayOfWeek by vm.firstDayOfWeek.collectAsState(initial = 1)
        val hideEmptyWeeks by vm.hideEmptyWeeks.collectAsState(initial = false)
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        SemesterPage(
            semesterStart = semesterStart, totalWeeks = totalWeeks, periodsPerDay = periodsPerDay,
            firstDayOfWeek = firstDayOfWeek, hideEmptyWeeks = hideEmptyWeeks,
            onSemesterStartChange = { vm.setSemesterStart(it) },
            onTotalWeeksChange = { vm.setTotalWeeks(it) },
            onPeriodsPerDayChange = { vm.setPeriodsPerDay(it) },
            onFirstDayOfWeekChange = { vm.setFirstDayOfWeek(it) },
            onHideEmptyWeeksChange = { vm.setHideEmptyWeeks(it) },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}

class SubSettingsAppearanceActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
        val darkMode by vm.darkMode.collectAsState(initial = "system")
        val language by vm.language.collectAsState(initial = "system")
        val startPage by vm.startPage.collectAsState(initial = "today")
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        AppearancePage(
            darkMode = darkMode, language = language, startPage = startPage, blurEffect = blurEffect,
            onDarkModeChange = { vm.setDarkMode(it) },
            onLanguageChange = { vm.setLanguage(it) },
            onStartPageChange = { vm.setStartPage(it) },
            onBlurEffectChange = { vm.setBlurEffect(it) },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}

class SubSettingsScheduleStyleActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
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
        val showDateInHeader by vm.showDateInHeader.collectAsState(initial = false)
        val diffColorPerWeek by vm.diffColorPerWeek.collectAsState(initial = false)
        val showHiddenCourses by vm.showHiddenCourses.collectAsState(initial = false)
        val compactNavBar by vm.compactNavBar.collectAsState(initial = true)
        val pillContentMode by vm.pillContentMode.collectAsState(initial = 0)
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        ScheduleStylePage(
            gridHeight = gridHeight, gridCorner = gridCorner, gridSpacing = gridSpacing,
            showPeriodLabel = showPeriodLabel, autoGridHeight = autoGridHeight,
            mergeConsecutive = mergeConsecutive, showTimeLabel = showTimeLabel,
            detailedSplit = detailedSplit, colorEngine = colorEngine, colorGroupMode = colorGroupMode,
            showDateInHeader = showDateInHeader,
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
            onShowDateInHeaderChange = { vm.setShowDateInHeader(it) },
            diffColorPerWeek = diffColorPerWeek,
            onDiffColorPerWeekChange = { vm.setDiffColorPerWeek(it) },
            showHiddenCourses = showHiddenCourses,
            onShowHiddenCoursesChange = { vm.setShowHiddenCourses(it) },
            compactNavBar = compactNavBar,
            onCompactNavBarChange = { vm.setCompactNavBar(it) },
            pillContentMode = pillContentMode,
            onPillContentModeChange = { vm.setPillContentMode(it) },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}

class SubSettingsNotificationActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
        val reminderMinutes by vm.reminderMinutes.collectAsState(initial = 0)
        val reminderLiveUpdate by vm.reminderLiveUpdate.collectAsState(initial = true)
        val reminderExamLiveUpdate by vm.reminderExamLiveUpdate.collectAsState(initial = false)
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        NotificationPage(
            reminderMinutes = reminderMinutes, reminderLiveUpdate = reminderLiveUpdate,
            reminderExamLiveUpdate = reminderExamLiveUpdate,
            onReminderMinutesChange = { vm.setReminderMinutes(it) },
            onReminderLiveUpdateChange = { vm.setReminderLiveUpdate(it) },
            onReminderExamLiveUpdateChange = { vm.setReminderExamLiveUpdate(it) },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}

class SubSettingsSyncActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
        val autoSyncOnStart by vm.autoSyncOnStart.collectAsState(initial = true)
        val autoSyncIntervalValue by vm.autoSyncIntervalValue.collectAsState(initial = 1)
        val autoSyncIntervalUnit by vm.autoSyncIntervalUnit.collectAsState(initial = "d")
        val tokenHeartbeat by vm.tokenHeartbeat.collectAsState(initial = true)
        val showExamSchedule by vm.showExamSchedule.collectAsState(initial = false)
        val examLookaheadWeeks by vm.examLookaheadWeeks.collectAsState(initial = 1)
        val diffColorPerWeek by vm.diffColorPerWeek.collectAsState(initial = false)
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        SyncPage(
            autoSyncOnStart = autoSyncOnStart, autoSyncIntervalValue = autoSyncIntervalValue,
            autoSyncIntervalUnit = autoSyncIntervalUnit, tokenHeartbeat = tokenHeartbeat,
            showExamSchedule = showExamSchedule, examLookaheadWeeks = examLookaheadWeeks,
            diffColorPerWeek = diffColorPerWeek,
            onAutoSyncOnStartChange = { vm.setAutoSyncOnStart(it) },
            onAutoSyncIntervalValueChange = { vm.setAutoSyncIntervalValue(it) },
            onAutoSyncIntervalUnitChange = { vm.setAutoSyncIntervalUnit(it) },
            onTokenHeartbeatChange = { vm.setTokenHeartbeat(it) },
            onShowExamScheduleChange = { vm.setShowExamSchedule(it) },
            onExamLookaheadWeeksChange = { vm.setExamLookaheadWeeks(it) },
            onDiffColorPerWeekChange = { vm.setDiffColorPerWeek(it) },
            onFetchExam = { vm.refreshExamSchedule() },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}

class SubSettingsDataActivity : SubSettingsBaseActivity() {
    @Composable
    override fun SubContent(vm: ScheduleViewModel, finish: () -> Unit) {
        val scope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current
        val blurEffect by vm.blurEffect.collectAsState(initial = true)
        DataPage(
            onExportJson = {
                scope.launch {
                    val json = vm.exportJson()
                    if (json != null && vm.saveJsonToDownload(json)) {
                        android.widget.Toast.makeText(context, "已导出到 Downloads/schedule_export.json", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            onImportJson = {
                android.widget.Toast.makeText(context, "请回设置主页用导入", android.widget.Toast.LENGTH_SHORT).show()
            },
            onExportIcs = {
                scope.launch {
                    val ics = vm.exportIcs()
                    if (ics != null && vm.saveIcsToDownload(ics)) {
                        android.widget.Toast.makeText(context, "已导出到 Downloads/schedule_export.ics", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBack = finish, blurEnabled = blurEffect
        )
    }
}
