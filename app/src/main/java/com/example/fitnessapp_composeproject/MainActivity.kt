package com.example.fitnessapp_composeproject

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.fitnessapp_composeproject.presentation.viewmodel.AccessViewModel
import com.example.fitnessapp_composeproject.presentation.navigation.MainRoot
import com.example.fitnessapp_composeproject.presentation.screens.*
import com.example.fitnessapp_composeproject.presentation.theme.FitnessAppTheme
import com.example.fitnessapp_composeproject.presentation.viewmodel.AuthUiState
import com.example.fitnessapp_composeproject.presentation.viewmodel.AuthViewModel

class MainActivity : FragmentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val accessViewModel: AccessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            val uiState by authViewModel.uiState.collectAsState()
            val currentAccess by authViewModel.accessContext.collectAsState()

            // Синхронизируем роль и JWT scopes между AuthViewModel и AccessViewModel
            LaunchedEffect(currentAccess) {
                accessViewModel.setAccessContext(currentAccess)
            }

            var isRecoveryScreenMode by remember { mutableStateOf(false) }

            FitnessAppTheme(darkTheme = darkTheme) {
                Surface {
                    if (isRecoveryScreenMode) {
                        RecoveryScreen(
                            viewModel = authViewModel,
                            onBack = { isRecoveryScreenMode = false }
                        )
                    } else {
                        when (val state = uiState) {
                            is AuthUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is AuthUiState.Login -> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onNavigateToRecovery = { isRecoveryScreenMode = true }
                                )
                            }
                            is AuthUiState.Locked -> {
                                LockedScreen(remainingMillis = state.remainingMillis)
                            }
                            is AuthUiState.PinSetup -> {
                                PinSetupScreen(
                                    viewModel = authViewModel,
                                    recoveryCode = state.generatedCode
                                )
                            }
                            is AuthUiState.PinEntry -> {
                                PinEntryScreen(viewModel = authViewModel)
                            }
                            is AuthUiState.Authenticated, is AuthUiState.StepUpRequired -> {
                                MainRoot(
                                    isDarkTheme = darkTheme,
                                    onThemeChanged = { darkTheme = it },
                                    authViewModel = authViewModel,
                                    accessViewModel = accessViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}