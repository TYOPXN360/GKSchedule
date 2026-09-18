package com.ty.gkschedule.ui.theme

import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

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

// ponytail: QPR2结论——Dialog卡片真磨砂对第三方App不可做：
// ponytail: 反射BackgroundBlurDrawable被blocklist；窗口级需translucent但Compose Dialog全屏糊整屏；
// ponytail: miuix只能采同窗口源，卡片内容自身透明录下来就是灰白块，糊完还是灰白。
// ponytail: BlurCard=半透明tint卡（开糊半透明/关糊纯色）；真糊只留顶栏/底栏/FAB（同窗口有源可采）
@Composable
fun BlurCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    radiusDp: Float = 32f,
    backgroundColor: Color = Color.Unspecified,
    cornerRadiusDp: Float = 28f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    val bg = if (backgroundColor != Color.Unspecified) {
        if (enabled) backgroundColor else backgroundColor.copy(alpha = 1f)
    } else Color.Transparent

    // ponytail: Dialog默认60%黑幕压死底子，降到12%才透光
    val view = LocalView.current
    DisposableEffect(view) {
        view.applyDialogWindowBlur()
        view.post { view.applyDialogWindowBlur() }
        onDispose { }
    }

    Column(modifier = modifier.clip(shape).background(bg)) { content() }
}

// ponytail: A方案验证失败——Compose Dialog窗口全屏，窗口级模糊必糊整屏（含卡片外）
// ponytail: QPR2上只糊卡片内对第三方App不可做，回退tint半透明卡（不穿帮）；反射探测日志保留
private fun View.applyDialogWindowBlur() {
    findDialogWindow()?.let { w ->
        w.setDimAmount(0.12f)
    }
}

@Composable
fun BackdropBottomSheet(
    backdrop: Backdrop,
    enabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .clickable(onClick = onDismiss)
        )
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            visibleState = transitionState,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)) + fadeIn(tween(180))
        ) {
            BlurCardSurface(
                backdrop = backdrop,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                cornerRadiusDp = 28f,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                content = content
            )
        }
    }
}

@Composable
fun BackdropDialog(
    backdrop: Backdrop,
    enabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .clickable(onClick = onDismiss)
        )
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visibleState = transitionState,
            enter = slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(220)) + fadeIn(tween(160))
        ) {
            BlurCardSurface(
                backdrop = backdrop,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(24.dp).align(Alignment.Center),
                cornerRadiusDp = 28f,
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
                content = content
            )
        }
    }
}

@Composable
private fun BlurCardSurface(
    backdrop: Backdrop,
    enabled: Boolean,
    modifier: Modifier,
    backgroundColor: Color,
    cornerRadiusDp: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = { blur(28.dp.toPx()) },
                ) else Modifier
            )
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 1f))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
fun BackdropAlertDialog(
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    blurEnabled: Boolean = true,
) {
    BackdropDialog(backdrop = backdrop, enabled = blurEnabled, onDismiss = onDismissRequest) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            if (title != null) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.headlineSmall) { title() }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (text != null) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) { text() }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                dismissButton?.invoke()
                Spacer(modifier = Modifier.width(8.dp))
                confirmButton()
            }
        }
    }
}

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
