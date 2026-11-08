package com.example.fitnessapp_composeproject.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 1. Описываем кастомные цвета, выходящие за рамки стандартного ColorScheme
data class ExtendedColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val badgeBackground: Color
)

// 2. Создаем дефолтные значения для светлой/темной темы
val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        gradientStart = Color(0xFF218CA9),
        gradientEnd = Color(0xFFB3B4F8),
        badgeBackground = Color(0xFF3F51B5)
    )
}

val DarkExtendedColors = ExtendedColors(
    gradientStart = Color(0xFF2C3E50),
    gradientEnd = Color(0xFF4CA1AF),
    badgeBackground = Color(0xFF5C6BC0)
)