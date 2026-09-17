package com.keepfit.feature.workouts

data class ExerciseInput(
    val name: String,
    val muscleGroup: String,
    val equipment: String? = null,
    val targetMuscle: String? = null,
    val secondaryMuscles: String? = null,
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
        equipment: String,
        targetMuscle: String,
        secondaryMuscles: String,
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
                equipment = equipment.trim().ifEmpty { null },
                targetMuscle = targetMuscle.trim().ifEmpty { null },
                secondaryMuscles = secondaryMuscles.trim().ifEmpty { null },
                instructions = instructions.trim().ifEmpty { null },
                notes = notes.trim().ifEmpty { null },
                isBodyweight = isBodyweight,
            ),
        )
    }
}
