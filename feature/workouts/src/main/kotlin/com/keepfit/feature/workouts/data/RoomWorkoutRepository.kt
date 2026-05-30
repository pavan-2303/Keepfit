package com.keepfit.feature.workouts.data

import android.net.Uri
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.ExerciseLogEntity
import com.keepfit.core.database.workout.ExerciseMediaEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.SetLogEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.media.ExerciseMediaStore
import com.keepfit.feature.workouts.CompletedSetInput
import com.keepfit.feature.workouts.ExerciseInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val mediaStore: ExerciseMediaStore,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) : WorkoutRepository {
    override fun observeExercises(query: String): Flow<List<Exercise>> =
        dao.observeExercises(query).map { entities -> entities.map(ExerciseEntity::toModel) }

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplateDetails().map { details ->
            details.map { template ->
                WorkoutTemplate(
                    id = template.template.id,
                    name = template.template.name,
                    notes = template.template.notes,
                    exercises = template.exercises
                        .sortedBy { it.templateExercise.position }
                        .map {
                            TemplateExercise(
                                id = it.templateExercise.id,
                                exerciseId = it.templateExercise.exerciseId,
                                exerciseName = it.exerciseName,
                                targetSets = it.templateExercise.targetSets,
                                targetReps = it.templateExercise.targetReps,
                                notes = it.templateExercise.notes,
                            )
                        },
                )
            }
        }

    override fun observeWeeklySchedule(): Flow<List<PlannedWorkout>> =
        dao.observeWeeklySchedule().map { rows -> rows.map { it.toModel() } }

    override fun observeTodayPlan(): Flow<List<PlannedWorkout>> =
        dao.observePlannedWorkouts(today().dayOfWeek).map { rows -> rows.map { it.toModel() } }

    override fun observeActiveWorkout(): Flow<ActiveWorkout?> =
        dao.observeActiveSession().mapLatest { details ->
            details?.let {
                ActiveWorkout(
                    sessionId = it.session.id,
                    templateName = it.session.workoutTemplateId
                        ?.let { templateId -> dao.findTemplateDetails(templateId) }
                        ?.template
                        ?.name
                        ?: "Workout",
                    workoutDate = it.session.workoutDate,
                    exercises = it.exercises
                        .sortedBy { log -> log.exerciseLog.position }
                        .map { log ->
                            ActiveExercise(
                                exerciseLogId = log.exerciseLog.id,
                                exerciseName = log.exerciseName,
                                notes = log.exerciseLog.notes,
                                previousSet = dao.findPreviousSet(log.exerciseLog.exerciseId)?.let {
                                    LoggedSet(id = "previous", repetitions = it.repetitions, weightKg = it.weightKg)
                                },
                                sets = log.sets.sortedBy(SetLogEntity::position).map {
                                    LoggedSet(id = it.id, repetitions = it.repetitions, weightKg = it.weightKg)
                                },
                            )
                        },
                )
            }
        }

    override fun observeHistory(): Flow<List<WorkoutHistory>> =
        dao.observeSessionHistory().map { sessions ->
            sessions.map {
                WorkoutHistory(
                    sessionId = it.session.id,
                    workoutDate = it.session.workoutDate,
                    completedAt = requireNotNull(it.session.completedAt),
                    exerciseNames = it.exercises
                        .sortedBy { log -> log.exerciseLog.position }
                        .map { log -> log.exerciseName },
                )
            }
        }

    override fun observeRecords(): Flow<List<PersonalRecord>> =
        dao.observePersonalRecords().map { records ->
            records.map {
                PersonalRecord(
                    exerciseName = it.exerciseName,
                    highestWeightKg = it.highestWeightKg,
                    highestRepetitions = it.highestRepetitions,
                )
            }
        }

    override suspend fun saveExercise(id: String?, input: ExerciseInput, mediaUri: Uri?) {
        val exerciseId = id ?: idFactory()
        val now = clock()
        val importedMedia = mediaUri?.let(mediaStore::import)
        dao.upsertExercise(
            ExerciseEntity(
                id = exerciseId,
                name = input.name,
                muscleGroup = input.muscleGroup,
                instructions = input.instructions,
                notes = input.notes,
                isBodyweight = input.isBodyweight,
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
            ),
        )
        importedMedia?.let {
            dao.upsertExerciseMedia(
                ExerciseMediaEntity(
                    id = it.id,
                    exerciseId = exerciseId,
                    mediaType = it.mediaType,
                    relativePath = it.relativePath,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    createdAt = now,
                ),
            )
        }
    }

    override suspend fun archiveExercise(id: String) = dao.archiveExercise(id, clock())

    override suspend fun createTemplate(name: String, exerciseIds: List<String>) {
        require(name.isNotBlank()) { "Enter a template name." }
        require(exerciseIds.isNotEmpty()) { "Choose at least one exercise." }
        val templateId = idFactory()
        val now = clock()
        dao.upsertTemplate(
            WorkoutTemplateEntity(templateId, name.trim(), null, now, now, null),
        )
        dao.replaceTemplateExercises(
            templateId = templateId,
            exercises = exerciseIds.mapIndexed { index, exerciseId ->
                WorkoutTemplateExerciseEntity(
                    id = idFactory(),
                    workoutTemplateId = templateId,
                    exerciseId = exerciseId,
                    position = index,
                    targetSets = 3,
                    targetReps = "8-10",
                    notes = null,
                )
            },
        )
    }

    override suspend fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String) {
        val now = clock()
        val planId = "default-weekly-plan"
        dao.deactivateWeeklyPlans()
        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = planId,
                name = "Default week",
                startsOn = today().with(DayOfWeek.MONDAY),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        dao.replacePlannedWorkout(
            PlannedWorkoutEntity(idFactory(), planId, templateId, dayOfWeek, 0),
        )
    }

    override suspend fun startOrResume(plannedWorkout: PlannedWorkout): String {
        dao.findActiveSession()?.let { return it.id }
        val template = requireNotNull(dao.findTemplateDetails(plannedWorkout.templateId))
        val sessionId = idFactory()
        dao.insertSession(
            WorkoutSessionEntity(
                id = sessionId,
                workoutTemplateId = template.template.id,
                plannedWorkoutId = plannedWorkout.id,
                workoutDate = today(),
                startedAt = clock(),
                completedAt = null,
                notes = null,
            ),
        )
        template.exercises.sortedBy { it.templateExercise.position }.forEachIndexed { index, item ->
            dao.insertExerciseLog(
                ExerciseLogEntity(
                    id = idFactory(),
                    workoutSessionId = sessionId,
                    exerciseId = item.templateExercise.exerciseId,
                    position = index,
                    notes = null,
                ),
            )
        }
        return sessionId
    }

    override suspend fun addSet(exerciseLogId: String, input: CompletedSetInput) {
        dao.insertSetLog(
            SetLogEntity(
                id = idFactory(),
                exerciseLogId = exerciseLogId,
                position = dao.countSets(exerciseLogId),
                repetitions = input.repetitions,
                weightKg = input.weightKg,
                isCompleted = true,
            ),
        )
    }

    override suspend fun updateExerciseNotes(exerciseLogId: String, notes: String) =
        dao.updateExerciseLogNotes(exerciseLogId, notes.trim().ifEmpty { null })

    override suspend fun completeActiveWorkout() {
        dao.findActiveSession()?.let { dao.completeSession(it.id, clock()) }
    }
}

private fun ExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    instructions = instructions,
    notes = notes,
    isBodyweight = isBodyweight,
)

private fun com.keepfit.core.database.workout.PlannedWorkoutRow.toModel() = PlannedWorkout(
    id = id,
    templateId = workoutTemplateId,
    templateName = templateName,
    dayOfWeek = dayOfWeek,
)
