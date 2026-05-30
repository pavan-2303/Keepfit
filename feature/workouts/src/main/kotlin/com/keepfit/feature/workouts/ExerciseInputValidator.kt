package com.keepfit.feature.workouts

data class ExerciseInput(
    val name: String,
    val muscleGroup: String,
    val instructions: String?,
    val notes: String?,
    val isBodyweight: Boolean,
)

sealed interface ExerciseValidationResult {
    data class Valid(val input: ExerciseInput) : ExerciseValidationResult
    data class Invalid(val message: String) : ExerciseValidationResult
}

object ExerciseInputValidator {
    fun validate(
        name: String,
        muscleGroup: String,
        instructions: String,
        notes: String,
        isBodyweight: Boolean,
    ): ExerciseValidationResult {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            return ExerciseValidationResult.Invalid("Enter an exercise name.")
        }
        val normalizedGroup = muscleGroup.trim()
        if (normalizedGroup.isEmpty()) {
            return ExerciseValidationResult.Invalid("Enter a muscle group.")
        }
        return ExerciseValidationResult.Valid(
            ExerciseInput(
                name = normalizedName,
                muscleGroup = normalizedGroup,
                instructions = instructions.trim().ifEmpty { null },
                notes = notes.trim().ifEmpty { null },
                isBodyweight = isBodyweight,
            ),
        )
    }
}

