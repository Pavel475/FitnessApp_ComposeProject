package com.example.fitnessapp_composeproject.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    object Workout : Screen("workout", "Тренировка")
    object Profile : Screen("profile", "Профиль")
    object Settings : Screen("settings", "Настройки")
}