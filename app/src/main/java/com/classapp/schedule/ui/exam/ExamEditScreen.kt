package com.classapp.schedule.ui.exam

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.data.Course
import com.classapp.schedule.ui.theme.LocalAppIsDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditScreen(
    course: Course?,
    periodsPerDay: Int,
    semesterStart: LocalDate,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onBack: () -> Unit
) {
    val isDark = LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    var name by remember { mutableStateOf(course?.name ?: "") }
    var classroom by remember { mutableStateOf(course?.classroom ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var examMethod by remember { mutableStateOf(course?.remark?.split("\n")?.getOrNull(1) ?: "闭卷") }
    var remarkText by remember { mutableStateOf(course?.remark?.split("\n")?.getOrNull(2) ?: "") }

    var examDate by remember {
        mutableStateOf(
            if (course != null) {
                val weekOffset = (course.weekRange.toIntOrNull() ?: 1) - 1
                try {
                    val base = LocalDate.now().withMonth(9).withDayOfMonth(1)
                    base.plusWeeks(weekOffset.toLong()).plusDays((course.dayOfWeek - 1).toLong())
                } catch (_: Exception) { LocalDate.now() }
            } else LocalDate.now()
        )
    }
    var startTime by remember { mutableStateOf(course?.customStartTime ?: "09:00") }
    var endTime by remember { mutableStateOf(course?.customEndTime ?: "11:00") }

    var showM3DatePicker by remember { mutableStateOf(false) }
    var showM3StartTimePicker by remember { mutableStateOf(false) }
    var showM3EndTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = scaffoldBg,
        topBar = {
            TopAppBar(
                title = { Text(if (course == null) "添加考试安排" else "编辑考试安排", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (course != null) {
                        IconButton(onClick = { onDelete(course) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("考试科目") }, leadingIcon = { Icon(Icons.Default.Class, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Date selection card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.clickable { showM3DatePicker = true }.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("考试日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(examDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Time range card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("考试时间", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showM3StartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("开始: $startTime") }
                        OutlinedButton(onClick = { showM3EndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("结束: $endTime") }
                    }
                }
            }

            OutlinedTextField(
                value = classroom, onValueChange = { classroom = it },
                label = { Text("考场 / 教室") }, leadingIcon = { Icon(Icons.Default.Room, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = teacher, onValueChange = { teacher = it },
                label = { Text("监考教师 (选填)") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("考试方式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("闭卷", "开卷", "机考", "开卷(半)").forEach { method ->
                        FilterChip(selected = examMethod == method, onClick = { examMethod = method }, label = { Text(method) })
                    }
                }
            }

            OutlinedTextField(
                value = remarkText, onValueChange = { remarkText = it },
                label = { Text("其他备注 (选填)") }, leadingIcon = { Icon(Icons.Default.Assignment, null) },
                modifier = Modifier.fillMaxWidth(), minLines = 1,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val daysDiff = ChronoUnit.DAYS.between(semesterStart, examDate).toInt()
                    val targetWeek = (daysDiff / 7) + 1
                    val dayOfWeek = examDate.dayOfWeek.value

                    fun timeToPeriod(timeStr: String): Int {
                        val hour = timeStr.split(":")[0].toInt()
                        return when { hour < 10 -> 1; hour < 12 -> 3; hour < 16 -> 5; hour < 18 -> 7; else -> 9 }
                    }
                    val startP = timeToPeriod(startTime)

                    val savedCourse = Course(
                        id = course?.id ?: -abs(System.currentTimeMillis() % 1000000L + 2000000L),
                        name = name, teacher = teacher, classroom = classroom,
                        dayOfWeek = dayOfWeek, startPeriod = startP, periods = 2,
                        weekRange = targetWeek.toString(),
                        remark = "$startTime-$endTime\n$examMethod\n$remarkText".trimEnd(),
                        isCustomTime = true, customStartTime = startTime, customEndTime = endTime,
                        isManuallyEdited = true
                    )
                    onSave(savedCourse)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                enabled = name.isNotBlank()
            ) { Text("保存安排", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // M3 DatePicker
    if (showM3DatePicker) {
        val initialEpochMillis = remember(examDate) { examDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)
        DatePickerDialog(
            onDismissRequest = { showM3DatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        examDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showM3DatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showM3DatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    // M3 TimePicker - start time
    if (showM3StartTimePicker) {
        val startParts = startTime.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = startParts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showM3StartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showM3StartTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showM3StartTimePicker = false }) { Text("取消") } },
            title = { Text("选择开始时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }
        )
    }

    // M3 TimePicker - end time
    if (showM3EndTimePicker) {
        val endParts = endTime.split(":")
        val timePickerState = rememberTimePickerState(
            initialHour = endParts.getOrNull(0)?.toIntOrNull() ?: 11,
            initialMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showM3EndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showM3EndTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showM3EndTimePicker = false }) { Text("取消") } },
            title = { Text("选择结束时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }
        )
    }
}
