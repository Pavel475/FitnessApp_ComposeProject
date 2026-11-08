package com.example.fitnessapp_composeproject.domain.model

data class Exercise(
    val id: Int,
    val name: String,
    val imageUrl: String,          // Уникальная картинка упражнения
    val durationSeconds: Int,      // Время выполнения (основной параметр таймера)
    val reps: Int = 0,             // Дефолтное значение, если где-то потребуется по логике
    val description: String = "Динамичное упражнение для здоровья"
)

data class WorkoutProgram(
    val id: Int,
    val title: String,
    val durationMinutes: Int,
    val exerciseCount: Int,
    val imageUrl: String,          // Картинка программы тренировки
    val exercises: List<Exercise>
)