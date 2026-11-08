package com.example.fitnessapp_composeproject.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage

@Composable
fun AdaptiveWorkoutCard(
    title: String,
    durationMinutes: Int,
    exerciseCount: Int,
    imageUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            // ЛР 8: Передаём параметр mergeDescendants прямо в модификатор semantics
            .semantics(mergeDescendants = true) { }
    ) {
        BoxWithConstraints(modifier = Modifier.padding(12.dp)) {
            // Явно обращаемся к maxWidth внутри BoxWithConstraintsScope (ЛР 3)
            val isNarrowScreen = this.maxWidth < 400.dp

            if (isNarrowScreen) {
                // Узкий экран (телефон): Выравнивание через ConstraintLayout (ЛР 3)
                ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
                    val (image, titleRef, infoRef) = createRefs()

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Обложка тренировки $title",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .constrainAs(image) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                            }
                    )

                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.constrainAs(titleRef) {
                            top.linkTo(parent.top)
                            start.linkTo(image.end, margin = 12.dp)
                        }
                    )

                    Text(
                        text = "$durationMinutes мин • $exerciseCount упражнений",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.constrainAs(infoRef) {
                            top.linkTo(titleRef.bottom, margin = 8.dp)
                            start.linkTo(image.end, margin = 12.dp)
                        }
                    )
                }
            } else {
                // Широкий экран (планшет / горизонтальная ориентация)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$durationMinutes минут • $exerciseCount упражнения в программе",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}