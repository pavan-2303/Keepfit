package com.keepfit.feature.assistant.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.AssistantAccessStatus
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssistantScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disconnectedCoachUsesCompactConnectionStateWithoutLocalQuota() {
        var connectCount = 0
        show(
            state = AssistantUiState(accessState = AssistantAccessState()),
            onConnect = { connectCount++ },
        )

        composeRule.onNodeWithText("Not connected").assertIsDisplayed()
        composeRule.onNodeWithText("Connect OpenRouter").performClick()
        composeRule.onNodeWithText("Today's requests").assertDoesNotExist()
        composeRule.onNodeWithText("Your AI access").assertDoesNotExist()
        assertEquals(1, connectCount)
    }

    @Test
    fun connectedCoachPrioritizesConversationAndQueryComposer() {
        show(
            AssistantUiState(
                accessState = connectedAccess(),
                messages = listOf(
                    AssistantChatMessage("1", AssistantMessageRole.USER, "How am I doing?", 1L),
                    AssistantChatMessage("2", AssistantMessageRole.ASSISTANT, "You trained three times this week.", 2L),
                ),
            ),
        )

        composeRule.onNodeWithText("You trained three times this week.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send question").assertIsDisplayed()
        composeRule.onNodeWithText("Create reviewable proposal").assertDoesNotExist()
        composeRule.onNodeWithText("Plan next week").assertDoesNotExist()
    }

    @Test
    fun progressContextUseIsVisibleAfterAResponse() {
        show(
            AssistantUiState(
                accessState = connectedAccess(),
                messages = listOf(
                    AssistantChatMessage("2", AssistantMessageRole.ASSISTANT, "Consistency improved.", 2L),
                ),
                lastResponseUsedLocalContext = true,
            ),
        )

        composeRule.onNodeWithText("Recent Keepfit activity was included in the last answer.").assertIsDisplayed()
    }

    @Test
    fun disclosureExplainsProviderLimitsAndExcludedData() {
        show(AssistantUiState(showPrivacyDisclosure = true))

        composeRule.onNodeWithText("Before you connect").assertIsDisplayed()
        composeRule.onNodeWithText("Continue to OpenRouter").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Your OpenRouter account controls provider limits and credits. Keepfit does not impose its own daily request cap.",
        ).assertIsDisplayed()
    }

    @Test
    fun connectedCoachKeepsResponseAndComposerReachableAtCompactWidthAndLargeText() {
        val state = AssistantUiState(
            accessState = connectedAccess(),
            draftMessage = "What should I focus on next?",
            messages = listOf(
                AssistantChatMessage(
                    "1",
                    AssistantMessageRole.ASSISTANT,
                    "Keep the next workout simple and repeatable.",
                    1L,
                ),
            ),
        )

        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.width(360.dp).height(640.dp)) {
                    AssistantScreen(
                        uiState = state,
                        isEnabled = true,
                        validationMessage = null,
                        onDraftChange = {},
                        onSend = {},
                        onRetry = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Keep the next workout simple and repeatable.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send question").assertIsDisplayed()
    }

    private fun show(
        state: AssistantUiState,
        onConnect: () -> Unit = {},
    ) {
        composeRule.setContent {
            AssistantScreen(
                uiState = state,
                isEnabled = true,
                validationMessage = null,
                onDraftChange = {},
                onSend = {},
                onRetry = {},
                onConnect = onConnect,
            )
        }
    }

    private fun connectedAccess() = AssistantAccessState(
        status = AssistantAccessStatus.CONNECTED,
        hasCredential = true,
    )
}
