package com.ty.gdust_schedule.ui.exam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ty.gdust_schedule.data.Course
import com.ty.gdust_schedule.data.ExamEntity
import com.ty.gdust_schedule.data.ScheduleResolver
import com.ty.gdust_schedule.data.ScheduleItem
import com.ty.gdust_schedule.util.CourseColors
import com.ty.gdust_schedule.ui.theme.LocalAppIsDark
import com.ty.gdust_schedule.ui.theme.Md3Card
import com.ty.gdust_schedule.ui.theme.Md3CardVariant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    exams: List<ExamEntity>,
    colorCourses: List<Course> = emptyList(),
    customExams: List<Course> = emptyList(),
    isLoading: Boolean,
    semesterStart: LocalDate,
    examYear: String,
    examSemester: String,
    colorEngine: Int = 0,
    colorGroupMode: Int = 2,
    examLookaheadWeeks: Int = 1,
    showExamSchedule: Boolean = false,
    onShowExamScheduleChange: (Boolean) -> Unit = {},
    onExamLookaheadWeeksChange: (Int) -> Unit = {},
    onAddExam: () -> Unit = {},
    onEditExam: (ExamEntity) -> Unit = {},
    getStartTime: (Int) -> String = { "" },
    getEndTime: (Int) -> String = { "" },
    currentWeek: Int = 1,
    diffColorPerWeek: Boolean = false,
    showReloginDialog: Boolean = false,
    captchaImageBase64: String? = null,
    onYearChange: (String) -> Unit,
    onSemesterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismissRelogin: () -> Unit = {},
    onRefreshCaptcha: () -> Unit = {},
    onQuickRelogin: (String) -> Unit = {},
    onBack: () -> Unit
) {
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

    val isDark = LocalAppIsDark.current
    val themeHue = CourseColors.currentThemeHue()
    val colorScheduleItems = remember(colorCourses, exams, customExams, semesterStart, getStartTime, getEndTime) {
        ScheduleResolver.buildItems(colorCourses, exams, true, semesterStart, getStartTime, getEndTime) +
            customExams.map { ScheduleItem.CourseItem(it) }
    }
    val colorAssignments = remember(colorScheduleItems, colorGroupMode, diffColorPerWeek) {
        ScheduleResolver.buildColorAssignments(colorScheduleItems, colorGroupMode, diffColorPerWeek)
    }
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    var detailItem by remember { mutableStateOf<ScheduleItem.ExamItem?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = scaffoldBg,
        topBar = {
            TopAppBar(
                title = { Text("考试安排", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldBg, scrolledContainerColor = scaffoldBg),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExam,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加考试")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter card
            Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var yearExpanded by remember { mutableStateOf(false) }
                        val currentYear = LocalDate.now().year
                        val years = ((currentYear - 3)..currentYear).map { "$it-${it + 1}" }.reversed()
                        OutlinedCard(onClick = { yearExpanded = true }, modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(examYear.ifEmpty { "学年" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box {
                            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                                years.forEach { y -> DropdownMenuItem(text = { Text(y) }, onClick = { onYearChange(y); yearExpanded = false }) }
                            }
                        }

                        var semExpanded by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { semExpanded = true }, modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (examSemester == "1") "第一学期" else "第二学期", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Box {
                            DropdownMenu(expanded = semExpanded, onDismissRequest = { semExpanded = false }) {
                                DropdownMenuItem(text = { Text("第一学期") }, onClick = { onSemesterChange("1"); semExpanded = false })
                                DropdownMenuItem(text = { Text("第二学期") }, onClick = { onSemesterChange("2"); semExpanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(text = "请在校园网下获取", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Button(
                            onClick = onRefresh,
                            enabled = !isLoading && examYear.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("获取数据", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Show exam schedule in timetable switch
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("以课表形式显示考试安排", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("开启后考试日程将作为方块平铺在大盘网格中", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                        Switch(
                            checked = showExamSchedule,
                            onCheckedChange = onShowExamScheduleChange,
                            thumbContent = if (showExamSchedule) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null
                        )
                    }

                    // Exam preview weeks stepper
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("考试预览周数", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("今日页提前多少周显示考试", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = { if (examLookaheadWeeks > 1) onExamLookaheadWeeksChange(examLookaheadWeeks - 1) }, enabled = examLookaheadWeeks > 1) {
                                Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("$examLookaheadWeeks 周", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { if (examLookaheadWeeks < 20) onExamLookaheadWeeksChange(examLookaheadWeeks + 1) }, enabled = examLookaheadWeeks < 20) {
                                Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Exam list
            if (isLoading && exams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (exams.isEmpty() && customExams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "暂无考试安排", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            } else {
                val sortedExams = exams.sortedBy { it.examDate }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedExams) { exam ->
                        val examWeek = ScheduleResolver.examWeek(exam, semesterStart, currentWeek)
                        val colorAssignment = colorAssignments.get(
                            courseName = exam.courseName,
                            classroom = exam.classroom,
                            week = examWeek,
                            colorGroupMode = colorGroupMode,
                            diffColorPerWeek = diffColorPerWeek
                        )
                        val examColor = CourseColors.getColorSync(
                            engine = colorEngine,
                            groupMode = colorGroupMode,
                            courseName = exam.courseName,
                            classroom = exam.classroom,
                            classroomIndex = colorAssignment.classroomColorIndex,
                            colorIndex = colorAssignment.colorIndex,
                            week = examWeek,
                            diffColorPerWeek = diffColorPerWeek,
                            isDark = isDark,
                            themeHue = themeHue
                        )
                        ExamCard(
                            exam = exam,
                            examColor = examColor,
                            onClick = { detailItem = ScheduleResolver.examItem(exam, semesterStart, getStartTime, getEndTime) }
                        )
                    }

                    // Custom exams from local DB
                    items(customExams) { course ->
                        val weekOffset = (course.weekRange.toIntOrNull() ?: 1) - 1
                        val examDate = try { semesterStart.plusWeeks(weekOffset.toLong()).plusDays((course.dayOfWeek - 1).toLong()) } catch (_: Exception) { LocalDate.now() }
                        val now = LocalDate.now()
                        val isPast = examDate.isBefore(now)
                        val daysLeft = if (!isPast) ChronoUnit.DAYS.between(now, examDate) else -1L
                        val colorAssignment = colorAssignments.get(
                            courseName = course.name,
                            classroom = course.classroom,
                            week = weekOffset + 1,
                            colorGroupMode = colorGroupMode,
                            diffColorPerWeek = diffColorPerWeek
                        )
                        val examColor = CourseColors.getColorSync(
                            engine = colorEngine,
                            groupMode = colorGroupMode,
                            courseName = course.name,
                            classroom = course.classroom,
                            classroomIndex = colorAssignment.classroomColorIndex,
                            colorIndex = colorAssignment.colorIndex,
                            week = weekOffset + 1,
                            diffColorPerWeek = diffColorPerWeek,
                            isDark = isDark,
                            themeHue = themeHue
                        )
                        Md3Card(onClick = { /* Custom exams don't use the shared detail sheet */ }, modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(if (isPast) examColor.container.copy(alpha = 0.2f) else examColor.container))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isPast) 0.5f else 1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(course.classroom, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!isPast && daysLeft >= 0) { Spacer(modifier = Modifier.width(8.dp)); Surface(shape = CircleShape, color = if (daysLeft == 0L) MaterialTheme.colorScheme.errorContainer else examColor.container, contentColor = if (daysLeft == 0L) MaterialTheme.colorScheme.onErrorContainer else examColor.content) { Text(if (daysLeft == 0L) "今天" else "剩 ${daysLeft} 天", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) } }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail sheet
    detailItem?.let { item ->
        val detailWeek = item.weekRange.toIntOrNull() ?: currentWeek
        val colorAssignment = colorAssignments.get(
            item = item,
            week = detailWeek,
            colorGroupMode = colorGroupMode,
            diffColorPerWeek = diffColorPerWeek
        )
        com.ty.gdust_schedule.ui.weekly.ScheduleItemDetailSheet(
            item = item, getStartTime = getStartTime, getEndTime = getEndTime,
            onDismiss = { detailItem = null },
            onEdit = {
                detailItem = null
                onEditExam(item.exam)
            },
            colorEngine = colorEngine,
            colorGroupMode = colorGroupMode,
            colorIndex = colorAssignment.colorIndex,
            classroomColorIndex = colorAssignment.classroomColorIndex,
            currentWeek = detailWeek, diffColorPerWeek = diffColorPerWeek
        )
    }

    // Re-login dialog
    if (showReloginDialog) {
        var captcha by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = onDismissRelogin,
            title = { Text("教务系统登录过期") },
            text = {
                Column {
                    Text("请输入验证码重新登录", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!captchaImageBase64.isNullOrEmpty()) {
                        val bitmap = remember(captchaImageBase64) {
                            try {
                                val bytes = android.util.Base64.decode(captchaImageBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Card(modifier = Modifier.size(width = 120.dp, height = 56.dp).clickable { onRefreshCaptcha() }, shape = MaterialTheme.shapes.extraSmall) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Captcha", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = captcha, onValueChange = { captcha = it }, label = { Text("验证码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { if (captcha.isNotBlank()) { onQuickRelogin(captcha); onDismissRelogin() } }, enabled = captcha.isNotBlank()) { Text("登录") } },
            dismissButton = { TextButton(onClick = onDismissRelogin) { Text("取消") } }
        )
    }
}

@Composable
private fun ExamCard(exam: ExamEntity, examColor: CourseColors.CourseColorPair, onClick: () -> Unit) {
    val examDate = try { LocalDate.parse(exam.examDate) } catch (_: Exception) { null }
    val now = LocalDate.now()
    val today = examDate != null && examDate == now

    // Parse exam time range for time-aware state
    val timeParts = exam.examTimeRange.split("-")
    val startMins = if (timeParts.size == 2) { val p = timeParts[0].trim().split(":"); p.getOrNull(0)?.toIntOrNull()?.let { h -> h * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) } } else null
    val endMins = if (timeParts.size == 2) { val p = timeParts[1].trim().split(":"); p.getOrNull(0)?.toIntOrNull()?.let { h -> h * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0) } } else null
    val nowMins = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute

    val isPast = when {
        examDate?.isBefore(now) == true -> true
        today && endMins != null && nowMins > endMins -> true
        else -> false
    }
    val isNow = today && startMins != null && endMins != null && nowMins in startMins..endMins
    val alpha = if (isPast) 0.5f else 1f
    val daysLeft = if (examDate != null && !isPast && !isNow) ChronoUnit.DAYS.between(now, examDate) else -1L
    val progress = if (isPast) {
        1f
    } else if (isNow) {
        val start = startMins ?: 0
        val end = endMins ?: start
        val duration = end - start
        if (duration > 0) ((nowMins - start).toFloat() / duration).coerceIn(0f, 1f) else 0f
    } else {
        0f
    }

    Md3Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (isPast) examColor.container.copy(alpha = 0.2f) else examColor.container))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(exam.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isPast) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text("已结束", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (isNow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = CircleShape, color = examColor.container) {
                                Text("进行中", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = examColor.content)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (exam.examDate.isNotEmpty()) Text(exam.examDate + " " + exam.examTimeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                        if (exam.classroom.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); Text(exam.classroom, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
                    }
                    if (exam.examMethod.isNotEmpty()) Text(exam.examMethod, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f))
                }
                if (!isPast && !isNow && daysLeft >= 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = CircleShape, color = if (daysLeft == 0L) MaterialTheme.colorScheme.errorContainer else examColor.container,
                        contentColor = if (daysLeft == 0L) MaterialTheme.colorScheme.onErrorContainer else examColor.content) {
                        Text(if (daysLeft == 0L) "今天" else "剩 ${daysLeft} 天", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            // Progress bar for in-progress or recently finished exams
            if (isNow || (isPast && today)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    color = examColor.container,
                    trackColor = examColor.container.copy(alpha = 0.15f)
                )
            }
        }
    }
}
