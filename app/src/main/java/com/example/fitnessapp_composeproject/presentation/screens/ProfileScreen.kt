package com.example.fitnessapp_composeproject.presentation.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.fitnessapp_composeproject.data.remote.dto.FitnessReportDto
import com.example.fitnessapp_composeproject.data.repository.FitnessFileRepository
import com.example.fitnessapp_composeproject.presentation.viewmodel.AuthViewModel
import com.example.fitnessapp_composeproject.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FitnessFileRepository(context) }

    // Проверяем необходимость Step-up (прошло ли > 5 минут с последней аутентификации)
    var isUnlocked by remember { mutableStateOf(!authViewModel.checkStepUpRequired()) }

    // Состояния данных профиля
    var weight by remember { mutableStateOf(70) }
    var completedWorkouts by remember { mutableStateOf(12) }
    var calories by remember { mutableStateOf(3450) }

    // При старте экрана пробуем загрузить сохраненный локальный отчет
    LaunchedEffect(Unit) {
        repository.loadLocalReport().onSuccess { report ->
            report?.let {
                weight = it.weightKg
                completedWorkouts = it.completedWorkoutsCount
                calories = it.totalCalories
            }
        }
    }

    fun saveLocally() {
        coroutineScope.launch {
            val report = FitnessReportDto(
                weightKg = weight,
                completedWorkoutsCount = completedWorkouts,
                totalCalories = calories
            )
            repository.saveLocalReport(report)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val report = FitnessReportDto(
                    weightKg = weight,
                    completedWorkoutsCount = completedWorkouts,
                    totalCalories = calories
                )
                repository.exportToUri(it, report)
                    .onSuccess {
                        Toast.makeText(context, "Отчет успешно экспортирован!", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { err ->
                        Toast.makeText(context, "Ошибка экспорта: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                repository.importFromUri(it)
                    .onSuccess { report ->
                        weight = report.weightKg
                        completedWorkouts = report.completedWorkoutsCount
                        calories = report.totalCalories
                        saveLocally()
                        Toast.makeText(context, "Отчет успешно импортирован!", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { err ->
                        Toast.makeText(context, "Ошибка импорта: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    val extendedColors = LocalExtendedColors.current
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(extendedColors.gradientStart, extendedColors.gradientEnd)
    )

    var showPinDialog by remember { mutableStateOf(false) }
    var stepUpPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        if (!isUnlocked) {
            // ЭКРАН-ЗАГЛУШКА STEP-UP
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Защищенный профиль",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Прошло более 5 минут с момента входа. Подтвердите личность с помощью PIN-кода для просмотра персональных данных.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFFDDDDDD)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showPinDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Подтвердить личность (Step-up)")
                }
            }

            // ДИАЛОГ ВВОДА PIN ДЛЯ STEP-UP
            if (showPinDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showPinDialog = false
                        stepUpPin = ""
                        pinError = false
                    },
                    title = { Text("Подтверждение личности") },
                    text = {
                        Column {
                            Text("Введите ваш локальный PIN-код:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = stepUpPin,
                                onValueChange = {
                                    stepUpPin = it
                                    pinError = false
                                },
                                label = { Text("PIN-код") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                isError = pinError
                            )
                            if (pinError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Неверный PIN-код",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (authViewModel.verifyStepUpPin(stepUpPin)) {
                                showPinDialog = false
                                isUnlocked = true
                                stepUpPin = ""
                                pinError = false
                            } else {
                                pinError = true
                            }
                        }) {
                            Text("Подтвердить")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPinDialog = false
                            stepUpPin = ""
                            pinError = false
                        }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        } else {
            // ОСНОВНОЙ КОНТЕНТ ПРОФИЛЯ
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Профиль атлета",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEEEEEE)
                    )

                    // Кнопка выхода из системы (Logout)
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Выйти из аккаунта",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }

                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    isVisible = true
                }

                val alphaAnim by animateFloatAsState(
                    targetValue = if (isVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 1000),
                    label = "ProfileCardFadeIn"
                )

                val offsetYAnim by animateDpAsState(
                    targetValue = if (isVisible) 0.dp else 30.dp,
                    animationSpec = tween(durationMillis = 1000),
                    label = "ProfileCardSlideUp"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            alpha = alphaAnim,
                            translationY = offsetYAnim.value
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Имя: Павел Фомичёв",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Рост: 178 см", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Вес: $weight кг", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Цель: Набор мышечной массы",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = "Статистика и отчёты",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                    color = Color(0xFFEEEEEE)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(title = "Тренировок", value = completedWorkouts.toString(), modifier = Modifier.weight(1f))
                    StatCard(
                        title = "Калории",
                        value = "$calories ккал",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("fitness-report.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Экспорт отчета")
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Импорт отчета")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}