package com.example.fitnessapp_composeproject

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.fitnessapp_composeproject.presentation.components.AppButton
import com.example.fitnessapp_composeproject.presentation.components.ButtonSize
import com.example.fitnessapp_composeproject.presentation.components.ButtonStyle
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class AppButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appButton_disabled_doesNotTriggerClick() {
        var clicked = false

        composeTestRule.setContent {
            AppButton(
                text = "Тест",
                onClick = { clicked = true },
                enabled = false, // Кнопка выключена
                style = ButtonStyle.PRIMARY,
                size = ButtonSize.MEDIUM
            )
        }

        // Проверяем через SemanticsMatcher, что кнопка имеет состояние disabled (isEnable == false)
        composeTestRule
            .onNodeWithText("Тест")
            .assertIsNotEnabled() // Проверка стандартного семантического свойства enabled/disabled

        // Пытаемся кликнуть по выключенной кнопке
        composeTestRule
            .onNodeWithText("Тест")
            .performClick()

        // Убеждаемся, что лямбда клика не выполнилась
        assertFalse("Клик не должен срабатывать при enabled = false", clicked)
    }
}