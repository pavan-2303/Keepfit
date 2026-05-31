package com.keepfit.core.preferences

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit)

    suspend fun updateAssistantSettings(enabled: Boolean)

    suspend fun updateRestTimerSeconds(seconds: Int)

    suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int)

    suspend fun updateTransformationReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: Int,
        minute: Int,
    )
}
