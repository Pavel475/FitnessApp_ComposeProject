package com.example.fitnessapp_composeproject.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram
import com.example.fitnessapp_composeproject.presentation.components.ConfettiOverlay
import com.example.fitnessapp_composeproject.presentation.viewmodel.WorkoutActiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutProgram,
    onFinishWorkout: () -> Unit,
    viewModel: WorkoutActiveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val exerciseIndex = viewModel.currentExerciseIndex
    val currentExercise = workout.exercises.getOrNull(exerciseIndex) ?: workout.exercises.first()

    LaunchedEffect(Unit) {
        viewModel.startWorkout(workout)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (viewModel.isResting) "Отдых" else "Выполнение упражнения") },
                    actions = {
                        TextButton(onClick = onFinishWorkout) {
                            Text("Завершить", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Упражнение ${exerciseIndex + 1} из ${workout.exercises.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (viewModel.isResting) "Отдыхайте перед следующим шагом" else currentExercise.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Индивидуальная картинка конкретного упражнения из сети
                    AsyncImage(
                        model = currentExercise.imageUrl,
                        contentDescription = "Техника выполнения",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Рассчитываем прогресс кругового индикатора динамически относительно длительности упражнения
                    val maxTime = if (viewModel.isResting) 15f else currentExercise.durationSeconds.toFloat()
                    val progressValue = if (maxTime > 0) viewModel.timeLeft / maxTime else 0f

                    CircularProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                    )
                    Text(
                        text = "${viewModel.timeLeft}с",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousExercise(workout) },
                        enabled = exerciseIndex > 0
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Назад", modifier = Modifier.size(36.dp))
                    }

                    FloatingActionButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Пауза/Пуск",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextExercise(workout) },
                        enabled = exerciseIndex < workout.exercises.size
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Далее", modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        // Финальный экран завершения тренировки с конфетти
        AnimatedVisibility(
            visible = viewModel.isWorkoutFinished,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ConfettiOverlay(onAnimationFinished = {})

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🎉 Тренировка завершена!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Отличная работа! Вы успешно прошли все упражнения программы.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onFinishWorkout,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Вернуться к списку")
                        }
                    }
                }
            }
        }
    }
}