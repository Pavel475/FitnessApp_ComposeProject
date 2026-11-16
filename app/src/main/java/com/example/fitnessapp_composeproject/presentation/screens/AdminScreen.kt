package com.example.fitnessapp_composeproject.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessapp_composeproject.data.AccessAuditEntry
import com.example.fitnessapp_composeproject.data.Permission
import com.example.fitnessapp_composeproject.presentation.viewmodel.AccessViewModel

@Composable
fun AdminScreen(accessViewModel: AccessViewModel) {
    val accessContext by accessViewModel.accessContext.collectAsState()
    val auditLogs by accessViewModel.auditLogs.collectAsState()

    // Загружаем журнал при открытии экрана, если есть права
    LaunchedEffect(accessContext) {
        if (accessContext.can(Permission.READ_AUDIT)) {
            accessViewModel.loadAuditLogs()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Журнал доступа",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (accessContext.can(Permission.READ_AUDIT)) {
                IconButton(onClick = { accessViewModel.loadAuditLogs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить журнал")
                }
            }
        }

        if (accessContext.can(Permission.READ_AUDIT)) {
            if (auditLogs.isEmpty()) {
                Text(
                    text = "Записей в журнале пока нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(auditLogs) { entry ->
                        AuditLogItemCard(entry)
                    }
                }
            }
        } else {
            // Защита на случай прямого попадания на экран без роли ADMIN
            Text(
                text = "Раздел недоступен для текущей учетной записи.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuditLogItemCard(entry: AccessAuditEntry) {
    val statusColor = if (entry.result) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val statusText = if (entry.result) "ALLOWED" else "DENIED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${entry.userId} → ${entry.action}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Text(
                text = entry.dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            entry.details?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}