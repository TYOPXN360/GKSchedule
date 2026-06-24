package com.classapp.schedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// MD3E Expressive Shapes
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
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

    // Always use dynamic color (Monet) — minSdk=31 guarantees support
    val context = LocalContext.current
    val colorScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
