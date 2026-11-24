package com.example.fitnessapp_composeproject.data.security

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

sealed class ReplayCheckResult {
    object Valid : ReplayCheckResult()
    data class ClockDriftExceeded(val driftSeconds: Long) : ReplayCheckResult()
    object ReplayDetected : ReplayCheckResult()
}

class NonceCache(
    private val allowedWindowSeconds: Long = 30L // Окно ±30 сек по требованию задания
) {
    // Храним пару: nonce -> timestamp (в секундах) для возможности очистки старых записей
    private val seenNonces = ConcurrentHashMap<String, Long>()

    fun validateAndStore(nonce: String, clientTimestampSec: Long): ReplayCheckResult {
        val serverNowSec = System.currentTimeMillis() / 1000L
        val drift = abs(serverNowSec - clientTimestampSec)

        // 1. Проверка временного окна (±30 сек)
        if (drift > allowedWindowSeconds) {
            return ReplayCheckResult.ClockDriftExceeded(drift)
        }

        // Очищаем просроченные nonce старше 2 * window, чтобы не забивать память
        val expirationThreshold = serverNowSec - (allowedWindowSeconds * 2)
        seenNonces.entries.removeIf { it.value < expirationThreshold }

        // 2. Атомарная проверка уникальности nonce
        val previous = seenNonces.putIfAbsent(nonce, clientTimestampSec)
        if (previous != null) {
            return ReplayCheckResult.ReplayDetected
        }

        return ReplayCheckResult.Valid
    }

    fun clear() {
        seenNonces.clear()
    }
}