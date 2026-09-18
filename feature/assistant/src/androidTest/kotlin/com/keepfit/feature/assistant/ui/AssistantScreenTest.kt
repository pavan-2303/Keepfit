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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.AssistantAccessStatus
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantUiState
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutDay
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutExercise
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.conversation.AssistantConversation
import com.keepfit.feature.assistant.conversation.CoachPersona
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
        composeRule.onNodeWithText("Create plan").assertIsDisplayed()
    }

    @Test
    fun planDraftIsPreviewedAndRequiresExplicitApply() {
        var applied = 0
        show(
            state = AssistantUiState(
                accessState = connectedAccess(),
                pendingDraftPlan = AssistantDraftWorkoutPlan(
                    name = "Strong start",
                    overview = "A manageable week.",
                    days = listOf(
                        AssistantDraftWorkoutDay(
                            dayOfWeek = java.time.DayOfWeek.MONDAY,
                            templateName = "Full body A",
                            exercises = listOf(
                                AssistantDraftWorkoutExercise("exercise-id", "Goblet squat", 3, "8-12"),
                            ),
                        ),
                    ),
                ),
            ),
            onApplyPlan = { applied++ },
        )

        composeRule.onNodeWithText("Review before applying").assertIsDisplayed()
        composeRule.onNodeWithText("Goblet squat · 3 × 8-12").assertIsDisplayed()
        assertEquals(0, applied)
        composeRule.onNodeWithText("Apply plan").performClick()
        assertEquals(1, applied)
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
    fun assistantMarkdownIsRenderedWithoutRawMarkers() {
        show(
            AssistantUiState(
                accessState = connectedAccess(),
                messages = listOf(
                    AssistantChatMessage(
                        "2",
                        AssistantMessageRole.ASSISTANT,
                        "**Train consistently**",
                        2L,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Train consistently").assertIsDisplayed()
        composeRule.onNodeWithText("**Train consistently**").assertDoesNotExist()
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
    fun coachPickerExplainsAllPersonalitiesAndReturnsSelection() {
        var selected: CoachPersona? = null
        show(
            state = AssistantUiState(
                accessState = connectedAccess(),
                showCoachPicker = true,
            ),
            onChooseCoach = { selected = it },
        )

        composeRule.onNodeWithText("Choose your Coach").assertIsDisplayed()
        composeRule.onNodeWithText("Mira").assertIsDisplayed()
        composeRule.onAllNodesWithText("Rook")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Choose Atlas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Choose Rook").performScrollTo().performClick()

        assertEquals(CoachPersona.ROOK, selected)
    }

    @Test
    fun disconnectedCoachHidesPersonaChoicesUntilOpenRouterIsConnected() {
        show(
            state = AssistantUiState(
                accessState = AssistantAccessState(),
                showCoachPicker = true,
            ),
        )

        composeRule.onNodeWithText("Connect OpenRouter").assertIsDisplayed()
        composeRule.onNodeWithText("Choose your Coach").assertDoesNotExist()
        composeRule.onNodeWithText("Mira").assertDoesNotExist()
        composeRule.onNodeWithText("Choose Rook").assertDoesNotExist()
    }

    @Test
    fun conversationHistoryLetsUserSwitchThreads() {
        var selectedId: String? = null
        val conversations = listOf(
            conversation("thread-2", CoachPersona.ROOK, "Fix my weekly plan", 2L),
            conversation("thread-1", CoachPersona.MIRA, "Build consistency", 1L),
        )
        show(
            state = AssistantUiState(
                accessState = connectedAccess(),
                conversations = conversations,
                activeConversationId = "thread-2",
            ),
            onSelectConversation = { selectedId = it },
        )

        composeRule.onAllNodesWithText("Rook")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Conversation history").performClick()
        composeRule.onNodeWithText("Build consistency").performClick()

        assertEquals("thread-1", selectedId)
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
        onChooseCoach: (CoachPersona) -> Unit = {},
        onSelectConversation: (String) -> Unit = {},
        onApplyPlan: () -> Unit = {},
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
                onChooseCoach = onChooseCoach,
                onSelectConversation = onSelectConversation,
                onApplyPlan = onApplyPlan,
            )
        }
    }

    private fun conversation(
        id: String,
        coach: CoachPersona,
        title: String,
        updatedAt: Long,
    ) = AssistantConversation(
        id = id,
        coach = coach,
        title = title,
        createdAtUtcEpochMillis = updatedAt,
        updatedAtUtcEpochMillis = updatedAt,
    )

    private fun connectedAccess() = AssistantAccessState(
        status = AssistantAccessStatus.CONNECTED,
        hasCredential = true,
    )
}
