package com.ty.gkschedule.ui.theme

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

// ponytail: 递归找Dialog真实Window——DialogLayout本身就是DialogWindowProvider，只查parent/context永远null
fun View.findDialogWindow(): Window? {
    var current: View? = this
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    val ctx = this.context
    if (ctx is DialogWindowProvider) return ctx.window
    return null
}

// ponytail: 卡片区域毛玻璃——BackgroundBlurDrawable矩形=载体bounds，只糊卡片不糊全屏
// ponytail: 跨窗口糊背后Activity靠window级blurBehindRadius+FLAG_BLUR_BEHIND（S31+）
@Composable
fun BlurCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    radiusDp: Float = 32f,
    backgroundColor: Color = Color.Unspecified,
    cornerRadiusDp: Float = 28f,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(cornerRadiusDp.dp)

    // ponytail: Dialog默认60%黑幕压死底子，降到12%才透光；只留卡内区域糊，
    // 窗口级blurBehind会全屏糊宿主การ
    DisposableEffect(view, radiusDp) {
        view.applyDialogWindowBlur()
        view.post { view.applyDialogWindowBlur() }
        val winListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = v.applyDialogWindowBlur()
            override fun onViewDetachedFromWindow(v: View) {}
        }
        view.addOnAttachStateChangeListener(winListener)
        onDispose { view.removeOnAttachStateChangeListener(winListener) }
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
            // ponytail: 关开关纯色——糊开才半透明透糊
            Spacer(Modifier.matchParentSize().background(backgroundColor.copy(alpha = 1f)))
        }
        content()
    }
}

// ponytail: 窗口半透明+主题BlurDialog——LOS24 DecorView源码：isTranslucent才创建模糊，否则半径置0
// ponytail: 系统DecorView接管createBackgroundBlurDrawable（免反射），App层只调官方setBackgroundBlurRadius
private fun View.applyDialogWindowBlur() {
    findDialogWindow()?.let { w ->
        w.setDimAmount(0.12f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                w.setBackgroundDrawableResource(android.R.color.transparent)
                val px = (28 * resources.displayMetrics.density).toInt().coerceIn(1, 150)
                w.setBackgroundBlurRadius(px)
                w.attributes = w.attributes.also { it.blurBehindRadius = px }
                w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

private fun applyBlur(
    host: View, composeView: View, radiusDp: Float, cornerRadiusDp: Float, backgroundColor: Color,
): Drawable? = runCatching {
    val density = host.resources.displayMetrics.density
    // ponytail: QPR1生效QPR2失效——先反射探测断裂点，QPR2变化可能在这几步任一
    val root = runCatching {
        View::class.java.getDeclaredMethod("getViewRootImpl").apply { isAccessible = true }
            .invoke(composeView)
    }.onFailure { e -> android.util.Log.e("BlurProbe", "getViewRootImpl FAIL: $e") }.getOrNull()
    if (root == null) {
        val tintOnly = GradientDrawable().apply { setColor(backgroundColor.toArgb()); cornerRadius = cornerRadiusDp * density }
        host.background = tintOnly
        return tintOnly
    }
    val blur = runCatching {
        root.javaClass.getDeclaredMethod("createBackgroundBlurDrawable").apply { isAccessible = true }
            .invoke(root) as? Drawable
    }.onFailure { e -> android.util.Log.e("BlurProbe", "createBackgroundBlurDrawable FAIL: $e") }.getOrNull()
    if (blur == null) {
        val tintOnly = GradientDrawable().apply { setColor(backgroundColor.toArgb()); cornerRadius = cornerRadiusDp * density }
        host.background = tintOnly
        return tintOnly
    }
    val px = (radiusDp * density).toInt().coerceIn(1, 150)
    val cornerPx = cornerRadiusDp * density
    runCatching {
        blur.javaClass.getDeclaredMethod("setBlurRadius", Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(blur, px)
    }.onFailure { e -> android.util.Log.e("BlurProbe", "setBlurRadius FAIL: $e") }
    runCatching {
        blur.javaClass.getDeclaredMethod("setCornerRadius", Float::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(blur, cornerPx)
    }.onFailure { e -> android.util.Log.e("BlurProbe", "setCornerRadius FAIL: $e") }
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

// ponytail: 整卡包裹毛玻璃AlertDialog——BasicAlertDialog无自带实心底，整张BlurCard一体成型
// ponytail: 关开关=纯色卡（enabled=false走Spacer底）；调用方Dialog/Dropdown/Sheet/详情卡全要透传开关
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    blurEnabled: Boolean = true,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        BlurCard(
            enabled = blurEnabled,
            modifier = Modifier.fillMaxWidth(),
            radiusDp = 32f,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            cornerRadiusDp = 28f
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    ) { icon() }
                }
                if (title != null) {
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.headlineSmall
                        ) { title() }
                    }
                }
                if (text != null) {
                    Box(
                        modifier = Modifier
                            .weight(weight = 1f, fill = false)
                            .padding(bottom = 24.dp)
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium
                        ) { text() }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

// ponytail: 整卡包裹毛玻璃DatePickerDialog——DatePicker自身底色调用点置透明
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
    blurEnabled: Boolean = true,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BlurCard(
            enabled = blurEnabled,
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight(),
            radiusDp = 32f,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
            cornerRadiusDp = 28f
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun BlurDropdownMenu(    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    blurEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // ponytail: 阴影只留一层——DropdownMenu自带tonal表面色+BlurCard糊底叠色=双层直角；
    // 容器透明+阴影0，形状全交BlurCard圆角
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        BlurCard(
            enabled = blurEnabled,
            modifier = Modifier,
            radiusDp = 28f,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
            cornerRadiusDp = 12f
        ) {
            Column { content() }
        }
    }
}
