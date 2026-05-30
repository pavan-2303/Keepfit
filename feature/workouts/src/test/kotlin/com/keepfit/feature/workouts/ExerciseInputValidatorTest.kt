package com.keepfit.feature.workouts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseInputValidatorTest {
    @Test
    fun validExerciseIsNormalized() {
        val result = ExerciseInputValidator.validate(
            name = "  Bench Press ",
            muscleGroup = " Chest ",
            instructions = "",
            notes = "  Keep wrists stacked ",
            isBodyweight = false,
        )

        assertTrue(result is ExerciseValidationResult.Valid)
        assertEquals(
            ExerciseInput(
                name = "Bench Press",
                muscleGroup = "Chest",
                instructions = null,
                notes = "Keep wrists stacked",
                isBodyweight = false,
            ),
            (result as ExerciseValidationResult.Valid).input,
        )
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(
            ExerciseValidationResult.Invalid("Enter an exercise name."),
            ExerciseInputValidator.validate("", "Chest", "", "", false),
        )
    }
}

