package com.classapp.schedule.ui.exam

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import com.classapp.schedule.api.ExamInfo
import com.classapp.schedule.util.CourseColors
import com.classapp.schedule.ui.theme.LocalAppIsDark
import com.classapp.schedule.ui.theme.Md3Card
import com.classapp.schedule.ui.theme.Md3CardVariant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    exams: List<ExamInfo>,
    isLoading: Boolean,
    semesterStart: LocalDate,
    examYear: String,
    examSemester: String,
    colorGroupMode: Int = 2,
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
    val scaffoldBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer

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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Year + Semester selector + fetch button
            Md3Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                variant = Md3CardVariant.Elevated
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var yearExpanded by remember { mutableStateOf(false) }
                        val currentYear = LocalDate.now().year
                        val years = ((currentYear - 3)..currentYear).map { "$it-${it + 1}" }.reversed()
                        OutlinedCard(onClick = { yearExpanded = true }, modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(examYear.ifEmpty { "学年" }, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                            }
                        }
                        Box {
                            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                                years.forEach { y -> DropdownMenuItem(text = { Text(y) }, onClick = { onYearChange(y); yearExpanded = false }) }
                            }
                        }

                        var semExpanded by remember { mutableStateOf(false) }
                        OutlinedCard(onClick = { semExpanded = true }, modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (examSemester == "1") "第一学期" else "第二学期", style = MaterialTheme.typography.bodyMedium)
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

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Text(text = "请在校园网下获取", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Button(onClick = onRefresh, enabled = !isLoading && examYear.isNotEmpty()) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (exams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.School, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "暂无考试安排", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            } else {
                val sortedExams = exams.sortedBy { it.getExamDate() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedExams) { exam ->
                        val examColor = CourseColors.getColorSync(colorGroupMode, exam.kcmc, exam.cdmc, isDark = isDark)
                        ExamCard(exam = exam, examColor = examColor)
                    }
                }
            }
        }
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
private fun ExamCard(exam: ExamInfo, examColor: com.classapp.schedule.util.CourseColors.CourseColorPair) {
    val examDate = try { LocalDate.parse(exam.getExamDate()) } catch (_: Exception) { null }
    val now = LocalDate.now()
    val isPast = examDate?.isBefore(now) == true
    val alpha = if (isPast) 0.5f else 1f
    val daysLeft = if (examDate != null && !isPast) ChronoUnit.DAYS.between(now, examDate) else -1L

    Md3Card(modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(48.dp).clip(RoundedCornerShape(2.dp))
                .background(if (isPast) examColor.content.copy(alpha = 0.2f) else examColor.content))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exam.kcmc, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isPast) { Spacer(modifier = Modifier.width(6.dp)); Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exam.kssj.isNotEmpty()) Text(exam.kssj, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    if (exam.cdmc.isNotEmpty()) { Spacer(modifier = Modifier.width(8.dp)); Text(exam.cdmc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)) }
                }
                if (exam.ksfs.isNotEmpty()) Text(exam.ksfs, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f))
            }
            if (!isPast && daysLeft >= 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = CircleShape, color = if (daysLeft == 0L) MaterialTheme.colorScheme.errorContainer else examColor.container,
                    contentColor = if (daysLeft == 0L) MaterialTheme.colorScheme.onErrorContainer else examColor.content) {
                    Text(if (daysLeft == 0L) "今天" else "剩 ${daysLeft} 天", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
