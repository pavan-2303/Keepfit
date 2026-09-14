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

enum class NutritionTrackingDepth {
    DETAILED_MACROS,
    CALORIES_PROTEIN,
    MEAL_QUALITY,
    DISABLED,
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
    val weeklyReviewPaused: Boolean = false,
    val nutritionTrackingDepth: NutritionTrackingDepth = NutritionTrackingDepth.DETAILED_MACROS,
    val nutritionTargetRangePercent: Int = 10,
    val workoutReminder: ReminderTime = ReminderTime(enabled = false, hour = 18, minute = 0),
    val transformationReminder: WeeklyReminderTime = WeeklyReminderTime(
        enabled = false,
        dayOfWeek = DayOfWeek.SUNDAY,
        hour = 9,
        minute = 0,
    ),
    val assistant: AssistantSettings = AssistantSettings(),
)
