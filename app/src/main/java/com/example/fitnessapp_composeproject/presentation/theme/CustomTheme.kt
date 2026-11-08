package com.example.fitnessapp_composeproject.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF0288D1),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE3F2FD)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF64B5F6),
    surface = Color(0xFF121212),
    background = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2C2C)
)

val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)

@Composable
fun FitnessAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}