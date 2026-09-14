package com.keepfit.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AssistantSettingsCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectionSettingsPointToTheDedicatedCoachTab() {
        composeRule.setContent {
            AssistantSettingsCard(
                onTestAssistantConnection = {},
                onOpenAssistant = {},
            )
        }

        composeRule.onNodeWithText("OpenRouter assistant").assertIsDisplayed()
        composeRule.onNodeWithText("Check saved access").assertIsDisplayed()
        composeRule.onNodeWithText("Open assistant").assertIsDisplayed()
        composeRule.onNodeWithText("There is no Keepfit daily request cap and no pasted API key.", substring = true).assertIsDisplayed()
    }
}
