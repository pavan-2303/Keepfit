package com.keepfit.feature.review

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReviewRulesTest {
    private val rules = WeeklyReviewRules()

    @Test
    fun `window uses the most recently completed monday through sunday`() {
        val window = rules.windowFor(LocalDate.of(2026, 9, 12))

        assertEquals(LocalDate.of(2026, 8, 31), window.reviewStart)
        assertEquals(LocalDate.of(2026, 9, 6), window.reviewEnd)
        assertEquals(LocalDate.of(2026, 8, 24), window.comparisonStart)
        assertEquals(LocalDate.of(2026, 9, 14), window.nextWeekStart)
        assertEquals(LocalDate.of(2026, 9, 20), window.nextWeekEnd)
    }

    @Test
    fun `monday reviews the week that ended yesterday`() {
        val window = rules.windowFor(LocalDate.of(2026, 9, 14))

        assertEquals(LocalDate.of(2026, 9, 7), window.reviewStart)
        assertEquals(LocalDate.of(2026, 9, 13), window.reviewEnd)
    }

    @Test
    fun `return is recognized after an empty comparison week`() {
        val result = rules.evaluate(
            input(
                planned = listOf(day(1, completed = true), day(3, completed = false)),
                comparisonCompletions = 0,
                olderCompletions = 2,
            ),
        )

        assertTrue(result.achievements.any { it.kind == ReviewAchievementKind.RETURNED })
        assertTrue(result.achievements.any { it.kind == ReviewAchievementKind.PARTIAL_CONSISTENCY })
    }

    @Test
    fun `low completion prioritizes minimum and move drafts and caps at two`() {
        val result = rules.evaluate(
            input(
                planned = listOf(
                    day(1, completed = false),
                    day(3, completed = false),
                    day(5, completed = true, energy = 2, difficulty = 4),
                ),
            ),
        )

        assertEquals(2, result.drafts.size)
        assertEquals(ReviewDraftType.MINIMUM_SESSION, result.drafts[0].type)
        assertEquals(ReviewDraftType.MOVE_SESSION, result.drafts[1].type)
        assertTrue(result.drafts.all { it.reason.isNotBlank() })
    }

    @Test
    fun `strong completion and improvement proposes one bounded set increase`() {
        val result = rules.evaluate(
            input(
                planned = listOf(
                    day(1, completed = true, energy = 4, difficulty = 3),
                    day(3, completed = true, energy = 4, difficulty = 3),
                    day(5, completed = true, energy = 5, difficulty = 2),
                ),
                performanceImproved = true,
            ),
        )

        assertEquals(listOf(ReviewDraftType.ADD_ONE_SET), result.drafts.map { it.type })
        assertTrue(result.achievements.any { it.kind == ReviewAchievementKind.IMPROVED })
    }

    @Test
    fun `partial completion proposes a shorter session without failure language`() {
        val result = rules.evaluate(
            input(
                planned = listOf(
                    day(1, completed = true),
                    day(3, completed = true),
                    day(5, completed = false),
                ),
            ),
        )

        assertEquals(ReviewDraftType.SHORTEN_SESSION, result.drafts.first().type)
        assertFalse(result.motivation.contains("fail", ignoreCase = true))
        assertFalse(result.motivation.contains("streak", ignoreCase = true))
    }

    @Test
    fun `no workout evidence produces insufficient data and no drafts`() {
        val result = rules.evaluate(input(planned = emptyList()))

        assertFalse(result.hasEnoughData)
        assertTrue(result.drafts.isEmpty())
    }

    @Test
    fun `available nutrition and step trends are included without becoming required`() {
        val withSignals = rules.evaluate(
            input(
                planned = listOf(day(1, completed = true)),
                nutrition = ReviewNutritionSignal(4, 110.0, 3, 95.0),
                steps = ReviewStepsSignal(8_200, 7_500),
            ),
        )
        val withoutSignals = rules.evaluate(input(planned = listOf(day(1, completed = true))))

        assertEquals(2, withSignals.supportingSignals.size)
        assertTrue(withoutSignals.supportingSignals.isEmpty())
        assertTrue(withoutSignals.hasEnoughData)
    }

    @Test
    fun `meal quality supplies its own neutral weekly evidence`() {
        val result = rules.evaluate(
            input(
                planned = listOf(day(1, completed = true)),
                nutrition = ReviewNutritionSignal(
                    loggedDays = 3,
                    averageProteinGrams = 0.0,
                    comparisonLoggedDays = 2,
                    comparisonAverageProteinGrams = 0.0,
                    summary = "Meal check-ins were made on 3 of 7 days, including 4 balanced choices.",
                ),
            ),
        )

        assertEquals(
            "Meal check-ins were made on 3 of 7 days, including 4 balanced choices.",
            result.supportingSignals.single(),
        )
    }

    private fun input(
        planned: List<ReviewWorkoutDay>,
        comparisonCompletions: Int = 1,
        olderCompletions: Int = 0,
        performanceImproved: Boolean = false,
        nutrition: ReviewNutritionSignal? = null,
        steps: ReviewStepsSignal? = null,
    ) = WeeklyReviewInput(
        window = rules.windowFor(LocalDate.of(2026, 9, 12)),
        workoutDays = planned,
        comparisonCompletions = comparisonCompletions,
        olderCompletions = olderCompletions,
        performanceImproved = performanceImproved,
        nutrition = nutrition,
        steps = steps,
    )

    private fun day(
        dayOffset: Long,
        completed: Boolean,
        energy: Int? = null,
        difficulty: Int? = null,
    ) = ReviewWorkoutDay(
        date = LocalDate.of(2026, 8, 31).plusDays(dayOffset),
        plannedWorkoutId = "plan-$dayOffset",
        templateId = "template-$dayOffset",
        title = "Workout $dayOffset",
        planned = true,
        completed = completed,
        energyLevel = energy,
        difficulty = difficulty,
    )
}
