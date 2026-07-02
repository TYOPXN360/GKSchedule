package com.classapp.schedule.ui.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classapp.schedule.R
import com.classapp.schedule.data.Course
import com.classapp.schedule.util.CourseColors
import com.classapp.schedule.util.JsonImportExport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    course: Course?,
    periodsPerDay: Int,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onBack: () -> Unit
) {
    val isEditing = course != null
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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

    var showAiPanel by remember { mutableStateOf(false) }
    var aiClipboardInput by remember { mutableStateOf("") }
    var aiErrorHint by remember { mutableStateOf("") }

    val courseAiPrompt = """
        你是一个课程表数据结构化清洗专家。请将用户接下来发送的任意格式的课程安排文本（或图片识别出来的杂乱文字），提取并重整为以下标准 JSON 对象（由于只需填入当前表单，请只返回一个包含单个对象的纯 JSON 数组，严禁包含任何 Markdown ``` 块包裹包裹标识）：
        [
          {
            "name": "高等数学",
            "teacher": "张教授",
            "classroom": "第一教学楼302",
            "dayOfWeek": 1, 
            "startPeriod": 3,
            "periods": 2,
            "weekRange": "1-16",
            "remark": "带好教材"
          }
        ]
        注意：dayOfWeek 必须是数字 (1=周一, 7=周日)；startPeriod 为开始节次，periods 为持续节次。
    """.trimIndent()

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

    val isDark = com.classapp.schedule.ui.theme.LocalAppIsDark.current
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars, containerColor = scaffoldBg,
        topBar = {
            TopAppBar(title = { Text(if (isEditing) stringResource(R.string.edit_course) else stringResource(R.string.add_new_course)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg, scrolledContainerColor = scaffoldBg),
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (isEditing) { IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) } } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // AI Import Panel — MD3E compliant
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { showAiPanel = !showAiPanel }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Text("从AI中导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(if (showAiPanel) "收起" else "展开", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    AnimatedVisibility(visible = showAiPanel) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("1. 复制下方专用提示词，贴入任意 AI 软件中，并附带你的课表文字或截图。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { clipboardManager.setText(AnnotatedString(courseAiPrompt)); android.widget.Toast.makeText(context, "提示词已复制！", android.widget.Toast.LENGTH_SHORT).show() }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("复制 AI 解析提示词", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                            Text("2. 将 AI 结构化返回的纯 JSON 复制粘贴到下方文本框中。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = aiClipboardInput, onValueChange = { aiClipboardInput = it; aiErrorHint = "" }, placeholder = { Text("[{\"name\": \"...\"}]", style = MaterialTheme.typography.bodyMedium) }, leadingIcon = { Icon(Icons.Default.DataObject, null) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4, shape = MaterialTheme.shapes.medium)
                            if (aiErrorHint.isNotEmpty()) Text(aiErrorHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            FilledTonalButton(onClick = {
                                if (aiClipboardInput.isBlank()) return@FilledTonalButton
                                try {
                                    val parsedList = JsonImportExport.importFromJson(aiClipboardInput.trim())
                                    val extracted = parsedList.firstOrNull()
                                    if (extracted != null) { name = extracted.name; teacher = extracted.teacher; classroom = extracted.classroom; dayOfWeek = extracted.dayOfWeek.coerceIn(1, 7); startPeriod = extracted.startPeriod.coerceIn(1, periodsPerDay); periods = extracted.periods.coerceIn(1, periodsPerDay); remark = extracted.remark; if (extracted.weekRange != "all" && extracted.weekRange != "odd" && extracted.weekRange != "even") { weekRange = "custom"; customWeekRange = extracted.weekRange } else { weekRange = extracted.weekRange; customWeekRange = "" }; aiErrorHint = ""; showAiPanel = false; android.widget.Toast.makeText(context, "导入成功，请核对表单！", android.widget.Toast.LENGTH_SHORT).show() } else { aiErrorHint = "解析成功，但未匹配到课程节点。" }
                                } catch (e: Exception) { aiErrorHint = "JSON 语法错误，请确保复制了完整的代码块。" }
                            }, shape = MaterialTheme.shapes.medium, enabled = aiClipboardInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("解析并注入课程表单", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // Form fields
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.course_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text(stringResource(R.string.course_teacher)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = classroom, onValueChange = { classroom = it }, label = { Text(stringResource(R.string.course_classroom)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text(stringResource(R.string.day_of_week), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { dayNames.forEachIndexed { index, dn -> FilterChip(selected = dayOfWeek == index + 1, onClick = { dayOfWeek = index + 1 }, label = { Text(dn, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f)) } }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.custom_time), style = MaterialTheme.typography.labelLarge); Switch(checked = isCustomTime, onCheckedChange = { isCustomTime = it }, thumbContent = if (isCustomTime) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) }
            if (isCustomTime) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = { showM3StartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("${stringResource(R.string.custom_start_time)}: $customStartTime") }; OutlinedButton(onClick = { showM3EndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("${stringResource(R.string.custom_end_time)}: $customEndTime") } } } else { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.start_period), style = MaterialTheme.typography.labelLarge); Spacer(modifier = Modifier.weight(1f)); Stepper(value = startPeriod, min = 1, max = periodsPerDay, onChange = { startPeriod = it }) }; Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.duration), style = MaterialTheme.typography.labelLarge); Spacer(modifier = Modifier.weight(1f)); Stepper(value = periods, min = 1, max = (periodsPerDay - startPeriod + 1).coerceAtLeast(1), onChange = { periods = it }) } }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(modifier = Modifier.weight(1f)) { Text("在课表中隐藏", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold); Text("勾选后此节课将不再显示在今日和课表主页中", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = isHidden, onCheckedChange = { isHidden = it }, thumbContent = if (isHidden) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) } } else null) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            Text(stringResource(R.string.week_range), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { weekRangeOptions.forEach { (key, label) -> FilterChip(selected = weekRange == key || (key == "custom" && weekRange !in listOf("all", "odd", "even")), onClick = { weekRange = key }, label = { Text(label, style = MaterialTheme.typography.labelSmall) }) } }
            if (weekRange == "custom") { OutlinedTextField(value = customWeekRange, onValueChange = { customWeekRange = it }, label = { Text(stringResource(R.string.week_range_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text(stringResource(R.string.course_remark)) }, placeholder = { Text(stringResource(R.string.course_remark_hint)) }, modifier = Modifier.fillMaxWidth(), minLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { val finalWeekRange = when (weekRange) { "all", "odd", "even" -> weekRange; else -> customWeekRange.ifEmpty { weekRange } }; onSave(Course(id = course?.id ?: 0, name = name.trim(), teacher = teacher.trim(), classroom = classroom.trim(), dayOfWeek = dayOfWeek, startPeriod = startPeriod, periods = periods, colorIndex = colorIndex, weekRange = finalWeekRange, remark = remark.trim(), isCustomTime = isCustomTime, customStartTime = customStartTime, customEndTime = customEndTime, isManuallyEdited = true, isHidden = isHidden)) }, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        }
    }
    if (showDeleteDialog && course != null) { AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text(stringResource(R.string.confirm_delete)) }, text = { Text(stringResource(R.string.confirm_delete_msg)) }, confirmButton = { TextButton(onClick = { onDelete(course); showDeleteDialog = false }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }) }
    if (showM3StartTimePicker) { val startParts = customStartTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = startParts.getOrNull(0)?.toIntOrNull() ?: 8, initialMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3StartTimePicker = false }, confirmButton = { TextButton(onClick = { customStartTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3StartTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3StartTimePicker = false }) { Text("取消") } }, title = { Text(stringResource(R.string.custom_start_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
    if (showM3EndTimePicker) { val endParts = customEndTime.split(":"); val timePickerState = rememberTimePickerState(initialHour = endParts.getOrNull(0)?.toIntOrNull() ?: 9, initialMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true); AlertDialog(onDismissRequest = { showM3EndTimePicker = false }, confirmButton = { TextButton(onClick = { customEndTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute); showM3EndTimePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showM3EndTimePicker = false }) { Text("取消") } }, title = { Text(stringResource(R.string.custom_end_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }, text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) } }) }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
