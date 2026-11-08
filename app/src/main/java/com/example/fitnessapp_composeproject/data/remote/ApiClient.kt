package com.example.fitnessapp_composeproject.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // ⚠️ СЮДА ВСТАВЬ СВОЙ БАЗОВЫЙ URL ОТ MOCKAPI.io (например: "https://65a123...mockapi.io/api/")
    private const val BASE_URL = "https://6aeb53b0ee39618d7bf83243.mockapi.io/"

    // Логгер для отслеживания сетевых запросов в Logcat (удобно для отладки)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}