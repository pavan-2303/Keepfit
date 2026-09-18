package com.keepfit.feature.workouts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.keepfit.core.designsystem.KeepfitTheme
import com.keepfit.feature.workouts.planning.ActivityLevel
import com.keepfit.feature.workouts.planning.ExperienceLevel
import com.keepfit.feature.workouts.planning.JourneyGoal
import com.keepfit.feature.workouts.planning.LimitationArea
import com.keepfit.feature.workouts.planning.SleepDuration
import com.keepfit.feature.workouts.planning.StarterPlanStage
import com.keepfit.feature.workouts.planning.StarterPlanUiState
import com.keepfit.feature.workouts.planning.StarterWeekPlanner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StarterPlanScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setupExplainsOfflinePlanningAndShowsRouteChooser() {
        var generated = false
        var state by mutableStateOf(StarterPlanUiState())
        composeRule.setContent {
            KeepfitTheme {
                StarterPlanScreen(
                    state = state,
                    onCreateDraft = { generated = true },
                    onChoosePlanningRoute = { state = state.copy(stage = StarterPlanStage.ROUTE_CHOICE) },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Personal assessment · 1 of 9").assertIsDisplayed()
        composeRule.onNodeWithText("What result matters most right now?").assertIsDisplayed()
        repeat(8) { composeRule.onNodeWithText("Continue").performScrollTo().performClick() }
        composeRule.onNodeWithText("Review your starting point").assertIsDisplayed()
        composeRule.onNodeWithText("Choose how to plan").performScrollTo().performClick()
        composeRule.onNodeWithText("Choose your next step").assertIsDisplayed()
        composeRule.onNodeWithText("Build an offline starter week").performClick()

        composeRule.runOnIdle {
            assertTrue(generated)
        }
    }

    @Test
    fun routeChooserOffersManualAndAiDraftPaths() {
        var manual = false
        var coach = false
        composeRule.setContent {
            KeepfitTheme {
                StarterPlanScreen(
                    state = StarterPlanUiState(stage = StarterPlanStage.ROUTE_CHOICE),
                    onBack = {},
                    onPlanManually = { manual = true },
                    onAskCoach = { coach = true },
                )
            }
        }

        composeRule.onNodeWithText("Plan it myself").performClick()
        composeRule.onNodeWithText("Let AI personalize a draft").performClick()

        composeRule.runOnIdle {
            assertTrue(manual)
            assertTrue(coach)
        }
        composeRule.onNodeWithText("The AI draft is only a preview", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun assessmentCapturesActivitySleepAndLimitations() {
        var state by mutableStateOf(StarterPlanUiState())
        composeRule.setContent {
            KeepfitTheme {
                StarterPlanScreen(
                    state = state,
                    onBack = {},
                    onActivitySelected = { state = state.copy(input = state.input.copy(activityLevel = it)) },
                    onSleepDurationSelected = { state = state.copy(input = state.input.copy(sleepDuration = it)) },
                    onLimitationAreaToggled = {
                        state = state.copy(input = state.input.copy(limitationAreas = state.input.limitationAreas + it))
                    },
                    onLimitationNotesChanged = { state = state.copy(input = state.input.copy(limitationNotes = it)) },
                )
            }
        }

        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Highly active").performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Usually under 6 hours").performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Knees").performClick()
        composeRule.onNodeWithText("Movements or advice to avoid").performTextInput("Avoid jumping")

        composeRule.runOnIdle {
            assertTrue(state.input.activityLevel == ActivityLevel.HIGHLY_ACTIVE)
            assertTrue(state.input.sleepDuration == SleepDuration.UNDER_SIX_HOURS)
            assertTrue(LimitationArea.KNEES in state.input.limitationAreas)
            assertTrue(state.input.limitationNotes == "Avoid jumping")
        }
    }

    @Test
    fun previewExposesEditsAndRequiresExplicitApply() {
        val initialState = StarterPlanUiState()
        val draft = StarterWeekPlanner().createDraft(initialState.input).getOrThrow()
        var renamed = false
        var removed = false
        var applied = false
        composeRule.setContent {
            KeepfitTheme {
                StarterPlanScreen(
                    state = initialState.copy(stage = StarterPlanStage.PREVIEW, draft = draft),
                    onBack = {},
                    onTemplateNameChanged = { _, _ -> renamed = true },
                    onRemoveExercise = { _, _ -> removed = true },
                    onApply = { applied = true },
                )
            }
        }

        composeRule.onNodeWithText("Nothing changes until you tap Use this week.").assertIsDisplayed()
        composeRule.onNodeWithText("Foundation A").performTextReplacement("My Monday")
        composeRule.onNodeWithContentDescription("Remove Chair Squat").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Use this week"))
        composeRule.onNodeWithText("Use this week").performClick()

        composeRule.runOnIdle {
            assertTrue(renamed)
            assertTrue(removed)
            assertTrue(applied)
        }
    }

    @Test
    fun returningSetupDisplaysSavedGoalAndExperience() {
        val savedState = StarterPlanUiState(
            input = StarterPlanUiState().input.copy(
                goal = JourneyGoal.CONSISTENCY,
                experienceLevel = ExperienceLevel.INTERMEDIATE,
            ),
        )
        composeRule.setContent {
            KeepfitTheme { StarterPlanScreen(state = savedState, onBack = {}) }
        }

        composeRule.onNodeWithText("Build consistency").assertIsSelected()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Comfortable with the basics").assertIsSelected()
    }

    @Test
    fun setupRemainsUsableAtCompactWidthAndLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KeepfitTheme {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        StarterPlanScreen(state = StarterPlanUiState(), onBack = {})
                    }
                }
            }
        }

        composeRule.onNodeWithText("What result matters most right now?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Where are you starting from?").performScrollTo().assertIsDisplayed()
    }
}
