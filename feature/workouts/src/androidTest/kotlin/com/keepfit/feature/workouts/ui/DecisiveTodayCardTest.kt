package com.keepfit.feature.workouts.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keepfit.core.designsystem.KeepfitTheme
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayChangeType
import com.keepfit.feature.workouts.today.TodayExercise
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutDecision
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutState
import com.keepfit.feature.workouts.today.TodayWorkoutStatus
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DecisiveTodayCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-09-12")
    private val action = TodayWorkoutAction(
        occurrenceId = null,
        plannedWorkoutId = "planned",
        templateId = "template",
        title = "Foundation A",
        originalDate = today,
        scheduledDate = today,
        decision = TodayWorkoutDecision.FULL,
        exercises = listOf(
            TodayExercise("te-1", "exercise-1", "Chair squat", 3, "8-10"),
            TodayExercise("te-2", "exercise-2", "Incline push-up", 3, "8-10"),
        ),
    )

    @Test
    fun plannedStateHasOneDominantStartAndAdaptationActions() {
        var started = false
        var requested: TodayChangeRequest? = null
        composeRule.setContent {
            KeepfitTheme {
                DecisiveTodayCard(
                    state = TodayWorkoutState(TodayWorkoutStatus.PLANNED, primary = action),
                    today = today,
                    onStart = { started = true },
                    onPreviewChange = { requested = it },
                )
            }
        }

        composeRule.onNodeWithText("Today's move").assertIsDisplayed()
        composeRule.onNodeWithText("Foundation A").assertIsDisplayed()
        composeRule.onNodeWithText("Start now").performClick()
        composeRule.runOnIdle { assertTrue(started) }

        composeRule.onNodeWithText("Adapt today").performClick()
        composeRule.onNodeWithText("Shorter workout").performClick()
        composeRule.runOnIdle { assertEquals(TodayChangeType.SHORTEN, requested?.type) }
    }

    @Test
    fun previewExplainsTheChangeAndRequiresConfirmation() {
        var confirmed = false
        var dismissed = false
        val request = TodayChangeRequest(
            plannedWorkoutId = "planned",
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.MINIMUM,
        )
        composeRule.setContent {
            KeepfitTheme {
                DecisiveTodayCard(
                    state = TodayWorkoutState(TodayWorkoutStatus.PLANNED, primary = action),
                    preview = TodayWorkoutPreview(
                        request = request,
                        title = action.title,
                        currentDescription = "2 exercises planned for today",
                        proposedDescription = "2 essential exercises for up to two sets each",
                        reason = "A minimum session protects the habit on a constrained day.",
                        exercises = action.exercises,
                    ),
                    today = today,
                    onConfirmPreview = { confirmed = true },
                    onDismissPreview = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText("Review today's change").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing changes until you confirm.").assertIsDisplayed()
        composeRule.onNodeWithText("Keep current").performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }

        composeRule.onNodeWithText("Confirm change").performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun skippedAndMissedStatesOfferClearRecovery() {
        var restored = false
        var recovered: TodayChangeRequest? = null
        composeRule.setContent {
            KeepfitTheme {
                DecisiveTodayCard(
                    state = TodayWorkoutState(
                        status = TodayWorkoutStatus.SKIPPED,
                        primary = action.copy(
                            occurrenceId = "occurrence",
                            decision = TodayWorkoutDecision.SKIPPED,
                        ),
                        missed = action.copy(originalDate = today.minusDays(2), scheduledDate = today.minusDays(2)),
                    ),
                    today = today,
                    onRestore = { restored = true },
                    onPreviewChange = { recovered = it },
                )
            }
        }

        composeRule.onNodeWithText("Skipped for today").assertIsDisplayed()
        composeRule.onNodeWithText("Restore workout").performClick()
        composeRule.runOnIdle { assertTrue(restored) }

        composeRule.onNodeWithText("Recover Foundation A").performClick()
        composeRule.runOnIdle {
            assertEquals(TodayChangeType.RESCHEDULE, recovered?.type)
            assertEquals(today, recovered?.targetDate)
        }
    }

    @Test
    fun loadingResumeCompletedRescheduledAndRestStatesStayActionable() {
        val currentState = mutableStateOf(TodayWorkoutState())
        var resumed = false
        var restored = false
        var openedWorkouts = false
        composeRule.setContent {
            KeepfitTheme {
                DecisiveTodayCard(
                    state = currentState.value,
                    today = today,
                    onResume = { resumed = true },
                    onRestore = { restored = true },
                    onOpenWorkouts = { openedWorkouts = true },
                )
            }
        }

        composeRule.onNodeWithText("Finding your next move").assertIsDisplayed()

        composeRule.runOnIdle {
            currentState.value = TodayWorkoutState(
                status = TodayWorkoutStatus.RESUME,
                activeWorkout = ActiveWorkout("session", "Foundation A", today, emptyList()),
            )
        }
        composeRule.onNodeWithText("Resume workout").performClick()
        composeRule.runOnIdle { assertTrue(resumed) }

        composeRule.runOnIdle {
            currentState.value = TodayWorkoutState(
                status = TodayWorkoutStatus.COMPLETED,
                primary = action,
            )
        }
        composeRule.onNodeWithText("Workout complete").assertIsDisplayed()
        composeRule.onNodeWithText("View workouts").performClick()
        composeRule.runOnIdle { assertTrue(openedWorkouts) }

        composeRule.runOnIdle {
            currentState.value = TodayWorkoutState(
                status = TodayWorkoutStatus.RESCHEDULED,
                primary = action.copy(
                    occurrenceId = "rescheduled",
                    scheduledDate = today.plusDays(2),
                    decision = TodayWorkoutDecision.RESCHEDULED,
                ),
            )
        }
        composeRule.onNodeWithText("Restore workout").performClick()
        composeRule.runOnIdle { assertTrue(restored) }

        composeRule.runOnIdle {
            currentState.value = TodayWorkoutState(status = TodayWorkoutStatus.REST_DAY)
        }
        composeRule.onNodeWithText("Recovery day").assertIsDisplayed()
        composeRule.onNodeWithText("Adjust plan").assertIsDisplayed()
    }

    @Test
    fun plannedStateKeepsItsDecisionAndActionsReachableAtCompactWidthAndLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KeepfitTheme(reduceMotion = true) {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        DecisiveTodayCard(
                            state = TodayWorkoutState(TodayWorkoutStatus.PLANNED, primary = action),
                            today = today,
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("Today's move").assertIsDisplayed()
        composeRule.onNodeWithText("Foundation A").assertIsDisplayed()
        composeRule.onNodeWithText("Start now").assertIsDisplayed()
        composeRule.onNodeWithText("Adapt today").assertIsDisplayed()
    }

    @Test
    fun recoveryCopyStacksTheExplanationBelowTheTitle() {
        composeRule.setContent {
            KeepfitTheme {
                DecisiveTodayCard(
                    state = TodayWorkoutState(TodayWorkoutStatus.REST_DAY),
                    today = today,
                )
            }
        }

        val titleBounds = composeRule.onNodeWithText("Recovery day").fetchSemanticsNode().boundsInRoot
        val descriptionBounds = composeRule
            .onNodeWithText("No workout planned. Adjust the week if needed.")
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(descriptionBounds.top >= titleBounds.bottom)
    }
}
