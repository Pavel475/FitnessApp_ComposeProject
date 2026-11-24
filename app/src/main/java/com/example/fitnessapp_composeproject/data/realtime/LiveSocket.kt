package com.example.fitnessapp_composeproject.data.realtime

import com.example.fitnessapp_composeproject.data.security.RequestSigner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import kotlin.math.min

class LiveSocket(
    private val clientProvider: () -> OkHttpClient,
    private val signer: RequestSigner
) {
    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var shouldReconnect = false
    private var reconnectAttempt = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow("Отключен")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _lastAckMessage = MutableStateFlow<String?>(null)
    val lastAckMessage: StateFlow<String?> = _lastAckMessage.asStateFlow()

    fun connect(wssUrl: String, shortLivedTicket: String) {
        currentUrl = "$wssUrl?ticket=$shortLivedTicket"
        shouldReconnect = true
        reconnectAttempt = 0
        openConnection()
    }

    private fun openConnection() {
        val url = currentUrl ?: return
        _connectionState.value = "Подключение по wss://..."

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = clientProvider().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                _connectionState.value = "Подключен (wss:// защищён)"
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _lastAckMessage.value = text
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = "Ошибка связи: ${t.localizedMessage ?: "TLS/Network error"}"
                scheduleReconnectWithBackoff()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "Соединение закрыто ($code)"
                if (shouldReconnect) {
                    scheduleReconnectWithBackoff()
                }
            }
        })
    }

    /**
     * Автопереподключение с экспоненциальным backoff: 1s, 2s, 4s, 8s... максимум 30s
     */
    private fun scheduleReconnectWithBackoff() {
        if (!shouldReconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delaySeconds = min(30L, 1L shl reconnectAttempt)
            reconnectAttempt++
            _connectionState.value = "Переподключение через ${delaySeconds}с (попытка #$reconnectAttempt)..."
            delay(delaySeconds * 1000L)
            if (shouldReconnect) {
                openConnection()
            }
        }
    }

    /**
     * Отправка чувствительных метрик с криптографической подписью каждого фрейма
     */
    fun sendSignedMetricFrame(heartRate: Int, lat: Double, lon: Double): Boolean {
        val ws = webSocket ?: return false
        val payload = """{"hr":$heartRate,"lat":$lat,"lon":$lon}"""
        val ts = System.currentTimeMillis() / 1000L
        val nonce = UUID.randomUUID().toString()

        // Подписываем фрейм так же, как REST-запрос: method="WS", path="/live", body=payload
        val signature = signer.sign("WS", "/live", payload.toByteArray(Charsets.UTF_8), ts, nonce)
        val signedFrame = "$ts|$nonce|$signature|$payload"

        return ws.send(signedFrame)
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client closed")
        webSocket = null
        _connectionState.value = "Отключен"
    }
}