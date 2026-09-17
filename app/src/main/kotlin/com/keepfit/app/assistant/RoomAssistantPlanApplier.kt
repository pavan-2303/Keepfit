package com.keepfit.app.assistant

import androidx.room.withTransaction
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.workout.PlannedWorkoutEntity
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
            val exerciseIds = draft.days.flatMap { day -> day.exercises.map { it.exerciseId } }.distinct()
            require(exerciseIds.isNotEmpty() && exerciseIds.none(String::isBlank)) {
                "Every reviewed exercise must come from the local catalogue."
            }
            val personalExerciseIds = workoutDao.findActiveExercises()
                .associate { PersonalExerciseAlias.forId(it.id) to it.id }
            exerciseIds.forEach { exerciseAlias ->
                require(exerciseAlias in personalExerciseIds) {
                    "A reviewed exercise is no longer available. Generate the plan again."
                }
            }

            val now = clock()
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
                            exerciseId = requireNotNull(personalExerciseIds[exercise.exerciseId]),
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

    companion object {
        const val AI_PLAN_ORIGIN = "AI_PLAN"
    }
}
