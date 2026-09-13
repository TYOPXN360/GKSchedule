package com.ty.gkschedule

import android.os.Build
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

// ponytail: 系统窗口blurBehind唯一入口；S以下/用户关开关时原样回退，不糊字不糊图
fun Modifier.blurBehind(enabled: Boolean, radiusDp: Float = 24f): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@composed this
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        if (enabled) applyBehind(view, radiusDp) else clearBehind(view)
        onDispose { clearBehind(view) }
    }
    this
}

private fun applyBehind(view: View, radiusDp: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (!view.isAttachedToWindow) {
        view.post { applyBehind(view, radiusDp) }
        return
    }
    try {
        val window = (view.context as? android.app.Activity)?.window ?: return
        val radiusPx = (radiusDp * view.resources.displayMetrics.density).toInt().coerceIn(1, 150)
        // ponytail: 只动自家窗口blurBehind半径，不碰dim/alpha；图标文字在上层正常绘制
        val attrs = window.attributes
        attrs.blurBehindRadius = radiusPx
        window.attributes = attrs
        window.setBackgroundBlurRadius(radiusPx)
    } catch (_: Exception) {
    }
}

private fun clearBehind(view: View) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    try {
        val window = (view.context as? android.app.Activity)?.window ?: return
        val attrs = window.attributes
        attrs.blurBehindRadius = 0
        window.attributes = attrs
        window.setBackgroundBlurRadius(0)
    } catch (_: Exception) {
    }
}
