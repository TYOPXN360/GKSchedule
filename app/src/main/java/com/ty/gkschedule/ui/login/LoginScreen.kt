package com.ty.gkschedule.ui.login

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ty.gkschedule.CaptchaState
import com.ty.gkschedule.LoginState
import com.ty.gkschedule.R
import com.ty.gkschedule.ui.theme.CaptchaImageWithStatus
import com.ty.gkschedule.ui.theme.CaptchaStatusLine
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginState: LoginState?,
    captchaImageBase64: String?,
    captchaState: CaptchaState,
    captchaText: String?,
    hasSavedCredentials: Boolean,
    onRefreshCaptcha: () -> Unit,
    onLogin: (studentId: String, password: String, captcha: String) -> Unit,
    onQuickRelogin: (captcha: String) -> Unit,
    onWebViewLogin: () -> Unit,
    onBack: () -> Unit,
    blurEnabled: Boolean = true
) {
    // ponytail: miuix源层
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
    var studentId by remember { mutableStateOf("") }
    val passwordState = rememberTextFieldState()
    var captcha by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ponytail: 端侧 OCR 识别出来后自动填入；用户手改过也不锁，下一张图来了再覆盖
    LaunchedEffect(captchaText) {
        if (!captchaText.isNullOrEmpty()) captcha = captchaText
    }

    val isLoading = loginState is LoginState.Loading
    val errorMessage = (loginState as? LoginState.Error)?.message
    val context = androidx.compose.ui.platform.LocalContext.current
    val successMessage = (loginState as? LoginState.Success)?.let { context.getString(R.string.login_success_fmt, it.name, it.studentId) }
    val importResult = (loginState as? LoginState.ImportResult)?.let { context.getString(R.string.import_done_fmt, it.count) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            com.ty.gkschedule.ui.theme.BlurTopBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                backdrop = backdrop,
                blurEnabled = blurEnabled
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.surface)
                                // ponytail: 避让走滚动内padding，源纹理全屏录(含顶栏身后)
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.login_school_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.login_school_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Student ID
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text(stringResource(R.string.login_student_id)) },
                modifier = Modifier.fillMaxWidth().semantics {
                    this[androidx.compose.ui.semantics.SemanticsProperties.ContentType] = androidx.compose.ui.autofill.ContentType.Username
                },
                singleLine = true,
                enabled = !isLoading
            )

            // Password
            OutlinedSecureTextField(
                state = passwordState,
                label = { Text(stringResource(R.string.login_password)) },
                modifier = Modifier.fillMaxWidth().semantics {
                    this[androidx.compose.ui.semantics.SemanticsProperties.ContentType] = androidx.compose.ui.autofill.ContentType.Password
                },
                enabled = !isLoading,
                textObfuscationMode = if (passwordVisible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.login_password_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            )

            // Captcha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = captcha,
                    onValueChange = { captcha = it },
                    label = { Text(stringResource(R.string.login_captcha)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isLoading
                )

                CaptchaImageWithStatus(
                    captchaImageBase64 = captchaImageBase64,
                    captchaState = captchaState,
                    onRefreshCaptcha = onRefreshCaptcha
                )
                IconButton(onClick = onRefreshCaptcha, enabled = !isLoading && captchaState !is CaptchaState.Loading) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.captcha_refresh))
                }
            }

            // 验证码状态副标题：加载中 / 加载失败（失败时给出原因）
            CaptchaStatusLine(captchaState = captchaState)

            // Quick re-login with saved encrypted credentials.
            // Removed from login page - quick login only available from AboutScreen after token expiry.

            // Login button
            Button(
                onClick = { onLogin(studentId, passwordState.text.toString(), captcha) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && studentId.isNotBlank() && passwordState.text.isNotBlank() && captcha.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.login_button))
            }

            // WebView login option
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Button(
                onClick = onWebViewLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.webview_login))
            }

            // === Server response display ===
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.step_error),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(errorMessage, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.step_success),
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(successMessage, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (importResult != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.step_import),
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Text(importResult, color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Tips
            Text(
                text = stringResource(R.string.login_tips),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
