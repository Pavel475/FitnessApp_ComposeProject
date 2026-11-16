package com.example.fitnessapp_composeproject.data

enum class Role {
    EMPLOYEE,
    SECURITY,
    ADMIN
}

enum class Permission(val requiredRole: Role, val scopeKey: String) {
    VIEW_PASS(Role.EMPLOYEE, "pass:view"),
    SCAN_VISITOR(Role.SECURITY, "scan:execute"),
    READ_AUDIT(Role.ADMIN, "audit:read")
}

data class AccessContext(
    val userId: String,
    val role: Role,
    val scopes: Set<String> // Источник истины от «сервера» (JWT claims)
) {
    // Проверка прав по scope (серверный/моковый источник) + клиентская проверка
    fun can(permission: Permission): Boolean {
        return scopes.contains(permission.scopeKey) && role.ordinal >= permission.requiredRole.ordinal
    }
}