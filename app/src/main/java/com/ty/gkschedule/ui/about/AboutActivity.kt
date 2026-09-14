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
                // ponytail: 底色必须与主App源层同值（亮surfaceContainer/暗surface）。
                // 用 background 在亮色下更白一档：背景纯白、卡片反显不白，且铺到顶让状态栏也发白
                val pageBg = if (com.ty.gkschedule.ui.theme.LocalAppIsDark.current) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Surface(modifier = Modifier.fillMaxSize(), color = pageBg) {
                    val blurEffect by vm.blurEffect.collectAsState(initial = true)
                    AboutDetailPage(onBack = { finish() }, blurEnabled = blurEffect)
                }
            }
        }
    }
}
