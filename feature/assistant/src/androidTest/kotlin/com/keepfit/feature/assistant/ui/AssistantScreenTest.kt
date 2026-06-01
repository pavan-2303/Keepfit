package com.keepfit.feature.assistant.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutDay
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutExercise
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.data.AssistantUiState
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssistantScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failedSendStateKeepsDraftVisible() {
        composeRule.setContent {
            AssistantScreen(
                uiState = AssistantUiState(
                    draftMessage = "Keep my drafted question",
                    errorMessage = "Assistant request failed.",
                ),
                isEnabled = true,
                validationMessage = null,
                onBack = {},
                onDraftChange = {},
                onSend = {},
                onSummarizeProgress = {},
                onDraftWeeklyPlan = {},
                onApplyDraftPlan = {},
                onDismissDraftPlan = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Keep my drafted question").assertIsDisplayed()
        composeRule.onNodeWithText("Last request failed").assertIsDisplayed()
        composeRule.onNodeWithText("Assistant request failed.").assertIsDisplayed()
    }

    @Test
    fun draftPlanReviewShowsActionsAndInvokesCallbacks() {
        var applyCount = 0
        var dismissCount = 0

        composeRule.setContent {
            AssistantScreen(
                uiState = AssistantUiState(
                    pendingDraftPlan = AssistantDraftWorkoutPlan(
                        name = "Balanced Foundation Plan",
                        overview = "A conservative 4-day split.",
                        days = listOf(
                            AssistantDraftWorkoutDay(
                                dayOfWeek = DayOfWeek.MONDAY,
                                templateName = "Upper Body A",
                                notes = "Focus on controlled movements.",
                                exercises = listOf(
                                    AssistantDraftWorkoutExercise(
                                        name = "Bench Press",
                                        targetSets = 3,
                                        targetReps = "8-12",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                isEnabled = true,
                validationMessage = null,
                onBack = {},
                onDraftChange = {},
                onSend = {},
                onSummarizeProgress = {},
                onDraftWeeklyPlan = {},
                onApplyDraftPlan = { applyCount++ },
                onDismissDraftPlan = { dismissCount++ },
                onRetry = {},
            )
        }

        composeRule.onNodeWithText("Balanced Foundation Plan").assertIsDisplayed()
        composeRule.onNodeWithText("Apply draft").assertIsDisplayed()
        composeRule.onNodeWithText("Dismiss draft").assertIsDisplayed()
        composeRule.onNodeWithText("Apply draft").performClick()
        composeRule.onNodeWithText("Dismiss draft").performClick()

        assertEquals(1, applyCount)
        assertEquals(1, dismissCount)
    }
}
