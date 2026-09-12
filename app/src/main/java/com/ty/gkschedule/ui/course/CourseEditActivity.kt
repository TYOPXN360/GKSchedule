package com.ty.gkschedule.ui.course

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.ui.theme.GKScheduleTheme

// ponytail: 课程编辑独立Activity，返回走系统级预测动画；与ExamActivity同模式
class CourseEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val courseId = intent.getLongExtra("courseId", -1L)

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")

            GKScheduleTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val courses by vm.courses.collectAsState(initial = emptyList())
                    val periodsPerDay by vm.periodsPerDay.collectAsState(initial = 10)
                    var currentCourse by remember { mutableStateOf<Course?>(null) }

                    LaunchedEffect(courseId, courses) {
                        currentCourse = if (courseId > 0) {
                            courses.find { it.id == courseId } ?: vm.getCourseById(courseId)
                        } else null
                    }

                    // ponytail: id<=0且DB空时直接给空表单，不白屏等collect
                    CourseEditScreen(
                        course = currentCourse,
                        allCourses = courses,
                        periodsPerDay = periodsPerDay,
                        onSave = { savedCourse, hiddenScopeName ->
                            vm.saveCourse(savedCourse, hiddenScopeName)
                            finish()
                        },
                        onDelete = {
                            vm.deleteCourse(it)
                            finish()
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
