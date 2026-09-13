package com.ty.gkschedule.ui.exam

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.data.ExamEntity
import com.ty.gkschedule.ui.theme.GKScheduleTheme
import kotlinx.coroutines.launch

// ponytail: 考试页独立Activity，返回走系统级预测动画；新增/编辑在内部状态切换，不进NavHost
class ExamActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialExamId = intent.getLongExtra("examId", -1L)

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val scope = rememberCoroutineScope()
            val context = this
            val darkMode by vm.darkMode.collectAsState(initial = "system")

            GKScheduleTheme(darkTheme = darkMode) {
                val examList by vm.examList.collectAsState(initial = emptyList())
                val courses by vm.courses.collectAsState(initial = emptyList())
                val semesterStart by vm.semesterStart.collectAsState(initial = java.time.LocalDate.now())
                val examLoading by vm.examLoading.collectAsState()
                val examYear by vm.examYear.collectAsState()
                val examSemester by vm.examSemester.collectAsState()
                val showExamReloginDialog by vm.showExamReloginDialog.collectAsState()
                val captchaImage by vm.captchaImage.collectAsState()
                val colorEngine by vm.colorEngine.collectAsState(initial = 0)
                val colorGroupMode by vm.colorGroupMode.collectAsState(initial = 2)
                val examLookaheadWeeks by vm.examLookaheadWeeks.collectAsState(initial = 1)
                val showExamSchedule by vm.showExamSchedule.collectAsState(initial = false)
                val diffColorPerWeek by vm.diffColorPerWeek.collectAsState(initial = false)
                val blurEffect by vm.blurEffect.collectAsState(initial = true)
                val selectedWeek by vm.selectedWeek.collectAsState()

                var editingExam by remember { mutableStateOf<ExamEntity?>(null) }
                var creating by remember { mutableStateOf(initialExamId == 0L) }

                LaunchedEffect(examList, initialExamId) {
                    if (initialExamId > 0) editingExam = examList.find { it.id == initialExamId }
                }

                when {
                    creating || editingExam != null -> ExamEditScreen(
                        exam = editingExam,
                        semesterStart = semesterStart,
                        onSave = { entities ->
                            vm.saveExams(entities)
                            scope.launch {
                                vm.messages.collect { msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    return@collect
                                }
                            }
                            android.widget.Toast.makeText(context, "成功导入 ${entities.size} 场考试！", android.widget.Toast.LENGTH_SHORT).show()
                            creating = false
                            editingExam = null
                        },
                        onDelete = { entity ->
                            vm.deleteExamById(entity.id)
                            creating = false
                            editingExam = null
                        },
                        onBack = { creating = false; editingExam = null },
                        blurEnabled = blurEffect                    )
                    else -> ExamScreen(
                        exams = examList,
                        colorCourses = courses,
                        customExams = emptyList(),
                        isLoading = examLoading,
                        semesterStart = semesterStart,
                        examYear = examYear,
                        examSemester = examSemester,
                        colorEngine = colorEngine,
                        colorGroupMode = colorGroupMode,
                        examLookaheadWeeks = examLookaheadWeeks,
                        showExamSchedule = showExamSchedule,
                        onShowExamScheduleChange = { vm.setShowExamSchedule(it) },
                        onExamLookaheadWeeksChange = { vm.setExamLookaheadWeeks(it) },
                        onAddExam = { creating = true },
                        onEditExam = { editingExam = it },
                        getStartTime = { vm.getStartTime(it) },
                        getEndTime = { vm.getEndTime(it) },
                        currentWeek = selectedWeek,
                        diffColorPerWeek = diffColorPerWeek,
                        showReloginDialog = showExamReloginDialog,
                        captchaImageBase64 = captchaImage,
                        onYearChange = { vm.setExamYear(it) },
                        onSemesterChange = { vm.setExamSemester(it) },
                        onRefresh = { vm.refreshExamSchedule() },
                        onDismissRelogin = { vm.dismissExamReloginDialog() },
                        onRefreshCaptcha = { vm.refreshCaptcha() },
                        onQuickRelogin = { cap -> vm.quickRelogin(cap) },
                        onBack = { finish() },
                        blurEnabled = blurEffect
                    )
                }
            }
        }
    }
}
