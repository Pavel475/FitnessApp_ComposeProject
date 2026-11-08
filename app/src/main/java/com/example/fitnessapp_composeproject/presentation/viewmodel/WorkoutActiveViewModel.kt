package com.example.fitnessapp_composeproject.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutActiveViewModel : ViewModel() {
    var currentExerciseIndex by mutableStateOf(0)
        private set
    var timeLeft by mutableStateOf(0)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var isResting by mutableStateOf(false)
        private set
    var isWorkoutFinished by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    fun startWorkout(workout: WorkoutProgram) {
        currentExerciseIndex = 0
        isPaused = false
        isResting = false
        isWorkoutFinished = false

        // Устанавливаем длительность первого упражнения из сети
        val firstExercise = workout.exercises.firstOrNull()
        timeLeft = firstExercise?.durationSeconds ?: 45

        runTimer(workout)
    }

    private fun runTimer(workout: WorkoutProgram) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (!isPaused && !isWorkoutFinished) {
                    if (timeLeft > 0) {
                        timeLeft--
                    } else {
                        if (!isResting) {
                            // Переходим в режим отдыха
                            isResting = true
                            timeLeft = 15 // Отдых 15 секунд
                        } else {
                            // Проверяем, есть ли еще упражнения
                            if (currentExerciseIndex < workout.exercises.size - 1) {
                                currentExerciseIndex++
                                isResting = false
                                // Берем длительность следующего упражнения из сети
                                timeLeft = workout.exercises[currentExerciseIndex].durationSeconds
                            } else {
                                // Тренировка полностью завершена
                                isWorkoutFinished = true
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun nextExercise(workout: WorkoutProgram) {
        if (currentExerciseIndex < workout.exercises.size - 1) {
            currentExerciseIndex++
            isResting = false
            timeLeft = workout.exercises[currentExerciseIndex].durationSeconds
        } else {
            isWorkoutFinished = true
        }
    }

    fun previousExercise(workout: WorkoutProgram) {
        if (currentExerciseIndex > 0) {
            currentExerciseIndex--
            isResting = false
            timeLeft = workout.exercises[currentExerciseIndex].durationSeconds
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}