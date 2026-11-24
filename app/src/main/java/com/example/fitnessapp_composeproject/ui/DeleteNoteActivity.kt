package com.example.fitnessapp_composeproject.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class DeleteNoteActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val noteId = intent?.getStringExtra("note_id") ?: "unknown"

        // Уязвимость 2: секрет/токен в открытом SharedPreferences
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val demoToken = prefs.getString("session_token", "demo_secret_token_987654") ?: ""
        prefs.edit().putString("session_token", demoToken).apply()

        // Уязвимость 5: утечка токена в logcat
        Log.d("AuthDebug", "token=$demoToken")

        // Уязвимость 1: действие удаления без проверки прав и авторизации
        Log.w("DeleteNoteActivity", "Deleted note_id=$noteId without auth check")
        Toast.makeText(this, "Удалена заметка: $noteId", Toast.LENGTH_SHORT).show()
        finish()
    }
}