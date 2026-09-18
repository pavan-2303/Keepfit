package com.keepfit.app.assistant

import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.model.calculateAge
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.assistant.planning.AssistantPlanContext
import com.keepfit.feature.assistant.planning.AssistantPlanContextDataSource
import com.keepfit.feature.assistant.planning.AssistantPlanExerciseOption
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.WorkoutRepository
import com.keepfit.feature.workouts.planning.StarterExerciseCatalog
import com.keepfit.feature.workouts.planning.StarterPlanRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class KeepfitAssistantPlanContextDataSource(
    private val starterPlanRepository: StarterPlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val database: KeepfitDatabase,
    private val activeProfileStore: ActiveProfileStore,
    private val today: () -> LocalDate = LocalDate::now,
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
            .filterNot { it.name.normalized() in avoidedNames }
            .sortedWith(
                compareBy<Exercise> { starterRank[it.name.normalized()] ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() },
            )
            .take(MAX_OPTIONS)
            .map { exercise ->
                AssistantPlanExerciseOption(
                    id = PersonalExerciseAlias.forId(exercise.id),
                    name = exercise.name,
                    equipment = exercise.equipment ?: if (exercise.isBodyweight) "Bodyweight" else null,
                    targetMuscle = exercise.targetMuscle ?: exercise.muscleGroup,
                )
            }
            .toList()
        val profileId = requireNotNull(activeProfileStore.observeActiveProfileId().first()) {
            "Complete your local profile before creating an AI plan."
        }
        val profile = requireNotNull(database.bodyProfileDao().findProfile(profileId)) {
            "Complete your local profile before creating an AI plan."
        }
        val latestMeasurement = database.transformationDao().listMeasurements(profileId)
            .maxWithOrNull(compareBy({ it.measurementDate }, { it.createdAt }))
        return AssistantPlanContext(
            goal = preferences.goal.label,
            experience = preferences.experienceLevel.label,
            sessionMinutes = preferences.sessionMinutes,
            preferredDays = preferences.preferredDays,
            equipment = preferences.equipment.mapTo(linkedSetOf()) { it.label },
            exercises = exercises,
            ageYears = calculateAge(profile.birthDate, today()),
            heightCm = profile.heightCm,
            weightKg = latestMeasurement?.weightKg,
            activityLevel = preferences.activityLevel.label,
            sleepDuration = preferences.sleepDuration.label,
            sleepSchedule = preferences.sleepSchedule.label,
            currentBuild = preferences.currentBuild.label,
            routineChallenges = preferences.routineChallenges.mapTo(linkedSetOf()) { it.label },
            limitationAreas = preferences.limitationAreas.mapTo(linkedSetOf()) { it.label },
            limitationNotes = preferences.limitationNotes,
        )
    }

    private fun String.normalized(): String = lowercase().filter(Char::isLetterOrDigit)

    private companion object {
        const val MAX_OPTIONS = 60
    }
}
