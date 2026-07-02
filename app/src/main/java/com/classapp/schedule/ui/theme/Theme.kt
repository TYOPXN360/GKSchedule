package com.classapp.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val LocalAppIsDark = compositionLocalOf { false }

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColorScheme = lightColorScheme(
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

private val DarkColorScheme = darkColorScheme(
    surface = Color(0xFF161A1F),
    onSurface = Color(0xFFE6E0E9),
    surfaceContainerHigh = Color(0xFF282C34),
    surfaceContainerHighest = Color(0xFF30343C),
    surfaceContainer = Color(0xFF1E2228),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

@Composable
fun ClassAppTheme(
    darkTheme: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (darkTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark -> {
            val dynamicDark = dynamicDarkColorScheme(context)
            val pColor = dynamicDark.primary

            // 轨道一：底部大盘底色（surface）
            val wSurface = 1f / 12f
            val sR = (8f * (1f - wSurface) + pColor.red * 255f * wSurface).toInt().coerceIn(0, 255)
            val sG = (10f * (1f - wSurface) + pColor.green * 255f * wSurface).toInt().coerceIn(0, 255)
            val sB = (12f * (1f - wSurface) + pColor.blue * 255f * wSurface).toInt().coerceIn(0, 255)
            val dynamicSurface = Color(sR, sG, sB)

            // 轨道二：卡片框色（surfaceContainerHigh）
            val wCard = 1f / 6f
            val cR = (13f * (1f - wCard) + pColor.red * 255f * wCard).toInt().coerceIn(0, 255)
            val cG = (13f * (1f - wCard) + pColor.green * 255f * wCard).toInt().coerceIn(0, 255)
            val cB = (15f * (1f - wCard) + pColor.blue * 255f * wCard).toInt().coerceIn(0, 255)
            val dynamicContainerHigh = Color(cR, cG, cB)

            // 平滑层级
            val dynamicContainer = Color((sR + cR) / 2, (sG + cG) / 2, (sB + cB) / 2)
            val dynamicContainerHighest = Color((cR + 8).coerceAtMost(255), (cG + 8).coerceAtMost(255), (cB + 8).coerceAtMost(255))

            dynamicDark.copy(
                surface = dynamicSurface,
                surfaceContainer = dynamicContainer,
                surfaceContainerHigh = dynamicContainerHigh,
                surfaceContainerHighest = dynamicContainerHighest
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isDark -> {
            val dynamicLight = dynamicLightColorScheme(context)
            if (dynamicLight.surface.luminance() < 0.85f) LightColorScheme else dynamicLight
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAppIsDark provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
