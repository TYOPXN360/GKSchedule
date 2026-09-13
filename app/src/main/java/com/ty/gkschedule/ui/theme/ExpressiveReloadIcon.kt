package com.ty.gkschedule.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ponytail: MD3E花瓣形变刷新图标——Canvas原生4瓣+动态取色，无外部依赖
@Composable
fun ExpressiveReloadIcon(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_reload")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph"
    )

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorTertiary = MaterialTheme.colorScheme.tertiary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorContainer = MaterialTheme.colorScheme.primaryContainer

    val petalColors = listOf(
        colorPrimary.copy(alpha = 0.88f),
        colorTertiary.copy(alpha = 0.88f),
        colorSecondary.copy(alpha = 0.88f),
        colorContainer.copy(alpha = 0.92f)
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val currentRotation = if (isRefreshing) rotation else 0f
        val currentMorph = if (isRefreshing) morphProgress else 0.85f

        rotate(currentRotation, pivot = center) {
            val petalCount = 4
            val angleStep = 180f / petalCount

            val petalLength = this.size.width * currentMorph * 0.95f
            val petalWidth = this.size.width * (0.32f + (1f - currentMorph) * 0.25f)
            val corner = CornerRadius(petalWidth / 2f, petalWidth / 2f)

            for (i in 0 until petalCount) {
                rotate(degrees = i * angleStep, pivot = center) {
                    drawRoundRect(
                        color = petalColors[i % petalColors.size],
                        topLeft = Offset(
                            x = center.x - petalWidth / 2f,
                            y = center.y - petalLength / 2f
                        ),
                        size = Size(petalWidth, petalLength),
                        cornerRadius = corner,
                        blendMode = BlendMode.SrcOver
                    )
                }
            }

            drawCircle(
                color = Color.White.copy(alpha = if (isRefreshing) 0.45f else 0.30f),
                radius = petalWidth * 0.35f,
                center = center
            )
        }
    }
}
