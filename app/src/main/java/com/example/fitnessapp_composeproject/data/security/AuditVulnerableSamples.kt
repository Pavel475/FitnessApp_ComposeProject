package com.example.fitnessapp_composeproject.data.security

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import okhttp3.OkHttpClient
import okhttp3.Request

object AuditVulnerableSamples {

    // Уязвимость 3: cleartext HTTP-запрос к api.notes.example
    fun sendCleartextRequest(): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://api.notes.example/v1/notes")
            .build()
        return runCatching {
            client.newCall(request).execute().use { "HTTP ${it.code}" }
        }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }
    }

    // Уязвимость 6: WebView с включенным JS, JavascriptInterface и загрузкой http://
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    fun setupInsecureWebView(webView: WebView, secretToken: String) {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun getToken(): String = secretToken
        }, "NotesBridge")
        webView.loadUrl("http://api.notes.example/embed")
    }

    // Уязвимость 8: копирование чувствительного текста в буфер обмена без очистки через 30 сек
    fun copyNoteToClipboardWithoutClear(context: Context, noteText: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("note_secret", noteText))
    }
}