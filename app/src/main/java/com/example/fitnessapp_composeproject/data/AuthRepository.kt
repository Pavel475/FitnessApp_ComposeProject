package com.example.fitnessapp_composeproject.data

sealed class AuthLoginResult {
    data class Success(val session: Session, val accessContext: AccessContext) : AuthLoginResult()
    data class Failed(val attemptsLeft: Int) : AuthLoginResult()
    data class Locked(val remainingMillis: Long) : AuthLoginResult()
}

class AuthRepository {
    private var failedAttempts = 0
    private var lockedUntil = 0L

    // Словарь пользователей и их паролей (мок-БД сервера)
    private val usersPasswords = mutableMapOf(
        "pavel" to "password123",
        "guard" to "guard123",
        "admin" to "admin123"
    )

    fun updatePassword(user: String, newPass: String) {
        if (usersPasswords.containsKey(user)) {
            usersPasswords[user] = newPass
        }
    }

    fun login(user: String, pass: String): AuthLoginResult {
        val now = System.currentTimeMillis()

        if (now < lockedUntil) {
            return AuthLoginResult.Locked(lockedUntil - now)
        }

        val expectedPassword = usersPasswords[user]
        if (expectedPassword != null && pass == expectedPassword) {
            failedAttempts = 0

            // Формируем AccessContext (эмуляция парсинга JWT-claims с сервера)
            val accessContext = getAccessContextForUser(user)

            val session = Session(
                userId = user,
                accessToken = "mock_jwt_${user}_${now}",
                refreshExpiresAt = now + 7 * 24 * 3600_000L
            )
            return AuthLoginResult.Success(session, accessContext)
        } else {
            failedAttempts++
            if (failedAttempts >= 4) {
                lockedUntil = now + 5 * 60_000L
                failedAttempts = 0
                return AuthLoginResult.Locked(5 * 60_000L)
            }
            return AuthLoginResult.Failed(4 - failedAttempts)
        }
    }

    fun getAccessContextForUser(user: String): AccessContext {
        return when (user) {
            "guard" -> AccessContext(
                userId = user,
                role = Role.SECURITY,
                scopes = setOf("pass:view", "scan:execute")
            )
            "admin" -> AccessContext(
                userId = user,
                role = Role.ADMIN,
                scopes = setOf("pass:view", "scan:execute", "audit:read")
            )
            else -> AccessContext(
                userId = user,
                role = Role.EMPLOYEE,
                scopes = setOf("pass:view")
            )
        }
    }

    /**
     * 3-й уровень защиты («Сервер»): проверка права на сканирование пропуска по scope
     */
    fun executeServerScan(ctx: AccessContext, targetPassId: String): Result<String> {
        if ("scan:execute" !in ctx.scopes) {
            return Result.failure(SecurityException("Server 403 Forbidden: missing scope 'scan:execute'"))
        }
        return Result.success("Пропуск $targetPassId подтвержден (статус: АКТИВЕН)")
    }

    /**
     * 3-й уровень защиты («Сервер»): проверка права на чтение аудита по scope[cite: 6]
     */
    fun fetchServerAudit(ctx: AccessContext, auditRepo: AuditRepository): Result<List<AccessAuditEntry>> {
        if ("audit:read" !in ctx.scopes) {
            return Result.failure(SecurityException("Server 403 Forbidden: missing scope 'audit:read'"))
        }
        return Result.success(auditRepo.getAuditEntries())
    }

    fun revokeRefresh() {
        // Отзыв токена при logout
    }
}