package com.example.fitnessapp_composeproject.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Стили визуального оформления кнопок
enum class ButtonStyle {
    PRIMARY,   // Заполненная кнопка с основным акцентным цветом
    SECONDARY, // Тонированная кнопка с вторичным фоном
    OUTLINED   // Кнопка с прозрачным фоном и цветной обводкой (страйком)
}

// Размеры кнопок (отступы и размер шрифта)
enum class ButtonSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    icon: ImageVector? = null, // Опциональная иконка
    containerColor: Color? = null, // Кастомный цвет фона
    contentColor: Color? = null  // Кастомный цвет текста и иконки
) {
    // 1. Настройка размеров: высота, размер шрифта и внутренние отступы
    val (height, fontSize, padding) = when (size) {
        ButtonSize.SMALL -> Triple(36.dp, 14.sp, PaddingValues(horizontal = 10.dp, vertical = 4.dp))
        ButtonSize.MEDIUM -> Triple(46.dp, 16.sp, PaddingValues(horizontal = 16.dp, vertical = 8.dp))
        ButtonSize.LARGE -> Triple(56.dp, 18.sp, PaddingValues(horizontal = 24.dp, vertical = 12.dp))
    }

    val shape = RoundedCornerShape(14.dp)

    // Контент кнопки (текст + опциональная иконка)
    val buttonContent: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(text = text, fontSize = fontSize)
    }

    // 2. Рендеринг в зависимости от выбранного стиля с поддержкой кастомных цветов
    when (style) {
        ButtonStyle.PRIMARY -> {
            val colors = ButtonDefaults.buttonColors(
                containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
                contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary
            )
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = padding,
                modifier = modifier.height(height),
                content = buttonContent
            )
        }
        ButtonStyle.SECONDARY -> {
            val colors = ButtonDefaults.buttonColors(
                containerColor = containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
                contentColor = contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer
            )
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                colors = colors,
                contentPadding = padding,
                modifier = modifier.height(height),
                content = buttonContent
            )
        }
        ButtonStyle.OUTLINED -> {
            val borderColor = if (containerColor != null) containerColor else MaterialTheme.colorScheme.primary
            val colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor ?: MaterialTheme.colorScheme.primary,
                containerColor = containerColor ?: Color.Transparent
            )
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                border = BorderStroke(1.5.dp, borderColor),
                colors = colors,
                contentPadding = padding,
                modifier = modifier.height(height),
                content = buttonContent
            )
        }
    }
}