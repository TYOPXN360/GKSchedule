package com.ty.gkschedule.ui.theme

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

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

    Column(modifier = modifier.clip(shape).background(bg)) { content() }
}

// ponytail: A方案验证失败——Compose Dialog窗口全屏，窗口级模糊必糊整屏（含卡片外）
// ponytail: QPR2上只糊卡片内对第三方App不可做，回退tint半透明卡（不穿帮）；反射探测日志保留
@Composable
fun BackdropBottomSheet(
    backdrop: Backdrop,
    enabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var sheetHeight by remember { mutableIntStateOf(0) }
    var dismissing by remember { mutableStateOf(false) }
    fun dismissWithAnimation() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            sheetOffset.animateTo(sheetHeight.toFloat().coerceAtLeast(1f), tween(220))
            onDismiss()
        }
    }
    PredictiveBackHandler { progress: kotlinx.coroutines.flow.Flow<androidx.activity.BackEventCompat> ->
        var committed = false
        try {
            progress.collect { event ->
                sheetOffset.snapTo(sheetHeight * event.progress)
            }
            committed = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            sheetOffset.animateTo(0f, spring())
            throw e
        } finally {
            if (committed) dismissWithAnimation()
        }
    }
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.12f))
                .pointerInput(dismissing) {
                    detectTapGestures(onTap = { dismissWithAnimation() })
                }
        )
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            visibleState = transitionState,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)) + fadeIn(tween(180))
        ) {
            BlurCardSurface(
                backdrop = backdrop,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, sheetOffset.value.roundToInt()) }
                    .onSizeChanged { sheetHeight = it.height }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (!dismissing) {
                                    scope.launch { sheetOffset.snapTo((sheetOffset.value + dragAmount).coerceAtLeast(0f)) }
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    if (sheetOffset.value > sheetHeight * 0.25f) {
                                        dismissWithAnimation()
                                    } else {
                                        sheetOffset.animateTo(0f, spring())
                                    }
                                }
                            }
                        )
                    },
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
