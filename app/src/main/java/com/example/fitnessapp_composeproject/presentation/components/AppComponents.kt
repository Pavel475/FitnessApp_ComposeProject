package com.example.fitnessapp_composeproject.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonStatus { IDLE, LOADING, SUCCESS, ERROR }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .semantics { contentDescription = text }
    ) {
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
fun StatusButton(
    status: ButtonStatus,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (status) {
        ButtonStatus.IDLE -> MaterialTheme.colorScheme.primary
        ButtonStatus.LOADING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        ButtonStatus.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ButtonStatus.ERROR -> MaterialTheme.colorScheme.error
    }

    Button(
        onClick = onClick,
        enabled = status == ButtonStatus.IDLE,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .semantics {
                contentDescription = when (status) {
                    ButtonStatus.LOADING -> "Загрузка..."
                    ButtonStatus.SUCCESS -> "Успешно выполненное действие"
                    ButtonStatus.ERROR -> "Ошибка выполнения"
                    else -> text
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when (status) {
                ButtonStatus.LOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                ButtonStatus.SUCCESS -> Icon(Icons.Default.Check, contentDescription = null)
                ButtonStatus.ERROR -> Icon(Icons.Default.Error, contentDescription = null)
                ButtonStatus.IDLE -> Text(text = text, fontSize = 16.sp)
            }
            if (status != ButtonStatus.IDLE) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (status) {
                        ButtonStatus.LOADING -> "Загрузка..."
                        ButtonStatus.SUCCESS -> "Готово"
                        ButtonStatus.ERROR -> "Ошибка"
                        else -> ""
                    }
                )
            }
        }
    }
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label: $value" }
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}