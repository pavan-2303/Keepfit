package com.keepfit.feature.workouts

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.PersonalRecord
import com.keepfit.feature.workouts.data.PlannedWorkout
import com.keepfit.feature.workouts.data.WorkoutHistory
import com.keepfit.feature.workouts.data.WorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    val exercises: StateFlow<List<Exercise>> = searchQuery
        .flatMapLatest(repository::observeExercises)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates: StateFlow<List<WorkoutTemplate>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val schedule: StateFlow<List<PlannedWorkout>> = repository.observeWeeklySchedule()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todayPlan: StateFlow<List<PlannedWorkout>> = repository.observeTodayPlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeWorkout: StateFlow<ActiveWorkout?> = repository.observeActiveWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<WorkoutHistory>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val records: StateFlow<List<PersonalRecord>> = repository.observeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _timerSeconds = MutableStateFlow<Int?>(null)
    val timerSeconds: StateFlow<Int?> = _timerSeconds.asStateFlow()
    private var timerJob: Job? = null

    fun search(query: String) {
        searchQuery.value = query
    }

    fun saveExercise(
        id: String?,
        name: String,
        muscleGroup: String,
        instructions: String,
        notes: String,
        isBodyweight: Boolean,
        mediaUri: Uri?,
    ) {
        when (
            val result = ExerciseInputValidator.validate(
                name,
                muscleGroup,
                instructions,
                notes,
                isBodyweight,
            )
        ) {
            is ExerciseValidationResult.Invalid -> _message.value = result.message
            is ExerciseValidationResult.Valid -> launchWrite("Exercise saved.") {
                repository.saveExercise(id, result.input, mediaUri)
            }
        }
    }

    fun archiveExercise(id: String) = launchWrite("Exercise archived.") {
        repository.archiveExercise(id)
    }

    fun createTemplate(name: String, exerciseIds: List<String>) =
        launchWrite("Workout template saved.") {
            repository.createTemplate(name, exerciseIds)
        }

    fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String) =
        launchWrite("Weekly plan updated.") {
            repository.assignTemplate(dayOfWeek, templateId)
        }

    fun startWorkout(plannedWorkout: PlannedWorkout, onStarted: () -> Unit = {}) =
        launchWrite(null) {
            repository.startOrResume(plannedWorkout)
            onStarted()
        }

    fun addSet(exerciseLogId: String, repetitions: String, weightKg: String) {
        runCatching { SetInputValidator.validate(repetitions, weightKg) }
            .onSuccess { input ->
                launchWrite("Set logged.") {
                    repository.addSet(exerciseLogId, input)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun updateExerciseNotes(exerciseLogId: String, notes: String) =
        launchWrite("Notes saved.") {
            repository.updateExerciseNotes(exerciseLogId, notes)
        }

    fun completeWorkout(onCompleted: () -> Unit = {}) =
        launchWrite("Workout completed.") {
            repository.completeActiveWorkout()
            timerJob?.cancel()
            _timerSeconds.value = null
            onCompleted()
        }

    fun startRestTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (seconds in 90 downTo 0) {
                _timerSeconds.value = seconds
                delay(1_000)
            }
            _timerSeconds.value = null
            context.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
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

