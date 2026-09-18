package com.keepfit.feature.workouts.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.NutritionTrackingDepth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class StarterPlanStage { SETUP, ROUTE_CHOICE, PREVIEW, APPLIED }

data class StarterPlanUiState(
    val input: StarterPlanInput = StarterPlanInput(
        goal = JourneyGoal.GENERAL_FITNESS,
        experienceLevel = ExperienceLevel.BEGINNER,
        preferredDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        sessionMinutes = 30,
        equipment = setOf(EquipmentOption.BODYWEIGHT),
        avoidedExerciseKeys = emptySet(),
    ),
    val nutritionTrackingDepth: NutritionTrackingDepth = NutritionTrackingDepth.DETAILED_MACROS,
    val nutritionTargetRangePercent: Int = 10,
    val stage: StarterPlanStage = StarterPlanStage.SETUP,
    val draft: StarterWeekDraft? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class StarterPlanViewModel @Inject constructor(
    private val repository: StarterPlanRepository,
    private val planner: StarterWeekPlanner,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StarterPlanUiState())
    val state: StateFlow<StarterPlanUiState> = _state.asStateFlow()
    private var preferenceSaveJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { repository.loadPreferences() }
                .onSuccess { saved -> if (saved != null) _state.update { it.copy(input = saved) } }
                .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update {
                    it.copy(
                        nutritionTrackingDepth = settings.nutritionTrackingDepth,
                        nutritionTargetRangePercent = settings.nutritionTargetRangePercent,
                    )
                }
            }
        }
    }

    fun selectGoal(goal: JourneyGoal) = updateInput { copy(goal = goal) }
    fun selectExperience(level: ExperienceLevel) = updateInput { copy(experienceLevel = level) }
    fun selectDuration(minutes: Int) = updateInput { copy(sessionMinutes = minutes) }
    fun selectActivity(level: ActivityLevel) = updateInput { copy(activityLevel = level) }
    fun selectSleepDuration(duration: SleepDuration) = updateInput { copy(sleepDuration = duration) }
    fun selectSleepSchedule(schedule: SleepSchedule) = updateInput { copy(sleepSchedule = schedule) }
    fun selectCurrentBuild(build: CurrentBuild) = updateInput { copy(currentBuild = build) }

    fun toggleRoutineChallenge(challenge: RoutineChallenge) = updateInput {
        copy(routineChallenges = routineChallenges.toggle(challenge))
    }

    fun toggleLimitationArea(area: LimitationArea) = updateInput {
        copy(limitationAreas = limitationAreas.toggle(area))
    }

    fun updateLimitationNotes(notes: String) = updateInput {
        copy(limitationNotes = notes.take(500).trimStart().ifBlank { null })
    }

    fun toggleDay(day: DayOfWeek) = updateInput {
        val updated = preferredDays.toggle(day)
        if (updated.size > 4) this else copy(preferredDays = updated)
    }

    fun toggleEquipment(equipmentOption: EquipmentOption) = updateInput {
        if (equipmentOption == EquipmentOption.BODYWEIGHT) this
        else copy(equipment = equipment.toggle(equipmentOption) + EquipmentOption.BODYWEIGHT)
    }

    fun toggleAvoidedExercise(key: String) = updateInput {
        copy(avoidedExerciseKeys = avoidedExerciseKeys.toggle(key))
    }

    fun selectNutritionDepth(depth: NutritionTrackingDepth) {
        _state.update { it.copy(nutritionTrackingDepth = depth, message = null) }
        viewModelScope.launch {
            settingsRepository.updateNutritionTracking(depth, _state.value.nutritionTargetRangePercent)
        }
    }

    fun createDraft() {
        planner.createDraft(_state.value.input)
            .onSuccess { draft -> _state.update { it.copy(stage = StarterPlanStage.PREVIEW, draft = draft, message = null) } }
            .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
    }

    fun showRouteChoice() = _state.update { it.copy(stage = StarterPlanStage.ROUTE_CHOICE, message = null) }

    fun editSetup() = _state.update { it.copy(stage = StarterPlanStage.SETUP, message = null) }

    fun renameDay(dayIndex: Int, name: String) = updateDraft { draft ->
        draft.copy(days = draft.days.mapIndexed { index, day -> if (index == dayIndex) day.copy(templateName = name) else day })
    }

    fun removeExercise(dayIndex: Int, exerciseKey: String) = updateDraft { draft ->
        draft.copy(
            days = draft.days.mapIndexed { index, day ->
                if (index == dayIndex && day.exercises.size > 1) {
                    day.copy(exercises = day.exercises.filterNot { it.key == exerciseKey })
                } else day
            },
        )
    }

    fun addExercise(dayIndex: Int, definition: StarterExerciseDefinition) = updateDraft { draft ->
        val reference = draft.days.getOrNull(dayIndex)?.exercises?.firstOrNull()
        draft.copy(
            days = draft.days.mapIndexed { index, day ->
                if (index != dayIndex || day.exercises.any { it.key == definition.key }) day
                else day.copy(
                    exercises = day.exercises + StarterPlanExercise(
                        key = definition.key,
                        name = definition.name,
                        muscleGroup = definition.muscleGroup,
                        instructions = definition.instructions,
                        isBodyweight = definition.isBodyweight,
                        targetSets = reference?.targetSets ?: 2,
                        targetReps = reference?.targetReps ?: "8-12",
                    ),
                )
            },
        )
    }

    fun applyWeek() {
        val draft = _state.value.draft ?: return
        _state.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.apply(_state.value.input, draft) }
                .onSuccess { _state.update { it.copy(stage = StarterPlanStage.APPLIED, isSaving = false) } }
                .onFailure { error -> _state.update { it.copy(isSaving = false, message = error.userMessage()) } }
        }
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    private fun updateInput(transform: StarterPlanInput.() -> StarterPlanInput) {
        _state.update { it.copy(input = it.input.transform(), message = null) }
        val latest = _state.value.input
        preferenceSaveJob?.cancel()
        preferenceSaveJob = viewModelScope.launch {
            runCatching { repository.savePreferences(latest) }
                .onFailure { error -> _state.update { it.copy(message = error.userMessage()) } }
        }
    }

    private fun updateDraft(transform: (StarterWeekDraft) -> StarterWeekDraft) =
        _state.update { state -> state.draft?.let { state.copy(draft = transform(it)) } ?: state }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
    private fun Throwable.userMessage(): String = message ?: "Something went wrong. Try again."
}
