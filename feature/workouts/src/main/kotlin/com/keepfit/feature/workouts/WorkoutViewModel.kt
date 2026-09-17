package com.keepfit.feature.workouts

import android.content.Context
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.PersonalRecord
import com.keepfit.feature.workouts.data.PlannedWorkout
import com.keepfit.feature.workouts.data.WorkoutFeedback
import com.keepfit.feature.workouts.data.WorkoutHistory
import com.keepfit.feature.workouts.data.WorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutTemplate
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutState
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
    private val savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context,
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
    val todayWorkout: StateFlow<TodayWorkoutState> = repository.observeTodayWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayWorkoutState())
    val activeWorkout: StateFlow<ActiveWorkout?> = repository.observeActiveWorkout()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<WorkoutHistory>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val records: StateFlow<List<PersonalRecord>> = repository.observeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val restTimerDurationSeconds: StateFlow<Int> = repository.observeRestTimerSeconds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 90)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _todayPreview = MutableStateFlow<TodayWorkoutPreview?>(null)
    val todayPreview: StateFlow<TodayWorkoutPreview?> = _todayPreview.asStateFlow()

    private val _todayChangeInProgress = MutableStateFlow(false)
    val todayChangeInProgress: StateFlow<Boolean> = _todayChangeInProgress.asStateFlow()

    private val _timerSeconds = MutableStateFlow<Int?>(null)
    val timerSeconds: StateFlow<Int?> = _timerSeconds.asStateFlow()
    private var timerJob: Job? = null

    private val _workoutWriteInProgress = MutableStateFlow(false)
    val workoutWriteInProgress: StateFlow<Boolean> = _workoutWriteInProgress.asStateFlow()

    init {
        savedStateHandle.get<Long>(REST_TIMER_END_AT)?.let(::runRestTimerUntil)
    }

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

    fun deleteExercise(id: String) = launchWrite("Exercise deleted.") {
        repository.deleteExercise(id)
    }

    fun createTemplate(name: String, exerciseIds: List<String>) =
        launchWrite("Workout template saved.") {
            repository.createTemplate(name, exerciseIds)
        }

    fun updateTemplate(id: String, name: String, exerciseIds: List<String>) =
        launchWrite("Workout template updated.") {
            repository.updateTemplate(id, name, exerciseIds)
        }

    fun renameTemplate(id: String, name: String) = launchWrite("Template renamed.") {
        repository.renameTemplate(id, name)
    }

    fun addTemplateExercises(id: String, exerciseIds: List<String>) =
        launchWrite("Exercises added.") {
            repository.addTemplateExercises(id, exerciseIds)
        }

    fun updateTemplateExercise(
        templateId: String,
        templateExerciseId: String,
        targetSets: Int,
        targetReps: String?,
    ) = launchWrite("Exercise targets updated.") {
        repository.updateTemplateExercise(templateId, templateExerciseId, targetSets, targetReps)
    }

    fun removeTemplateExercise(templateId: String, templateExerciseId: String) =
        launchWrite("Exercise removed from template.") {
            repository.removeTemplateExercise(templateId, templateExerciseId)
        }

    fun deleteTemplate(id: String) = launchWrite("Workout template deleted.") {
        repository.deleteTemplate(id)
    }

    fun deleteTemplates(ids: Set<String>) = launchWrite("Workout templates deleted.") {
        repository.deleteTemplates(ids)
    }

    fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String) =
        launchWrite("Weekly plan updated.") {
            repository.assignTemplate(dayOfWeek, templateId)
        }

    fun clearPlannedWorkout(dayOfWeek: DayOfWeek) =
        launchWrite("Planned workout cleared.") {
            repository.clearPlannedWorkout(dayOfWeek)
        }

    fun startWorkout(plannedWorkout: PlannedWorkout, onStarted: () -> Unit = {}) =
        launchWrite(null, onSuccess = onStarted) {
            repository.startOrResume(plannedWorkout)
        }

    fun startTodayWorkout(action: TodayWorkoutAction, onStarted: () -> Unit = {}) =
        launchWrite(null, onSuccess = onStarted) {
            repository.startOrResumeToday(action)
        }

    fun previewTodayChange(request: TodayChangeRequest) {
        viewModelScope.launch {
            _todayChangeInProgress.value = true
            runCatching { repository.previewTodayChange(request) }
                .onSuccess { _todayPreview.value = it }
                .onFailure { _message.value = it.message ?: "Today's plan could not be changed." }
            _todayChangeInProgress.value = false
        }
    }

    fun confirmTodayChange() {
        val request = _todayPreview.value?.request ?: return
        viewModelScope.launch {
            _todayChangeInProgress.value = true
            runCatching { repository.confirmTodayChange(request) }
                .onSuccess {
                    _todayPreview.value = null
                    _message.value = null
                }
                .onFailure { _message.value = it.message ?: "Today's plan could not be changed." }
            _todayChangeInProgress.value = false
        }
    }

    fun dismissTodayPreview() {
        _todayPreview.value = null
    }

    fun addSet(exerciseLogId: String, repetitions: String, weightKg: String) {
        runCatching { SetInputValidator.validate(repetitions, weightKg) }
            .onSuccess { input ->
                launchWrite("Set logged.", onSuccess = ::startRestTimer) {
                    repository.addSet(exerciseLogId, input)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun repeatPreviousSet(exerciseLogId: String) =
        launchWrite("Previous set repeated.", onSuccess = ::startRestTimer) {
            repository.repeatPreviousSet(exerciseLogId)
        }

    fun substituteActiveExercise(exerciseLogId: String, replacementExerciseId: String) =
        launchWrite("Exercise substituted for this session.") {
            repository.substituteActiveExercise(exerciseLogId, replacementExerciseId)
        }

    fun convertActiveWorkoutToMinimum() = launchWrite("Minimum session ready.") {
        repository.convertActiveWorkoutToMinimum()
    }

    fun updateExerciseNotes(exerciseLogId: String, notes: String) =
        launchWrite("Notes saved.") {
            repository.updateExerciseNotes(exerciseLogId, notes)
        }

    fun completeWorkout(feedback: WorkoutFeedback? = null, onCompleted: () -> Unit = {}) =
        launchWrite(
            successMessage = "Workout completed.",
            onSuccess = {
                stopRestTimer()
                onCompleted()
            },
        ) {
            repository.completeActiveWorkout(feedback)
        }

    fun startRestTimer() {
        val endAt = System.currentTimeMillis() + restTimerDurationSeconds.value * 1_000L
        savedStateHandle[REST_TIMER_END_AT] = endAt
        runRestTimerUntil(endAt)
    }

    private fun runRestTimerUntil(endAt: Long) {
        if (endAt <= System.currentTimeMillis()) {
            stopRestTimer()
            return
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = ((endAt - System.currentTimeMillis() + 999L) / 1_000L).toInt()
                if (remaining <= 0) break
                _timerSeconds.value = remaining
                delay(250)
            }
            _timerSeconds.value = null
            savedStateHandle.remove<Long>(REST_TIMER_END_AT)
            context.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    private fun stopRestTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerSeconds.value = null
        savedStateHandle.remove<Long>(REST_TIMER_END_AT)
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun launchWrite(
        successMessage: String?,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _workoutWriteInProgress.value = true
            try {
                runCatching { block() }
                    .onSuccess {
                        _message.value = successMessage
                        onSuccess()
                    }
                    .onFailure { _message.value = it.message ?: "Something went wrong." }
            } finally {
                _workoutWriteInProgress.value = false
            }
        }
    }

    private companion object {
        const val REST_TIMER_END_AT = "active_workout_rest_timer_end_at"
    }
}
