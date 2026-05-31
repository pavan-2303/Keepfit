package com.keepfit.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsInputValidatorTest {
    @Test
    fun acceptsValidRestTimer() {
        assertEquals(120, SettingsInputValidator.validateRestTimerSeconds("120").getOrThrow())
    }

    @Test
    fun rejectsOutOfRangeRestTimer() {
        assertEquals(
            "Rest timer must be between 15 and 600 seconds.",
            SettingsInputValidator.validateRestTimerSeconds("10").exceptionOrNull()?.message,
        )
    }

    @Test
    fun acceptsValidReminderTime() {
        val result = SettingsInputValidator.validateReminder(true, "18", "30")
        assertTrue(result.isSuccess)
        assertEquals(18, result.getOrThrow().hour)
        assertEquals(30, result.getOrThrow().minute)
    }

    @Test
    fun rejectsInvalidMinute() {
        assertEquals(
            "Minute must be between 0 and 59.",
            SettingsInputValidator.validateReminder(true, "18", "61").exceptionOrNull()?.message,
        )
    }
}
