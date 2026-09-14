package com.ty.gkschedule.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// ponytail: 后台运行=电池优化白名单——REQUEST_IGNORE_BATTERY_OPTIMIZATIONS只是清单声明，
// 真正弹"允许后台运行吗"系统页靠ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS intent
object BackgroundRun {
    fun isIgnoringOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    // ponytail: 开关打开时先弹系统页，用户允许后才真正打开；拒绝则开关保持关闭
    @Composable
    fun rememberRequester(onResult: (allowed: Boolean) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { onResult(isIgnoringOptimizations(context)) }
        return remember(context) {
            {
                if (isIgnoringOptimizations(context)) onResult(true)
                else runCatching { launcher.launch(requestIntent(context)) }
                    .onFailure { onResult(false) }
            }
        }
    }
}
