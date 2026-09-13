package com.ty.gkschedule.ui.theme

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView

// ponytail: 卡片区域毛玻璃——BackgroundBlurDrawable矩形=载体bounds，只糊卡片不糊全屏
// decor全屏挂会整屏糊，载体等大View+LayerDrawable合成才是doubaoime原意
@Composable
fun BlurCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    radiusDp: Float = 28f,
    backgroundColor: Color = Color.Unspecified,
    cornerRadiusDp: Float = 28f,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    Box(modifier) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AndroidView(
                factory = { ctx ->
                    View(ctx).apply {
                        val d = applyBlur(this, view, radiusDp, cornerRadiusDp, backgroundColor)
                        android.util.Log.d("blurcard", "applied=${d != null}")
                        if (d == null && backgroundColor != Color.Unspecified) {
                            setBackgroundColor(backgroundColor.copy(alpha = 1f).toArgb())
                        }
                    }
                },
                modifier = Modifier.matchParentSize(),
                update = {},
            )
        } else if (backgroundColor != Color.Unspecified) {
            Spacer(Modifier.matchParentSize().background(backgroundColor))
        }
        content()
    }
}

private fun applyBlur(
    host: View, composeView: View, radiusDp: Float, cornerRadiusDp: Float, backgroundColor: Color,
): Drawable? = runCatching {
    val root = View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }
        .invoke(composeView) ?: return null
    val blur = root.javaClass.getDeclaredMethod("createBackgroundBlurDrawable").apply { isAccessible = true }
        .invoke(root) as? Drawable ?: return null
    val px = (radiusDp * host.resources.displayMetrics.density).toInt().coerceIn(1, 150)
    blur.javaClass.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType)
        .apply { isAccessible = true }.invoke(blur, px)
    runCatching {
        blur.javaClass.getDeclaredMethod("setCornerRadius", Float::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(blur, cornerRadiusDp * host.resources.displayMetrics.density)
    }
    val layers = if (backgroundColor != Color.Unspecified)
        arrayOf(blur, ColorDrawable(backgroundColor.toArgb())) else arrayOf(blur)
    host.background = LayerDrawable(layers)
    blur
}.getOrNull()
