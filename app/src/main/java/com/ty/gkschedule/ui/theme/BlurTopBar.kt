package com.ty.gkschedule.ui.theme

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// ponytail: 可滚动页糊顶栏统一入口——兄弟backdrop纹理+自身糊版+55%底+字在上；关开关/S以下回退纯色
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurLargeTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: androidx.compose.foundation.layout.WindowInsets = TopAppBarDefaults.windowInsets,
    backdrop: GraphicsLayer? = null,
    srcPos: Offset = Offset.Zero,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.55f else 1f
    )
    val blurR = with(LocalDensity.current) { 24.dp.toPx() }
    val blurFx = remember(useBlur, blurR) {
        if (!useBlur) null else RenderEffect
            .createBlurEffect(blurR, blurR, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
    val blurred = rememberGraphicsLayer()
    val barPos = remember { mutableStateOf(Offset.Zero) }
    androidx.compose.material3.LargeTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barBg,
            scrolledContainerColor = barBg
        ),
        modifier = Modifier
            .onGloballyPositioned { barPos.value = it.positionInRoot() }
            .drawWithContent {
                val fx = blurFx
                if (fx != null && backdrop != null) {
                    blurred.renderEffect = fx
                    blurred.record {
                        translate(srcPos.x - barPos.value.x, srcPos.y - barPos.value.y) { drawLayer(backdrop) }
                    }
                    drawLayer(blurred)
                    drawRect(barBg)
                }
                drawContent()
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: GraphicsLayer? = null,
    srcPos: Offset = Offset.Zero,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.55f else 1f
    )
    val blurR = with(LocalDensity.current) { 24.dp.toPx() }
    val blurFx = remember(useBlur, blurR) {
        if (!useBlur) null else RenderEffect
            .createBlurEffect(blurR, blurR, Shader.TileMode.CLAMP)
            .asComposeRenderEffect()
    }
    val blurred = rememberGraphicsLayer()
    val barPos = remember { mutableStateOf(Offset.Zero) }
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barBg,
            scrolledContainerColor = barBg
        ),
        modifier = Modifier
            .onGloballyPositioned { barPos.value = it.positionInRoot() }
            .drawWithContent {
                val fx = blurFx
                if (fx != null && backdrop != null) {
                    blurred.renderEffect = fx
                    blurred.record {
                        translate(srcPos.x - barPos.value.x, srcPos.y - barPos.value.y) { drawLayer(backdrop) }
                    }
                    drawLayer(blurred)
                    drawRect(barBg)
                }
                drawContent()
            }
    )
}
