package com.example.fitnessapp_composeproject.data.remote

import com.example.fitnessapp_composeproject.data.remote.dto.WorkoutDto
import retrofit2.http.GET

interface ApiService {

    // Этот метод говорит Retrofit: сделай GET-запрос по адресу <базовый_адрес>/workouts
    @GET("workouts")
    suspend fun getWorkouts(): List<WorkoutDto>
}