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
                val blurEffect by vm.blurEffect.collectAsState(initial = true)
                    var currentCourse by remember { mutableStateOf<Course?>(null) }
                    var loaded by remember { mutableStateOf(false) }

                    LaunchedEffect(courseId) {
                        currentCourse = if (courseId > 0) {
                            // ponytail: 先读内存列表（快），miss 再查库（跨 Activity 时 collect 还没吐数据）
                            courses.find { it.id == courseId } ?: vm.getCourseById(courseId)
                        } else null
                        loaded = true
                    }
                    // DB 后到时补一次，避免 collect 晚到导致空表单
                    LaunchedEffect(courses) {
                        if (courseId > 0 && currentCourse == null) {
                            courses.find { it.id == courseId }?.let {
                                currentCourse = it
                                loaded = true
                            }
                        }
                    }

                    // ponytail: id<=0且DB空时直接给空表单，不白屏等collect
                    if (loaded || courseId <= 0) {
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
                            onBack = { finish() },
                            blurEnabled = blurEffect
                        )
                    }
                }
            }
        }
    }
}
