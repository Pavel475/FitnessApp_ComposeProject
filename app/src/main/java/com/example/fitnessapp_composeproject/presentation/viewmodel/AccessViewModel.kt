package com.example.fitnessapp_composeproject.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fitnessapp_composeproject.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val auditRepository = AuditRepository(context)
    private val authRepository = AuthRepository()

    // Текущий контекст прав доступа (по умолчанию минимальные права до входа)
    private val _accessContext = MutableStateFlow(
        AccessContext(userId = "guest", role = Role.EMPLOYEE, scopes = emptySet())
    )
    val accessContext: StateFlow<AccessContext> = _accessContext.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow<String?>(null)
    val scanStatusMessage: StateFlow<String?> = _scanStatusMessage.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AccessAuditEntry>>(emptyList())
    val auditLogs: StateFlow<List<AccessAuditEntry>> = _auditLogs.asStateFlow()

    /**
     * Установка контекста доступа после успешной аутентификации
     */
    fun setAccessContext(ctx: AccessContext) {
        _accessContext.value = ctx
        auditRepository.record(
            userId = ctx.userId,
            action = "session:init",
            result = true,
            details = "Role assigned: ${ctx.role}, scopes: ${ctx.scopes}"
        )
    }

    /**
     * 2-й уровень защиты (ViewModel): попытка выполнить сканирование пропуска[cite: 6]
     */
    fun onScanRequested(targetPassId: String) {
        val ctx = _accessContext.value

        // Проверка прав на уровне ViewModel (защита от обхода UI)[cite: 6]
        if (!ctx.can(Permission.SCAN_VISITOR)) {
            auditRepository.record(
                userId = ctx.userId,
                action = Permission.SCAN_VISITOR.scopeKey,
                result = false,
                details = "ViewModel blocked: role ${ctx.role} lacks SCAN_VISITOR"
            )
            _scanStatusMessage.value = "Отказано в доступе (требуется роль SECURITY)"
            return
        }

        // 3-й уровень защиты: обращение к «серверу»[cite: 6]
        authRepository.executeServerScan(ctx, targetPassId)
            .onSuccess { serverReply ->
                auditRepository.record(
                    userId = ctx.userId,
                    action = Permission.SCAN_VISITOR.scopeKey,
                    result = true,
                    details = "Scanned pass: $targetPassId -> OK"
                )
                _scanStatusMessage.value = serverReply
            }
            .onFailure { err ->
                auditRepository.record(
                    userId = ctx.userId,
                    action = Permission.SCAN_VISITOR.scopeKey,
                    result = false,
                    details = "Server denied: ${err.message}"
                )
                _scanStatusMessage.value = err.message
            }
    }

    /**
     * Загрузка журнала доступа (2-й и 3-й уровни защиты)[cite: 6]
     */
    fun loadAuditLogs() {
        val ctx = _accessContext.value

        if (!ctx.can(Permission.READ_AUDIT)) {
            auditRepository.record(
                userId = ctx.userId,
                action = Permission.READ_AUDIT.scopeKey,
                result = false,
                details = "ViewModel blocked: role ${ctx.role} lacks READ_AUDIT"
            )
            _auditLogs.value = emptyList()
            return
        }

        authRepository.fetchServerAudit(ctx, auditRepository)
            .onSuccess { entries ->
                auditRepository.record(
                    userId = ctx.userId,
                    action = Permission.READ_AUDIT.scopeKey,
                    result = true,
                    details = "Audit log read (${entries.size} records)"
                )
                _auditLogs.value = entries
            }
            .onFailure { err ->
                auditRepository.record(
                    userId = ctx.userId,
                    action = Permission.READ_AUDIT.scopeKey,
                    result = false,
                    details = "Server denied: ${err.message}"
                )
            }
    }

    fun clearScanMessage() {
        _scanStatusMessage.value = null
    }
}