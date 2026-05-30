package com.keepfit.feature.workouts

import org.junit.Assert.assertEquals
import org.junit.Test

class SetInputValidatorTest {
    @Test
    fun validCompletedSetIsParsed() {
        assertEquals(
            CompletedSetInput(repetitions = 10, weightKg = 62.5),
            SetInputValidator.validate("10", "62.5"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWeightIsRejected() {
        SetInputValidator.validate("10", "-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingRepetitionsAreRejected() {
        SetInputValidator.validate("", "20")
    }
}

