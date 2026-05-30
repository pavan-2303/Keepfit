package com.keepfit.app.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileInputValidatorTest {
    @Test
    fun validNameAndHeightCreateProfileInput() {
        val result = ProfileInputValidator.validate(
            displayName = "  Pavan  ",
            heightCm = "178",
        )

        assertTrue(result is ProfileValidationResult.Valid)
        assertEquals(
            ProfileInput(displayName = "Pavan", heightCm = 178.0),
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
}

