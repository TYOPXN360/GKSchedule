package com.ty.gkschedule.ui.login

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.LoginState
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.ui.theme.GKScheduleTheme

// ponytail: 登录独立Activity，返回走系统级预测动画；与Exam/CourseEdit同模式
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawableResource(android.R.color.transparent)
        val startQr = intent.getBooleanExtra("startQr", false)

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")

            GKScheduleTheme(darkTheme = darkMode) {
                val view = LocalView.current
                val isDark = when (darkMode) {
                    "dark" -> true
                    "light" -> false
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
                LaunchedEffect(isDark) {
                    val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
                    androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                    androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
                }
                val pageBg = if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Surface(modifier = Modifier.fillMaxSize(), color = pageBg) {
                    val loginState by vm.loginState.collectAsState()
                    val captchaImage by vm.captchaImage.collectAsState()
                    val hasSavedCredentials by vm.hasSavedCredentials.collectAsState(initial = false)
                    val blurEffect by vm.blurEffect.collectAsState(initial = false)
                    var showQr by remember(startQr) { mutableStateOf(startQr) }

                    LaunchedEffect(Unit) { vm.clearLoginError(); if (captchaImage == null) vm.refreshCaptcha() }
                    // ponytail: 成功回我的页——独立Activity直接finish，由About页状态刷新
                    LaunchedEffect(loginState) {
                        if (loginState is LoginState.Success || loginState is LoginState.ImportResult) {
                            kotlinx.coroutines.delay(600); finish()
                        }
                    }
                    if (showQr) {
                        WebViewLoginScreen(loginState = loginState, api = vm.api, onLoginSuccess = { loginCode -> vm.webViewLogin(loginCode) }, onBack = { showQr = false }, blurEnabled = blurEffect)
                        // ponytail: 扫码成功直接回我的页，跳过中间账号密码页
                        LaunchedEffect(loginState) {
                            if (loginState is LoginState.Success || loginState is LoginState.ImportResult) {
                                kotlinx.coroutines.delay(1200); finish()
                            }
                        }
                    } else {
                        LoginScreen(loginState = loginState, captchaImageBase64 = captchaImage, hasSavedCredentials = hasSavedCredentials, onRefreshCaptcha = { vm.refreshCaptcha() }, onLogin = { sid, pwd, cap -> vm.login(sid, pwd, cap) }, onQuickRelogin = { cap -> vm.quickRelogin(cap) }, onWebViewLogin = { showQr = true }, onBack = { vm.clearLoginError(); finish() }, blurEnabled = blurEffect)
                    }
                }
            }
        }
    }
}
