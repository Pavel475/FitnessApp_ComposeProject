package com.example.fitnessapp_composeproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fitnessapp_composeproject.data.AuditRepository
import com.example.fitnessapp_composeproject.presentation.theme.FitnessAppTheme
import com.example.fitnessapp_composeproject.ui.theme.FitnessApp_ComposeProjectTheme

class InviteActivity : ComponentActivity() {

    // Строгое регулярное выражение: только латиница, цифры, '_' и '-', длина 8..64 символа
    private val validTokenRegex = Regex("^[A-Za-z0-9_-]{8,64}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auditRepository = AuditRepository(applicationContext)

        // Извлекаем токен либо из query-параметра URI (?token=...), либо из Intent Extra
        val uriToken = intent?.data?.getQueryParameter("token")
        val extraToken = intent?.getStringExtra("token")
        val rawToken = uriToken ?: extraToken

        // Валидация параметров ДО любого использования
        val isTokenValid = !rawToken.isNullOrBlank() && rawToken.matches(validTokenRegex)

        if (isTokenValid) {
            auditRepository.record(
                userId = "invite_guest",
                action = "deeplink:invite",
                result = true,
                details = "Valid invite token accepted: ${rawToken?.take(4)}***"
            )
        } else {
            auditRepository.record(
                userId = "invite_guest",
                action = "deeplink:invite",
                result = false,
                details = "Blocked invalid or forged deep link token: '$rawToken'"
            )
        }

        setContent {
            FitnessAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InviteScreenContent(
                        isValid = isTokenValid,
                        token = rawToken.orEmpty(),
                        onProceedToLogin = {
                            // Переходим в основное приложение для завершения входа/регистрации
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(mainIntent)
                            finish()
                        },
                        onClose = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteScreenContent(
    isValid: Boolean,
    token: String,
    onProceedToLogin: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isValid) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Приглашение подтверждено",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Токен приглашения прошел проверку безопасности. Вы можете перейти к регистрации и входу в корпоративную систему.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onProceedToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Перейти к входу в систему")
            }
        } else {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Приглашение недействительно",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ссылка-приглашение повреждена, устарела или не прошла проверку формата токена.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Закрыть")
            }
        }
    }
}