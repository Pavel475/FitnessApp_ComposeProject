package com.example.fitnessapp_composeproject.data.api

import com.example.fitnessapp_composeproject.data.security.RequestSigner
import com.example.fitnessapp_composeproject.data.security.RequestSigningInterceptor
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.tls.HandshakeCertificates
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.concurrent.TimeUnit

class ApiClient(
    private val signer: RequestSigner,
    private val trustedServerCerts: List<X509Certificate>,
    private val primaryPin: String,
    private val backupPin: String
) {
    val signingInterceptor = RequestSigningInterceptor(signer)

    /**
     * Создает настроенный OkHttpClient.
     * @param enablePinning если true — работает строгий CertificatePinner (fail-closed);
     *                      если false — пиннинг отключен (используется для демонстрации MITM-перехвата).
     * @param forceWrongPin если true — подставляется заведомо неверный пин для проверки отклонения соединения.
     */
    fun buildClient(
        enablePinning: Boolean = true,
        forceWrongPin: Boolean = false
    ): OkHttpClient {
        // Настраиваем доверие только к нашим самоподписанным корневым/промежуточным CA (без trust-all!)
        val clientCertificatesBuilder = HandshakeCertificates.Builder()
            .addPlatformTrustedCertificates()

        trustedServerCerts.forEach { cert ->
            clientCertificatesBuilder.addTrustedCertificate(cert)
        }
        val clientCertificates = clientCertificatesBuilder.build()

        val builder = OkHttpClient.Builder()
            .sslSocketFactory(
                clientCertificates.sslSocketFactory(),
                clientCertificates.trustManager
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(signingInterceptor)

        if (enablePinning) {
            val pinnerBuilder = CertificatePinner.Builder()
            if (forceWrongPin) {
                // Заведомо неверный пин для проверки fail-closed поведения
                pinnerBuilder.add("localhost", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            } else {
                // Два пина: primary + backup (для поддержки плановой ротации сертификата)
                pinnerBuilder.add("localhost", primaryPin)
                pinnerBuilder.add("localhost", backupPin)
                pinnerBuilder.add("api.tracker.example", primaryPin)
                pinnerBuilder.add("api.tracker.example", backupPin)
            }
            builder.certificatePinner(pinnerBuilder.build())
        }

        return builder.build()
    }

    /**
     * Отправка чувствительных гео-метрик тренировки по защищенному HTTPS-каналу
     */
    fun syncWorkoutGeoHistory(
        baseUrl: String,
        payloadJson: String,
        enablePinning: Boolean = true,
        forceWrongPin: Boolean = false
    ): Result<String> = runCatching {
        val client = buildClient(enablePinning = enablePinning, forceWrongPin = forceWrongPin)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/workouts/sync")
            .post(payloadJson.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}: $bodyStr")
            }
            "HTTP ${response.code} OK: $bodyStr"
        }
    }

    /**
     * Имитация Replay-атаки: повторная отправка запроса с тем же самым X-Nonce и X-Timestamp
     */
    fun sendReplayAttack(baseUrl: String, payloadJson: String): Result<String> = runCatching {
        val client = buildClient(enablePinning = true)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val bodyBytes = payloadJson.toByteArray(Charsets.UTF_8)

        val fixedTs = System.currentTimeMillis() / 1000L
        val fixedNonce = UUID.randomUUID().toString()
        val signature = signer.sign("POST", "/api/workouts/sync", bodyBytes, fixedTs, fixedNonce)

        // Создаём клиент БЕЗ авто-интерцептора, чтобы вручную дважды отправить идентичные заголовки
        val rawClient = OkHttpClient.Builder()
            .sslSocketFactory(
                HandshakeCertificates.Builder()
                    .apply { trustedServerCerts.forEach { addTrustedCertificate(it) } }
                    .build().sslSocketFactory(),
                HandshakeCertificates.Builder()
                    .apply { trustedServerCerts.forEach { addTrustedCertificate(it) } }
                    .build().trustManager
            )
            .build()

        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/workouts/sync")
            .header("X-Timestamp", fixedTs.toString())
            .header("X-Nonce", fixedNonce)
            .header("X-Signature", signature)
            .post(payloadJson.toRequestBody(mediaType))
            .build()

        // 1-й запрос должен пройти (200 OK)
        val firstResp = rawClient.newCall(req).execute().use { "${it.code} ${it.body?.string()}" }
        // 2-й запрос с тем же nonce должен быть отклонен сервером (409 Replay)
        val secondResp = rawClient.newCall(req).execute().use { "${it.code} ${it.body?.string()}" }

        "1-й запрос: $firstResp\n2-й запрос (Replay): $secondResp"
    }

    /**
     * Имитация подмены тела запроса (MITM / модификация payload при старой подписи)
     */
    fun sendTamperedPayloadAttack(baseUrl: String): Result<String> = runCatching {
        val originalBody = """{"distance_km": 5.2, "lat": 56.11, "lon": 47.48}"""
        val tamperedBody = """{"distance_km": 99.9, "lat": 0.00, "lon": 0.00}"""

        val ts = System.currentTimeMillis() / 1000L
        val nonce = UUID.randomUUID().toString()
        // Подписываем оригинальное тело, но отправляем подмененное!
        val signatureForOriginal = signer.sign(
            "POST", "/api/workouts/sync", originalBody.toByteArray(Charsets.UTF_8), ts, nonce
        )

        val certs = HandshakeCertificates.Builder()
            .apply { trustedServerCerts.forEach { addTrustedCertificate(it) } }
            .build()

        val rawClient = OkHttpClient.Builder()
            .sslSocketFactory(certs.sslSocketFactory(), certs.trustManager)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val req = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/workouts/sync")
            .header("X-Timestamp", ts.toString())
            .header("X-Nonce", nonce)
            .header("X-Signature", signatureForOriginal)
            .post(tamperedBody.toRequestBody(mediaType))
            .build()

        rawClient.newCall(req).execute().use { resp ->
            "Ответ сервера на подмену тела: HTTP ${resp.code} -> ${resp.body?.string()}"
        }
    }
}