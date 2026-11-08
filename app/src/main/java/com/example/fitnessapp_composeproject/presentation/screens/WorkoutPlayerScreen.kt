package com.example.fitnessapp_composeproject.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fitnessapp_composeproject.domain.model.Exercise
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram
import com.example.fitnessapp_composeproject.presentation.components.ExpandableExerciseCard
import com.example.fitnessapp_composeproject.presentation.components.AppButton
import com.example.fitnessapp_composeproject.presentation.components.ButtonSize
import com.example.fitnessapp_composeproject.presentation.components.ButtonStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlayerScreen(
    workout: WorkoutProgram,
    onBackClick: () -> Unit
) {
    // Используем изменяемое состояние для списка упражнений данной тренировки
    var currentExercises by remember { mutableStateOf(workout.exercises) }

    var customRepsMap by remember {
        mutableStateOf(workout.exercises.associate { it.id to it.reps })
    }

    // Состояние для отображения диалога выбора нового упражнения
    var showAddDialog by remember { mutableStateOf(false) }

    // Состояние переключения в режим активной тренировки с таймером
    var isWorkoutStarted by remember { mutableStateOf(false) }

    if (isWorkoutStarted) {
        // Передаем в активную тренировку обновленный список упражнений
        val updatedWorkout = workout.copy(exercises = currentExercises)
        ActiveWorkoutScreen(
            workout = updatedWorkout,
            onFinishWorkout = { isWorkoutStarted = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(workout.title) },
                    navigationIcon = {
                        AppButton(
                            text = "Назад",
                            onClick = onBackClick,
                            style = ButtonStyle.OUTLINED,
                            size = ButtonSize.SMALL,
                            icon = Icons.Default.ArrowBack,
                            contentColor = Color(0xFFFFFFFF),
                            containerColor = Color(0xABA50EE5),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AppButton(
                            text = "Начать тренировку",
                            onClick = { isWorkoutStarted = true },
                            style = ButtonStyle.PRIMARY,
                            size = ButtonSize.LARGE,
                            modifier = Modifier.fillMaxWidth(),
                            contentColor = Color(0xFFC8FFEC),
                            containerColor = Color(0xFF5A7EFF)
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Базовая длительность: ${workout.durationMinutes} мин",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Настройте план упражнений под себя:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Выводим список упражнений с возможностью удаления каждого
                items(currentExercises, key = { it.id }) { exercise ->
                    val currentReps = customRepsMap[exercise.id] ?: exercise.reps

                    val secondsPerRep = if (exercise.reps > 0) exercise.durationSeconds.toFloat() / exercise.reps else 0f
                    val calculatedDuration = (secondsPerRep * currentReps).toInt().coerceAtLeast(5)

                    ExpandableExerciseCard(
                        exercise = exercise,
                        targetReps = currentReps,
                        calculatedDuration = calculatedDuration,
                        onRepsChanged = { newReps ->
                            customRepsMap = customRepsMap.toMutableMap().apply {
                                this[exercise.id] = newReps
                            }
                        },
                        onReset = {
                            customRepsMap = customRepsMap.toMutableMap().apply {
                                this[exercise.id] = exercise.reps
                            }
                        },
                        onDelete = {
                            currentExercises = currentExercises.filter { it.id != exercise.id }
                        }
                    )
                }

                // Кнопка с плюсом (+) для добавления нового упражнения
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить упражнение")
                    }
                }
            }
        }
    }

    // Диалог выбора дополнительных упражнений («Велосипед», «Качели»)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить упражнение") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val bikeExercise = Exercise(
                                id = System.currentTimeMillis().toInt(),
                                name = "Велосипед",
                                imageUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500",
                                durationSeconds = 40,
                                reps = 15,
                                description = "Интенсивное кардио-упражнение"
                            )
                            currentExercises = currentExercises + bikeExercise
                            showAddDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Велосипед")
                    }

                    Button(
                        onClick = {
                            val swingExercise = Exercise(
                                id = System.currentTimeMillis().toInt(),
                                name = "Качели",
                                imageUrl = "https://images.unsplash.com/photo-1434682881907-b43d6017bffd?w=500",
                                durationSeconds = 30,
                                reps = 10,
                                description = "Динамическое упражнение на баланс"
                            )
                            currentExercises = currentExercises + swingExercise
                            showAddDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Качели")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}