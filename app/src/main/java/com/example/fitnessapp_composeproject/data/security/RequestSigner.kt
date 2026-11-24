package com.example.fitnessapp_composeproject.data.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RequestSigner(private val secretKey: ByteArray) {

    /**
     * Формирует каноническую строку и вычисляет HMAC-SHA256 в hex-формате.
     * Тело запроса хешируется через SHA-256, чтобы не загружать большие payload в строку целиком.
     */
    fun sign(
        method: String,
        path: String,
        body: ByteArray,
        timestamp: Long,
        nonce: String
    ): String {
        val bodyHashHex = sha256Hex(body)
        val canonical = "${method.uppercase()}\n$path\n$bodyHashHex\n$timestamp\n$nonce"
        return hmacSha256Hex(secretKey, canonical.toByteArray(Charsets.UTF_8))
    }

    /**
     * Проверка подписи на стороне мок-сервера (сравнение за константное время для защиты от timing-атак)
     */
    fun verify(
        method: String,
        path: String,
        body: ByteArray,
        timestamp: Long,
        nonce: String,
        providedSignature: String
    ): Boolean {
        val expected = sign(method, path, body, timestamp, nonce)
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            providedSignature.toByteArray(Charsets.UTF_8)
        )
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(secret: ByteArray, message: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val rawHmac = mac.doFinal(message)
        return rawHmac.joinToString("") { "%02x".format(it) }
    }
}