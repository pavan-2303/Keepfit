package com.keepfit.feature.workouts.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.keepfit.core.designsystem.KeepfitTheme
import com.keepfit.feature.workouts.data.ActiveExercise
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.LoggedSet
import com.keepfit.feature.workouts.data.WorkoutFeedback
import com.keepfit.feature.workouts.execution.ProgressionChange
import com.keepfit.feature.workouts.execution.ProgressionSuggestion
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActiveWorkoutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsProgressTargetAndPrefilledPreviousValues() {
        composeRule.setContent {
            KeepfitTheme {
                ActiveWorkoutScreen(
                    workout = workout,
                    exercises = library,
                    timerSeconds = null,
                    snackbarHostState = SnackbarHostState(),
                    isWriting = false,
                    onAddSet = { _, _, _ -> },
                    onRepeatPrevious = {},
                    onSaveNotes = { _, _ -> },
                    onStartTimer = {},
                    onSubstitute = { _, _ -> },
                    onMinimum = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("1 of 6 sets complete").assertIsDisplayed()
        composeRule.onAllNodesWithText("Target 3 sets · 8-10 reps")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Repetitions for Exercise 1")
            .assertTextContains("10")
        composeRule.onNodeWithContentDescription("Weight for Exercise 1")
            .assertTextContains("20")
        composeRule.onAllNodesWithText("Add 0.5 kg next time.", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun repeatAndMinimumRequireClearActions() {
        var repeated = ""
        var minimumCalled = false
        composeRule.setContent {
            KeepfitTheme {
                ActiveWorkoutScreen(
                    workout = workout,
                    exercises = library,
                    timerSeconds = 47,
                    snackbarHostState = SnackbarHostState(),
                    isWriting = false,
                    onAddSet = { _, _, _ -> },
                    onRepeatPrevious = { repeated = it },
                    onSaveNotes = { _, _ -> },
                    onStartTimer = {},
                    onSubstitute = { _, _ -> },
                    onMinimum = { minimumCalled = true },
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("Rest 0:47").assertIsDisplayed()
        composeRule.onAllNodesWithText("Repeat previous")[0].performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals("active-log-1", repeated) }

        composeRule.onNodeWithText("Adapt session").performClick()
        composeRule.onNodeWithText("Use minimum session").performClick()
        composeRule.onNodeWithText("Keep first 2 exercises").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm minimum session").performClick()
        composeRule.runOnIdle { assertTrue(minimumCalled) }
    }

    @Test
    fun completionSupportsFeedbackOrSkippingIt() {
        var feedback: WorkoutFeedback? = WorkoutFeedback()
        var calls = 0
        composeRule.setContent {
            KeepfitTheme {
                ActiveWorkoutScreen(
                    workout = workout,
                    exercises = library,
                    timerSeconds = null,
                    snackbarHostState = SnackbarHostState(),
                    isWriting = false,
                    onAddSet = { _, _, _ -> },
                    onRepeatPrevious = {},
                    onSaveNotes = { _, _ -> },
                    onStartTimer = {},
                    onSubstitute = { _, _ -> },
                    onMinimum = {},
                    onComplete = { feedback = it; calls++ },
                )
            }
        }

        composeRule.onNodeWithText("Finish workout").performClick()
        composeRule.onNodeWithContentDescription("Energy 4").performClick()
        composeRule.onNodeWithContentDescription("Difficulty 3").performClick()
        composeRule.onNodeWithText("Save and finish").performClick()
        composeRule.runOnIdle {
            assertEquals(1, calls)
            assertEquals(WorkoutFeedback(4, 3), feedback)
        }
    }

    @Test
    fun supportedExerciseGuideOpensAndClosesWithoutChangingWorkoutState() {
        val guidedWorkout = workout.copy(
            exercises = workout.exercises.mapIndexed { index, exercise ->
                if (index == 0) {
                    exercise.copy(
                        exerciseId = "5ca9f46f-1ae9-5ff8-9627-32cd73c56a13",
                        exerciseName = "Barbell bench press",
                    )
                } else {
                    exercise
                }
            },
        )
        composeRule.setContent {
            KeepfitTheme {
                ActiveWorkoutScreen(
                    workout = guidedWorkout,
                    exercises = library,
                    timerSeconds = null,
                    snackbarHostState = SnackbarHostState(),
                    isWriting = false,
                    onAddSet = { _, _, _ -> },
                    onRepeatPrevious = {},
                    onSaveNotes = { _, _ -> },
                    onStartTimer = {},
                    onSubstitute = { _, _ -> },
                    onMinimum = {},
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithText("View guide").performScrollTo().performClick()
        composeRule.onNodeWithText("Barbell bench press guide").assertIsDisplayed()
        composeRule.onNodeWithText("Close guide").performClick()
        composeRule.onNodeWithText("1 of 6 sets complete").assertIsDisplayed()
        composeRule.onAllNodesWithText("Set 1  ·  20 kg × 10")[0].assertIsDisplayed()
    }

    private val workout = ActiveWorkout(
        sessionId = "active-session",
        templateName = "Foundation A",
        workoutDate = LocalDate.parse("2026-09-12"),
        exercises = (1..2).map { index ->
            ActiveExercise(
                exerciseLogId = "active-log-$index",
                exerciseId = "exercise-$index",
                exerciseName = "Exercise $index",
                notes = null,
                targetSets = 3,
                targetReps = "8-10",
                previousSets = List(3) { position -> LoggedSet("previous-$position", 10, 20.0) },
                nextSetSuggestion = LoggedSet("previous-1", 10, 20.0),
                progressionSuggestion = ProgressionSuggestion(
                    repetitions = 10,
                    weightKg = 20.5,
                    change = ProgressionChange.INCREASE_WEIGHT,
                    explanation = "All 3 target sets reached 10 reps. Add 0.5 kg next time.",
                ),
                sets = if (index == 1) listOf(LoggedSet("set-1", 10, 20.0)) else emptyList(),
            )
        },
    )

    private val library = (1..3).map { index ->
        Exercise("exercise-$index", "Exercise $index", "General", null, null, false)
    }
}
