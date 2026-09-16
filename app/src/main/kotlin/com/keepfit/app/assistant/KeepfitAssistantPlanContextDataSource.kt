package com.keepfit.app.assistant

import com.keepfit.feature.assistant.planning.AssistantPlanContext
import com.keepfit.feature.assistant.planning.AssistantPlanContextDataSource
import com.keepfit.feature.assistant.planning.AssistantPlanExerciseOption
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.WorkoutRepository
import com.keepfit.feature.workouts.planning.EquipmentOption
import com.keepfit.feature.workouts.planning.StarterExerciseCatalog
import com.keepfit.feature.workouts.planning.StarterPlanRepository
import kotlinx.coroutines.flow.first

class KeepfitAssistantPlanContextDataSource(
    private val starterPlanRepository: StarterPlanRepository,
    private val workoutRepository: WorkoutRepository,
) : AssistantPlanContextDataSource {
    override suspend fun loadContext(): AssistantPlanContext {
        val preferences = requireNotNull(starterPlanRepository.loadPreferences()) {
            "Complete your fitness preferences before creating an AI plan."
        }
        val avoidedNames = StarterExerciseCatalog.exercises
            .filter { it.key in preferences.avoidedExerciseKeys }
            .mapTo(hashSetOf()) { it.name.normalized() }
        val starterRank = StarterExerciseCatalog.exercises
            .filterNot { it.key in preferences.avoidedExerciseKeys }
            .withIndex()
            .associate { it.value.name.normalized() to it.index }
        val exercises = workoutRepository.observeExercises("").first()
            .asSequence()
            .filter { it.source == BUNDLED_CATALOGUE_SOURCE }
            .filterNot { it.name.normalized() in avoidedNames }
            .filter { it.supports(preferences.equipment) }
            .sortedWith(
                compareBy<Exercise> { starterRank[it.name.normalized()] ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() },
            )
            .take(MAX_OPTIONS)
            .map { exercise ->
                AssistantPlanExerciseOption(
                    id = exercise.id,
                    name = exercise.name,
                    equipment = exercise.equipment ?: if (exercise.isBodyweight) "Bodyweight" else null,
                    targetMuscle = exercise.targetMuscle ?: exercise.muscleGroup,
                )
            }
            .toList()
        return AssistantPlanContext(
            goal = preferences.goal.label,
            experience = preferences.experienceLevel.label,
            sessionMinutes = preferences.sessionMinutes,
            preferredDays = preferences.preferredDays,
            equipment = preferences.equipment.mapTo(linkedSetOf()) { it.label },
            exercises = exercises,
        )
    }

    private fun Exercise.supports(available: Set<EquipmentOption>): Boolean {
        if (EquipmentOption.FULL_GYM in available) return true
        if (isBodyweight && EquipmentOption.BODYWEIGHT in available) return true
        val label = equipment.orEmpty().lowercase()
        return (EquipmentOption.DUMBBELLS in available && "dumbbell" in label) ||
            (EquipmentOption.RESISTANCE_BANDS in available && ("band" in label || "resistance" in label)) ||
            (EquipmentOption.BODYWEIGHT in available && ("body" in label || label.isBlank()))
    }

    private fun String.normalized(): String = lowercase().filter(Char::isLetterOrDigit)

    private companion object {
        const val MAX_OPTIONS = 60
        const val BUNDLED_CATALOGUE_SOURCE = "hasaneyldrm/exercises-dataset"
    }
}
