package com.ty.gkschedule.ui.theme

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

// ponytail: Dialog/Sheet独立窗口糊背后——doubaoime同款createBackgroundBlurDrawable挂自己decor
// blurBehindRadius只改attrs不relayout，下发不到SurfaceFlinger，用这套
fun Modifier.windowBlurBehind(enabled: Boolean, radiusDp: Float = 28f): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@composed this
    val view = LocalView.current
    DisposableEffect(view, enabled, radiusDp) {
        val decor = view.rootView
        val blur = if (enabled) applyDecor(decor, radiusDp) else null
        android.util.Log.d("blurwin", "vr=${System.identityHashCode(decor)} applied=${blur != null}")
        onDispose { if (blur != null && decor.background === blur) decor.background = null }
    }
    this
}

private fun applyDecor(decor: View, radiusDp: Float): Drawable? = runCatching {
    val root = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }.invoke(decor)
        ?: return null
    val blur = root.javaClass.getDeclaredMethod("createBackgroundBlurDrawable").apply { isAccessible = true }.invoke(root)
        as? Drawable ?: return null
    val px = (radiusDp * decor.resources.displayMetrics.density).toInt().coerceIn(1, 150)
    blur.javaClass.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType).apply { isAccessible = true }.invoke(blur, px)
    decor.background = blur
    decor.invalidate()
    blur
}.getOrNull()
