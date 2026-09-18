package com.keepfit.app.assistant

import androidx.room.withTransaction
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.data.AssistantPlanApplier
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first

class RoomAssistantPlanApplier(
    private val database: KeepfitDatabase,
    private val activeProfileStore: ActiveProfileStore,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) : AssistantPlanApplier {
    override suspend fun applyDraftPlan(draft: AssistantDraftWorkoutPlan): Result<Unit> = runCatching {
        require(draft.days.isNotEmpty()) { "The reviewed plan must contain at least one workout." }
        require(draft.days.map { it.dayOfWeek }.distinct().size == draft.days.size) {
            "Each reviewed workout must use a different day."
        }
        val profileId = requireNotNull(activeProfileStore.observeActiveProfileId().first()) {
            "Complete your local profile before applying a plan."
        }
        database.withTransaction {
            val workoutDao = database.workoutDao()
            val now = clock()
            val activeExercises = workoutDao.findActiveExercises()
            val personalExerciseIds = activeExercises
                .associate { PersonalExerciseAlias.forId(it.id) to it.id }
            val exerciseIdsByName = activeExercises.associateTo(linkedMapOf()) { it.name.normalized() to it.id }
            draft.days.flatMap { it.exercises }.forEach { exercise ->
                if (exercise.exerciseId.isNotBlank()) {
                    require(exercise.exerciseId in personalExerciseIds) {
                        "A reviewed exercise is no longer available. Generate the plan again."
                    }
                } else {
                    val definition = requireNotNull(exercise.newExercise) {
                        "A new exercise is missing its reviewed definition. Generate the plan again."
                    }
                    require(definition.name.isNotBlank() && definition.muscleGroup.isNotBlank() && definition.instructions.isNotBlank()) {
                        "A new exercise definition is incomplete. Generate the plan again."
                    }
                    exerciseIdsByName.getOrPut(definition.name.normalized()) {
                        val exerciseId = idFactory()
                        workoutDao.upsertExercise(
                            ExerciseEntity(
                                id = exerciseId,
                                name = definition.name.trim(),
                                muscleGroup = definition.muscleGroup.trim(),
                                instructions = definition.instructions.trim(),
                                notes = "Created from an approved AI plan.",
                                isBodyweight = definition.isBodyweight,
                                createdAt = now,
                                updatedAt = now,
                                archivedAt = null,
                                equipment = definition.equipment?.trim()?.takeIf(String::isNotEmpty),
                                targetMuscle = definition.targetMuscle?.trim()?.takeIf(String::isNotEmpty),
                                secondaryMuscles = definition.secondaryMuscles?.trim()?.takeIf(String::isNotEmpty),
                            ),
                        )
                        exerciseId
                    }
                }
            }
            workoutDao.deactivateWeeklyPlansForProfile(profileId)
            workoutDao.archiveTemplatesByOriginForProfile(profileId, AI_PLAN_ORIGIN, now)
            val planId = idFactory()
            workoutDao.upsertWeeklyPlan(
                WeeklyPlanEntity(
                    id = planId,
                    name = draft.name.trim().ifEmpty { "AI workout plan" },
                    startsOn = today().with(DayOfWeek.MONDAY),
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                    bodyProfileId = profileId,
                ),
            )
            draft.days.forEachIndexed { dayPosition, day ->
                require(day.exercises.isNotEmpty()) { "Each reviewed workout must contain an exercise." }
                val templateId = idFactory()
                workoutDao.upsertTemplate(
                    WorkoutTemplateEntity(
                        id = templateId,
                        name = day.templateName.trim().ifEmpty { "Workout ${dayPosition + 1}" },
                        notes = day.notes ?: draft.overview,
                        createdAt = now,
                        updatedAt = now,
                        archivedAt = null,
                        origin = AI_PLAN_ORIGIN,
                        bodyProfileId = profileId,
                    ),
                )
                workoutDao.insertTemplateExercises(
                    day.exercises.mapIndexed { exercisePosition, exercise ->
                        WorkoutTemplateExerciseEntity(
                            id = idFactory(),
                            workoutTemplateId = templateId,
                            exerciseId = if (exercise.exerciseId.isNotBlank()) {
                                requireNotNull(personalExerciseIds[exercise.exerciseId])
                            } else {
                                requireNotNull(exerciseIdsByName[exercise.name.normalized()])
                            },
                            position = exercisePosition,
                            targetSets = requireNotNull(exercise.targetSets) { "Target sets are required." },
                            targetReps = exercise.targetReps,
                            notes = exercise.notes,
                        )
                    },
                )
                workoutDao.upsertPlannedWorkout(
                    PlannedWorkoutEntity(
                        id = idFactory(),
                        weeklyPlanId = planId,
                        workoutTemplateId = templateId,
                        dayOfWeek = day.dayOfWeek,
                        position = dayPosition,
                    ),
                )
            }
        }
    }

    private fun String.normalized(): String = lowercase().filter(Char::isLetterOrDigit)

    companion object {
        const val AI_PLAN_ORIGIN = "AI_PLAN"
    }
}
