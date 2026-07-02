package com.classapp.schedule.ui.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Room
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.data.Course
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.classapp.schedule.util.JsonImportExport
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
    onSave: (List<Course>) -> Unit,
    onDelete: (Course) -> Unit,
    onBack: () -> Unit
) {
    val isDark = LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var name by remember { mutableStateOf(course?.name ?: "") }
    var classroom by remember { mutableStateOf(course?.classroom ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var examMethod by remember { mutableStateOf(course?.remark?.split("\n")?.getOrNull(1) ?: "闭卷") }
    var remarkText by remember { mutableStateOf(course?.remark?.split("\n")?.getOrNull(2) ?: "") }
    var examDate by remember { mutableStateOf(if (course != null) { val weekOffset = (course.weekRange.toIntOrNull() ?: 1) - 1; try { semesterStart.plusWeeks(weekOffset.toLong()).plusDays((course.dayOfWeek - 1).toLong()) } catch (_: Exception) { LocalDate.now() } } else LocalDate.now()) }
    var startTime by remember { mutableStateOf(course?.customStartTime ?: "09:00") }
    var endTime by remember { mutableStateOf(course?.customEndTime ?: "11:00") }
    var showM3DatePicker by remember { mutableStateOf(false) }
    var showM3StartTimePicker by remember { mutableStateOf(false) }
    var showM3EndTimePicker by remember { mutableStateOf(false) }
    var batchExams by remember { mutableStateOf<List<Course>>(emptyList()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiClipboardInput by remember { mutableStateOf("") }
    var aiErrorHint by remember { mutableStateOf("") }

    val examAiPrompt = """
        你是一个考务日程结构化清洗专家。请将用户提供的考务通知、考试安排文本，精准提取并整理为标准的 JSON 数组格式。
        要求：
        1. 必须使用标准的 ```json ... ``` 代码块包裹返回的 JSON 数据。
        2. 如果同一场考试分布在多个不同的考场/教室，请务必将其合并为单场考试对象，并将考场合并为一个字符串，用逗号隔开（例如: "classroom": "某某考场101, 某某考场102"）。
        3. 即使只有一场考试，也必须返回一个包裹着对象的标准的 JSON 数组。
        标准 JSON 格式示例：
        ```json
        [
          {
            "name": "示例考试科目",
            "classroom": "某某考场101, 某某考场102",
            "teacher": "监考老师姓名",
            "examDate": "2026-01-01", 
            "startTime": "09:00",
            "endTime": "11:00",
            "examMethod": "闭卷",
            "remark": "带好有效身份证件"
          }
        ]
        ```
        注意：examDate 必须是 YYYY-MM-DD 格式。
    """.trimIndent()

    fun extractJsonArrayString(input: String): String {
        val start = input.indexOf('['); val end = input.lastIndexOf(']')
        if (start != -1 && end != -1 && end > start) return input.substring(start, end + 1)
        return input
    }

    Scaffold(containerColor = scaffoldBg, topBar = {
        TopAppBar(title = { Text(if (course == null) "添加考试安排" else "编辑考试安排", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg, scrolledContainerColor = scaffoldBg),
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { if (course != null) { IconButton(onClick = { onDelete(course) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(modifier = Modifier.height(4.dp))

            // AI Import Panel
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f), MaterialTheme.shapes.large), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showAiPanel = !showAiPanel }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary); Text("从AI中导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        Text(if (showAiPanel) "收起" else "展开", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                    }
                    AnimatedVisibility(visible = showAiPanel) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("1. 复制提示词发送给 AI。直接复制包含 markdown 格式的混杂聊天记录即可。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { clipboardManager.setText(AnnotatedString(examAiPrompt)); android.widget.Toast.makeText(context, "提示词已复制！", android.widget.Toast.LENGTH_SHORT).show() }, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("复制 AI 考务解析提示词", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            Text("2. 在下方贴入包含 JSON 数组代码块的聊天数据全文：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = aiClipboardInput, onValueChange = { aiClipboardInput = it; aiErrorHint = "" }, placeholder = { Text("支持贴入包含大模型寒暄文本的前后交际复合消息内容...", style = MaterialTheme.typography.bodyMedium) }, leadingIcon = { Icon(Icons.Default.DataObject, null) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = MaterialTheme.shapes.medium)
                            if (aiErrorHint.isNotEmpty()) Text(aiErrorHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            FilledTonalButton(onClick = {
                                if (aiClipboardInput.isBlank()) return@FilledTonalButton
                                try {
                                    val cleanedJson = extractJsonArrayString(aiClipboardInput.trim())
                                    val jsonArray = org.json.JSONArray(cleanedJson)
                                    val parsedExams = mutableListOf<Course>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val eName = obj.optString("name"); val eClassroom = obj.optString("classroom"); val eTeacher = obj.optString("teacher"); val eMethod = obj.optString("examMethod", "闭卷"); val eStartTime = obj.optString("startTime", "09:00"); val eEndTime = obj.optString("endTime", "11:00"); val eRemark = obj.optString("remark"); val dateStr = obj.optString("examDate")
                                        val parsedDate = if (dateStr.isNotEmpty()) LocalDate.parse(dateStr) else LocalDate.now()
                                        val daysDiff = ChronoUnit.DAYS.between(semesterStart, parsedDate).toInt(); val targetWeek = (daysDiff / 7) + 1; val dayOfWeek = parsedDate.dayOfWeek.value
                                        fun timeToPeriod(timeStr: String): Int { val hour = timeStr.split(":")[0].toIntOrNull() ?: 9; return when { hour < 10 -> 1; hour < 12 -> 3; hour < 16 -> 5; hour < 18 -> 7; else -> 9 } }
                                        parsedExams.add(Course(id = course?.id ?: -abs(System.currentTimeMillis() % 1000000L + 2000000L + i), name = eName, teacher = eTeacher, classroom = eClassroom, dayOfWeek = dayOfWeek, startPeriod = timeToPeriod(eStartTime), periods = 2, weekRange = targetWeek.toString(), remark = "$eStartTime-$eEndTime\n$eMethod\n$eRemark".trimEnd(), isCustomTime = true, customStartTime = eStartTime, customEndTime = eEndTime, isManuallyEdited = true))
                                    }
                                    if (parsedExams.isNotEmpty()) {
                                        batchExams = parsedExams; selectedTabIndex = 0
                                        val first = parsedExams[0]; name = first.name; classroom = first.classroom; teacher = first.teacher; startTime = first.customStartTime; endTime = first.customEndTime
                                        val remarkParts = first.remark.split("\n"); examMethod = remarkParts.getOrNull(1) ?: "闭卷"; remarkText = remarkParts.getOrNull(2) ?: ""
                                        val weekOffset = (first.weekRange.toIntOrNull() ?: 1) - 1; examDate = semesterStart.plusWeeks(weekOffset.toLong()).plusDays((first.dayOfWeek - 1).toLong())
                                        aiErrorHint = ""; showAiPanel = false
                                        android.widget.Toast.makeText(context, "成功录入 ${parsedExams.size} 场考试，请通过多标签切换审核！", android.widget.Toast.LENGTH_SHORT).show()
                                    } else { aiErrorHint = "未发现有效的考务配置信息。" }
                                } catch (e: Exception) { aiErrorHint = "考务清洗失败，请确保贴入的信息中带有标准的 [ ... ] 数组节点。" }
                            }, shape = MaterialTheme.shapes.medium, enabled = aiClipboardInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("智能清洗并解析注入", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // Multi-tab review
            if (batchExams.size > 1) {
                ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 0.dp, containerColor = Color.Transparent, modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)) {
                    batchExams.forEachIndexed { index, _ ->
                        Tab(selected = selectedTabIndex == index, onClick = {
                            val currentState = Course(id = course?.id ?: 0, name = name.trim(), teacher = teacher.trim(), classroom = classroom.trim(), dayOfWeek = examDate.dayOfWeek.value, startPeriod = 1, periods = 2, weekRange = ((ChronoUnit.DAYS.between(semesterStart, examDate).toInt() / 7) + 1).toString(), remark = "$startTime-$endTime\n$examMethod\n$remarkText".trimEnd(), isCustomTime = true, customStartTime = startTime, customEndTime = endTime, isManuallyEdited = true)
                            batchExams = batchExams.toMutableList().apply { this[selectedTabIndex] = currentState }
                            selectedTabIndex = index; val target = batchExams[index]; name = target.name; classroom = target.classroom; teacher = target.teacher; startTime = target.customStartTime; endTime = target.customEndTime
                            val remarkParts = target.remark.split("\n"); examMethod = remarkParts.getOrNull(1) ?: "闭卷"; remarkText = remarkParts.getOrNull(2) ?: ""
                            val weekOffset = (target.weekRange.toIntOrNull() ?: 1) - 1; examDate = semesterStart.plusWeeks(weekOffset.toLong()).plusDays((target.dayOfWeek - 1).toLong())
                        }, text = { Text("考试 ${index + 1}", fontWeight = FontWeight.Bold) })
                    }
                }
            }

            // Form fields
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("考试科目") }, leadingIcon = { Icon(Icons.Default.Class, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Row(modifier = Modifier.clickable { showM3DatePicker = true }.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(16.dp)); Column { Text("考试日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(examDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) } } }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(16.dp)); Text("考试时间", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }; Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = { showM3StartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("开始: $startTime") }; OutlinedButton(onClick = { showM3EndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("结束: $endTime") } } } }
            OutlinedTextField(value = classroom, onValueChange = { classroom = it }, label = { Text("考场 / 教室") }, leadingIcon = { Icon(Icons.Default.Room, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("监考教师 (选填)") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("考试方式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("闭卷", "开卷", "机考", "开卷(半)").forEach { method -> FilterChip(selected = examMethod == method, onClick = { examMethod = method }, label = { Text(method) }) } } }
            OutlinedTextField(value = remarkText, onValueChange = { remarkText = it }, label = { Text("其他备注 (选填)") }, leadingIcon = { Icon(Icons.Default.Assignment, null) }, modifier = Modifier.fillMaxWidth(), minLines = 1, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (name.isBlank()) return@Button
                val daysDiff = ChronoUnit.DAYS.between(semesterStart, examDate).toInt(); val targetWeek = (daysDiff / 7) + 1; val dayOfWeek = examDate.dayOfWeek.value
                fun timeToPeriod(timeStr: String): Int { val hour = timeStr.split(":")[0].toInt(); return when { hour < 10 -> 1; hour < 12 -> 3; hour < 16 -> 5; hour < 18 -> 7; else -> 9 } }
                val currentExam = Course(id = course?.id ?: -abs(System.currentTimeMillis() % 1000000L + 2000000L), name = name, teacher = teacher, classroom = classroom, dayOfWeek = dayOfWeek, startPeriod = timeToPeriod(startTime), periods = 2, weekRange = targetWeek.toString(), remark = "$startTime-$endTime\n$examMethod\n$remarkText".trimEnd(), isCustomTime = true, customStartTime = startTime, customEndTime = endTime, isManuallyEdited = true)
                if (batchExams.isNotEmpty()) {
                    val finalBatch = batchExams.toMutableList().apply { this[selectedTabIndex] = currentExam }
                    onSave(finalBatch)
                } else { onSave(listOf(currentExam)) }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), enabled = name.isNotBlank()) { Text(if (batchExams.size > 1) "保存全部考试安排 (${batchExams.size})" else "保存安排", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showM3DatePicker) { val initialEpochMillis = remember(examDate) { examDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }; val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis); DatePickerDialog(onDismissRequest = { showM3DatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> examDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() }; showM3DatePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3DatePicker = false }) { Text("取消") } }) { DatePicker(state = datePickerState) } }
    if (showM3StartTimePicker) { val startParts = startTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = startParts.getOrNull(0)?.toIntOrNull() ?: 9, initialMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3StartTimePicker = false }, confirmButton = { TextButton(onClick = { startTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3StartTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3StartTimePicker = false }) { Text("取消") } }, title = { Text("选择开始时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
    if (showM3EndTimePicker) { val endParts = endTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = endParts.getOrNull(0)?.toIntOrNull() ?: 11, initialMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3EndTimePicker = false }, confirmButton = { TextButton(onClick = { endTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3EndTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3EndTimePicker = false }) { Text("取消") } }, title = { Text("选择结束时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
}
