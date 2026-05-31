package com.keepfit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.settings.data.NutritionGoalSettings
import com.keepfit.feature.settings.data.SettingsGoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val goalsRepository: SettingsGoalsRepository,
) : ViewModel() {
    val appSettings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val nutritionGoals: StateFlow<NutritionGoalSettings> = goalsRepository.observeNutritionGoals()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NutritionGoalSettings(null, null, null, null),
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun saveUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = launchWrite("Units saved.") {
        settingsRepository.updateUnits(weightUnit, measurementUnit)
    }

    fun saveRestTimer(seconds: String) {
        SettingsInputValidator.validateRestTimerSeconds(seconds)
            .onSuccess { normalizedSeconds ->
                launchWrite("Rest timer saved.") {
                    settingsRepository.updateRestTimerSeconds(normalizedSeconds)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun saveWorkoutReminder(enabled: Boolean, hour: String, minute: String) {
        SettingsInputValidator.validateReminder(enabled, hour, minute)
            .onSuccess { reminder ->
                launchWrite("Workout reminder saved.") {
                    settingsRepository.updateWorkoutReminder(reminder.enabled, reminder.hour, reminder.minute)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun saveTransformationReminder(enabled: Boolean, dayOfWeekOrdinal: Int, hour: String, minute: String) {
        SettingsInputValidator.validateWeeklyReminder(enabled, dayOfWeekOrdinal, hour, minute)
            .onSuccess { reminder ->
                launchWrite("Progress reminder saved.") {
                    settingsRepository.updateTransformationReminder(
                        reminder.enabled,
                        reminder.dayOfWeek.value,
                        reminder.hour,
                        reminder.minute,
                    )
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun saveNutritionGoals(
        calories: String,
        protein: String,
        carbohydrates: String,
        fat: String,
    ) = launchWrite("Nutrition goals saved.") {
        goalsRepository.updateNutritionGoals(
            dailyCalorieGoal = calories.trim().toDoubleOrNull(),
            dailyProteinGoalGrams = protein.trim().toDoubleOrNull(),
            dailyCarbohydrateGoalGrams = carbohydrates.trim().toDoubleOrNull(),
            dailyFatGoalGrams = fat.trim().toDoubleOrNull(),
        )
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun launchWrite(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = successMessage }
                .onFailure { _message.value = it.message ?: "Something went wrong." }
        }
    }
}
