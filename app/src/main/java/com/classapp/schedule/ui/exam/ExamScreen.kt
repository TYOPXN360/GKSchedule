package com.classapp.schedule.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
    examYear: String,
    examSemester: String,
    onYearChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    // Auto-detect current academic year and semester on first display
    LaunchedEffect(Unit) {
        if (examYear.isEmpty()) {
            val now = LocalDate.now()
            val month = now.monthValue
            val startYear = if (month >= 9) now.year else now.year - 1
            onYearChange("$startYear-${startYear + 1}")
        }
        if (examSemester.isEmpty()) {
            val month = LocalDate.now().monthValue
            onSemesterChange(if (month in 2..7) "2" else "1")
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("考试安排") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Year + Semester selector + fetch button
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Year selector
                        var yearExpanded by remember { mutableStateOf(false) }
                        val currentYear = java.time.LocalDate.now().year
                        val years = ((currentYear - 3)..currentYear).map { "$it-${it + 1}" }.reversed()
                        OutlinedCard(
                            onClick = { yearExpanded = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(examYear.ifEmpty { "学年" }, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        // Dropdown in a separate Box overlay
                        Box {
                            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                                years.forEach { y ->
                                    DropdownMenuItem(text = { Text(y) }, onClick = { onYearChange(y); yearExpanded = false })
                                }
                            }
                        }

                        // Semester selector
                        var semExpanded by remember { mutableStateOf(false) }
                        OutlinedCard(
                            onClick = { semExpanded = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (examSemester == "1") "第一学期" else "第二学期",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        Box {
                            DropdownMenu(expanded = semExpanded, onDismissRequest = { semExpanded = false }) {
                                DropdownMenuItem(text = { Text("第一学期") }, onClick = { onSemesterChange("1"); semExpanded = false })
                                DropdownMenuItem(text = { Text("第二学期") }, onClick = { onSemesterChange("2"); semExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(
                            text = "请在校园网下获取",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRefresh,
                            enabled = !isLoading && examYear.isNotEmpty()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("获取")
                            }
                        }
                    }
                }
            }

            // Content
            if (isLoading && exams.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (exams.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    }
                }
            } else {
                // Card list sorted by date
                val sortedExams = exams.sortedBy { it.getExamDate() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedExams) { exam ->
                        ExamCard(exam = exam)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: ExamInfo, index: Int = 0) {
    val examDate = try { LocalDate.parse(exam.getExamDate()) } catch (_: Exception) { null }
    val isPast = examDate?.isBefore(LocalDate.now()) == true
    val alpha = if (isPast) 0.5f else 1f

    // Per-exam color based on course name hash — consistent across screens
    val examColors = com.classapp.schedule.util.CourseColors.getColors(0, count = 16)
    val colorIdx = exam.kcmc.hashCode().and(0x7fffffff) % examColors.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 0.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator — per exam color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPast) com.classapp.schedule.util.CourseColors.getBackground(colorIdx, examColors).copy(alpha = 0.3f)
                        else com.classapp.schedule.util.CourseColors.getBackground(colorIdx, examColors)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exam.kcmc,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                    if (isPast) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exam.kssj.isNotEmpty()) {
                        Text(
                            text = exam.kssj,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    }
                    if (exam.cdmc.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = exam.cdmc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    }
                }
                if (exam.ksfs.isNotEmpty()) {
                    Text(
                        text = exam.ksfs,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)
                    )
                }
            }
        }
    }
}
