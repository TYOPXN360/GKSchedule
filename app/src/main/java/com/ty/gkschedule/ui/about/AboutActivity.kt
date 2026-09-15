package com.ty.gkschedule.ui.about

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.gkschedule.ScheduleViewModel
import com.ty.gkschedule.ui.theme.GKScheduleTheme

// ponytail: 关于页独立Activity，返回走系统级预测动画；与SettingsActivity同模式
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")

            GKScheduleTheme(darkTheme = darkMode) {
                // ponytail: 独立Activity必须自管状态栏图标——enableEdgeToEdge默认跟系统/主题走，
                // 不读App的darkMode；亮色下漏设=白底白图标（与MainActivity同款修复）
                val view = androidx.compose.ui.platform.LocalView.current
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
                // ponytail: 底色必须与主App源层同值（亮surfaceContainer/暗surface）。
                // 用 background 在亮色下更白一档：背景纯白、卡片反显不白，且铺到顶让状态栏也发白
                val pageBg = if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Surface(modifier = Modifier.fillMaxSize(), color = pageBg) {
                    val blurEffect by vm.blurEffect.collectAsState(initial = false)
                    // ponytail: 通知带更新信息进页——自动弹更新框，复用现有Dialog
                    val showUpdateDialog = intent.getBooleanExtra(
                        com.ty.gkschedule.util.UpdateNotificationHelper.EXTRA_SHOW_UPDATE_DIALOG, false)
                    val latestVersion = intent.getStringExtra(
                        com.ty.gkschedule.util.UpdateNotificationHelper.EXTRA_UPDATE_LATEST_VERSION) ?: ""
                    val downloadUrl = intent.getStringExtra(
                        com.ty.gkschedule.util.UpdateNotificationHelper.EXTRA_UPDATE_DOWNLOAD_URL) ?: ""
                    val updateNotes = intent.getStringExtra(
                        com.ty.gkschedule.util.UpdateNotificationHelper.EXTRA_UPDATE_NOTES) ?: ""
                    val fileSize = intent.getLongExtra(
                        com.ty.gkschedule.util.UpdateNotificationHelper.EXTRA_UPDATE_FILE_SIZE, 0L)
                    AboutDetailPage(
                        onBack = { finish() }, blurEnabled = blurEffect,
                        autoShowUpdateDialog = showUpdateDialog,
                        preloadedUpdateInfo = if (showUpdateDialog && latestVersion.isNotEmpty()) {
                            com.ty.gkschedule.util.UpdateInfo(
                                currentVersion = com.ty.gkschedule.util.UpdateChecker.getCurrentVersion(this@AboutActivity),
                                latestVersion = latestVersion, releaseName = "",
                                releaseNotes = updateNotes, downloadUrl = downloadUrl,
                                fileSize = fileSize, isUpdateAvailable = true)
                        } else null
                    )
                }
            }
        }
    }
}
