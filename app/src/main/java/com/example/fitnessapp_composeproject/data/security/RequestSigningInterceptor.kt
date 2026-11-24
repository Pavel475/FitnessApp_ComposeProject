package com.example.fitnessapp_composeproject.data.security

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.util.UUID

class RequestSigningInterceptor(
    private val signer: RequestSigner
) : Interceptor {

    // Вспомогательные флаги для демонстрации проверок из чек-листа (в обычной работе = 0 / false)
    @Volatile var simulatedClockOffsetSec: Long = 0L
    @Volatile var tamperBodyAfterSigning: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Считываем байты тела запроса, если оно есть
        val bodyBytes = originalRequest.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        } ?: ByteArray(0)

        val timestamp = (System.currentTimeMillis() / 1000L) + simulatedClockOffsetSec
        val nonce = UUID.randomUUID().toString()
        val path = originalRequest.url.encodedPath

        // Вычисляем подпись от реальных данных
        val signature = signer.sign(
            method = originalRequest.method,
            path = path,
            body = bodyBytes,
            timestamp = timestamp,
            nonce = nonce
        )

        val signedRequest = originalRequest.newBuilder()
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(signedRequest)
    }
}