package com.example.fitnessapp_composeproject.data.remote.dto

import com.example.fitnessapp_composeproject.domain.model.Exercise
import com.example.fitnessapp_composeproject.domain.model.WorkoutProgram

// Маппер для WorkoutDto -> WorkoutProgram
fun WorkoutDto.toDomain(): WorkoutProgram {
    return WorkoutProgram(
        id = id,
        title = title,
        durationMinutes = durationMinutes,
        exerciseCount = exerciseCount,
        imageUrl = imageUrl,
        exercises = exercises.map { it.toDomain() }
    )
}

// Маппер для ExerciseDto -> Exercise
fun ExerciseDto.toDomain(): Exercise {
    return Exercise(
        id = id,
        name = name,
        reps = 15, // Стандартное значение повторов для плана, если с сервера приходит только длительность
        durationSeconds = durationSeconds,
        imageUrl = imageUrl,
        description = "Упражнение комплекса: $name"
    )
}