package com.example.fitnessapp_composeproject.data

/**
 * Данные сессии пользователя.
 * accessToken находится ИСКЛЮЧИТЕЛЬНО в оперативке (RAM) процесса.
 */
data class Session(
    val userId: String,
    val accessToken: String,
    val refreshExpiresAt: Long
)

class SessionManager {
    // Access-токен храним только в памяти (никаких SharedPreferences / DataStore в открытом виде)
    private var activeSession: Session? = null

    val current: Session? get() = activeSession

    fun start(session: Session) {
        activeSession = session
    }

    fun clear() {
        activeSession = null
    }

    fun isAlive(now: Long = System.currentTimeMillis()): Boolean {
        val session = activeSession ?: return false
        return now < session.refreshExpiresAt
    }
}