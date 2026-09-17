package com.keepfit.feature.review.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.keepfit.feature.review.ReviewDraft
import com.keepfit.feature.review.ReviewDraftType
import com.keepfit.feature.review.ReviewWorkoutDay
import com.keepfit.feature.review.WeeklyReviewInput
import com.keepfit.feature.review.WeeklyReviewRules
import com.keepfit.feature.review.WeeklyReviewSnapshot
import com.keepfit.feature.review.WeeklyReviewUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WeeklyReviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyReviewSupportsSelectingEditingAndApprovingDraft() {
        val result = readyResult()
        var selected: ReviewDraft? = null
        var approved = false
        composeRule.setContent {
            MaterialTheme {
                WeeklyReviewScreen(
                    state = WeeklyReviewUiState(
                        isLoading = false,
                        snapshot = WeeklyReviewSnapshot.Ready(result),
                        selectedDraft = result.drafts.first(),
                    ),
                    onBack = {},
                    onRetry = {},
                    onSelectDraft = { selected = it },
                    onToggleEditing = {},
                    onChooseDate = {},
                    onApprove = { approved = true },
                    onDismiss = {},
                    onPause = {},
                    onResume = {},
                )
            }
        }

        composeRule.onNodeWithText("Your week, without a score").assertIsDisplayed()
        composeRule.onNodeWithText("Move").performClick()
        composeRule.onNodeWithText("Apply for next week").performClick()

        assertEquals(ReviewDraftType.MOVE_SESSION, selected?.type)
        assertTrue(approved)
    }

    @Test
    fun insufficientStateOffersANonBlockingControl() {
        val result = WeeklyReviewRules().evaluate(
            WeeklyReviewInput(
                window = WeeklyReviewRules().windowFor(LocalDate.of(2026, 9, 12)),
                workoutDays = emptyList(),
                comparisonCompletions = 0,
                olderCompletions = 0,
                performanceImproved = false,
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                WeeklyReviewScreen(
                    state = WeeklyReviewUiState(false, WeeklyReviewSnapshot.Insufficient(result)),
                    onBack = {}, onRetry = {}, onSelectDraft = {}, onToggleEditing = {},
                    onChooseDate = {}, onApprove = {}, onDismiss = {}, onPause = {}, onResume = {},
                )
            }
        }
        composeRule.onNodeWithText("Not enough workout history yet").assertIsDisplayed()

    }

    @Test
    fun pausedStateOffersResume() {
        composeRule.setContent {
            MaterialTheme {
                WeeklyReviewScreen(
                    state = WeeklyReviewUiState(false, WeeklyReviewSnapshot.Paused),
                    onBack = {}, onRetry = {}, onSelectDraft = {}, onToggleEditing = {},
                    onChooseDate = {}, onApprove = {}, onDismiss = {}, onPause = {}, onResume = {},
                )
            }
        }
        composeRule.onNodeWithText("Resume weekly reviews").assertIsDisplayed()
    }

    @Test
    fun readyReviewCanBeDismissedWithoutSelectingADraft() {
        val result = readyResult()
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                WeeklyReviewScreen(
                    state = WeeklyReviewUiState(false, WeeklyReviewSnapshot.Ready(result), result.drafts.first()),
                    onBack = {}, onRetry = {}, onSelectDraft = {}, onToggleEditing = {}, onChooseDate = {},
                    onApprove = {}, onDismiss = { dismissed = true }, onPause = {}, onResume = {},
                )
            }
        }

        composeRule.onNodeWithText("Dismiss this review").performScrollTo().performClick()

        assertTrue(dismissed)
    }

    private fun readyResult() = WeeklyReviewRules().evaluate(
        WeeklyReviewInput(
            window = WeeklyReviewRules().windowFor(LocalDate.of(2026, 9, 12)),
            workoutDays = listOf(
                ReviewWorkoutDay(LocalDate.of(2026, 8, 31), "monday", "template", "Foundation", true, false),
                ReviewWorkoutDay(LocalDate.of(2026, 9, 2), "wednesday", "template", "Foundation", true, false),
                ReviewWorkoutDay(LocalDate.of(2026, 9, 4), "friday", "template", "Foundation", true, true, 2, 4),
            ),
            comparisonCompletions = 1,
            olderCompletions = 1,
            performanceImproved = false,
        ),
    )
}
