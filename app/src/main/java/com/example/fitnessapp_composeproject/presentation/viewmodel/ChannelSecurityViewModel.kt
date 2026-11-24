package com.example.fitnessapp_composeproject.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp_composeproject.data.api.ApiClient
import com.example.fitnessapp_composeproject.data.api.SecureMockServer
import com.example.fitnessapp_composeproject.data.realtime.LiveSocket
import com.example.fitnessapp_composeproject.data.security.RequestSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID

class ChannelSecurityViewModel : ViewModel() {

    // В реальном проекте секрет HMAC получается после авторизации/ключевого обмена и хранится в Keystore
    private val hmacSecret = "super_secret_fitness_channel_key_2026".toByteArray(Charsets.UTF_8)
    private val signer = RequestSigner(hmacSecret)

    val mockServer = SecureMockServer(signer = signer)
    val apiClient = ApiClient(
        signer = signer,
        trustedServerCerts = listOf(
            mockServer.primaryHeldCert.certificate,
            mockServer.backupHeldCert.certificate
        ),
        primaryPin = mockServer.primaryPin,
        backupPin = mockServer.backupPin
    )

    val liveSocket = LiveSocket(
        clientProvider = { apiClient.buildClient(enablePinning = true) },
        signer = signer
    )

    private val _lastOperationResult = MutableStateFlow("Ожидание проверки...")
    val lastOperationResult: StateFlow<String> = _lastOperationResult.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs: StateFlow<List<String>> = _serverLogs.asStateFlow()

    private val _isUsingBackupCert = MutableStateFlow(false)
    val isUsingBackupCert: StateFlow<Boolean> = _isUsingBackupCert.asStateFlow()

    private val _pinningEnabled = MutableStateFlow(true)
    val pinningEnabled: StateFlow<Boolean> = _pinningEnabled.asStateFlow()

    init {
        startServer()
    }

    private fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            mockServer.start(useBackup = false)
            refreshLogs()
        }
    }

    fun togglePinning(enabled: Boolean) {
        _pinningEnabled.value = enabled
        _lastOperationResult.value = if (enabled) {
            "Certificate Pinning ВКЛЮЧЁН (режим fail-closed)"
        } else {
            "ВНИМАНИЕ: Certificate Pinning ОТКЛЮЧЁН (режим для MITM-эксперимента)"
        }
    }

    // 1. Штатная защищённая отправка гео-истории тренировки
    fun testNormalSync() {
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.signingInterceptor.simulatedClockOffsetSec = 0L
            val geoPayload = """{"workout_id":"run_101","distance_km":5.4,"route":[[56.112,47.481],[56.115,47.485]]}"""
            val res = apiClient.syncWorkoutGeoHistory(
                baseUrl = mockServer.baseUrl,
                payloadJson = geoPayload,
                enablePinning = _pinningEnabled.value
            )
            _lastOperationResult.value = res.fold(
                onSuccess = { "УСПЕХ: $it" },
                onFailure = { "ОШИБКА TLS/КАНАЛА: ${it.javaClass.simpleName}: ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 2. Проверка запрета Cleartext (http://) политикой NSC
    fun testCleartextBlocked() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val plainClient = OkHttpClient()
                val req = Request.Builder()
                    .url("http://localhost:${mockServer.port}/api/workouts/sync")
                    .build()
                plainClient.newCall(req).execute().use { "HTTP ${it.code}" }
            }
            _lastOperationResult.value = result.fold(
                onSuccess = { "Неожиданно прошло: $it" },
                onFailure = { "БЛОКИРОВКА NSC (ожидаемо): ${it.javaClass.simpleName}: ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 3. Проверка неверного пина (fail-closed)
    fun testWrongPin() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = apiClient.syncWorkoutGeoHistory(
                baseUrl = mockServer.baseUrl,
                payloadJson = """{"test":"wrong_pin"}""",
                enablePinning = true,
                forceWrongPin = true
            )
            _lastOperationResult.value = res.fold(
                onSuccess = { "Неожиданно прошло: $it" },
                onFailure = { "PINNING FAIL-CLOSED (ожидаемо): ${it.javaClass.simpleName}: ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 4. Ротация сертификата сервера (Primary <-> Backup)
    fun testRotateCertificate() {
        viewModelScope.launch(Dispatchers.IO) {
            mockServer.rotateCertificate()
            _isUsingBackupCert.value = mockServer.useBackupCertificate
            _lastOperationResult.value = "Сервер переключён на ${if (mockServer.useBackupCertificate) "BACKUP" else "PRIMARY"} сертификат. Проверьте синхронизацию!"
            refreshLogs()
        }
    }

    // 5. Имитация Replay-атаки (повтор одного и того же nonce)
    fun testReplayAttack() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = apiClient.sendReplayAttack(
                baseUrl = mockServer.baseUrl,
                payloadJson = """{"workout_id":"run_replay","distance_km":3.0}"""
            )
            _lastOperationResult.value = res.fold(
                onSuccess = { it },
                onFailure = { "Ошибка: ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 6. Имитация подмены тела запроса после подписи
    fun testTamperedBody() {
        viewModelScope.launch(Dispatchers.IO) {
            val res = apiClient.sendTamperedPayloadAttack(baseUrl = mockServer.baseUrl)
            _lastOperationResult.value = res.fold(
                onSuccess = { it },
                onFailure = { "Ошибка: ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 7. Имитация сдвига часов устройства (+5 минут > ±30 сек)
    fun testClockDrift() {
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.signingInterceptor.simulatedClockOffsetSec = 300L // +5 минут
            val res = apiClient.syncWorkoutGeoHistory(
                baseUrl = mockServer.baseUrl,
                payloadJson = """{"workout_id":"run_drift","distance_km":2.1}"""
            )
            apiClient.signingInterceptor.simulatedClockOffsetSec = 0L // возвращаем норму
            _lastOperationResult.value = res.fold(
                onSuccess = { it },
                onFailure = { "CLOCK DRIFT ОТКЛОНЁН (ожидаемо): ${it.message}" }
            )
            refreshLogs()
        }
    }

    // 8. WebSocket (wss://): подключение и отправка подписанного фрейма метрик
    fun connectWebSocket() {
        val ticket = "wsticket_${UUID.randomUUID().toString().take(8)}"
        liveSocket.connect(mockServer.wsUrl, ticket)
    }

    fun sendLiveMetricFrame() {
        viewModelScope.launch(Dispatchers.IO) {
            val sent = liveSocket.sendSignedMetricFrame(heartRate = 148, lat = 56.1142, lon = 47.4829)
            if (!sent) {
                _lastOperationResult.value = "WebSocket не подключен. Сначала нажмите «Подключить wss://»"
            }
            refreshLogs()
        }
    }

    fun disconnectWebSocket() {
        liveSocket.disconnect()
    }

    fun refreshLogs() {
        _serverLogs.value = mockServer.serverLogs
    }

    override fun onCleared() {
        super.onCleared()
        liveSocket.disconnect()
        mockServer.stop()
    }
}