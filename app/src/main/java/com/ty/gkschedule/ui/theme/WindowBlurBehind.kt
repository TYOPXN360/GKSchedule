package com.ty.gkschedule.ui.theme

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

// ponytail: Dialog/Sheet独立窗口糊背后主窗口——设在自己窗口上，设主窗口糊的是桌面
fun Modifier.windowBlurBehind(enabled: Boolean, radiusDp: Float = 28f): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@composed this
    val view = LocalView.current
    DisposableEffect(view, enabled, radiusDp) {
        val applied = applyToOwnWindow(view, radiusDp)
        android.util.Log.d("blurwin", "vr=${System.identityHashCode(view.rootView)} applied=$applied")
        onDispose { if (applied) applyToOwnWindow(view, 0f) }
    }
    this
}

private fun applyToOwnWindow(view: View, radiusDp: Float): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    return runCatching {
        val root = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }.invoke(view)
            ?: return false
        val px = (radiusDp * view.resources.displayMetrics.density).toInt().coerceIn(0, 150)
        val attrs = root.javaClass.getDeclaredMethod("getWindowAttributes").apply { isAccessible = true }.invoke(root)
            as WindowManager.LayoutParams
        if (px > 0) attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        attrs.setBlurBehindRadius(px)
        root.javaClass.getDeclaredMethod("requestUpdateAttributes", WindowManager.LayoutParams::class.java)
            .apply { isAccessible = true }.invoke(root, attrs)
        true
    }.getOrDefault(false)
}

// ponytail: compose层只读开关，不自己调WindowManager
@Composable
fun rememberWindowBlurEnabled(fallback: Boolean = true): Boolean = fallback
