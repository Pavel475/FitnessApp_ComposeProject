package com.example.fitnessapp_composeproject.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

class RecoveryManager(context: Context) {
    private val prefs = context.getSharedPreferences("recovery_prefs", Context.MODE_PRIVATE)
    private val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Без I, O, 0, 1

    /**
     * Генерация 8-символьного кода восстановления и сохранение его SHA-256 хеша
     */
    fun generateAndSaveRecoveryCode(): String {
        val code = (1..8).map { ALPHABET.random() }.joinToString("")
        val hash = hashString(code)
        prefs.edit().putString("recovery_hash", hash).apply()
        return code
    }

    /**
     * Только проверка хэша (без удаления)
     */
    fun verifyOnly(inputCode: String): Boolean {
        val storedHash = prefs.getString("recovery_hash", null) ?: return false
        val inputHash = hashString(inputCode.uppercase())
        return storedHash == inputHash
    }

    /**
     * Окончательное удаление (потребление) кода после успешного прохождения всех проверок
     */
    fun consumeCode() {
        prefs.edit().remove("recovery_hash").apply()
    }

    fun hasRecoveryCode(): Boolean = prefs.contains("recovery_hash")

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}