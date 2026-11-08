package com.example.fitnessapp_composeproject.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class FitnessReportDto(
    val schemaVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val athleteName: String = "Павел Фомичёв",
    val heightCm: Int = 178,
    val weightKg: Int = 70,
    val goal: String = "Набор мышечной массы",
    val completedWorkoutsCount: Int = 12,
    val totalCalories: Int = 3450
)