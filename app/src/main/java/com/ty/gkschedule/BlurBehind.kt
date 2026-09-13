package com.ty.gkschedule

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

// ponytail: 同一窗口内跨层糊（doubaoime同款createBackgroundBlurDrawable）；S以下/关开关原样回退
fun Modifier.blurBehind(enabled: Boolean, radiusDp: Float = 24f): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@composed this
    val view = LocalView.current
    DisposableEffect(view, enabled, radiusDp) {
        val applied = if (enabled) applyBehind(view, radiusDp) else null
        onDispose { applied?.let { clearBehind(view, it) } ?: clearBehind(view, null) }
    }
    this
}

private var View.appliedBlur: Drawable? by ViewBlurTag

private fun applyBehind(view: View, radiusDp: Float): Drawable? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    if (!view.isAttachedToWindow) {
        view.post { if (view.getTag(BlurBehindTag.id) != null) applyBehind(view, radiusDp) }
        view.setTag(BlurBehindTag.id, true)
        return null
    }
    view.setTag(BlurBehindTag.id, null)
    try {
        // ponytail: 反射拿ViewRootImpl.createBackgroundBlurDrawable，糊的是身后已绘制内容，自身字图不动
        val getRoot = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }
        val rootImpl = getRoot.invoke(view.rootView) ?: return null
        val create = rootImpl.javaClass.getDeclaredMethod("createBackgroundBlurDrawable").apply { isAccessible = true }
        val blur = create.invoke(rootImpl) as? Drawable ?: return null
        val radiusPx = (radiusDp * view.resources.displayMetrics.density).toInt().coerceIn(1, 150)
        blur.javaClass.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(blur, radiusPx)
        // ponytail: 只糊不盖色；取色靠药丸55%底色透上来混
        view.appliedBlur = blur
        view.background = blur
        view.invalidate()
        return blur
    } catch (_: Exception) {
        return null
    }
}

private fun clearBehind(view: View, applied: Drawable? = null) {
    try {
        if (view.getTag(BlurBehindTag.id) != null) view.setTag(BlurBehindTag.id, null)
        if (applied != null && view.background === applied) view.background = null
        else if (view.appliedBlur != null && view.background === view.appliedBlur) {
            view.background = null
            view.appliedBlur = null
        }
        view.invalidate()
    } catch (_: Exception) {
    }
}

private object BlurBehindTag {
    val id: Int = 0x7F0B1E01.toInt()
}

private object ViewBlurTag : kotlin.properties.ReadWriteProperty<View, Drawable?> {
    private val map = java.util.WeakHashMap<View, Drawable?>()
    override fun getValue(thisRef: View, property: kotlin.reflect.KProperty<*>): Drawable? = map[thisRef]
    override fun setValue(thisRef: View, property: kotlin.reflect.KProperty<*>, value: Drawable?) {
        if (value == null) map.remove(thisRef) else map[thisRef] = value
    }
}
