package com.example.fitnessapp_composeproject.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    val x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun ConfettiOverlay(onAnimationFinished: () -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)

    // Генерируем статичный набор частиц один раз
    val particles = remember {
        List(70) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f, // Начинают выше экрана
                speed = Random.nextFloat() * 0.005f + 0.003f,
                size = Random.nextFloat() * 12f + 6f,
                color = colors.random()
            )
        }
    }

    // Длительность анимации ровно 4 секунды
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ConfettiProgress"
    )

    // Таймер на автозакрытие экрана через 4 секунды
    LaunchedEffect(Unit) {
        delay(4000)
        onAnimationFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            // Вычисляем текущую позицию по вертикали
            val currentY = (particle.y + progress * (1.5f + particle.speed * 50f)) * height
            val currentX = particle.x * width

            // Рисуем частицу (квадратное конфетти)
            drawRect(
                color = particle.color,
                topLeft = androidx.compose.ui.geometry.Offset(currentX, currentY % height),
                size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.6f)
            )
        }
    }
}