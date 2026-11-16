package com.example.fitnessapp_composeproject.data

import android.content.Context
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(InternalSerializationApi::class)
@Serializable
data class AccessAuditEntry(
    val timestamp: Long,
    val dateFormatted: String,
    val userId: String,
    val action: String,      // например, "scan:execute", "audit:read"
    val result: Boolean,     // true = ALLOWED, false = DENIED
    val details: String? = null
)

class AuditRepository(private val context: Context) {
    private val auditFile = File(context.filesDir, "access_audit.log")
    private val json = Json { ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Append-only запись решения доступа в файл
     */
    fun record(userId: String, action: String, result: Boolean, details: String? = null) {
        runCatching {
            val now = System.currentTimeMillis()
            val entry = AccessAuditEntry(
                timestamp = now,
                dateFormatted = dateFormat.format(Date(now)),
                userId = userId,
                action = action,
                result = result,
                details = details
            )
            val jsonLine = json.encodeToString(entry) + "\n"
            auditFile.appendText(jsonLine)
        }
    }

    /**
     * Чтение журнала доступа (только для роли ADMIN)
     */
    fun getAuditEntries(): List<AccessAuditEntry> {
        if (!auditFile.exists()) return emptyList()
        return runCatching {
            auditFile.readLines()
                .filter { it.isNotBlank() }
                .map { json.decodeFromString<AccessAuditEntry>(it) }
                .reversed() // Свежие сверху
        }.getOrDefault(emptyList())
    }
}