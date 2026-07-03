package com.ty.gkschedule.ui.course

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.R
import com.ty.gkschedule.data.Course
import com.ty.gkschedule.util.CourseColors
import com.ty.gkschedule.util.JsonImportExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    course: Course?,
    allCourses: List<Course> = emptyList(),
    periodsPerDay: Int,
    onSave: (Course, String?) -> Unit,
    onDelete: (Course) -> Unit,
    onBack: () -> Unit
) {
    val isEditing = course != null
    val hiddenScopeName = course?.name
    val context = LocalContext.current
    val clipboardManager = remember(context) { context.getSystemService(ClipboardManager::class.java) }

    var name by remember { mutableStateOf(course?.name ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var classroom by remember { mutableStateOf(course?.classroom ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(course?.dayOfWeek ?: 1) }
    var startPeriod by remember { mutableIntStateOf(course?.startPeriod ?: 1) }
    var periods by remember { mutableIntStateOf(course?.periods ?: 1) }
    var colorIndex by remember { mutableIntStateOf(course?.colorIndex ?: 0) }
    var isHidden by remember { mutableStateOf(course?.isHidden ?: false) }
    var weekRange by remember { mutableStateOf(course?.weekRange ?: "all") }
    var customWeekRange by remember { mutableStateOf(if (course?.weekRange == "all" || course?.weekRange == "odd" || course?.weekRange == "even") "" else (course?.weekRange ?: "")) }
    var remark by remember { mutableStateOf(course?.remark ?: "") }
    var isCustomTime by remember { mutableStateOf(course?.isCustomTime ?: false) }
    var customStartTime by remember { mutableStateOf(course?.customStartTime ?: "08:00") }
    var customEndTime by remember { mutableStateOf(course?.customEndTime ?: "09:00") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showM3StartTimePicker by remember { mutableStateOf(false) }
    var showM3EndTimePicker by remember { mutableStateOf(false) }

    var batchCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // When editing an existing course, load all same-name courses as tabs
    LaunchedEffect(course) {
        if (course != null && allCourses.isNotEmpty()) {
            val sameNameCourses = allCourses
                .filter { it.name == course.name }
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))
            if (sameNameCourses.size > 1) {
                batchCourses = sameNameCourses
                selectedTabIndex = sameNameCourses.indexOfFirst { it.id == course.id }.coerceAtLeast(0)
                val target = sameNameCourses[selectedTabIndex]
                teacher = target.teacher; classroom = target.classroom
                dayOfWeek = target.dayOfWeek; startPeriod = target.startPeriod; periods = target.periods; remark = target.remark
                if (target.weekRange != "all" && target.weekRange != "odd" && target.weekRange != "even") { weekRange = "custom"; customWeekRange = target.weekRange } else { weekRange = target.weekRange; customWeekRange = "" }
                isCustomTime = target.isCustomTime; customStartTime = target.customStartTime; customEndTime = target.customEndTime
                isHidden = sameNameCourses.any { it.isHidden }
            }
        }
    }

    var showAiPanel by remember { mutableStateOf(false) }
    var aiClipboardInput by remember { mutableStateOf("") }
    var aiErrorHint by remember { mutableStateOf("") }

    val courseAiPrompt = """
        你是一个课程表数据结构化清洗专家。请将用户接下来发送的任意格式的课程安排文本（或图片识别出来的杂乱文字），提取并重整为标准 JSON 数组格式。
        要求：
        1. 必须使用标准的 ```json ... ``` 代码块包裹返回的 JSON 数据。
        2. 如果同一门课程在同一个时间段内分布在多个不同的教室，请务必将其合并为一个课程对象，并将教室合并为一个字符串，用逗号隔开（例如: "classroom": "某某教学楼A101, 某某教学楼A102"）。
        3. 即使只有一门课程，也必须返回一个包裹着对象的标准的 JSON 数组。
        标准 JSON 数组结构体示例：
        ```json
        [
          {
            "name": "示例课程名称",
            "teacher": "任课老师姓名",
            "classroom": "某某教学楼A101, 某某教学楼A102",
            "dayOfWeek": 1, 
            "startPeriod": 3,
            "periods": 2,
            "weekRange": "1-16",
            "remark": "备注说明信息"
          }
        ]
        ```
        注意：dayOfWeek 必须是数字 (1=周一, 7=周日)；startPeriod 为开始节次，periods 为持续节次。
    """.trimIndent()

    fun extractJsonArrayString(input: String): String {
        val start = input.indexOf('[')
        val end = input.lastIndexOf(']')
        if (start != -1 && end != -1 && end > start) return input.substring(start, end + 1)
        return input
    }

    val dayNames = listOf(
        stringResource(R.string.mon), stringResource(R.string.tue),
        stringResource(R.string.wed), stringResource(R.string.thu),
        stringResource(R.string.fri), stringResource(R.string.sat),
        stringResource(R.string.sun)
    )
    val weekRangeOptions = listOf(
        "all" to stringResource(R.string.all_weeks), "odd" to stringResource(R.string.odd_weeks),
        "even" to stringResource(R.string.even_weeks), "custom" to stringResource(R.string.custom_weeks)
    )

    val isDark = com.ty.gkschedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    Scaffold(contentWindowInsets = WindowInsets.systemBars, containerColor = scaffoldBg,
        topBar = {
            TopAppBar(title = { Text(if (isEditing) stringResource(R.string.edit_course) else stringResource(R.string.add_new_course)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg, scrolledContainerColor = scaffoldBg),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (isEditing) { IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) } } })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // AI Import Panel
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.large), shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showAiPanel = !showAiPanel }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary); Text("从AI中导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        Text(if (showAiPanel) "收起" else "展开", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    AnimatedVisibility(visible = showAiPanel) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("1. 复制提示词到 AI 软件。你可以直接复制包含 AI 回复全文的整条消息，系统会自动剔除杂质代码符号。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { clipboardManager.setPrimaryClip(ClipData.newPlainText("AI course import prompt", courseAiPrompt)); android.widget.Toast.makeText(context, "提示词已复制！", android.widget.Toast.LENGTH_SHORT).show() }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("复制 AI 解析提示词", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            Text("2. 在下方贴入包含 JSON 代码块的消息全文：", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = aiClipboardInput, onValueChange = { aiClipboardInput = it; aiErrorHint = "" }, placeholder = { Text("支持包含 Markdown 标识或聊天问候语的整条消息复合文本...", style = MaterialTheme.typography.bodyMedium) }, leadingIcon = { Icon(Icons.Default.DataObject, null) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = MaterialTheme.shapes.medium)
                            if (aiErrorHint.isNotEmpty()) Text(aiErrorHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            FilledTonalButton(onClick = {
                                if (aiClipboardInput.isBlank()) return@FilledTonalButton
                                try {
                                    val cleanedJson = extractJsonArrayString(aiClipboardInput.trim())
                                    val parsedList = JsonImportExport.importFromJson(cleanedJson)
                                    val mappedCourses = parsedList.map { data ->
                                        Course(id = 0, name = data.name, teacher = data.teacher, classroom = data.classroom, dayOfWeek = data.dayOfWeek.coerceIn(1, 7), startPeriod = data.startPeriod.coerceIn(1, periodsPerDay), periods = data.periods.coerceIn(1, periodsPerDay), colorIndex = data.colorIndex, weekRange = data.weekRange, remark = data.remark, isCustomTime = data.isCustomTime, customStartTime = data.customStartTime.ifBlank { "08:00" }, customEndTime = data.customEndTime.ifBlank { "09:00" }, isManuallyEdited = true, isHidden = false)
                                    }
                                    if (mappedCourses.isNotEmpty()) {
                                        batchCourses = mappedCourses; selectedTabIndex = 0
                                        val first = mappedCourses[0]
                                        name = first.name; teacher = first.teacher; classroom = first.classroom
                                        dayOfWeek = first.dayOfWeek; startPeriod = first.startPeriod; periods = first.periods; remark = first.remark
                                        if (first.weekRange != "all" && first.weekRange != "odd" && first.weekRange != "even") { weekRange = "custom"; customWeekRange = first.weekRange } else { weekRange = first.weekRange; customWeekRange = "" }
                                        isCustomTime = first.isCustomTime; customStartTime = first.customStartTime; customEndTime = first.customEndTime; isHidden = false
                                        aiErrorHint = ""; showAiPanel = false
                                        android.widget.Toast.makeText(context, "成功识别到 ${mappedCourses.size} 门课程，请通过多标签页进行切换核对！", android.widget.Toast.LENGTH_SHORT).show()
                                    } else { aiErrorHint = "未在 JSON 数组中发现有效的课程节点。" }
                                } catch (e: Exception) { aiErrorHint = "智能中转清洗失败，请确保贴入的文本内含有完整的 [ ... ] 数组闭环代码。" }
                            }, shape = MaterialTheme.shapes.medium, enabled = aiClipboardInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("智能清洗并解析注入", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            CourseHiddenSwitch(isHidden = isHidden, onHiddenChange = { isHidden = it })
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            // Multi-tab review rail
            if (batchCourses.size > 1) {
                PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 0.dp, containerColor = Color.Transparent, modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)) {
                    batchCourses.forEachIndexed { index, _ ->
                        Tab(selected = selectedTabIndex == index, onClick = {
                            val currentId = batchCourses.getOrNull(selectedTabIndex)?.id ?: course?.id ?: 0L
                            val currentState = Course(id = currentId, name = name.trim(), teacher = teacher.trim(), classroom = classroom.trim(), dayOfWeek = dayOfWeek, startPeriod = startPeriod, periods = periods, colorIndex = colorIndex, weekRange = if (weekRange == "custom") customWeekRange.ifEmpty { "all" } else weekRange, remark = remark.trim(), isCustomTime = isCustomTime, customStartTime = customStartTime, customEndTime = customEndTime, isManuallyEdited = true, isHidden = isHidden)
                            batchCourses = batchCourses.toMutableList().apply { this[selectedTabIndex] = currentState }
                            selectedTabIndex = index
                            val target = batchCourses[index]
                            name = target.name; teacher = target.teacher; classroom = target.classroom; dayOfWeek = target.dayOfWeek; startPeriod = target.startPeriod; periods = target.periods; remark = target.remark
                            if (target.weekRange != "all" && target.weekRange != "odd" && target.weekRange != "even") { weekRange = "custom"; customWeekRange = target.weekRange } else { weekRange = target.weekRange; customWeekRange = "" }
                            isCustomTime = target.isCustomTime; customStartTime = target.customStartTime; customEndTime = target.customEndTime
                        }, text = {
                            val target = batchCourses[index]
                            val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
                            val dayLabel = if (target.dayOfWeek in 1..7) dayNames[target.dayOfWeek - 1] else "${target.dayOfWeek}"
                            Text("周${dayLabel} ${target.startPeriod}-${target.endPeriod()}节", fontWeight = FontWeight.Bold)
                        })
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.course_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text(stringResource(R.string.course_teacher)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = classroom, onValueChange = { classroom = it }, label = { Text(stringResource(R.string.course_classroom)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text(stringResource(R.string.day_of_week), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { dayNames.forEachIndexed { index, dn -> FilterChip(selected = dayOfWeek == index + 1, onClick = { dayOfWeek = index + 1 }, label = { Text(dn, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f)) } }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.custom_time), style = MaterialTheme.typography.labelLarge); Switch(checked = isCustomTime, onCheckedChange = { isCustomTime = it }, thumbContent = if (isCustomTime) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) }
            if (isCustomTime) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = { showM3StartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("${stringResource(R.string.custom_start_time)}: $customStartTime") }; OutlinedButton(onClick = { showM3EndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("${stringResource(R.string.custom_end_time)}: $customEndTime") } } } else { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.start_period), style = MaterialTheme.typography.labelLarge); Spacer(modifier = Modifier.weight(1f)); Stepper(value = startPeriod, min = 1, max = periodsPerDay, onChange = { startPeriod = it }) }; Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelLarge); Spacer(modifier = Modifier.weight(1f)); Stepper(value = periods, min = 1, max = (periodsPerDay - startPeriod + 1).coerceAtLeast(1), onChange = { periods = it }) } }
            Text(stringResource(R.string.week_range), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { weekRangeOptions.forEach { (key, label) -> FilterChip(selected = weekRange == key || (key == "custom" && weekRange !in listOf("all", "odd", "even")), onClick = { weekRange = key }, label = { Text(label, style = MaterialTheme.typography.labelSmall) }) } }
            if (weekRange == "custom") { OutlinedTextField(value = customWeekRange, onValueChange = { customWeekRange = it }, label = { Text(stringResource(R.string.week_range_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text(stringResource(R.string.course_remark)) }, placeholder = { Text(stringResource(R.string.course_remark_hint)) }, modifier = Modifier.fillMaxWidth(), minLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val finalWeekRange = when (weekRange) { "all", "odd", "even" -> weekRange; else -> customWeekRange.ifEmpty { weekRange } }
                val currentId = batchCourses.getOrNull(selectedTabIndex)?.id ?: course?.id ?: 0L
                onSave(Course(id = currentId, name = name.trim(), teacher = teacher.trim(), classroom = classroom.trim(), dayOfWeek = dayOfWeek, startPeriod = startPeriod, periods = periods, colorIndex = colorIndex, weekRange = finalWeekRange, remark = remark.trim(), isCustomTime = isCustomTime, customStartTime = customStartTime, customEndTime = customEndTime, isManuallyEdited = true, isHidden = isHidden), hiddenScopeName)
            }, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { Text(if (batchCourses.size > 1) "保存当前标签课程 (${selectedTabIndex + 1}/${batchCourses.size})" else stringResource(R.string.save)) }
        }
    }
    if (showDeleteDialog && course != null) { AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(stringResource(R.string.confirm_delete)) }, text = { Text(stringResource(R.string.confirm_delete_msg)) }, confirmButton = { TextButton(onClick = { onDelete(course); showDeleteDialog = false }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showM3StartTimePicker) { val startParts = customStartTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = startParts.getOrNull(0)?.toIntOrNull() ?: 8, initialMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3StartTimePicker = false }, confirmButton = { TextButton(onClick = { customStartTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3StartTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3StartTimePicker = false }) { Text("取消") } }, title = { Text(stringResource(R.string.custom_start_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
    if (showM3EndTimePicker) { val endParts = customEndTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = endParts.getOrNull(0)?.toIntOrNull() ?: 9, initialMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3EndTimePicker = false }, confirmButton = { TextButton(onClick = { customEndTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3EndTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3EndTimePicker = false }) { Text("取消") } }, title = { Text(stringResource(R.string.custom_end_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
}

@Composable
private fun CourseHiddenSwitch(isHidden: Boolean, onHiddenChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("隐藏该课程", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "勾选后该课程的所有排课实例都不再显示在今日和课表主页中",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isHidden,
            onCheckedChange = onHiddenChange,
            thumbContent = if (isHidden) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            }
        )
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
