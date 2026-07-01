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
import com.google.android.material.color.utilities.Hct

/**
 * Badge 色相来源
 * - Primary/Secondary/Tertiary: 从 MaterialTheme 对应色提取色相
 * - Error: 固定红色色相
 * - Neutral: 中性灰色
 * - Inverse: 反色
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

/**
 * 基于 Color 提取 HCT 色相，再生成 M3 标准容器/文字色
 */
@Composable
fun MonetIconBadgeTextColor(seedColor: Color): Color {
    val hue = Hct.fromInt(seedColor.value.toInt()).hue
    val isDark = LocalAppIsDark.current
    val tone = if (isDark) 95.0 else 10.0
    return Color(Hct.from(hue, 70.0, tone).toInt())
}

@Composable
private fun badgePaletteColors(palette: BadgeColorPalette): Pair<Color, Color> {
    val isDark = LocalAppIsDark.current
    val scheme = MaterialTheme.colorScheme

    return when (palette) {
        BadgeColorPalette.Primary -> {
            val hue = Hct.fromInt(scheme.primary.value.toInt()).hue
            hctPair(hue, isDark)
        }
        BadgeColorPalette.Secondary -> {
            val hue = Hct.fromInt(scheme.secondary.value.toInt()).hue
            hctPair(hue, isDark)
        }
        BadgeColorPalette.Tertiary -> {
            val hue = Hct.fromInt(scheme.tertiary.value.toInt()).hue
            hctPair(hue, isDark)
        }
        BadgeColorPalette.Neutral -> {
            // Neutral: 中性灰，无彩色
            scheme.surfaceVariant to scheme.onSurfaceVariant
        }
        BadgeColorPalette.Inverse -> {
            // Inverse: 反色
            scheme.inverseSurface to scheme.inverseOnSurface
        }
    }
}

/**
 * HCT 颜色对: container=容器色  content=文字色
 */
private fun hctPair(hue: Double, isDark: Boolean): Pair<Color, Color> {
    val containerTone = if (isDark) 30.0 else 90.0
    val contentTone = if (isDark) 95.0 else 10.0
    val container = Color(Hct.from(hue, 70.0, containerTone).toInt())
    val content = Color(Hct.from(hue, 70.0, contentTone).toInt())
    return container to content
}

// =========================================================================
// 旧代码兼容 (rgbToHsl/hslToColor)
// =========================================================================

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
