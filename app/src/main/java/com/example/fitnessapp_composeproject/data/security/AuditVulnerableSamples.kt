package com.example.fitnessapp_composeproject.data.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AuditVulnerableSamples {

    private const val CLIPBOARD_CLEAR_DELAY_MS = 30_000L
    private val mainHandler = Handler(Looper.getMainLooper())

    // Используем только HTTPS и задаём таймауты клиента
    fun sendSecureRequest(): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("https://api.notes.example/v1/notes")
            .build()

        return runCatching {
            client.newCall(request).execute().use { "HTTP ${it.code}" }
        }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
    }

    // Безопасная настройка WebView: без JavascriptInterface и без загрузки http://
    fun setupSecureWebView(webView: WebView, url: String) {
        require(url.startsWith("https://")) {
            "Разрешены только защищённые HTTPS-ссылки"
        }
        webView.settings.javaScriptEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.loadUrl(url)
    }

    // Копирование чувствительного текста с автоматической очисткой буфера обмена через 30 секунд
    fun copyNoteToClipboardWithAutoClear(context: Context, noteText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = "note_secret"
        val clip = ClipData.newPlainText(label, noteText)
        clipboard.setPrimaryClip(clip)

        mainHandler.postDelayed({
            val currentClip = clipboard.primaryClip
            val currentText = currentClip?.getItemAt(0)?.coerceToText(context)?.toString()
            if (currentText == noteText) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }, CLIPBOARD_CLEAR_DELAY_MS)
    }
}