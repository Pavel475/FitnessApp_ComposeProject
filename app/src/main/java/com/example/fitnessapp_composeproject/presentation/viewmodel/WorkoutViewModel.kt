package com.example.fitnessapp_composeproject.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp_composeproject.data.remote.ApiClient
import com.example.fitnessapp_composeproject.data.remote.dto.toDomain
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface WorkoutUiState {
    object Loading : WorkoutUiState
    data class Success(val workouts: List<WorkoutProgram>) : WorkoutUiState
    data class Error(val message: String) : WorkoutUiState
}

class WorkoutViewModel : ViewModel() {
    var uiState: WorkoutUiState by mutableStateOf(WorkoutUiState.Loading)
        private set

    // Флаг для pull-to-refresh, чтобы показывать индикатор без скрытия старого списка
    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    // Кэш в памяти: храним уже загруженный список, чтобы повторный вход не дергал сеть впустую
    private var cachedWorkouts: List<WorkoutProgram>? = null

    // Счётчик для имитации первых двух сетевых сбоев
    private var errorAttemptsCount = 0

    init {
        loadWorkouts()
    }

    fun loadWorkouts(isRefresh: Boolean = false) {
        viewModelScope.launch {
            // Если в памяти уже есть данные и это ручное обновление (pull-to-refresh),
            // включаем иконку обновления, но НЕ сбрасываем UI в состояние Loading.
            if (isRefresh && cachedWorkouts != null) {
                isRefreshing = true
            } else if (cachedWorkouts == null) {
                uiState = WorkoutUiState.Loading
                delay(2000) // Имитация задержки только при первом холодном старте
            }

            // Имитация первых двух ошибок сети
            if (errorAttemptsCount < 2) {
                errorAttemptsCount++
                isRefreshing = false
                if (cachedWorkouts == null) {
                    uiState = WorkoutUiState.Error("Сетевая ошибка: не удалось подключиться к серверу (Попытка $errorAttemptsCount из 2)")
                }
                return@launch
            }

            try {
                val workoutDtos = ApiClient.apiService.getWorkouts()
                val workouts = workoutDtos.map { it.toDomain() }

                // Сохраняем в кэш памяти
                cachedWorkouts = workouts
                uiState = WorkoutUiState.Success(workouts)
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Network error", e)
                if (cachedWorkouts == null) {
                    uiState = WorkoutUiState.Error("Ошибка загрузки данных: ${e.localizedMessage}")
                }
            } finally {
                isRefreshing = false
            }
        }
    }
}