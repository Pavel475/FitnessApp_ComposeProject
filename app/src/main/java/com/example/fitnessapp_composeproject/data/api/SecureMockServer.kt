package com.example.fitnessapp_composeproject.data.api

import com.example.fitnessapp_composeproject.data.security.NonceCache
import com.example.fitnessapp_composeproject.data.security.ReplayCheckResult
import com.example.fitnessapp_composeproject.data.security.RequestSigner
import okhttp3.CertificatePinner
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress

class SecureMockServer(
    private val signer: RequestSigner,
    private val serverNonceCache: NonceCache = NonceCache(allowedWindowSeconds = 30L)
) {
    private var server: MockWebServer? = null

    // Генерируем два самоподписанных сертификата: основной (Primary) и резервный (Backup) для проверки ротации
    val primaryHeldCert: HeldCertificate = HeldCertificate.Builder()
        .commonName("localhost")
        .addSubjectAlternativeName("localhost")
        .addSubjectAlternativeName("127.0.0.1")
        .build()

    val backupHeldCert: HeldCertificate = HeldCertificate.Builder()
        .commonName("localhost")
        .addSubjectAlternativeName("localhost")
        .addSubjectAlternativeName("127.0.0.1")
        .build()

    // Вычисляем реальные SPKI SHA-256 пины для обоих сертификатов
    val primaryPin: String = CertificatePinner.pin(primaryHeldCert.certificate)
    val backupPin: String = CertificatePinner.pin(backupHeldCert.certificate)

    var useBackupCertificate: Boolean = false
        private set

    val port: Int
        get() = server?.port ?: 0

    val baseUrl: String
        get() = "https://localhost:$port/"

    val wsUrl: String
        get() = "wss://localhost:$port/live"

    // Лог событий на стороне «сервера» для наглядности в UI и отчете
    private val _serverLogs = mutableListOf<String>()
    val serverLogs: List<String> get() = _serverLogs.toList()

    fun start(useBackup: Boolean = false) {
        stop()
        useBackupCertificate = useBackup
        val activeCert = if (useBackup) backupHeldCert else primaryHeldCert

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(activeCert)
            .build()

        val mockServer = MockWebServer()
        mockServer.useHttps(serverCertificates.sslSocketFactory(), false)

        mockServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: "/"

                // 1. Обработка WebSocket handshake (/live)
                if (path == "/live" && request.getHeader("Upgrade")?.equals("websocket", ignoreCase = true) == true) {
                    logServer("WS Handshake принят по wss:// ($path)")
                    return MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            handleWsFrame(webSocket, text)
                        }
                    })
                }

                // 2. Проверка HMAC-подписи, timestamp и nonce для всех REST-запросов
                val tsHeader = request.getHeader("X-Timestamp")?.toLongOrNull()
                val nonceHeader = request.getHeader("X-Nonce")
                val sigHeader = request.getHeader("X-Signature")
                val bodyBytes = request.body.clone().readByteArray()

                if (tsHeader == null || nonceHeader.isNullOrBlank() || sigHeader.isNullOrBlank()) {
                    logServer("401 REJECTED ($path): отсутствуют заголовки подписи")
                    return MockResponse()
                        .setResponseCode(401)
                        .setBody("""{"error":"Missing security headers"}""")
                }

                // Проверка Replay и окна ±30 сек
                when (val replayResult = serverNonceCache.validateAndStore(nonceHeader, tsHeader)) {
                    is ReplayCheckResult.ClockDriftExceeded -> {
                        logServer("409 REJECTED ($path): Clock drift ${replayResult.driftSeconds}s > 30s")
                        return MockResponse()
                            .setResponseCode(409)
                            .setBody("""{"error":"Timestamp outside ±30s window (drift: ${replayResult.driftSeconds}s)"}""")
                    }
                    is ReplayCheckResult.ReplayDetected -> {
                        logServer("409 REJECTED ($path): Replay-атака! Повтор nonce=$nonceHeader")
                        return MockResponse()
                            .setResponseCode(409)
                            .setBody("""{"error":"Replay detected: nonce already used"}""")
                    }
                    is ReplayCheckResult.Valid -> { /* Продолжаем проверку подписи */ }
                }

                // Проверка целостности и подлинности подписи HMAC-SHA256
                val isSignatureValid = signer.verify(
                    method = request.method ?: "GET",
                    path = path,
                    body = bodyBytes,
                    timestamp = tsHeader,
                    nonce = nonceHeader,
                    providedSignature = sigHeader
                )

                if (!isSignatureValid) {
                    logServer("401 REJECTED ($path): Подпись HMAC не совпала (данные подменены!)")
                    return MockResponse()
                        .setResponseCode(401)
                        .setBody("""{"error":"Invalid HMAC signature"}""")
                }

                logServer("200 OK ($path): Подпись и nonce верны. Принято ${bodyBytes.size} байт")
                return MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"status":"accepted","server_ts":${System.currentTimeMillis() / 1000}}""")
            }
        }

        // Запускаем на localhost
        mockServer.start(InetAddress.getByName("127.0.0.1"), 0)
        server = mockServer
        logServer("HTTPS MockServer запущен на порту ${mockServer.port} (сертификат: ${if (useBackup) "BACKUP" else "PRIMARY"})")
    }

    private fun handleWsFrame(webSocket: WebSocket, rawFrame: String) {
        // Формат подписанного фрейма: ts|nonce|signature|payload
        val parts = rawFrame.split("|", limit = 4)
        if (parts.size < 4) {
            logServer("WS FRAME REJECTED: неверный формат кадра")
            webSocket.send("""{"status":"error","reason":"Invalid frame format"}""")
            return
        }
        val ts = parts[0].toLongOrNull() ?: 0L
        val nonce = parts[1]
        val sig = parts[2]
        val payload = parts[3]

        val replayCheck = serverNonceCache.validateAndStore(nonce, ts)
        if (replayCheck !is ReplayCheckResult.Valid) {
            logServer("WS FRAME REJECTED: Replay или Clock Drift")
            webSocket.send("""{"status":"error","reason":"WS Replay or Clock Drift"}""")
            return
        }

        val valid = signer.verify("WS", "/live", payload.toByteArray(Charsets.UTF_8), ts, nonce, sig)
        if (!valid) {
            logServer("WS FRAME REJECTED: Неверная подпись фрейма!")
            webSocket.send("""{"status":"error","reason":"WS Invalid signature"}""")
            return
        }

        logServer("WS FRAME OK: приняты живые метрики -> $payload")
        webSocket.send("""{"status":"ack","received":"$payload"}""")
    }

    fun rotateCertificate() {
        val nextBackupState = !useBackupCertificate
        logServer("Ротация сертификата сервера -> ${if (nextBackupState) "BACKUP" else "PRIMARY"}")
        start(useBackup = nextBackupState)
    }

    fun stop() {
        runCatching { server?.shutdown() }
        server = null
    }

    private fun logServer(msg: String) {
        synchronized(_serverLogs) {
            _serverLogs.add(0, msg)
            if (_serverLogs.size > 40) _serverLogs.removeLast()
        }
    }

    fun clearLogs() {
        synchronized(_serverLogs) { _serverLogs.clear() }
    }
}