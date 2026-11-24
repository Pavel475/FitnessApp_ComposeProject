package com.example.fitnessapp_composeproject.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp_composeproject.presentation.viewmodel.ChannelSecurityViewModel

@Composable
fun ChannelSecurityScreen(
    securityViewModel: ChannelSecurityViewModel = viewModel()
) {
    val lastResult by securityViewModel.lastOperationResult.collectAsState()
    val serverLogs by securityViewModel.serverLogs.collectAsState()
    val isBackupCert by securityViewModel.isUsingBackupCert.collectAsState()
    val pinningEnabled by securityViewModel.pinningEnabled.collectAsState()
    val wsState by securityViewModel.liveSocket.connectionState.collectAsState()
    val wsLastAck by securityViewModel.liveSocket.lastAckMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "Защита канала",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Карточка результата последней проверки
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Результат проверки:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastResult,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Переключатель Pinning (для MITM-эксперимента)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Certificate Pinning (OkHttp)", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Сертификат сервера: ${if (isBackupCert) "BACKUP" else "PRIMARY"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pinningEnabled,
                    onCheckedChange = { securityViewModel.togglePinning(it) }
                )
            }
        }

        // Кнопки проверок TLS и NSC
        Text("1. TLS, Pinning и NSC", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Button(
            onClick = { securityViewModel.testNormalSync() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Отправить гео-историю (HTTPS + HMAC)")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { securityViewModel.testCleartextBlocked() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("HTTP (NSC)")
            }
            OutlinedButton(
                onClick = { securityViewModel.testWrongPin() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Неверный пин")
            }
        }

        OutlinedButton(
            onClick = { securityViewModel.testRotateCertificate() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Ротация сертификата (Primary ↔ Backup)")
        }

        // Кнопки проверок HMAC и Replay
        Text("2. Подпись HMAC и Replay-защита", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { securityViewModel.testReplayAttack() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Replay (409)")
            }
            OutlinedButton(
                onClick = { securityViewModel.testTamperedBody() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Подмена (401)")
            }
        }

        OutlinedButton(
            onClick = { securityViewModel.testClockDrift() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Сдвиг часов +5 мин (Clock Drift)")
        }

        // Секция WebSocket (wss://)
        Text("3. WebSocket живых метрик (wss://)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Статус WS: $wsState", style = MaterialTheme.typography.bodySmall)
        wsLastAck?.let {
            Text("Ответ сервера: $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { securityViewModel.connectWebSocket() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Подключить wss://")
            }
            OutlinedButton(
                onClick = { securityViewModel.sendLiveMetricFrame() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Шли фрейм")
            }
        }

        // Логи мок-сервера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Лог HTTPS Mock-сервера:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { securityViewModel.refreshLogs() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (serverLogs.isEmpty()) {
                    Text("Логов пока нет", style = MaterialTheme.typography.bodySmall)
                } else {
                    serverLogs.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}