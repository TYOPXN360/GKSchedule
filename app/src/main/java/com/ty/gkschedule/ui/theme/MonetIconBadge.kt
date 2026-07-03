package com.ty.gkschedule.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Badge 色相来源
 * 直接使用 M3 标准语义色（已包含正确 Tone）
 */
enum class BadgeColorPalette {
    Primary, Secondary, Tertiary, Neutral, Inverse
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
    val (containerColor, contentColor) = badgePaletteColors(MaterialTheme.colorScheme, badgePalette)

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
    return badgePaletteColors(MaterialTheme.colorScheme, badgePalette).second
}

/**
 * 直接使用 M3 标准语义色，无需手动计算 Tone
 */
@Composable
private fun badgePaletteColors(scheme: androidx.compose.material3.ColorScheme, palette: BadgeColorPalette): Pair<Color, Color> {
    return when (palette) {
        BadgeColorPalette.Primary -> scheme.primaryContainer to scheme.onPrimaryContainer
        BadgeColorPalette.Secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
        BadgeColorPalette.Tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        BadgeColorPalette.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        BadgeColorPalette.Inverse -> scheme.inverseSurface to scheme.inverseOnSurface
    }
}

/**
 * 基于 seedColor 生成文字色（用于自定义场景）
 */
@Composable
fun MonetIconBadgeTextColor(seedColor: Color): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = LocalAppIsDark.current
    
    // 找到最接近的语义色
    return when {
        isHueClose(seedColor, scheme.primary) -> scheme.onPrimaryContainer
        isHueClose(seedColor, scheme.secondary) -> scheme.onSecondaryContainer
        isHueClose(seedColor, scheme.tertiary) -> scheme.onTertiaryContainer
        else -> scheme.onSurfaceVariant
    }
}

@Composable
private fun isHueClose(color: Color, target: Color): Boolean {
    val colorHue = com.google.android.material.color.utilities.Hct.fromInt(color.toArgb()).hue
    val targetHue = com.google.android.material.color.utilities.Hct.fromInt(target.toArgb()).hue
    val diff = kotlin.math.abs(colorHue - targetHue)
    return diff < 30 || diff > 330  // 30° 范围内视为同色相
}
