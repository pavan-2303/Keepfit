package com.keepfit.feature.settings

import java.time.DayOfWeek

data class ReminderInput(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)

data class WeeklyReminderInput(
    val enabled: Boolean,
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val minute: Int,
)

object SettingsInputValidator {
    fun validateBackupPassphrase(passphrase: String): Result<String> {
        val normalized = passphrase.trim()
        if (normalized.length < 8) {
            return Result.failure(IllegalArgumentException("Backup passphrase must be at least 8 characters."))
        }
        return Result.success(normalized)
    }

    fun validateRestTimerSeconds(seconds: String): Result<Int> {
        val value = seconds.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Rest timer must be a whole number."))
        if (value !in 15..600) {
            return Result.failure(IllegalArgumentException("Rest timer must be between 15 and 600 seconds."))
        }
        return Result.success(value)
    }

    fun validateReminder(enabled: Boolean, hour: String, minute: String): Result<ReminderInput> {
        val parsedHour = hour.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Hour must be between 0 and 23."))
        val parsedMinute = minute.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Minute must be between 0 and 59."))
        if (parsedHour !in 0..23) {
            return Result.failure(IllegalArgumentException("Hour must be between 0 and 23."))
        }
        if (parsedMinute !in 0..59) {
            return Result.failure(IllegalArgumentException("Minute must be between 0 and 59."))
        }
        return Result.success(ReminderInput(enabled, parsedHour, parsedMinute))
    }

    fun validateWeeklyReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: String,
        minute: String,
    ): Result<WeeklyReminderInput> =
        validateReminder(enabled, hour, minute).map { reminder ->
            WeeklyReminderInput(
                enabled = reminder.enabled,
                dayOfWeek = DayOfWeek.of(dayOfWeekOrdinal.coerceIn(1, 7)),
                hour = reminder.hour,
                minute = reminder.minute,
            )
        }
}
