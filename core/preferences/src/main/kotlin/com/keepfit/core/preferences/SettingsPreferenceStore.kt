package com.keepfit.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.DayOfWeek

internal val Context.keepfitDataStore by preferencesDataStore(name = "keepfit_settings")

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreAppSettingsRepository(
    private val context: Context,
    private val reminderScheduler: ReminderScheduler,
    private val activeProfileStore: ActiveProfileStore = DataStoreActiveProfileStore(context),
) : AppSettingsRepository {
    override fun observeSettings(): Flow<AppSettings> =
        activeProfileStore.observeActiveProfileId().flatMapLatest { profileId ->
            context.keepfitDataStore.data.map { preferences ->
                preferences.toAppSettings(profileId)
            }
        }

    override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.weightUnit(profileId)] = weightUnit.ordinal
            preferences[Keys.measurementUnit(profileId)] = measurementUnit.ordinal
        }
        syncReminders()
    }

    override suspend fun updateAssistantSettings(enabled: Boolean) {
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.ASSISTANT_ENABLED] = enabled
        }
    }

    override suspend fun updateCredentialRecoveryEnabled(enabled: Boolean) {
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.CREDENTIAL_RECOVERY_ENABLED] = enabled
        }
    }

    override suspend fun initializeProfileSettings(profileId: String, inheritLegacy: Boolean) {
        context.keepfitDataStore.edit { preferences ->
            if (preferences[Keys.initialized(profileId)] == true) return@edit
            if (inheritLegacy) {
                preferences.copyLegacyInt(Keys.WEIGHT_UNIT, Keys.weightUnit(profileId))
                preferences.copyLegacyInt(Keys.MEASUREMENT_UNIT, Keys.measurementUnit(profileId))
                preferences.copyLegacyInt(Keys.REST_TIMER_SECONDS, Keys.restTimerSeconds(profileId))
                preferences.copyLegacyBoolean(Keys.WEEKLY_REVIEW_PAUSED, Keys.weeklyReviewPaused(profileId))
                preferences.copyLegacyInt(Keys.NUTRITION_TRACKING_DEPTH, Keys.nutritionTrackingDepth(profileId))
                preferences.copyLegacyInt(Keys.NUTRITION_TARGET_RANGE_PERCENT, Keys.nutritionTargetRangePercent(profileId))
                preferences.copyLegacyBoolean(Keys.WORKOUT_REMINDER_ENABLED, Keys.workoutReminderEnabled(profileId))
                preferences.copyLegacyInt(Keys.WORKOUT_REMINDER_HOUR, Keys.workoutReminderHour(profileId))
                preferences.copyLegacyInt(Keys.WORKOUT_REMINDER_MINUTE, Keys.workoutReminderMinute(profileId))
                preferences.copyLegacyBoolean(
                    Keys.TRANSFORMATION_REMINDER_ENABLED,
                    Keys.transformationReminderEnabled(profileId),
                )
                preferences.copyLegacyInt(Keys.TRANSFORMATION_REMINDER_DAY, Keys.transformationReminderDay(profileId))
                preferences.copyLegacyInt(Keys.TRANSFORMATION_REMINDER_HOUR, Keys.transformationReminderHour(profileId))
                preferences.copyLegacyInt(Keys.TRANSFORMATION_REMINDER_MINUTE, Keys.transformationReminderMinute(profileId))
            }
            preferences[Keys.initialized(profileId)] = true
        }
    }

    override suspend fun updateRestTimerSeconds(seconds: Int) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.restTimerSeconds(profileId)] = seconds
        }
        syncReminders()
    }

    override suspend fun updateReduceMotion(enabled: Boolean) {
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.REDUCE_MOTION] = enabled
        }
    }

    override suspend fun updateWeeklyReviewPaused(paused: Boolean) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.weeklyReviewPaused(profileId)] = paused
        }
    }

    override suspend fun updateNutritionTracking(
        depth: NutritionTrackingDepth,
        targetRangePercent: Int,
    ) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.nutritionTrackingDepth(profileId)] = depth.ordinal
            preferences[Keys.nutritionTargetRangePercent(profileId)] = targetRangePercent.toSupportedNutritionRange()
        }
    }

    override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.workoutReminderEnabled(profileId)] = enabled
            preferences[Keys.workoutReminderHour(profileId)] = hour
            preferences[Keys.workoutReminderMinute(profileId)] = minute
        }
        syncReminders()
    }

    override suspend fun updateTransformationReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: Int,
        minute: Int,
    ) {
        val profileId = activeProfileStore.observeActiveProfileId().first()
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.transformationReminderEnabled(profileId)] = enabled
            preferences[Keys.transformationReminderDay(profileId)] = dayOfWeekOrdinal
            preferences[Keys.transformationReminderHour(profileId)] = hour
            preferences[Keys.transformationReminderMinute(profileId)] = minute
        }
        syncReminders()
    }

    private suspend fun syncReminders() {
        reminderScheduler.sync(observeSettings().first())
    }

    override suspend fun refreshReminders() = syncReminders()

    override suspend fun readProfileSettings(profileId: String): AppSettings =
        context.keepfitDataStore.data.first().toAppSettings(profileId)

    override suspend fun restoreProfileSettings(profileId: String, settings: AppSettings) {
        context.keepfitDataStore.edit { preferences ->
            preferences[Keys.weightUnit(profileId)] = settings.weightUnit.ordinal
            preferences[Keys.measurementUnit(profileId)] = settings.measurementUnit.ordinal
            preferences[Keys.restTimerSeconds(profileId)] = settings.restTimerSeconds
            preferences[Keys.weeklyReviewPaused(profileId)] = settings.weeklyReviewPaused
            preferences[Keys.nutritionTrackingDepth(profileId)] = settings.nutritionTrackingDepth.ordinal
            preferences[Keys.nutritionTargetRangePercent(profileId)] = settings.nutritionTargetRangePercent
            preferences[Keys.workoutReminderEnabled(profileId)] = settings.workoutReminder.enabled
            preferences[Keys.workoutReminderHour(profileId)] = settings.workoutReminder.hour
            preferences[Keys.workoutReminderMinute(profileId)] = settings.workoutReminder.minute
            preferences[Keys.transformationReminderEnabled(profileId)] = settings.transformationReminder.enabled
            preferences[Keys.transformationReminderDay(profileId)] = settings.transformationReminder.dayOfWeek.value
            preferences[Keys.transformationReminderHour(profileId)] = settings.transformationReminder.hour
            preferences[Keys.transformationReminderMinute(profileId)] = settings.transformationReminder.minute
            preferences[Keys.initialized(profileId)] = true
        }
    }

    private fun Preferences.toAppSettings(profileId: String?): AppSettings =
        AppSettings(
            weightUnit = WeightUnit.entries[this[Keys.weightUnit(profileId)] ?: WeightUnit.KG.ordinal],
            measurementUnit = MeasurementUnit.entries[this[Keys.measurementUnit(profileId)] ?: MeasurementUnit.CM.ordinal],
            restTimerSeconds = this[Keys.restTimerSeconds(profileId)] ?: 90,
            weeklyReviewPaused = this[Keys.weeklyReviewPaused(profileId)] ?: false,
            reduceMotion = this[Keys.REDUCE_MOTION] ?: false,
            credentialRecoveryEnabled = this[Keys.CREDENTIAL_RECOVERY_ENABLED] ?: false,
            nutritionTrackingDepth = NutritionTrackingDepth.entries.getOrElse(
                this[Keys.nutritionTrackingDepth(profileId)] ?: NutritionTrackingDepth.DETAILED_MACROS.ordinal,
            ) { NutritionTrackingDepth.DETAILED_MACROS },
            nutritionTargetRangePercent = (this[Keys.nutritionTargetRangePercent(profileId)] ?: 10).toSupportedNutritionRange(),
            workoutReminder = ReminderTime(
                enabled = this[Keys.workoutReminderEnabled(profileId)] ?: false,
                hour = this[Keys.workoutReminderHour(profileId)] ?: 18,
                minute = this[Keys.workoutReminderMinute(profileId)] ?: 0,
            ),
            transformationReminder = WeeklyReminderTime(
                enabled = this[Keys.transformationReminderEnabled(profileId)] ?: false,
                dayOfWeek = DayOfWeek.of(
                    (this[Keys.transformationReminderDay(profileId)] ?: DayOfWeek.SUNDAY.value).coerceIn(1, 7),
                ),
                hour = this[Keys.transformationReminderHour(profileId)] ?: 9,
                minute = this[Keys.transformationReminderMinute(profileId)] ?: 0,
            ),
            assistant = AssistantSettings(
                enabled = this[Keys.ASSISTANT_ENABLED] ?: false,
            ),
        )

    private object Keys {
        val WEIGHT_UNIT = intPreferencesKey("weight_unit")
        val MEASUREMENT_UNIT = intPreferencesKey("measurement_unit")
        val ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
        val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
        val WEEKLY_REVIEW_PAUSED = booleanPreferencesKey("weekly_review_paused")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val CREDENTIAL_RECOVERY_ENABLED = booleanPreferencesKey("credential_recovery_enabled")
        val NUTRITION_TRACKING_DEPTH = intPreferencesKey("nutrition_tracking_depth")
        val NUTRITION_TARGET_RANGE_PERCENT = intPreferencesKey("nutrition_target_range_percent")
        val WORKOUT_REMINDER_ENABLED = booleanPreferencesKey("workout_reminder_enabled")
        val WORKOUT_REMINDER_HOUR = intPreferencesKey("workout_reminder_hour")
        val WORKOUT_REMINDER_MINUTE = intPreferencesKey("workout_reminder_minute")
        val TRANSFORMATION_REMINDER_ENABLED = booleanPreferencesKey("transformation_reminder_enabled")
        val TRANSFORMATION_REMINDER_DAY = intPreferencesKey("transformation_reminder_day")
        val TRANSFORMATION_REMINDER_HOUR = intPreferencesKey("transformation_reminder_hour")
        val TRANSFORMATION_REMINDER_MINUTE = intPreferencesKey("transformation_reminder_minute")

        fun initialized(profileId: String) = booleanPreferencesKey("profile.$profileId.initialized")
        fun weightUnit(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.weight_unit") } ?: WEIGHT_UNIT
        fun measurementUnit(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.measurement_unit") } ?: MEASUREMENT_UNIT
        fun restTimerSeconds(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.rest_timer_seconds") } ?: REST_TIMER_SECONDS
        fun weeklyReviewPaused(profileId: String?) = profileId?.let { booleanPreferencesKey("profile.$it.weekly_review_paused") } ?: WEEKLY_REVIEW_PAUSED
        fun nutritionTrackingDepth(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.nutrition_tracking_depth") } ?: NUTRITION_TRACKING_DEPTH
        fun nutritionTargetRangePercent(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.nutrition_target_range_percent") } ?: NUTRITION_TARGET_RANGE_PERCENT
        fun workoutReminderEnabled(profileId: String?) = profileId?.let { booleanPreferencesKey("profile.$it.workout_reminder_enabled") } ?: WORKOUT_REMINDER_ENABLED
        fun workoutReminderHour(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.workout_reminder_hour") } ?: WORKOUT_REMINDER_HOUR
        fun workoutReminderMinute(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.workout_reminder_minute") } ?: WORKOUT_REMINDER_MINUTE
        fun transformationReminderEnabled(profileId: String?) = profileId?.let { booleanPreferencesKey("profile.$it.transformation_reminder_enabled") } ?: TRANSFORMATION_REMINDER_ENABLED
        fun transformationReminderDay(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.transformation_reminder_day") } ?: TRANSFORMATION_REMINDER_DAY
        fun transformationReminderHour(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.transformation_reminder_hour") } ?: TRANSFORMATION_REMINDER_HOUR
        fun transformationReminderMinute(profileId: String?) = profileId?.let { intPreferencesKey("profile.$it.transformation_reminder_minute") } ?: TRANSFORMATION_REMINDER_MINUTE
    }
}

private fun androidx.datastore.preferences.core.MutablePreferences.copyLegacyInt(
    source: Preferences.Key<Int>,
    target: Preferences.Key<Int>,
) {
    this[source]?.let { this[target] = it }
}

private fun androidx.datastore.preferences.core.MutablePreferences.copyLegacyBoolean(
    source: Preferences.Key<Boolean>,
    target: Preferences.Key<Boolean>,
) {
    this[source]?.let { this[target] = it }
}

private fun Int.toSupportedNutritionRange(): Int = when {
    this <= 7 -> 5
    this <= 12 -> 10
    else -> 15
}
