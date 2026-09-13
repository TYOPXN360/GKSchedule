package com.ty.gkschedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

// ponytail: 可滚动页糊顶栏统一入口——haze同窗口backdrop糊；关开关回退纯色
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurLargeTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: androidx.compose.foundation.layout.WindowInsets = TopAppBarDefaults.windowInsets,
    hazeState: HazeState? = null,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && hazeState != null
    // ponytail: 底色只给35%透明，让糊层透上来；haze的backgroundColor与containerColor叠加会压淡糊感
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.35f else 1f
    )
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
            if (useBlur) Modifier.hazeEffect(
                state = hazeState!!,
                style = dev.chrisbanes.haze.HazeDefaults.style(
                    backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                    blurRadius = 28.dp,
                    noiseFactor = 0f
                )
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
    hazeState: HazeState? = null,
    blurEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val useBlur = blurEnabled && hazeState != null
    // ponytail: 底色只给35%透明，让糊层透上来；haze的backgroundColor与containerColor叠加会压淡糊感
    val barBg = MaterialTheme.colorScheme.surface.copy(
        alpha = if (useBlur) 0.35f else 1f
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
            if (useBlur) Modifier.hazeEffect(
                state = hazeState!!,
                style = dev.chrisbanes.haze.HazeDefaults.style(
                    backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                    blurRadius = 28.dp,
                    noiseFactor = 0f
                )
            ) else Modifier
        )
    )
}
