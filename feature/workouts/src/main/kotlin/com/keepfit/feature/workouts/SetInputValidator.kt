package com.keepfit.feature.workouts

data class CompletedSetInput(
    val repetitions: Int,
    val weightKg: Double,
)

object SetInputValidator {
    fun validate(repetitions: String, weightKg: String): CompletedSetInput {
        val parsedRepetitions = repetitions.trim().toIntOrNull()
        require(parsedRepetitions != null && parsedRepetitions >= 0) {
            "Enter zero or more repetitions."
        }
        val parsedWeight = weightKg.trim().ifEmpty { "0" }.toDoubleOrNull()
        require(parsedWeight != null && parsedWeight >= 0.0) {
            "Enter a non-negative weight."
        }
        return CompletedSetInput(parsedRepetitions, parsedWeight)
    }
}

