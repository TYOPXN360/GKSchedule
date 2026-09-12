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
 * Badge 色板：LineageOS Settings 首页同款 10 套固定色
 * fg 恒 T30，bg 浅 T90/深 T80；灰走低 chroma
 */
enum class BadgeColorPalette {
    BlueVariant, Blue, Pink, Orange, Yellow, Green, Grey, Cyan, Red, Purple,
    // 旧名保留，映射到近似色
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
 * LineageOS 10 套固定色；旧 Primary/Secondary/Tertiary 映射到近似色
 */
@Composable
private fun badgePaletteColors(scheme: androidx.compose.material3.ColorScheme, palette: BadgeColorPalette): Pair<Color, Color> {
    val fixed = fixedBadgeColors(palette)
    if (fixed != null) return fixed
    return when (palette) {
        BadgeColorPalette.Primary -> fixedBadgeColors(BadgeColorPalette.Blue)!!
        BadgeColorPalette.Secondary -> fixedBadgeColors(BadgeColorPalette.Cyan)!!
        BadgeColorPalette.Tertiary -> fixedBadgeColors(BadgeColorPalette.Purple)!!
        BadgeColorPalette.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        else -> scheme.inverseSurface to scheme.inverseOnSurface
    }
}

// ponytail: HCT现场算 T30/T90(深T80)，不建xml表；grey低chroma
@Composable
private fun fixedBadgeColors(palette: BadgeColorPalette): Pair<Color, Color>? {
    val isDark = LocalAppIsDark.current
    val hue = when (palette) {
        BadgeColorPalette.BlueVariant -> 260.0
        BadgeColorPalette.Blue -> 255.0
        BadgeColorPalette.Pink -> 340.0
        BadgeColorPalette.Orange -> 70.0
        BadgeColorPalette.Yellow -> 105.0
        BadgeColorPalette.Green -> 150.0
        BadgeColorPalette.Grey -> 0.0
        BadgeColorPalette.Cyan -> 200.0
        BadgeColorPalette.Red -> 15.0
        BadgeColorPalette.Purple -> 300.0
        else -> return null
    }
    val chroma = if (palette == BadgeColorPalette.Grey) 12.0 else null
    val fg = com.google.android.material.color.utilities.Hct.from(hue, chroma ?: 48.0, 30.0).toInt()
    val bg = com.google.android.material.color.utilities.Hct.from(hue, chroma ?: 32.0, if (isDark) 80.0 else 90.0).toInt()
    return Color(bg) to Color(fg)
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
