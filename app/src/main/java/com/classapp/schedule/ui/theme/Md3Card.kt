package com.classapp.schedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class Md3CardVariant { Elevated, Filled, Outlined }

@Composable
fun Md3Card(
    modifier: Modifier = Modifier,
    variant: Md3CardVariant = Md3CardVariant.Elevated,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit
) {
    when (variant) {
        Md3CardVariant.Elevated -> Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 1.dp
        ) {
            Column { content() }
        }
        Md3CardVariant.Filled -> Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            content = content
        )
        Md3CardVariant.Outlined -> OutlinedCard(
            modifier = modifier,
            shape = shape,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Md3Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: Md3CardVariant = Md3CardVariant.Elevated,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit
) {
    when (variant) {
        Md3CardVariant.Elevated -> Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 1.dp
        ) {
            Column { content() }
        }
        Md3CardVariant.Filled -> Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            content = content
        )
        Md3CardVariant.Outlined -> OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            content = content
        )
    }
}

@Composable
fun SettingsIconBadge(
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(containerColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconColor
        )
    }
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
