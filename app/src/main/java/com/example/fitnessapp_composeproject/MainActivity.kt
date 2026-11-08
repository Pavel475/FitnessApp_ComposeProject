package com.example.fitnessapp_composeproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.example.fitnessapp_composeproject.presentation.navigation.MainRoot
import com.example.fitnessapp_composeproject.presentation.theme.FitnessAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Состояние темы с возможностью управления из UI (ЛР 3, 6)
            var darkTheme by remember { mutableStateOf(false) }

            FitnessAppTheme(darkTheme = darkTheme) {
                Surface {
                    MainRoot(
                        isDarkTheme = darkTheme,
                        onThemeChanged = { darkTheme = it }
                    )
                }
            }
        }
    }
}