package com.ty.gkschedule.ui.theme

import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.foundation.background
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
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

// ponytail: 卡片区域毛玻璃——miuix同窗口源层+drawBackdrop（QPR2反射野路已死，系统级删光）
// ponytail: Dialog窗口无源层可吃——源层挂卡内Column（内容自身），drawBackdrop吃它即卡片内糊
@Composable
fun BlurCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    radiusDp: Float = 32f,
    backgroundColor: Color = Color.Unspecified,
    cornerRadiusDp: Float = 28f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop()
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

    if (enabled) {
        // ponytail: 源层与采样层必须是兄弟——同节点自采样=RenderThread prepareTreeImpl SIGSEGV
        Box(
            modifier = modifier
                .clip(shape)
                .drawBackdropCompat(
                    backdrop = backdrop,
                    shape = { shape },
                    radiusDp = radiusDp,
                    onDrawSurface = { drawRect(bg) }
                )
        ) {
            Column(Modifier.layerBackdropCompat(backdrop)) {
                content()
            }
        }
    } else {
        Column(modifier = modifier.clip(shape).background(bg)) { content() }
    }
}

// ponytail: A方案验证失败——Compose Dialog窗口全屏，窗口级模糊必糊整屏（含卡片外）
// ponytail: QPR2上只糊卡片内对第三方App不可做，回退tint半透明卡（不穿帮）；反射探测日志保留
private fun View.applyDialogWindowBlur() {
    findDialogWindow()?.let { w ->
        w.setDimAmount(0.12f)
    }
}

// ponytail: miuix drawBackdrop/layerBackdrop薄封装——传backdrop+shape+半径，与顶栏/底栏同款签名
@Composable
private fun Modifier.layerBackdropCompat(backdrop: LayerBackdrop): Modifier =
    this.then(Modifier.layerBackdrop(backdrop))

@Composable
private fun Modifier.drawBackdropCompat(
    backdrop: LayerBackdrop,
    shape: () -> Shape,
    radiusDp: Float,
    onDrawSurface: DrawScope.() -> Unit
): Modifier = this.then(
    Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = { blur((radiusDp.dp).toPx()) },
        onDrawSurface = onDrawSurface
    )
)

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
