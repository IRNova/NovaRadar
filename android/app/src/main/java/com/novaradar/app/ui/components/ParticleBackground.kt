package com.novaradar.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    var x: Float, var y: Float,
    var speedY: Float,
    var radius: Float,
    var alpha: Float
)

@Composable
fun ParticleBackground(isLight: Boolean, modifier: Modifier = Modifier) {
    val particles = remember {
        List(40) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speedY = Random.nextFloat() * 0.003f + 0.001f,
                radius = Random.nextFloat() * 2f + 0.5f,
                alpha = Random.nextFloat() * 0.2f + 0.05f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "tick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            p.y -= p.speedY * h / 1000f
            if (p.y < 0f) {
                p.y = 1f
                p.x = Random.nextFloat()
            }

            val color = if (isLight)
                Color(0xFF1D4ED8).copy(alpha = p.alpha * 0.5f)
            else
                Color(0xFF3B82F6).copy(alpha = p.alpha * 0.7f)

            drawCircle(color = color, radius = p.radius * 2f, center = Offset(p.x * w, p.y * h))
        }
    }
}
