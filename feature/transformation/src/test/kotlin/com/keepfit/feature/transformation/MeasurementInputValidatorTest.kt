package com.keepfit.feature.transformation

import com.keepfit.feature.transformation.data.calculateBmi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementInputValidatorTest {
    @Test
    fun acceptsValidMeasurementInput() {
        val result = MeasurementInputValidator.validate(
            measurementDate = "2026-05-31",
            weightKg = "79.2",
            waistCm = "86",
            chestCm = "",
            hipsCm = "",
            leftArmCm = "",
            rightArmCm = "",
            leftThighCm = "",
            rightThighCm = "",
            notes = "Leaner",
        )

        assertTrue(result.isSuccess)
        assertEquals(79.2, requireNotNull(result.getOrThrow().weightKg), 0.0)
        assertEquals(86.0, requireNotNull(result.getOrThrow().waistCm), 0.0)
    }

    @Test
    fun rejectsMissingAllMeasurementsAndNotes() {
        val result = MeasurementInputValidator.validate(
            measurementDate = "2026-05-31",
            weightKg = "",
            waistCm = "",
            chestCm = "",
            hipsCm = "",
            leftArmCm = "",
            rightArmCm = "",
            leftThighCm = "",
            rightThighCm = "",
            notes = "",
        )

        assertEquals("Add at least one measurement or note.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsNonPositiveValues() {
        val result = MeasurementInputValidator.validate(
            measurementDate = "2026-05-31",
            weightKg = "0",
            waistCm = "",
            chestCm = "",
            hipsCm = "",
            leftArmCm = "",
            rightArmCm = "",
            leftThighCm = "",
            rightThighCm = "",
            notes = "",
        )

        assertEquals("Weight must be greater than 0.", result.exceptionOrNull()?.message)
    }

    @Test
    fun calculatesRoundedBmi() {
        assertEquals(25.0, requireNotNull(calculateBmi(178.0, 79.2)), 0.0)
    }
}
