package com.keepfit.feature.workouts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.keepfit.core.designsystem.KeepfitTheme
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.feature.workouts.planning.ExperienceLevel
import com.keepfit.feature.workouts.planning.JourneyGoal
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
    fun setupExplainsOfflinePlanningAndGeneratesPreview() {
        var generated = false
        var nutritionDepth: NutritionTrackingDepth? = null
        composeRule.setContent {
            KeepfitTheme {
                StarterPlanScreen(
                    state = StarterPlanUiState(),
                    onCreateDraft = { generated = true },
                    onNutritionDepthSelected = { nutritionDepth = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Step 2 of 8").assertIsDisplayed()
        composeRule.onNodeWithText("What do you want to work toward?").assertIsDisplayed()
        repeat(5) { composeRule.onNodeWithText("Continue").performScrollTo().performClick() }
        composeRule.onNodeWithText("Anything you prefer not to do?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("How much nutrition detail helps you?").assertIsDisplayed()
        composeRule.onNodeWithText("Calories + protein").performClick()
        composeRule.onNodeWithText("Review my week").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertTrue(generated)
            assertTrue(nutritionDepth == NutritionTrackingDepth.CALORIES_PROTEIN)
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

        composeRule.onNodeWithText("What do you want to work toward?").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("How familiar is training?").assertIsDisplayed()
    }
}
