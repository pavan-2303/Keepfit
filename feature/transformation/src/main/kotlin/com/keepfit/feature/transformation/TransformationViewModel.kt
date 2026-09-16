package com.keepfit.feature.transformation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.model.TransformationPose
import com.keepfit.feature.transformation.data.CurrentProgressOverview
import com.keepfit.feature.transformation.data.TransformationRepository
import com.keepfit.feature.transformation.data.TransformationTimeline
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

    val timeline: StateFlow<TransformationTimeline> = repository.observeTimeline()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TransformationTimeline(activeCycle = null, history = emptyList()),
        )

    val enabledPoses: StateFlow<List<TransformationPose>> = repository.observeEnabledPoses()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TransformationPose.defaultPoses,
        )

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

    fun saveCycleNotes(notes: String) =
        launchWrite("Transformation cycle updated.") {
            repository.saveCycleNotes(notes)
        }

    fun importPhoto(captureDate: LocalDate, pose: TransformationPose, uri: Uri) =
        launchWrite("${pose.label} photo saved.") {
            repository.importPhoto(captureDate, pose, uri)
        }

    fun setOptionalPoseEnabled(pose: TransformationPose, enabled: Boolean) =
        launchWrite(null) {
            repository.setOptionalPoseEnabled(pose, enabled)
        }

    fun closeActiveCycle() =
        launchWrite("Transformation cycle closed.") {
            repository.closeActiveCycle()
        }

    fun reopenCycle(cycleId: String) =
        launchWrite("Transformation cycle reopened.") {
            repository.reopenCycle(cycleId)
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
