package com.example.fitnessapp_composeproject.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram
import com.example.fitnessapp_composeproject.presentation.components.AdaptiveWorkoutCard
import com.example.fitnessapp_composeproject.presentation.viewmodel.WorkoutUiState
import com.example.fitnessapp_composeproject.presentation.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = viewModel()
) {
    val state = viewModel.uiState
    val isRefreshing = viewModel.isRefreshing

    var selectedWorkout by remember { mutableStateOf<WorkoutProgram?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (selectedWorkout != null) {
            WorkoutPlayerScreen(
                workout = selectedWorkout!!,
                onBackClick = { selectedWorkout = null }
            )
        } else {
            // Состояние для управления жестом свайпа вниз
            val refreshState = rememberPullToRefreshState()

            // Официальный контейнер Material 3 для жеста Pull-to-Refresh
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadWorkouts(isRefresh = true) },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when (state) {
                    is WorkoutUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is WorkoutUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Программы тренировок",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(state.workouts) { workout ->
                                AdaptiveWorkoutCard(
                                    title = workout.title,
                                    durationMinutes = workout.durationMinutes,
                                    exerciseCount = workout.exerciseCount,
                                    imageUrl = workout.imageUrl,
                                    onClick = {
                                        selectedWorkout = workout
                                    }
                                )
                            }
                        }
                    }
                    is WorkoutUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.loadWorkouts() }) {
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}