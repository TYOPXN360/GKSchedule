package com.classapp.schedule.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classapp.schedule.LoginState
import com.classapp.schedule.R
import com.classapp.schedule.ui.theme.Md3Card
import com.classapp.schedule.ui.theme.Md3CardVariant
import com.classapp.schedule.ui.theme.MonetIconBadge

@Composable
fun AboutScreen(
    loginState: LoginState?,
    savedStudentId: String,
    savedRealName: String,
    savedDeptName: String,
    semesterStart: java.time.LocalDate,
    totalWeeks: Int,
    periodsPerDay: Int,
    captchaImageBase64: String? = null,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onQuickRelogin: (String) -> Unit = {},
    onRefreshCaptcha: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenExam: () -> Unit = {}
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReloginDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Account card
        val isLoggedIn = loginState is LoginState.Success || loginState is LoginState.ImportResult || loginState is LoginState.TokenExpired
        val isTokenExpired = loginState is LoginState.TokenExpired

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLoggedIn) {
                    if (isTokenExpired) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer
                } else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (isLoggedIn) {
                // Logged in — show user info
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar circle
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = savedRealName.take(1),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = savedRealName.ifEmpty { "已登录" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (savedStudentId.isNotEmpty()) {
                            Text(
                                text = savedStudentId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        if (savedDeptName.isNotEmpty()) {
                            Text(
                                text = savedDeptName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (isTokenExpired) {
                            Text(
                                text = "登录已过期",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onRefreshCaptcha()
                                    showReloginDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("重新登录")
                            }
                        }
                    }
                    // Logout icon at top right
                    Surface(
                        onClick = onLogout,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = stringResource(R.string.logout),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Not logged in
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp).clickable(onClick = onLogin),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.about_login_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.login_button))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Semester info (read-only)
        val badgeColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
        Md3Card(modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated, shape = RoundedCornerShape(12.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.semester_start)) },
                    supportingContent = { Text(semesterStart.toString()) },
                    leadingContent = { MonetIconBadge(icon = Icons.Default.CalendarMonth, contentDescription = null, seedColor = badgeColors[0]) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.total_weeks)) },
                    supportingContent = { Text("$totalWeeks") },
                    leadingContent = { MonetIconBadge(icon = Icons.Default.DateRange, contentDescription = null, seedColor = badgeColors[1]) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.periods_per_day)) },
                    supportingContent = { Text("$periodsPerDay") },
                    leadingContent = { MonetIconBadge(icon = Icons.Default.AccessTime, contentDescription = null, seedColor = badgeColors[2]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Exam schedule
        Md3Card(modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated, shape = RoundedCornerShape(12.dp)) {
            ListItem(
                headlineContent = { Text("考试安排") },
                supportingContent = { Text("查看考试时间和考场") },
                leadingContent = { MonetIconBadge(icon = Icons.Default.School, contentDescription = null, seedColor = badgeColors[3]) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable(onClick = onOpenExam)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Settings
        Md3Card(modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated, shape = RoundedCornerShape(12.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_title)) },
                supportingContent = { Text(stringResource(R.string.about_settings_desc)) },
                leadingContent = { MonetIconBadge(icon = Icons.Default.Settings, contentDescription = null, seedColor = badgeColors[4]) },
                modifier = Modifier.clickable(onClick = onOpenSettings)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // About
        Md3Card(modifier = Modifier.fillMaxWidth(), variant = Md3CardVariant.Elevated, shape = RoundedCornerShape(12.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_title)) },
                supportingContent = { Text(stringResource(R.string.about_school)) },
                leadingContent = { MonetIconBadge(icon = Icons.AutoMirrored.Filled.Article, contentDescription = null, seedColor = badgeColors[5]) },
                modifier = Modifier.clickable { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.about_copyright),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${stringResource(R.string.app_name)} v1.0.0")
                    Text(stringResource(R.string.about_desc))

                    HorizontalDivider()

                    Text(stringResource(R.string.credits_title), style = MaterialTheme.typography.titleSmall)

                    // AI
                    CreditItem(stringResource(R.string.credits_ai), "XiaoMi-MiMo-v2.5-pro")

                    // Component library
                    CreditItem(stringResource(R.string.credits_component), "material-components-android")

                    // Language
                    CreditItem(stringResource(R.string.credits_language), "Jetpack Compose (Kotlin)")

                    // Design
                    CreditItem(stringResource(R.string.credits_design), "Material Design 3 Expressive")

                    HorizontalDivider()

                    Text(stringResource(R.string.credits_projects), style = MaterialTheme.typography.titleSmall)
                    Text("• SchedU (github.com/gnahz77/SchedU)", style = MaterialTheme.typography.bodySmall)
                    Text("• 拾光课程表 (github.com/XingHeYuZhuan/shiguangschedule)", style = MaterialTheme.typography.bodySmall)
                    Text("• TimeFlow (github.com/Lyxot/TimeFlow)", style = MaterialTheme.typography.bodySmall)

                    HorizontalDivider()

                    Text(stringResource(R.string.about_school))
                    Text("portal.gdust.edu.cn")
                    Text(stringResource(R.string.about_copyright))
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    // Re-login dialog (captcha only)
    if (showReloginDialog) {
        var captcha by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReloginDialog = false },
            title = { Text("重新登录") },
            text = {
                Column {
                    Text("请输入验证码", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (captchaImageBase64 != null && captchaImageBase64.isNotEmpty()) {
                        val bitmap = remember(captchaImageBase64) {
                            try {
                                val bytes = android.util.Base64.decode(captchaImageBase64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Card(
                                modifier = Modifier.size(width = 120.dp, height = 56.dp)
                                    .clickable { onRefreshCaptcha() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Captcha",
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = captcha,
                        onValueChange = { captcha = it },
                        label = { Text(stringResource(R.string.login_captcha)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (captcha.isNotBlank()) {
                            onQuickRelogin(captcha)
                            showReloginDialog = false
                        }
                    },
                    enabled = captcha.isNotBlank()
                ) { Text("登录") }
            },
            dismissButton = {
                TextButton(onClick = { showReloginDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun CreditItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
