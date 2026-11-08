package com.example.fitnessapp_composeproject.data.repository

import android.content.Context
import android.net.Uri
import com.example.fitnessapp_composeproject.data.remote.dto.FitnessReportDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class FitnessFileRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Внутренний файл приложения для автосохранения
    private val localFile = File(context.filesDir, "fitness_profile_report.json")

    // 1. Чтение локального отчета при старте (если файл существует)
    suspend fun loadLocalReport(): Result<FitnessReportDto?> = withContext(ioDispatcher) {
        runCatching {
            if (localFile.exists()) {
                val jsonString = localFile.readText()
                jsonFormat.decodeFromString<FitnessReportDto>(jsonString)
            } else {
                null
            }
        }
    }

    // 2. Атомарное сохранение локального отчета (через временный файл)
    suspend fun saveLocalReport(report: FitnessReportDto): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val tmpFile = File(context.filesDir, "fitness_profile_report.json.tmp")
            tmpFile.writeText(jsonFormat.encodeToString(report))
            if (localFile.exists()) {
                localFile.delete()
            }
            val success = tmpFile.renameTo(localFile)
            if (!success) {
                throw java.io.IOException("Не удалось переименовать временный файл при сохранении")
            }
            // Последним выражением будет Unit, поэтому runCatching вернет Result<Unit>
            Unit
        }
    }

    // 3. Экспорт данных в файл через системный диалог SAF (ACTION_CREATE_DOCUMENT)
    suspend fun exportToUri(uri: Uri, report: FitnessReportDto): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(jsonFormat.encodeToString(report))
            } ?: throw IllegalStateException("Не удалось открыть OutputStream для выбранного URI")
        }
    }

    // 4. Импорт данных из файла через системный диалог SAF (ACTION_OPEN_DOCUMENT)
    suspend fun importFromUri(uri: Uri): Result<FitnessReportDto> = withContext(ioDispatcher) {
        runCatching {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Не удалось прочитать InputStream для выбранного URI")

            jsonFormat.decodeFromString<FitnessReportDto>(jsonString)
        }
    }
}