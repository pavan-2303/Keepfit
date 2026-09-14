package com.keepfit.feature.workouts.today

import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.TemplateExercise
import com.keepfit.feature.workouts.data.WorkoutTemplate
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayWorkoutRulesTest {
    private val today = LocalDate.parse("2026-09-12")
    private val template = WorkoutTemplate(
        id = "template",
        name = "Foundation A",
        notes = null,
        exercises = (1..5).map { position ->
            TemplateExercise(
                id = "template-exercise-$position",
                exerciseId = "exercise-$position",
                exerciseName = "Exercise $position",
                targetSets = 3,
                targetReps = "8-10",
                notes = null,
            )
        },
    )

    @Test
    fun variantsAreDeterministicAndDoNotMutateTheTemplate() {
        val rules = TodayWorkoutRules()

        val shortened = rules.createVariant(template, TodayWorkoutVariant.SHORTENED)
        val repeated = rules.createVariant(template, TodayWorkoutVariant.SHORTENED)
        val minimum = rules.createVariant(template, TodayWorkoutVariant.MINIMUM)

        assertEquals(repeated, shortened)
        assertEquals(4, shortened.exercises.size)
        assertEquals(listOf(2, 2, 2, 2), shortened.exercises.map { it.targetSets })
        assertEquals(2, minimum.exercises.size)
        assertEquals(listOf(2, 2), minimum.exercises.map { it.targetSets })
        assertEquals(5, template.exercises.size)
        assertEquals(listOf(3, 3, 3, 3, 3), template.exercises.map { it.targetSets })
    }

    @Test
    fun activeSessionWinsOverEveryOtherTodayState() {
        val resolver = TodayWorkoutResolver()
        val active = ActiveWorkout(
            sessionId = "session",
            templateName = "In progress",
            workoutDate = today.minusDays(2),
            exercises = emptyList(),
        )

        val result = resolver.resolve(
            today = today,
            activeWorkout = active,
            candidates = listOf(candidate(originalDate = today)),
            sessions = listOf(completedSession()),
        )

        assertEquals(TodayWorkoutStatus.RESUME, result.status)
        assertEquals("session", result.activeSessionId)
    }

    @Test
    fun matchingLegacySessionSuppressesDuplicateStart() {
        val result = TodayWorkoutResolver().resolve(
            today = today,
            activeWorkout = null,
            candidates = listOf(candidate(originalDate = today)),
            sessions = listOf(completedSession()),
        )

        assertEquals(TodayWorkoutStatus.COMPLETED, result.status)
        assertEquals("Foundation A", result.primary?.title)
    }

    @Test
    fun confirmedOccurrenceWinsOverRecurringCandidate() {
        val recurring = candidate(originalDate = today)
        val shortened = recurring.copy(
            occurrenceId = "occurrence",
            decision = TodayWorkoutDecision.SHORTENED,
            exercises = recurring.exercises.take(2),
        )

        val result = TodayWorkoutResolver().resolve(
            today = today,
            activeWorkout = null,
            candidates = listOf(recurring, shortened),
            sessions = emptyList(),
        )

        assertEquals(TodayWorkoutStatus.PLANNED, result.status)
        assertEquals("occurrence", result.primary?.occurrenceId)
        assertEquals(TodayWorkoutDecision.SHORTENED, result.primary?.decision)
    }

    @Test
    fun completedRescheduledOccurrenceUsesItsScheduledDate() {
        val moved = candidate(originalDate = today.minusDays(1)).copy(
            occurrenceId = "moved",
            scheduledDate = today,
            decision = TodayWorkoutDecision.RESCHEDULED,
        )

        val result = TodayWorkoutResolver().resolve(
            today = today,
            activeWorkout = null,
            candidates = listOf(moved),
            sessions = listOf(
                TodayWorkoutSession(
                    id = "completed-moved",
                    occurrenceId = "moved",
                    plannedWorkoutId = moved.plannedWorkoutId,
                    workoutDate = today,
                    isCompleted = true,
                ),
            ),
        )

        assertEquals(TodayWorkoutStatus.COMPLETED, result.status)
    }

    @Test
    fun mostRecentUnresolvedWorkoutWithinSevenDaysIsRecoverable() {
        val result = TodayWorkoutResolver().resolve(
            today = today,
            activeWorkout = null,
            candidates = listOf(
                candidate(originalDate = today.minusDays(8)),
                candidate(originalDate = today.minusDays(5)),
                candidate(originalDate = today.minusDays(2)),
            ),
            sessions = emptyList(),
        )

        assertEquals(TodayWorkoutStatus.REST_DAY, result.status)
        assertEquals(today.minusDays(2), result.missed?.originalDate)
    }

    @Test
    fun completedSkippedAndOlderCandidatesAreNotOfferedAsMissed() {
        val completed = candidate(originalDate = today.minusDays(2))
        val skipped = candidate(originalDate = today.minusDays(3)).copy(
            occurrenceId = "skipped",
            decision = TodayWorkoutDecision.SKIPPED,
        )

        val result = TodayWorkoutResolver().resolve(
            today = today,
            activeWorkout = null,
            candidates = listOf(
                candidate(originalDate = today.minusDays(8)),
                completed,
                skipped,
            ),
            sessions = listOf(
                TodayWorkoutSession(
                    id = "completed-old",
                    occurrenceId = null,
                    plannedWorkoutId = completed.plannedWorkoutId,
                    workoutDate = completed.originalDate,
                    isCompleted = true,
                ),
            ),
        )

        assertNull(result.missed)
    }

    private fun candidate(originalDate: LocalDate): TodayWorkoutAction = TodayWorkoutAction(
        occurrenceId = null,
        plannedWorkoutId = "planned-${originalDate.dayOfMonth}",
        templateId = template.id,
        title = template.name,
        originalDate = originalDate,
        scheduledDate = originalDate,
        decision = TodayWorkoutDecision.FULL,
        exercises = TodayWorkoutRules().createVariant(template, TodayWorkoutVariant.FULL).exercises,
    )

    private fun completedSession() = TodayWorkoutSession(
        id = "completed",
        occurrenceId = null,
        plannedWorkoutId = "planned-${today.dayOfMonth}",
        workoutDate = today,
        isCompleted = true,
    )
}
