package com.keepfit.feature.workouts.planning

import androidx.room.withTransaction
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.journey.JourneyProfileEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class RoomStarterPlanRepository(
    private val database: KeepfitDatabase,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) : StarterPlanRepository {
    override suspend fun loadPreferences(): StarterPlanInput? {
        val bodyProfile = database.bodyProfileDao().findLocalProfile() ?: return null
        val profile = database.journeyDao().findForBodyProfile(bodyProfile.id) ?: return null
        return StarterPlanInput(
            goal = JourneyGoal.valueOf(profile.primaryGoal),
            experienceLevel = ExperienceLevel.valueOf(profile.experienceLevel),
            preferredDays = profile.preferredDays.toEnumSet<DayOfWeek>(),
            sessionMinutes = profile.sessionMinutes,
            equipment = profile.equipment.toEnumSet<EquipmentOption>(),
            avoidedExerciseKeys = profile.avoidedExerciseKeys.toStringSet(),
        )
    }

    override suspend fun savePreferences(input: StarterPlanInput) {
        persistPreferences(input, clock())
    }

    override suspend fun apply(input: StarterPlanInput, draft: StarterWeekDraft) {
        require(draft.days.isNotEmpty()) { "The reviewed week must contain at least one workout." }
        require(draft.days.map { it.dayOfWeek }.distinct().size == draft.days.size) {
            "Each workout in the reviewed week must use a different day."
        }
        database.withTransaction {
            val now = clock()
            persistPreferences(input, now)

            val workoutDao = database.workoutDao()
            workoutDao.deactivateWeeklyPlans()
            workoutDao.archiveTemplatesByOrigin(STARTER_PLAN_ORIGIN, now)

            val weeklyPlanId = idFactory()
            workoutDao.upsertWeeklyPlan(
                WeeklyPlanEntity(
                    id = weeklyPlanId,
                    name = draft.name.trim().ifEmpty { "Starter week" },
                    startsOn = today().with(DayOfWeek.MONDAY),
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            draft.days.forEachIndexed { dayPosition, day ->
                val templateId = idFactory()
                workoutDao.upsertTemplate(
                    WorkoutTemplateEntity(
                        id = templateId,
                        name = day.templateName.trim().ifEmpty { "Workout ${dayPosition + 1}" },
                        notes = "Created from the offline starter-week setup.",
                        createdAt = now,
                        updatedAt = now,
                        archivedAt = null,
                        origin = STARTER_PLAN_ORIGIN,
                    ),
                )
                val templateExercises = day.exercises.mapIndexed { exercisePosition, exercise ->
                    require(exercise.name.isNotBlank()) { "Every reviewed exercise must have a name." }
                    val exerciseEntity = workoutDao.findActiveExerciseByName(exercise.name)
                        ?: ExerciseEntity(
                            id = idFactory(),
                            name = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            instructions = exercise.instructions,
                            notes = "Included with Keepfit's offline starter library.",
                            isBodyweight = exercise.isBodyweight,
                            createdAt = now,
                            updatedAt = now,
                            archivedAt = null,
                        ).also { workoutDao.upsertExercise(it) }
                    WorkoutTemplateExerciseEntity(
                        id = idFactory(),
                        workoutTemplateId = templateId,
                        exerciseId = exerciseEntity.id,
                        position = exercisePosition,
                        targetSets = exercise.targetSets,
                        targetReps = exercise.targetReps,
                        notes = null,
                    )
                }
                require(templateExercises.isNotEmpty()) { "Each workout must contain at least one exercise." }
                workoutDao.insertTemplateExercises(templateExercises)
                workoutDao.upsertPlannedWorkout(
                    PlannedWorkoutEntity(
                        id = idFactory(),
                        weeklyPlanId = weeklyPlanId,
                        workoutTemplateId = templateId,
                        dayOfWeek = day.dayOfWeek,
                        position = dayPosition,
                    ),
                )
            }
        }
    }

    private suspend fun persistPreferences(input: StarterPlanInput, now: Long) {
        val bodyProfile = requireNotNull(database.bodyProfileDao().findLocalProfile()) {
            "Complete your local profile before creating a starter week."
        }
        val previousJourney = database.journeyDao().findForBodyProfile(bodyProfile.id)
        database.journeyDao().upsert(
            JourneyProfileEntity(
                id = previousJourney?.id ?: idFactory(),
                bodyProfileId = bodyProfile.id,
                primaryGoal = input.goal.name,
                experienceLevel = input.experienceLevel.name,
                preferredDays = input.preferredDays.toCanonicalCsv { it.name },
                sessionMinutes = input.sessionMinutes,
                equipment = input.equipment.toCanonicalCsv { it.name },
                avoidedExerciseKeys = input.avoidedExerciseKeys.sorted().joinToString(","),
                createdAt = previousJourney?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> String.toEnumSet(): Set<T> =
        toStringSet().mapTo(linkedSetOf()) { enumValueOf<T>(it) }

    private fun String.toStringSet(): Set<String> =
        split(',').map(String::trim).filter(String::isNotEmpty).toSet()

    private fun <T> Set<T>.toCanonicalCsv(name: (T) -> String): String =
        map(name).sorted().joinToString(",")

    companion object {
        const val STARTER_PLAN_ORIGIN = "STARTER_PLAN"
    }
}
