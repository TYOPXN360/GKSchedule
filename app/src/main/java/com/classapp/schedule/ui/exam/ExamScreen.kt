package com.classapp.schedule.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.classapp.schedule.api.ExamInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    exams: List<ExamInfo>,
    isLoading: Boolean,
    semesterStart: LocalDate,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("考试安排") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exams.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无考试安排",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请在校园网下获取",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            ExamScheduleGrid(
                exams = exams,
                semesterStart = semesterStart,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun ExamScheduleGrid(
    exams: List<ExamInfo>,
    semesterStart: LocalDate,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val examColors = listOf(
        Color(0xFFE3F2FD), Color(0xFFF3E5F5), Color(0xFFE8F5E9),
        Color(0xFFFFF3E0), Color(0xFFE0F7FA), Color(0xFFFCE4EC),
        Color(0xFFF1F8E9)
    )

    // Group exams by week and day
    val examsByWeek = remember(exams) {
        val result = mutableMapOf<Int, MutableList<Pair<Int, ExamInfo>>>()
        exams.forEach { exam ->
            try {
                val dateStr = exam.getExamDate()
                if (dateStr.isNotEmpty()) {
                    val date = LocalDate.parse(dateStr)
                    val daysDiff = ChronoUnit.DAYS.between(semesterStart, date).toInt()
                    val week = (daysDiff / 7) + 1
                    val dayOfWeek = date.dayOfWeek.value // 1=Mon, 7=Sun
                    result.getOrPut(week) { mutableListOf() }.add(dayOfWeek to exam)
                }
            } catch (_: Exception) {}
        }
        result.toSortedMap()
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 4.dp)
    ) {
        if (examsByWeek.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("无法解析考试日期", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        examsByWeek.forEach { (week, examList) ->
            // Week header
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "第${week}周",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Day headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                dayNames.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Exam grid for this week
            val examsByDay = examList.groupBy { it.first }
            val maxExamsInDay = examsByDay.values.maxOfOrNull { it.size } ?: 1

            repeat(maxExamsInDay) { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                    for (day in 1..7) {
                        val dayExams = examsByDay[day] ?: emptyList()
                        val exam = dayExams.getOrNull(row)
                        Box(
                            modifier = Modifier.weight(1f).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (exam != null) {
                                val colorIdx = examList.indexOf(exam) % examColors.size
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = examColors[colorIdx]),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = exam.second.kcmc,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = exam.second.getExamTimeRange(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (exam.second.cdmc.isNotEmpty()) {
                                            Text(
                                                text = exam.second.cdmc,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
