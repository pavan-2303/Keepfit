package com.keepfit.feature.workouts.execution

import com.keepfit.feature.workouts.data.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutExecutionRulesTest {
    private val rules = WorkoutExecutionRules()

    @Test
    fun nextSetUsesMatchingPreviousPositionThenFallsBackToLastSet() {
        val previous = listOf(
            loggedSet("one", repetitions = 10, weightKg = 20.0),
            loggedSet("two", repetitions = 9, weightKg = 20.0),
            loggedSet("three", repetitions = 8, weightKg = 20.0),
        )

        assertEquals(previous[0], rules.nextSet(previous, completedSetCount = 0))
        assertEquals(previous[1], rules.nextSet(previous, completedSetCount = 1))
        assertEquals(previous[2], rules.nextSet(previous, completedSetCount = 5))
        assertNull(rules.nextSet(emptyList(), completedSetCount = 0))
    }

    @Test
    fun completedWeightedTargetGetsSmallBoundedIncrease() {
        val prior = listOf(
            loggedSet("one", 10, 20.0),
            loggedSet("two", 10, 20.0),
            loggedSet("three", 10, 20.0),
        )

        val suggestion = rules.progression(prior, targetSets = 3, targetReps = "8-10")

        assertEquals(ProgressionChange.INCREASE_WEIGHT, suggestion?.change)
        assertEquals(10, suggestion?.repetitions)
        assertEquals(20.5, suggestion?.weightKg ?: 0.0, 0.0)
        assertEquals("All 3 target sets reached 10 reps. Add 0.5 kg next time.", suggestion?.explanation)
    }

    @Test
    fun weightedIncreaseIsClampedAtTwoPointFiveKilograms() {
        val prior = List(3) { index -> loggedSet("$index", 5, 200.0) }

        val suggestion = rules.progression(prior, targetSets = 3, targetReps = "5")

        assertEquals(202.5, suggestion?.weightKg ?: 0.0, 0.0)
    }

    @Test
    fun completedBodyweightTargetAddsOneRepWithinCap() {
        val prior = List(2) { index -> loggedSet("$index", 10, 0.0) }

        val suggestion = rules.progression(prior, targetSets = 2, targetReps = "8-10")

        assertEquals(ProgressionChange.INCREASE_REPS, suggestion?.change)
        assertEquals(11, suggestion?.repetitions)
        assertEquals(0.0, suggestion?.weightKg ?: -1.0, 0.0)
    }

    @Test
    fun incompleteOrNonNumericTargetsExplainWhyValuesRepeat() {
        val prior = listOf(
            loggedSet("one", 8, 15.0),
            loggedSet("two", 7, 15.0),
        )

        val incomplete = rules.progression(prior, targetSets = 3, targetReps = "8-10")
        val nonNumeric = rules.progression(prior, targetSets = 2, targetReps = "comfortable")

        assertEquals(ProgressionChange.REPEAT, incomplete?.change)
        assertEquals("Repeat the prior values until all 3 target sets reach 10 reps.", incomplete?.explanation)
        assertEquals(ProgressionChange.REPEAT, nonNumeric?.change)
        assertEquals("Repeat the prior values because this repetition target is not numeric.", nonNumeric?.explanation)
    }

    @Test
    fun noHistoryProducesNoProgressionSuggestion() {
        assertNull(rules.progression(emptyList(), targetSets = 3, targetReps = "8-10"))
    }

    private fun loggedSet(id: String, repetitions: Int, weightKg: Double) = LoggedSet(
        id = id,
        repetitions = repetitions,
        weightKg = weightKg,
    )
}
