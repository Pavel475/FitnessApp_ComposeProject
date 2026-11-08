package com.example.fitnessapp_composeproject.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessapp_composeproject.domain.model.Exercise

@Composable
fun ExpandableExerciseCard(
    exercise: Exercise,
    targetReps: Int,
    calculatedDuration: Int,
    onRepsChanged: (Int) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit // <-- Добавили callback для удаления
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Цель: $targetReps пов. • $calculatedDuration сек",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Правая часть: Корзина (перед стрелочкой) и стрелочка раскрытия
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Кнопка удаления
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить упражнение",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }

                    // Стрелочка раскрытия
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Раскрыть",
                            modifier = Modifier.rotate(if (expanded) 180f else 0f)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Регулировка повторений с кнопками -1, +1 и Сброс
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Повторения:", fontWeight = FontWeight.Medium)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppButton(
                                text = "-1",
                                onClick = { if (targetReps > 1) onRepsChanged(targetReps - 1) },
                                style = ButtonStyle.OUTLINED,
                                size = ButtonSize.MEDIUM,
                                contentColor = Color(0xFFFBFDD8),
                                containerColor = Color(0xBE3182A8)
                            )
                            AppButton(
                                text = "+1",
                                onClick = { onRepsChanged(targetReps + 1) },
                                style = ButtonStyle.OUTLINED,
                                size = ButtonSize.MEDIUM,
                                contentColor = Color(0xFFFBFDD8),
                                containerColor = Color(0xABE50EC8)
                            )
                            AppButton(
                                text = "Сброс",
                                onClick = onReset,
                                style = ButtonStyle.OUTLINED,
                                size = ButtonSize.MEDIUM,
                                contentColor = Color(0xFF1D94EF)
                            )
                        }
                    }
                }
            }
        }
    }
}