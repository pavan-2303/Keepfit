package com.keepfit.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
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

    @Test
    fun recoveryGuidanceSeparatesAutomaticAndCompleteBackup() {
        composeRule.setContent { BackupRecoveryOverview() }

        composeRule.onNodeWithText("Automatic recovery").assertIsDisplayed()
        composeRule.onNodeWithText("not guaranteed", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Private photos, imported exercise media, and OpenRouter access are not included", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("encrypted Keepfit backup for complete recovery", substring = true).assertIsDisplayed()
    }

    @Test
    fun connectedDeviceCanOfferSeparateVerifiedCredentialRecovery() {
        composeRule.setContent {
            AssistantSettingsCard(
                onTestAssistantConnection = {},
                onOpenAssistant = {},
                credentialRecoveryAvailable = true,
            )
        }

        composeRule.onNodeWithText("Recover access after reinstall").assertIsDisplayed()
        composeRule.onNodeWithText("separate from fitness backup", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("verifies it before reuse", substring = true).assertIsDisplayed()
    }

    @Test
    fun motionPreferenceExplainsWhatReducedMotionChanges() {
        var enabled = false
        composeRule.setContent {
            MotionPreferenceRow(
                reduceMotion = false,
                onReduceMotionChanged = { enabled = it },
            )
        }

        composeRule.onNodeWithText("Reduce motion").assertIsDisplayed()
        composeRule.onNodeWithText("Keeps every state change visible", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Reduce motion").performClick()
        composeRule.runOnIdle { assertTrue(enabled) }
    }
}
