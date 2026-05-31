package com.keepfit.core.preferences

import java.time.DayOfWeek

enum class WeightUnit {
    KG,
    LB,
}

enum class MeasurementUnit {
    CM,
    IN,
}

data class ReminderTime(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)

data class WeeklyReminderTime(
    val enabled: Boolean,
    val dayOfWeek: DayOfWeek,
    val hour: Int,
    val minute: Int,
)

data class AppSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val measurementUnit: MeasurementUnit = MeasurementUnit.CM,
    val restTimerSeconds: Int = 90,
    val workoutReminder: ReminderTime = ReminderTime(enabled = false, hour = 18, minute = 0),
    val transformationReminder: WeeklyReminderTime = WeeklyReminderTime(
        enabled = false,
        dayOfWeek = DayOfWeek.SUNDAY,
        hour = 9,
        minute = 0,
    ),
)
