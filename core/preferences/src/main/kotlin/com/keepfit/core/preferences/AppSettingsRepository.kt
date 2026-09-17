package com.keepfit.core.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit)

    suspend fun updateAssistantSettings(enabled: Boolean)

    suspend fun updateCredentialRecoveryEnabled(enabled: Boolean) {}

    suspend fun initializeProfileSettings(profileId: String, inheritLegacy: Boolean) {}

    suspend fun refreshReminders() {}

    suspend fun readProfileSettings(profileId: String): AppSettings = observeSettings().first()

    suspend fun restoreProfileSettings(profileId: String, settings: AppSettings) {}

    suspend fun updateRestTimerSeconds(seconds: Int)

    suspend fun updateReduceMotion(enabled: Boolean) {}

    suspend fun updateWeeklyReviewPaused(paused: Boolean) {}

    suspend fun updateNutritionTracking(depth: NutritionTrackingDepth, targetRangePercent: Int) {}

    suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int)

    suspend fun updateTransformationReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: Int,
        minute: Int,
    )
}
