package com.classapp.schedule.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BadgeColorPalette {
    Primary, Secondary, Tertiary, Error, Neutral, Inverse
}

@Composable
fun MonetIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    badgePalette: BadgeColorPalette,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 14.dp
) {
    val (containerColor, contentColor) = badgePaletteColors(badgePalette)

    Box(
        modifier = modifier
            .size(size)
            .background(color = containerColor, shape = RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = contentColor
        )
    }
}

@Composable
fun MonetIconBadgeTextColor(badgePalette: BadgeColorPalette): Color {
    return badgePaletteColors(badgePalette).second
}

@Composable
fun MonetIconBadgeTextColor(seedColor: Color): Color {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary
    val error = scheme.error
    return when {
        colorsClose(seedColor, primary) -> scheme.onPrimaryContainer
        colorsClose(seedColor, secondary) -> scheme.onSecondaryContainer
        colorsClose(seedColor, tertiary) -> scheme.onTertiaryContainer
        colorsClose(seedColor, error) -> scheme.onErrorContainer
        else -> scheme.onSurfaceVariant
    }
}

@Composable
private fun badgePaletteColors(palette: BadgeColorPalette): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (palette) {
        BadgeColorPalette.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        BadgeColorPalette.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
        BadgeColorPalette.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        BadgeColorPalette.Error -> scheme.errorContainer to scheme.onErrorContainer
        BadgeColorPalette.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        BadgeColorPalette.Inverse -> scheme.inverseSurface to scheme.inverseOnSurface
    }
}

private fun colorsClose(a: Color, b: Color, threshold: Float = 0.15f): Boolean {
    return kotlin.math.abs(a.red - b.red) < threshold &&
            kotlin.math.abs(a.green - b.green) < threshold &&
            kotlin.math.abs(a.blue - b.blue) < threshold
}

internal fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return floatArrayOf(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + (if (g < b) 6 else 0)) * 60f
        g -> ((b - r) / d + 2) * 60f
        else -> ((r - g) / d + 4) * 60f
    }
    return floatArrayOf(h, s, l)
}

internal fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60  -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}


