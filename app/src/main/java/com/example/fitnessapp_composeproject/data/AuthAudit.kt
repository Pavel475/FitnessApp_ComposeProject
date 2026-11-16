package com.example.fitnessapp_composeproject.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(InternalSerializationApi::class)
@Serializable
data class AuthEvent(
    val timestamp: Long,
    val dateFormatted: String,
    val userId: String,
    val kind: String,   // LOGIN_OK, LOGIN_FAIL, LOCK, STEP_UP, RECOVERY, LOGOUT, PIN_SETUP
    val detail: String? = null
)

class AuthAudit(private val context: Context) {
    private val auditFile = File(context.filesDir, "auth_audit.log")
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Запись события безопасности в файл
     */
    fun logEvent(userId: String, kind: String, detail: String? = null) {
        runCatching {
            val now = System.currentTimeMillis()
            val event = AuthEvent(
                timestamp = now,
                dateFormatted = dateFormat.format(Date(now)),
                userId = userId,
                kind = kind,
                detail = detail
            )
            val jsonLine = json.encodeToString(event) + "\n"
            auditFile.appendText(jsonLine)
        }
    }

    /**
     * Чтение истории событий безопасности для экрана Настроек/Аудита
     */
    fun getEvents(): List<AuthEvent> {
        if (!auditFile.exists()) return emptyList()
        return runCatching {
            auditFile.readLines()
                .filter { it.isNotBlank() }
                .map { json.decodeFromString<AuthEvent>(it) }
                .reversed() // Сначала свежие события
        }.getOrDefault(emptyList())
    }

    fun clearAuditLog() {
        if (auditFile.exists()) {
            auditFile.delete()
        }
    }
}