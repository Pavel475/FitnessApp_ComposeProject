package com.example.fitnessapp_composeproject.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp_composeproject.presentation.screens.ProfileScreen
import com.example.fitnessapp_composeproject.presentation.screens.SettingsScreen
import com.example.fitnessapp_composeproject.presentation.screens.WorkoutScreen

@Composable
fun MainRoot(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        Triple(Screen.Workout, Icons.Default.FitnessCenter, "Тренировка"),
        Triple(Screen.Profile, Icons.Default.Person, "Профиль"),
        Triple(Screen.Settings, Icons.Default.Settings, "Настройки")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Workout.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Workout.route) { WorkoutScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = onThemeChanged
                )
            }
        }
    }
}