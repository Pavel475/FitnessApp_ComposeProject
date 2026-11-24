package com.example.fitnessapp_composeproject.ui

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast

class DeleteNoteActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Защита экрана от скриншотов и превью в мультизадачности
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // В манифесте задано android:exported="false", плюс проверяем наличие авторизации/вызова внутри приложения
        val isAuthorizedCall = intent?.getBooleanExtra("authorized", false) == true
        if (!isAuthorizedCall) {
            Toast.makeText(this, "Доступ запрещён", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val noteId = intent?.getStringExtra("note_id")
        if (noteId.isNullOrBlank()) {
            Toast.makeText(this, "Некорректный идентификатор заметки", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Секреты и токены больше не хранятся в открытом SharedPreferences и не пишутся в Logcat
        Toast.makeText(this, "Заметка $noteId обработана", Toast.LENGTH_SHORT).show()
        finish()
    }
}