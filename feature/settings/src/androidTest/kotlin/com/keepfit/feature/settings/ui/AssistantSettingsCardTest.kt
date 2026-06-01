package com.keepfit.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AssistantSettingsCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun openAssistantIsDisabledWhenAssistantIsOff() {
        composeRule.setContent {
            AssistantSettingsCard(
                assistantEnabled = false,
                canOpenAssistant = false,
                onAssistantEnabledChange = {},
                onSaveAssistant = {},
                onTestAssistantConnection = {},
                onOpenAssistant = {},
            )
        }

        composeRule.onNodeWithText("Ollama Cloud assistant").assertIsDisplayed()
        composeRule.onNodeWithText("Save assistant").assertIsDisplayed()
        composeRule.onNodeWithText("Test connection").assertIsDisplayed()
        composeRule.onNodeWithText("Open assistant").assertIsNotEnabled()
    }

    @Test
    fun openAssistantIsEnabledWhenAssistantIsOn() {
        composeRule.setContent {
            AssistantSettingsCard(
                assistantEnabled = true,
                canOpenAssistant = true,
                onAssistantEnabledChange = {},
                onSaveAssistant = {},
                onTestAssistantConnection = {},
                onOpenAssistant = {},
            )
        }

        composeRule.onNodeWithText("Open assistant").assertIsEnabled()
    }
}
