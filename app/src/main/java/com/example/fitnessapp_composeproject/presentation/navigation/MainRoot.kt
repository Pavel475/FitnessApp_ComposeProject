package com.example.fitnessapp_composeproject.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp_composeproject.data.Permission
import com.example.fitnessapp_composeproject.presentation.viewmodel.AccessViewModel
import com.example.fitnessapp_composeproject.presentation.screens.*
import com.example.fitnessapp_composeproject.presentation.viewmodel.AuthViewModel

@Composable
fun MainRoot(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    authViewModel: AuthViewModel,
    accessViewModel: AccessViewModel
) {
    val navController = rememberNavController()
    val accessContext by accessViewModel.accessContext.collectAsState()

    // Формируем список вкладок динамически на основе прав (RBAC)
    val items = buildList {
        add(Triple(Screen.Workout, Icons.Default.FitnessCenter, "Тренировка"))
        add(Triple(Screen.Pass, Icons.Default.Badge, "Пропуск"))
        add(Triple(Screen.Scan, Icons.Default.QrCodeScanner, "Сканер"))

        // Вкладка журнала доступа видна ТОЛЬКО роли ADMIN (scope audit:read)
        if (accessContext.can(Permission.READ_AUDIT)) {
            add(Triple(Screen.AdminAudit, Icons.Default.AdminPanelSettings, "Журнал"))
        }

        add(Triple(Screen.Profile, Icons.Default.Person, "Профиль"))
        add(Triple(Screen.Settings, Icons.Default.Settings, "Настройки"))
    }

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
            composable(Screen.Pass.route) { PassScreen(accessViewModel = accessViewModel) }
            composable(Screen.Scan.route) { ScanScreen(accessViewModel = accessViewModel) }
            composable(Screen.AdminAudit.route) { AdminScreen(accessViewModel = accessViewModel) }
            composable(Screen.Profile.route) { ProfileScreen(authViewModel = authViewModel) }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = onThemeChanged
                )
            }
        }
    }
}