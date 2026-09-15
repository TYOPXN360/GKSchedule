package com.ty.gkschedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

// ponytail: 顶栏miuix糊——drawBackdrop吃backdrop源，RuntimeShader真模糊；关开关回退纯色
// ponytail: 糊矩形=Box bounds（含状态栏）；内层顶栏windowInsets=0+透明底，不叠第二层
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurLargeTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: Backdrop? = null,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && backdrop != null
    // ponytail: 顶栏底与主App底色同系——暗surface/亮surfaceContainer（全surface亮色断层）
    val isDark = LocalAppIsDark.current
    val barBg = (if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer).copy(
        alpha = if (useBlur) 0.40f else 1f
    )
    // ponytail: Large顶栏真折叠——miuix糊跟栏高收缩走，折叠黑条是糊层没跟上
    // ponytail: windowInsets交还M3（外层statusBarsPadding会推歪两行measure基线）
    Box(
        modifier = Modifier
            .then(
                if (useBlur) Modifier.drawBackdrop(
                    backdrop = backdrop!!,
                    shape = { RectangleShape },
                    effects = { blur(28.dp.toPx()) }
                ) else Modifier
            )
            .background(barBg)
    ) {
        LargeTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = TopAppBarDefaults.windowInsets,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: Backdrop? = null,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && backdrop != null
    // ponytail: 顶栏底与主App底色同系——暗surface/亮surfaceContainer（全surface亮色断层）
    val isDark = LocalAppIsDark.current
    val barBg = (if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer).copy(
        alpha = if (useBlur) 0.40f else 1f
    )
    Box(
        modifier = Modifier
            .then(
                if (useBlur) Modifier.drawBackdrop(
                    backdrop = backdrop!!,
                    shape = { RectangleShape },
                    effects = { blur(28.dp.toPx()) }
                ) else Modifier
            )
            .background(barBg)
    ) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}
