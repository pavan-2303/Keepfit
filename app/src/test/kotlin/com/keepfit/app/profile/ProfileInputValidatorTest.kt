package com.keepfit.app.profile

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileInputValidatorTest {
    @Test
    fun validNameAndHeightCreateProfileInput() {
        val result = ProfileInputValidator.validate(
            displayName = "  Pavan  ",
            heightCm = "178",
            birthDate = LocalDate.of(1992, 6, 15),
            startingWeightKg = "79.2",
            today = LocalDate.of(2026, 9, 16),
        )

        assertTrue(result is ProfileValidationResult.Valid)
        assertEquals(
            ProfileInput(
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = LocalDate.of(1992, 6, 15),
                startingWeightKg = 79.2,
            ),
            (result as ProfileValidationResult.Valid).input,
        )
    }

    @Test
    fun blankNameIsRejected() {
        val result = ProfileInputValidator.validate(
            displayName = " ",
            heightCm = "",
        )

        assertEquals(
            ProfileValidationResult.Invalid("Enter your name to continue."),
            result,
        )
    }

    @Test
    fun invalidHeightIsRejected() {
        val result = ProfileInputValidator.validate(
            displayName = "Pavan",
            heightCm = "0",
        )

        assertEquals(
            ProfileValidationResult.Invalid("Enter a height between 50 and 260 cm."),
            result,
        )
    }

    @Test
    fun futureBirthDateIsRejected() {
        val result = ProfileInputValidator.validate(
            displayName = "Pavan",
            heightCm = "178",
            birthDate = LocalDate.of(2027, 1, 1),
            startingWeightKg = "",
            today = LocalDate.of(2026, 9, 16),
        )

        assertEquals(
            ProfileValidationResult.Invalid("Choose a birth date in the past."),
            result,
        )
    }

    @Test
    fun invalidStartingWeightIsRejected() {
        val result = ProfileInputValidator.validate(
            displayName = "Pavan",
            heightCm = "178",
            birthDate = null,
            startingWeightKg = "0",
        )

        assertEquals(
            ProfileValidationResult.Invalid("Enter a starting weight between 10 and 500 kg."),
            result,
        )
    }
}
