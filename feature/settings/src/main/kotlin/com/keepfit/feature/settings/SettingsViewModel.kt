package com.keepfit.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.settings.data.BackupPreview
import com.keepfit.feature.settings.data.BackupRepository
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
    private val backupRepository: BackupRepository,
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

    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview: StateFlow<BackupPreview?> = _backupPreview.asStateFlow()

    private val _restartRequired = MutableStateFlow(false)
    val restartRequired: StateFlow<Boolean> = _restartRequired.asStateFlow()

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

    fun saveReduceMotion(enabled: Boolean) = launchWrite(null) {
        settingsRepository.updateReduceMotion(enabled)
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

    fun saveAssistantSettings(enabled: Boolean) = launchWrite("Assistant settings saved.") {
        settingsRepository.updateAssistantSettings(enabled)
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun clearBackupPreview() {
        _backupPreview.value = null
    }

    fun exportBackup(destinationUri: Uri, passphrase: String) {
        SettingsInputValidator.validateBackupPassphrase(passphrase)
            .onSuccess { normalizedPassphrase ->
                launchWrite("Backup exported.") {
                    backupRepository.exportBackup(destinationUri, normalizedPassphrase)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun previewBackup(sourceUri: Uri, passphrase: String) {
        SettingsInputValidator.validateBackupPassphrase(passphrase)
            .onSuccess { normalizedPassphrase ->
                _backupPreview.value = null
                viewModelScope.launch {
                    runCatching { backupRepository.previewBackup(sourceUri, normalizedPassphrase) }
                        .onSuccess {
                            _backupPreview.value = it
                            _message.value = "Backup preview loaded."
                        }
                        .onFailure {
                            _backupPreview.value = null
                            _message.value = it.message ?: "Something went wrong."
                        }
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun restoreBackup(sourceUri: Uri, passphrase: String) {
        SettingsInputValidator.validateBackupPassphrase(passphrase)
            .onSuccess { normalizedPassphrase ->
                viewModelScope.launch {
                    runCatching { backupRepository.restoreBackup(sourceUri, normalizedPassphrase) }
                        .onSuccess {
                            _restartRequired.value = true
                            _message.value = "Backup restored. Restarting the app."
                        }
                        .onFailure { _message.value = it.message ?: "Something went wrong." }
                }
            }
            .onFailure { _message.value = it.message }
    }

    private fun launchWrite(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = successMessage }
                .onFailure { _message.value = it.message ?: "Something went wrong." }
        }
    }
}
