package com.ty.gkschedule.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

// ponytail: 顶栏miuix糊——drawBackdrop吃backdrop源，RuntimeShader真模糊；关开关回退纯色
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurLargeTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: androidx.compose.foundation.layout.WindowInsets = TopAppBarDefaults.windowInsets,
    backdrop: Backdrop? = null,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && backdrop != null
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.75f else 1f
    )
    // ponytail: Large顶栏真折叠——miuix糊跟栏高收缩走，折叠黑条是糊层没跟上
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
        modifier = Modifier.then(
            if (useBlur) Modifier.drawBackdrop(
                backdrop = backdrop!!,
                shape = { androidx.compose.ui.graphics.RectangleShape },
                effects = { blur(28.dp.toPx()) }
            ) else Modifier
        )
    )
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
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.75f else 1f
    )
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barBg,
            scrolledContainerColor = barBg
        ),
        modifier = Modifier.then(
            if (useBlur) Modifier.drawBackdrop(
                backdrop = backdrop!!,
                shape = { androidx.compose.ui.graphics.RectangleShape },
                effects = { blur(28.dp.toPx()) }
            ) else Modifier
        )
    )
}
