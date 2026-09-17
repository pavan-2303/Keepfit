package com.keepfit.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMetricsTest {
    @Test
    fun bmiIsRoundedToOneDecimal() {
        assertEquals(25.0, requireNotNull(calculateBodyMassIndex(178.0, 79.2)), 0.0)
    }

    @Test
    fun bmiNeedsBothPositiveInputs() {
        assertNull(calculateBodyMassIndex(null, 79.2))
        assertNull(calculateBodyMassIndex(178.0, null))
        assertNull(calculateBodyMassIndex(0.0, 79.2))
    }

    @Test
    fun ageUsesWhetherBirthdayHasOccurredThisYear() {
        val today = LocalDate.of(2026, 9, 16)

        assertEquals(34, calculateAge(LocalDate.of(1992, 6, 15), today))
        assertEquals(33, calculateAge(LocalDate.of(1992, 12, 15), today))
        assertNull(calculateAge(LocalDate.of(2027, 1, 1), today))
    }
}
