package com.example.fitnessapp_composeproject.presentation.viewmodel

import android.app.Application
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp_composeproject.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher

sealed class AuthUiState {
    object Loading : AuthUiState()
    object Login : AuthUiState()                     // 1. Вход по логину/паролю
    data class Locked(val remainingMillis: Long) : AuthUiState() // Блокировка
    data class PinSetup(val generatedCode: String) : AuthUiState() // 2. Установка PIN + показ recovery
    object PinEntry : AuthUiState()                 // 3. Быстрый вход по PIN / биометрии
    object Authenticated : AuthUiState()             // Успешный вход в приложение
    data class StepUpRequired(val reason: String) : AuthUiState() // Требуется step-up для профиля
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    val sessionManager = SessionManager()
    val authAudit = AuthAudit(context)
    val pinVault = PinVault(context)
    val authRepository = AuthRepository()
    val recoveryManager = RecoveryManager(context)

    private val _accessContext = MutableStateFlow(
        AccessContext(userId = "guest", role = Role.EMPLOYEE, scopes = emptySet())
    )
    val accessContext: StateFlow<AccessContext> = _accessContext.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Состояние попыток и ошибок
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Время последней успешной сильной аутентификации (для Step-up окна в 5 минут)
    private var lastAuthTimestamp = 0L
    private val STEP_UP_WINDOW_MS = 20_000L

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        if (!pinVault.isPinSet()) {
            _uiState.value = AuthUiState.Login
        } else if (sessionManager.isAlive()) {
            _uiState.value = AuthUiState.Authenticated
        } else {
            _uiState.value = AuthUiState.PinEntry
        }
    }

    // --- 1. Логин + Пароль ---
    fun login(user: String, pass: String) {
        viewModelScope.launch {
            when (val result = authRepository.login(user, pass)) {
                is AuthLoginResult.Success -> {
                    sessionManager.start(result.session)
                    _accessContext.value = result.accessContext // <-- Сохраняем роль и scopes!
                    // Сохраняем userId в SharedPreferences, чтобы при входе по PIN знать, кто вошел
                    context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putString("last_user_id", user).apply()

                    authAudit.logEvent(user, "LOGIN_OK", "Role: ${result.accessContext.role}, scopes: ${result.accessContext.scopes}")
                    lastAuthTimestamp = System.currentTimeMillis()

                    if (!pinVault.isPinSet()) {
                        // Если PIN еще не задан — генерируем recovery код и просим поставить PIN
                        val recoveryCode = recoveryManager.generateAndSaveRecoveryCode()
                        _uiState.value = AuthUiState.PinSetup(recoveryCode)
                    } else {
                        _uiState.value = AuthUiState.Authenticated
                    }
                }
                is AuthLoginResult.Failed -> {
                    authAudit.logEvent(user, "LOGIN_FAIL", "Invalid password, attempts left: ${result.attemptsLeft}")
                    _errorMessage.value = "Неверный пароль. Осталось попыток: ${result.attemptsLeft}"
                }
                is AuthLoginResult.Locked -> {
                    authAudit.logEvent(user, "LOCK", "Account locked for 5 minutes")
                    _uiState.value = AuthUiState.Locked(result.remainingMillis)
                }
            }
        }
    }

    // --- 2. Установка PIN ---
    fun registerNewPin(pin: String) {
        if (pin.length < 4 || pin.length > 6) {
            _errorMessage.value = "PIN должен содержать от 4 до 6 цифр"
            return
        }
        pinVault.savePin(pin)
        // Сохраняем зашифрованный refresh токен
        runCatching {
            val cipher = pinVault.getEncryptCipher()
            val session = sessionManager.current ?: Session("user", "token", System.currentTimeMillis() + 86400000)
            pinVault.saveEncryptedRefresh(cipher, session.accessToken)
        }
        authAudit.logEvent("user", "PIN_SETUP", "New local PIN configured successfully")
        lastAuthTimestamp = System.currentTimeMillis()
        _uiState.value = AuthUiState.Authenticated
    }

    // --- 3. Вход по PIN ---
    fun authenticateWithPin(pin: String) {
        if (pinVault.verifyPin(pin)) {
            val savedUser = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                .getString("last_user_id", "pavel") ?: "pavel"

            // Восстанавливаем контекст доступа для этого пользователя
            _accessContext.value = authRepository.getAccessContextForUser(savedUser)

            authAudit.logEvent(savedUser, "PIN_OK", "Local PIN entry successful, role: ${_accessContext.value.role}")
            lastAuthTimestamp = System.currentTimeMillis()
            _uiState.value = AuthUiState.Authenticated
            _errorMessage.value = null
        } else {
            authAudit.logEvent("user", "PIN_FAIL", "Invalid local PIN")
            _errorMessage.value = "Неверный PIN-код"
        }
    }

    // --- 4. Биометрический вход через CryptoObject ---
    fun authenticateWithBiometrics(activity: FragmentActivity, onSuccess: () -> Unit) {
        runCatching {
            val cipher = pinVault.getEncryptCipher()
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход по биометрии")
                .setSubtitle("Подтвердите личность для доступа к фитнес-приложению")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val resCipher = result.cryptoObject?.cipher
                        if (resCipher != null) {
                            pinVault.decryptRefresh(resCipher).onSuccess {
                                authAudit.logEvent("user", "BIOMETRIC_OK", "Biometric unlock successful")
                                lastAuthTimestamp = System.currentTimeMillis()
                                _uiState.value = AuthUiState.Authenticated
                                onSuccess()
                            }
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        authAudit.logEvent("user", "BIOMETRIC_ERROR", "Error: $errString")
                        _errorMessage.value = "Ошибка биометрии: $errString"
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        authAudit.logEvent("user", "BIOMETRIC_FAIL", "Fingerprint not recognized")
                        _errorMessage.value = "Отпечаток не распознан"
                    }
                }
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }.onFailure {
            _errorMessage.value = "Биометрия недоступна на устройстве"
        }
    }

    // --- 5. Step-up проверка для защищенного экрана (Профиль) ---
    fun checkStepUpRequired(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastAuthTimestamp) > STEP_UP_WINDOW_MS
    }

    fun requestStepUp(reason: String) {
        _uiState.value = AuthUiState.StepUpRequired(reason)
    }

    fun onStepUpSuccess() {
        lastAuthTimestamp = System.currentTimeMillis()
        authAudit.logEvent("user", "STEP_UP", "Step-up authentication passed successfully")
        _uiState.value = AuthUiState.Authenticated
    }

    // --- 6. Recovery (Восстановление по коду) ---
    fun recoverWithCode(code: String, newPassword: String, activity: FragmentActivity, onSuccessRedirect: () -> Unit) {
        if (!recoveryManager.hasRecoveryCode()) {
            _errorMessage.value = "Код восстановления не найден"
            return
        }

        val isCodeValid = recoveryManager.verifyOnly(code)
        if (!isCodeValid) {
            _errorMessage.value = "Неверный код восстановления"
            authAudit.logEvent("user", "RECOVERY_FAIL", "Invalid recovery code entered")
            return
        }

        // 1. Обновляем пароль в репозитории
        authRepository.updatePassword("pavel", newPassword)

        // 2. Сжигаемспользованный код восстановления
        recoveryManager.consumeCode()

        // 3. Логинимся с новым паролем для создания сессии
        val result = authRepository.login("pavel", newPassword)
        if (result is AuthLoginResult.Success) {
            sessionManager.start(result.session)
            authAudit.logEvent("user", "RECOVERY", "Access restored successfully via recovery code")

            // 4. Генерируем новый код восстановления для будущего
            val newRecoveryCode = recoveryManager.generateAndSaveRecoveryCode()

            // Переводим в состояние настройки PIN и вызываем редирект UI
            _uiState.value = AuthUiState.PinSetup(newRecoveryCode)
            onSuccessRedirect()
        } else {
            _errorMessage.value = "Ошибка инициализации сессии после восстановления"
        }
    }

    fun verifyStepUpPin(pin: String): Boolean {
        val success = pinVault.verifyPin(pin)
        if (success) {
            lastAuthTimestamp = System.currentTimeMillis()
            authAudit.logEvent("user", "STEP_UP_PIN", "Step-up authentication passed via PIN")
        } else {
            authAudit.logEvent("user", "STEP_UP_PIN_FAIL", "Step-up failed: invalid PIN")
        }
        return success
    }

    // --- 7. Выход (Logout) ---
    fun logout() {
        authRepository.revokeRefresh()
        sessionManager.clear()
        _accessContext.value = AccessContext(userId = "guest", role = Role.EMPLOYEE, scopes = emptySet())
        authAudit.logEvent("user", "LOGOUT", "User logged out, session cleared")
        _uiState.value = AuthUiState.Login
    }
}