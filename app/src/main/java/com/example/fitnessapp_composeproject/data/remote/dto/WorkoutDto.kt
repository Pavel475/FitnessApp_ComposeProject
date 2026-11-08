package com.example.fitnessapp_composeproject.data.remote.dto

import com.google.gson.annotations.SerializedName

// Модель тренировочной программы из JSON
data class WorkoutDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("exerciseCount") val exerciseCount: Int,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("exercises") val exercises: List<ExerciseDto>
)

// Модель упражнения из JSON
data class ExerciseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("durationSeconds") val durationSeconds: Int,
)