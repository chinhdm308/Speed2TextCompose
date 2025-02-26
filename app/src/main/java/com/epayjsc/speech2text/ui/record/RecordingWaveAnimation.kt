package com.epayjsc.speech2text.ui.record

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordingWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "RecordingWaveAnimation")

    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave1"
    )

    val wave2 by infiniteTransition.animateFloat(
        initialValue = 30f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave2"
    )

    Canvas(modifier = Modifier.size(150.dp)) {
        drawRoundRect(
            color = Color.Red.copy(alpha = 0.7f),
            size = Size(wave1, 100f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = Color.Red.copy(alpha = 0.5f),
            size = Size(wave2, 80f),
            cornerRadius = CornerRadius(12f, 12f)
        )
    }
}
