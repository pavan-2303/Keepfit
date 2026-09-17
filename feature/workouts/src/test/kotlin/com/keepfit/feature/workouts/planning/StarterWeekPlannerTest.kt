package com.keepfit.feature.workouts.planning

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterWeekPlannerTest {
    private val planner = StarterWeekPlanner()

    @Test
    fun identicalInputProducesIdenticalDraft() {
        val input = defaultInput()

        assertEquals(
            planner.createDraft(input).getOrThrow(),
            planner.createDraft(input).getOrThrow(),
        )
    }

    @Test
    fun bodyweightPlanNeverUsesUnavailableEquipment() {
        val draft = planner.createDraft(
            defaultInput(equipment = setOf(EquipmentOption.BODYWEIGHT)),
        ).getOrThrow()

        val exerciseKeys = draft.days.flatMap { day -> day.exercises }.map { it.key }
        exerciseKeys.forEach { key ->
            val definition = StarterExerciseCatalog.byKey.getValue(key)
            assertTrue(definition.equipment.contains(EquipmentOption.BODYWEIGHT))
        }
    }

    @Test
    fun avoidedExercisesAreExcluded() {
        val avoided = setOf("bodyweight-squat", "incline-push-up")

        val draft = planner.createDraft(defaultInput(avoidedExerciseKeys = avoided)).getOrThrow()

        assertFalse(draft.days.flatMap { it.exercises }.any { it.key in avoided })
    }

    @Test
    fun sessionLengthControlsExerciseCount() {
        val expectedCounts = mapOf(15 to 3, 30 to 5, 45 to 6, 60 to 7)

        expectedCounts.forEach { (minutes, expectedCount) ->
            val draft = planner.createDraft(defaultInput(sessionMinutes = minutes)).getOrThrow()
            assertTrue(draft.days.all { it.exercises.size == expectedCount })
        }
    }

    @Test
    fun everyGoalAndExperiencePathProducesUsableSessions() {
        JourneyGoal.entries.forEach { goal ->
            ExperienceLevel.entries.forEach { experience ->
                val draft = planner.createDraft(
                    defaultInput(goal = goal, experienceLevel = experience),
                ).getOrThrow()

                assertEquals(3, draft.days.size)
                assertTrue(draft.days.all { day -> day.exercises.all { it.targetSets in 2..4 } })
                assertTrue(draft.days.all { day -> day.exercises.all { it.targetReps.isNotBlank() } })
                assertTrue(draft.rationale.isNotBlank())
            }
        }
    }

    @Test
    fun supportsOneToFourSelectedDays() {
        val orderedDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.SATURDAY,
        )

        (1..4).forEach { count ->
            val selectedDays = orderedDays.take(count).toSet()
            val draft = planner.createDraft(defaultInput(preferredDays = selectedDays)).getOrThrow()

            assertEquals(orderedDays.take(count), draft.days.map { it.dayOfWeek })
        }
    }

    @Test
    fun missingDaysAreRejectedWithUsefulMessage() {
        val failure = planner.createDraft(defaultInput(preferredDays = emptySet())).exceptionOrNull()

        assertEquals("Choose at least one training day.", failure?.message)
    }

    @Test
    fun unknownAvoidKeyIsRejected() {
        val failure = planner.createDraft(
            defaultInput(avoidedExerciseKeys = setOf("not-a-real-exercise")),
        ).exceptionOrNull()

        assertEquals("One or more avoided exercises are unavailable.", failure?.message)
    }

    @Test
    fun moreThanFourDaysAndUnsupportedDurationAreRejected() {
        val tooManyDays = planner.createDraft(
            defaultInput(preferredDays = DayOfWeek.entries.take(5).toSet()),
        ).exceptionOrNull()
        val unsupportedDuration = planner.createDraft(
            defaultInput(sessionMinutes = 20),
        ).exceptionOrNull()

        assertEquals("Choose no more than four training days for a starter week.", tooManyDays?.message)
        assertEquals("Choose a supported session length.", unsupportedDuration?.message)
    }

    private fun defaultInput(
        goal: JourneyGoal = JourneyGoal.GENERAL_FITNESS,
        experienceLevel: ExperienceLevel = ExperienceLevel.BEGINNER,
        preferredDays: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.FRIDAY,
        ),
        sessionMinutes: Int = 30,
        equipment: Set<EquipmentOption> = setOf(
            EquipmentOption.BODYWEIGHT,
            EquipmentOption.DUMBBELLS,
        ),
        avoidedExerciseKeys: Set<String> = emptySet(),
    ) = StarterPlanInput(
        goal = goal,
        experienceLevel = experienceLevel,
        preferredDays = preferredDays,
        sessionMinutes = sessionMinutes,
        equipment = equipment,
        avoidedExerciseKeys = avoidedExerciseKeys,
    )
}
