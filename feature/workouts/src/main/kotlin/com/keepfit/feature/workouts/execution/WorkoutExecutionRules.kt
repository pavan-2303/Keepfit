package com.keepfit.feature.workouts.execution

import com.keepfit.feature.workouts.data.LoggedSet
import kotlin.math.round

enum class ProgressionChange {
    REPEAT,
    INCREASE_WEIGHT,
    INCREASE_REPS,
}

data class ProgressionSuggestion(
    val repetitions: Int,
    val weightKg: Double,
    val change: ProgressionChange,
    val explanation: String,
)

class WorkoutExecutionRules {
    fun nextSet(
        previousSets: List<LoggedSet>,
        completedSetCount: Int,
    ): LoggedSet? = previousSets.getOrNull(completedSetCount) ?: previousSets.lastOrNull()

    fun progression(
        previousSets: List<LoggedSet>,
        targetSets: Int?,
        targetReps: String?,
    ): ProgressionSuggestion? {
        val previous = previousSets.lastOrNull() ?: return null
        val requiredSets = targetSets?.takeIf { it > 0 }
            ?: return repeat(previous, "Repeat the prior values because no set target is available.")
        val upperTarget = targetReps.parseUpperTarget()
            ?: return repeat(
                previous,
                "Repeat the prior values because this repetition target is not numeric.",
            )
        val reachedTarget = previousSets.size >= requiredSets &&
            previousSets.take(requiredSets).all { it.repetitions >= upperTarget }
        if (!reachedTarget) {
            return repeat(
                previous,
                "Repeat the prior values until all $requiredSets target sets reach $upperTarget reps.",
            )
        }

        return if (previous.weightKg > 0.0) {
            val increase = roundedWeightIncrease(previous.weightKg)
            ProgressionSuggestion(
                repetitions = previous.repetitions,
                weightKg = previous.weightKg + increase,
                change = ProgressionChange.INCREASE_WEIGHT,
                explanation = "All $requiredSets target sets reached $upperTarget reps. " +
                    "Add ${increase.formatWeight()} kg next time.",
            )
        } else {
            val nextRepetitions = (previous.repetitions + 1).coerceAtMost(upperTarget + 2)
            if (nextRepetitions == previous.repetitions) {
                repeat(previous, "Repeat the prior values to keep this bodyweight target steady.")
            } else {
                ProgressionSuggestion(
                    repetitions = nextRepetitions,
                    weightKg = 0.0,
                    change = ProgressionChange.INCREASE_REPS,
                    explanation = "All $requiredSets target sets reached $upperTarget reps. " +
                        "Add one rep next time.",
                )
            }
        }
    }

    private fun repeat(previous: LoggedSet, explanation: String) = ProgressionSuggestion(
        repetitions = previous.repetitions,
        weightKg = previous.weightKg,
        change = ProgressionChange.REPEAT,
        explanation = explanation,
    )

    private fun roundedWeightIncrease(weightKg: Double): Double =
        (round(weightKg * 0.025 * 2.0) / 2.0).coerceIn(0.5, 2.5)
}

private fun String?.parseUpperTarget(): Int? = this
    ?.let { target -> Regex("\\d+").findAll(target).lastOrNull()?.value?.toIntOrNull() }

private fun Double.formatWeight(): String = if (this % 1.0 == 0.0) {
    toInt().toString()
} else {
    toString()
}
