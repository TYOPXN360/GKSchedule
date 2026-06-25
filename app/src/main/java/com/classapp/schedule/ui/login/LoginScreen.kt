package com.classapp.schedule.ui.login

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.classapp.schedule.LoginState
import com.classapp.schedule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginState: LoginState?,
    captchaImageBase64: String?,
    hasSavedCredentials: Boolean,
    onRefreshCaptcha: () -> Unit,
    onLogin: (studentId: String, password: String, captcha: String) -> Unit,
    onQuickRelogin: (captcha: String) -> Unit,
    onWebViewLogin: () -> Unit,
    onBack: () -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading = loginState is LoginState.Loading
    val errorMessage = (loginState as? LoginState.Error)?.message
    val successMessage = (loginState as? LoginState.Success)?.let { "登录成功: ${it.name} (${it.studentId})" }
    val importResult = (loginState as? LoginState.ImportResult)?.let { "导入完成: ${it.count} 门课程" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.login_school_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.login_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
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

                if (captchaImageBase64 != null && captchaImageBase64.isNotEmpty()) {
                    val bitmap = remember(captchaImageBase64) {
                        try {
                            val bytes = Base64.decode(captchaImageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                IconButton(onClick = onRefreshCaptcha, enabled = !isLoading) {
                    Icon(Icons.Default.Refresh, "Refresh captcha")
                }
            }

            // Login button
            Button(
                onClick = { onLogin(studentId, password, captcha) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && studentId.isNotBlank() && password.isNotBlank() && captcha.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.login_button))
            }

            // Quick re-login (when credentials saved)
            if (hasSavedCredentials && loginState is LoginState.Error) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.quick_relogin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    Button(
                        onClick = { onQuickRelogin(captcha) },
                        enabled = !isLoading && captcha.isNotBlank()
                    ) {
                        Text(stringResource(R.string.quick_relogin))
                    }
                }
            }

            // WebView login option
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("❌ 错误", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(errorMessage, color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("✅ 成功", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(successMessage, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (importResult != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📚 导入", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
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
