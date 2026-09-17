package com.keepfit.feature.workouts.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.TemplateExercise
import com.keepfit.feature.workouts.data.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlanInformationArchitectureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun weeklyPlanLeadsAndSecondaryToolsAreNavigationNotTabs() {
        var opened = ""
        composeRule.setContent {
            MaterialTheme {
                PlanOverview(
                    templates = emptyList(),
                    schedule = emptyList(),
                    onAssign = { _, _ -> },
                    onClear = {},
                    onOpenStarterPlan = { opened = "starter" },
                    onOpenTemplates = { opened = "templates" },
                    onOpenExercises = { opened = "exercises" },
                    onOpenHistory = { opened = "history" },
                )
            }
        }

        composeRule.onNodeWithText("This week").assertIsDisplayed()
        composeRule.onNodeWithText("Plan tools").assertIsDisplayed()
        composeRule.onNodeWithText("Workouts").assertDoesNotExist()
        composeRule.onNodeWithText("Build my starter week").assertDoesNotExist()
        composeRule.onNodeWithText("Assign").assertDoesNotExist()
        composeRule.onNodeWithText("Create template").assertIsDisplayed()
        composeRule.onNodeWithText("Templates").performClick()
        composeRule.runOnIdle { assertEquals("templates", opened) }
    }

    @Test
    fun templateDetailsOfferDirectPrescriptionEditing() {
        val exercise = Exercise("exercise", "Goblet squat", "Legs", null, null, false)
        val template = WorkoutTemplate(
            id = "template",
            name = "Foundation",
            notes = null,
            exercises = listOf(TemplateExercise("row", exercise.id, exercise.name, 3, "8-10", null)),
        )
        composeRule.setContent {
            MaterialTheme {
                TemplateLibrary(
                    exercises = listOf(exercise),
                    templates = listOf(template),
                    onCreate = { _, _ -> },
                    onRename = { _, _ -> },
                    onAddExercises = { _, _ -> },
                    onUpdateExercise = { _, _, _, _ -> },
                    onRemoveExercise = { _, _ -> },
                    onDeleteTemplates = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Foundation").performClick()
        composeRule.onNodeWithText("3 sets · 8-10").assertIsDisplayed()
        composeRule.onNodeWithText("Edit template").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Edit targets for Goblet squat").performClick()
        composeRule.onNodeWithText("Edit Goblet squat").assertIsDisplayed()
        composeRule.onNodeWithText("Target sets").assertIsDisplayed()
        composeRule.onNodeWithText("Target reps").assertIsDisplayed()
    }

    @Test
    fun templateListSupportsExplicitMultiSelectDelete() {
        val exercise = Exercise("exercise", "Goblet squat", "Legs", null, null, false)
        val templates = listOf("Foundation", "Upper body").mapIndexed { index, name ->
            WorkoutTemplate(
                id = "template-$index",
                name = name,
                notes = null,
                exercises = listOf(TemplateExercise("row-$index", exercise.id, exercise.name, 3, "8-10", null)),
            )
        }
        composeRule.setContent {
            MaterialTheme {
                TemplateLibrary(
                    exercises = listOf(exercise),
                    templates = templates,
                    onCreate = { _, _ -> },
                    onRename = { _, _ -> },
                    onAddExercises = { _, _ -> },
                    onUpdateExercise = { _, _, _, _ -> },
                    onRemoveExercise = { _, _ -> },
                    onDeleteTemplates = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Select templates").performClick()
        composeRule.onNodeWithContentDescription("Select Foundation").performClick()
        composeRule.onNodeWithContentDescription("Select Upper body").performClick()
        composeRule.onNodeWithContentDescription("Delete 2 selected templates").performClick()
        composeRule.onNodeWithText("Delete 2 templates?").assertIsDisplayed()
    }
}
