package com.keepfit.feature.transformation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.feature.transformation.data.CurrentProgressOverview
import com.keepfit.feature.transformation.data.TransformationRepository
import com.keepfit.feature.transformation.data.TransformationWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransformationViewModel @Inject constructor(
    private val repository: TransformationRepository,
) : ViewModel() {
    val currentOverview: StateFlow<CurrentProgressOverview> = repository.observeCurrentOverview()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CurrentProgressOverview(latestMeasurement = null, heightCm = null, bmi = null),
        )

    val measurements = repository.observeMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeks: StateFlow<List<TransformationWeek>> = repository.observeWeeks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun saveMeasurement(
        measurementDate: String,
        weightKg: String,
        waistCm: String,
        chestCm: String,
        hipsCm: String,
        leftArmCm: String,
        rightArmCm: String,
        leftThighCm: String,
        rightThighCm: String,
        notes: String,
    ) {
        MeasurementInputValidator.validate(
            measurementDate = measurementDate,
            weightKg = weightKg,
            waistCm = waistCm,
            chestCm = chestCm,
            hipsCm = hipsCm,
            leftArmCm = leftArmCm,
            rightArmCm = rightArmCm,
            leftThighCm = leftThighCm,
            rightThighCm = rightThighCm,
            notes = notes,
        )
            .onSuccess { input ->
                launchWrite("Measurement saved.") {
                    repository.saveMeasurement(input)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun saveWeekNotes(weekStartDate: LocalDate, notes: String) =
        launchWrite("Week updated.") {
            repository.saveWeekNotes(weekStartDate, notes)
        }

    fun importPhoto(weekStartDate: LocalDate, angle: TransformationPhotoAngle, uri: Uri) =
        launchWrite("${angle.name.lowercase().replaceFirstChar(Char::titlecase)} photo saved.") {
            repository.importPhoto(weekStartDate, angle, uri)
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
