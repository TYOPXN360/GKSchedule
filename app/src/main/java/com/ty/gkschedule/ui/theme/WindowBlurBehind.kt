package com.ty.gkschedule.ui.theme

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogWindowProvider

// ponytail: 卡片区域毛玻璃——BackgroundBlurDrawable矩形=载体bounds，只糊卡片不糊全屏
// decor全屏挂会整屏糊，载体等大View+LayerDrawable合成才是doubaoime原意
// ponytail: attach时序——factory瞬间未挂载getViewRootImpl=null，监听+post+update三保险重试
// ponytail: DropdownMenu小菜单糊——Popup独立窗口，BlurCard挂菜单根，同重登录Dialog一套
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
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    // ponytail: Dialog默认60%黑幕先压死底子，对齐Sheet降到12%才透光
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: (view.context as? DialogWindowProvider)?.window
        window?.let { w -> w.setDimAmount(0.12f) }
        onDispose {}
    }

    Box(modifier = modifier.clip(shape)) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AndroidView(
                factory = { ctx ->
                    View(ctx).apply {
                        val attachListener = object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {
                                applyBlur(v, view, radiusDp, cornerRadiusDp, backgroundColor)
                            }
                            override fun onViewDetachedFromWindow(v: View) {}
                        }
                        addOnAttachStateChangeListener(attachListener)
                        if (isAttachedToWindow) {
                            applyBlur(this, view, radiusDp, cornerRadiusDp, backgroundColor)
                        } else {
                            post {
                                if (isAttachedToWindow) {
                                    applyBlur(this, view, radiusDp, cornerRadiusDp, backgroundColor)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.matchParentSize(),
                update = { v ->
                    if (v.isAttachedToWindow) {
                        applyBlur(v, view, radiusDp, cornerRadiusDp, backgroundColor)
                    }
                }
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
    val density = host.resources.displayMetrics.density
    val px = (radiusDp * density).toInt().coerceIn(1, 150)
    val cornerPx = cornerRadiusDp * density
    blur.javaClass.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType)
        .apply { isAccessible = true }.invoke(blur, px)
    runCatching {
        blur.javaClass.getDeclaredMethod("setCornerRadius", Float::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(blur, cornerPx)
    }
    // ponytail: GradientDrawable带圆角，ColorDrawable直角会盖住模糊层
    val tintDrawable = if (backgroundColor != Color.Unspecified) {
        GradientDrawable().apply {
            setColor(backgroundColor.toArgb())
            cornerRadius = cornerPx
        }
    } else null
    val layers = if (tintDrawable != null) arrayOf(blur, tintDrawable) else arrayOf(blur)
    host.background = LayerDrawable(layers)
    blur
}.getOrNull()

// ponytail: DropdownMenu小菜单糊——包一层Box吃insets，菜单项糊底色与重登录Dialog同款
@Composable
fun BlurDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = 6.dp
    ) {
        BlurCard(
            enabled = true,
            modifier = Modifier,
            radiusDp = 28f,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
            cornerRadiusDp = 12f
        ) {
            androidx.compose.foundation.layout.Column { content() }
        }
    }
}
