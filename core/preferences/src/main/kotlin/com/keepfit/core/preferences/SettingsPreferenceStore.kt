package com.keepfit.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore(name = "keepfit_settings")

class DataStoreAppSettingsRepository(
    private val context: Context,
    private val reminderScheduler: ReminderScheduler,
) : AppSettingsRepository {
    override fun observeSettings(): Flow<AppSettings> =
        context.dataStore.data.map { preferences -> preferences.toAppSettings() }

    override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) {
        context.dataStore.edit { preferences ->
            preferences[Keys.WEIGHT_UNIT] = weightUnit.ordinal
            preferences[Keys.MEASUREMENT_UNIT] = measurementUnit.ordinal
        }
        syncReminders()
    }

    override suspend fun updateRestTimerSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REST_TIMER_SECONDS] = seconds
        }
        syncReminders()
    }

    override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.WORKOUT_REMINDER_ENABLED] = enabled
            preferences[Keys.WORKOUT_REMINDER_HOUR] = hour
            preferences[Keys.WORKOUT_REMINDER_MINUTE] = minute
        }
        syncReminders()
    }

    override suspend fun updateTransformationReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: Int,
        minute: Int,
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TRANSFORMATION_REMINDER_ENABLED] = enabled
            preferences[Keys.TRANSFORMATION_REMINDER_DAY] = dayOfWeekOrdinal
            preferences[Keys.TRANSFORMATION_REMINDER_HOUR] = hour
            preferences[Keys.TRANSFORMATION_REMINDER_MINUTE] = minute
        }
        syncReminders()
    }

    private suspend fun syncReminders() {
        reminderScheduler.sync(observeSettings().first())
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            weightUnit = WeightUnit.entries[this[Keys.WEIGHT_UNIT] ?: WeightUnit.KG.ordinal],
            measurementUnit = MeasurementUnit.entries[this[Keys.MEASUREMENT_UNIT] ?: MeasurementUnit.CM.ordinal],
            restTimerSeconds = this[Keys.REST_TIMER_SECONDS] ?: 90,
            workoutReminder = ReminderTime(
                enabled = this[Keys.WORKOUT_REMINDER_ENABLED] ?: false,
                hour = this[Keys.WORKOUT_REMINDER_HOUR] ?: 18,
                minute = this[Keys.WORKOUT_REMINDER_MINUTE] ?: 0,
            ),
            transformationReminder = WeeklyReminderTime(
                enabled = this[Keys.TRANSFORMATION_REMINDER_ENABLED] ?: false,
                dayOfWeek = DayOfWeek.of((this[Keys.TRANSFORMATION_REMINDER_DAY] ?: DayOfWeek.SUNDAY.value).coerceIn(1, 7)),
                hour = this[Keys.TRANSFORMATION_REMINDER_HOUR] ?: 9,
                minute = this[Keys.TRANSFORMATION_REMINDER_MINUTE] ?: 0,
            ),
        )

    private object Keys {
        val WEIGHT_UNIT = intPreferencesKey("weight_unit")
        val MEASUREMENT_UNIT = intPreferencesKey("measurement_unit")
        val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
        val WORKOUT_REMINDER_ENABLED = booleanPreferencesKey("workout_reminder_enabled")
        val WORKOUT_REMINDER_HOUR = intPreferencesKey("workout_reminder_hour")
        val WORKOUT_REMINDER_MINUTE = intPreferencesKey("workout_reminder_minute")
        val TRANSFORMATION_REMINDER_ENABLED = booleanPreferencesKey("transformation_reminder_enabled")
        val TRANSFORMATION_REMINDER_DAY = intPreferencesKey("transformation_reminder_day")
        val TRANSFORMATION_REMINDER_HOUR = intPreferencesKey("transformation_reminder_hour")
        val TRANSFORMATION_REMINDER_MINUTE = intPreferencesKey("transformation_reminder_minute")
    }
}
