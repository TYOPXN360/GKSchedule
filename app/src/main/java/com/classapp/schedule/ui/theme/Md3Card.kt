package com.classapp.schedule.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Composable
fun Md3Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Md3Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        content = content
    )
}

@Composable
fun monetCardColor(seedColor: Color): Color {
    val isDark = LocalAppIsDark.current
    val hsl = rgbToHsl(seedColor.red, seedColor.green, seedColor.blue)
    return if (isDark) {
        hslToColor(hsl[0], 0.12f, 0.28f)
    } else {
        hslToColor(hsl[0], 0.40f, 0.94f)
    }
}
