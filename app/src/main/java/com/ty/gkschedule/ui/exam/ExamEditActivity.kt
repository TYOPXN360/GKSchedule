package com.ty.gkschedule.ui.exam

import com.ty.gkschedule.R
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.ui.theme.GKScheduleTheme
import java.time.LocalDate

// ponytail: 考试添加/编辑独立Activity——Lineage SubSettings同款，系统管返回+预测动画
class ExamEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val examId = intent.getLongExtra("examId", -1L)

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")
            val blurEffect by vm.blurEffect.collectAsState(initial = false)

            GKScheduleTheme(darkTheme = darkMode) {
                val examList by vm.examList.collectAsState(initial = emptyList())
                val semesterStart by vm.semesterStart.collectAsState(initial = LocalDate.now())
                var editingExam by remember { mutableStateOf(if (examId > 0) examList.find { it.id == examId } else null) }

                LaunchedEffect(examList, examId) {
                    if (examId > 0) editingExam = examList.find { it.id == examId }
                }

                ExamEditScreen(
                    exam = editingExam,
                    semesterStart = semesterStart,
                    onSave = { entities ->
                        vm.saveExams(entities)
                        android.widget.Toast.makeText(this, getString(R.string.exams_imported_fmt, entities.size), android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDelete = { entity ->
                        vm.deleteExamById(entity.id)
                        finish()
                    },
                    onBack = { finish() },
                    blurEnabled = blurEffect
                )
            }
        }
    }
}
