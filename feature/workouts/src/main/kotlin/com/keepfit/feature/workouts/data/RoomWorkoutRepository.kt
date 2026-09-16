package com.keepfit.feature.workouts.data

import android.net.Uri
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.ExerciseDetails
import com.keepfit.core.database.workout.ExerciseLogEntity
import com.keepfit.core.database.workout.ExerciseMediaEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.SetLogEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.database.workout.WorkoutOccurrenceDetails
import com.keepfit.core.database.workout.WorkoutOccurrenceEntity
import com.keepfit.core.database.workout.WorkoutOccurrenceExerciseEntity
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateDetails
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.media.ExerciseMediaStore
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.workouts.CompletedSetInput
import com.keepfit.feature.workouts.ExerciseInput
import com.keepfit.feature.workouts.execution.WorkoutExecutionRules
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayChangeType
import com.keepfit.feature.workouts.today.TodayExercise
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutDecision
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutResolver
import com.keepfit.feature.workouts.today.TodayWorkoutRules
import com.keepfit.feature.workouts.today.TodayWorkoutSession
import com.keepfit.feature.workouts.today.TodayWorkoutState
import com.keepfit.feature.workouts.today.TodayWorkoutVariant
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val mediaStore: ExerciseMediaStore,
    private val settingsRepository: AppSettingsRepository,
    private val activeProfileStore: ActiveProfileStore,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) : WorkoutRepository {
    private val todayRules = TodayWorkoutRules()
    private val todayResolver = TodayWorkoutResolver()
    private val todayWriteMutex = Mutex()
    private val executionRules = WorkoutExecutionRules()
    private val executionWriteMutex = Mutex()

    override fun observeRestTimerSeconds(): Flow<Int> =
        settingsRepository.observeSettings().map { it.restTimerSeconds }

    override fun observeExercises(query: String): Flow<List<Exercise>> =
        dao.observeExerciseDetails(query).map { details -> details.map { it.toModel(mediaStore) } }

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observeTemplateDetailsForProfile(profileId)
        }.map { details ->
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
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observeWeeklyScheduleForProfile(profileId)
        }.map { rows -> rows.map { it.toModel() } }

    override fun observeTodayPlan(): Flow<List<PlannedWorkout>> =
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observePlannedWorkoutsForProfile(profileId, today().dayOfWeek)
        }.map { rows -> rows.map { it.toModel() } }

    override fun observeTodayWorkout(): Flow<TodayWorkoutState> {
        val todayDate = today()
        val recoveryStart = todayDate.minusDays(7)
        val occurrenceEnd = todayDate.plusDays(7)
        return combine(
            observeWeeklySchedule(),
            observeTemplates(),
            activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
                dao.observeOccurrenceDetailsForProfile(profileId, recoveryStart, occurrenceEnd)
            },
            activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
                dao.observeSessionsBetweenForProfile(profileId, recoveryStart, todayDate)
            },
            observeActiveWorkout(),
        ) { schedule, templates, occurrences, sessions, activeWorkout ->
            val templatesById = templates.associateBy(WorkoutTemplate::id)
            val recurringCandidates = (0L..7L).flatMap { daysAgo ->
                val date = todayDate.minusDays(daysAgo)
                schedule
                    .filter { planned -> planned.dayOfWeek == date.dayOfWeek && !date.isBefore(planned.planStartsOn) }
                    .sortedBy(PlannedWorkout::position)
                    .mapNotNull { planned ->
                        templatesById[planned.templateId]?.let { template -> planned.toTodayAction(date, template) }
                    }
            }
            val occurrenceCandidates = occurrences.map(WorkoutOccurrenceDetails::toTodayAction)
            todayResolver.resolve(
                today = todayDate,
                activeWorkout = activeWorkout,
                candidates = recurringCandidates + occurrenceCandidates,
                sessions = sessions.map { session ->
                    TodayWorkoutSession(
                        id = session.id,
                        occurrenceId = session.workoutOccurrenceId,
                        plannedWorkoutId = session.plannedWorkoutId,
                        workoutDate = session.workoutDate,
                        isCompleted = session.completedAt != null,
                    )
                },
            )
        }
    }

    override fun observeActiveWorkout(): Flow<ActiveWorkout?> =
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observeActiveSessionForProfile(profileId).mapLatest { details -> profileId to details }
        }.mapLatest { (profileId, details) ->
            details?.let {
                ActiveWorkout(
                    sessionId = it.session.id,
                    templateName = it.session.workoutTemplateId
                        ?.let { templateId -> dao.findTemplateDetailsForProfile(templateId, profileId) }
                        ?.template
                        ?.name
                        ?: "Workout",
                    workoutDate = it.session.workoutDate,
                    sessionVariant = it.session.sessionVariant,
                    exercises = it.exercises
                        .sortedBy { log -> log.exerciseLog.position }
                        .map { log ->
                            val previousSets = dao.findPreviousSetsForProfile(profileId, log.exerciseLog.exerciseId)
                                .mapIndexed { index, set ->
                                    LoggedSet(
                                        id = "previous-$index",
                                        repetitions = set.repetitions,
                                        weightKg = set.weightKg,
                                    )
                                }
                            ActiveExercise(
                                exerciseLogId = log.exerciseLog.id,
                                exerciseId = log.exerciseLog.exerciseId,
                                exerciseName = log.exerciseName,
                                notes = log.exerciseLog.notes,
                                targetSets = log.exerciseLog.targetSets,
                                targetReps = log.exerciseLog.targetReps,
                                previousSets = previousSets,
                                nextSetSuggestion = executionRules.nextSet(previousSets, log.sets.size),
                                progressionSuggestion = executionRules.progression(
                                    previousSets = previousSets,
                                    targetSets = log.exerciseLog.targetSets,
                                    targetReps = log.exerciseLog.targetReps,
                                ),
                                sets = log.sets.sortedBy(SetLogEntity::position).map {
                                    LoggedSet(id = it.id, repetitions = it.repetitions, weightKg = it.weightKg)
                                },
                            )
                        },
                )
            }
        }

    override fun observeHistory(): Flow<List<WorkoutHistory>> =
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observeSessionHistoryForProfile(profileId)
        }.map { sessions ->
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
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest { profileId ->
            dao.observePersonalRecordsForProfile(profileId)
        }.map { records ->
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
        val existing = id?.let { dao.findActiveExercise(it) }
        val importedMedia = mediaUri?.let(mediaStore::import)
        dao.upsertExercise(
            ExerciseEntity(
                id = exerciseId,
                name = input.name,
                muscleGroup = input.muscleGroup,
                instructions = input.instructions,
                notes = input.notes,
                isBodyweight = input.isBodyweight,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                archivedAt = null,
                source = existing?.source,
                sourceId = existing?.sourceId,
                equipment = existing?.equipment,
                targetMuscle = existing?.targetMuscle,
                secondaryMuscles = existing?.secondaryMuscles,
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

    override suspend fun deleteExercise(id: String) {
        require(
            dao.countExerciseTemplateUsage(id) == 0 &&
                dao.countExerciseLogUsage(id) == 0 &&
                dao.countExerciseOccurrenceUsage(id) == 0,
        ) {
            "This exercise is already used in templates or workout history. Archive it instead."
        }
        dao.findExerciseMedia(id)?.let { mediaStore.delete(it.relativePath) }
        dao.deleteExercise(id)
    }

    override suspend fun createTemplate(name: String, exerciseIds: List<String>) {
        val profileId = requireProfileId()
        require(name.isNotBlank()) { "Enter a template name." }
        require(exerciseIds.isNotEmpty()) { "Choose at least one exercise." }
        val templateId = idFactory()
        val now = clock()
        dao.upsertTemplate(
            WorkoutTemplateEntity(templateId, name.trim(), null, now, now, null, bodyProfileId = profileId),
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

    override suspend fun deleteTemplate(id: String) {
        requireNotNull(dao.findTemplateForProfile(id, requireProfileId())) { "Workout template not found." }
        require(
            dao.countTemplateSessionUsage(id) == 0 && dao.countTemplateOccurrenceUsage(id) == 0,
        ) {
            "This template is already referenced by a dated workout or history and cannot be deleted."
        }
        dao.deletePlannedWorkoutsForTemplate(id)
        dao.deleteTemplate(id)
    }

    override suspend fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String) {
        val profileId = requireProfileId()
        val now = clock()
        val planId = "default-weekly-plan-$profileId"
        requireNotNull(dao.findTemplateForProfile(templateId, profileId)) { "Workout template not found." }
        dao.deactivateWeeklyPlansForProfile(profileId)
        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = planId,
                name = "Default week",
                startsOn = today().with(DayOfWeek.MONDAY),
                isActive = true,
                createdAt = now,
                updatedAt = now,
                bodyProfileId = profileId,
            ),
        )
        dao.replacePlannedWorkout(
            PlannedWorkoutEntity(idFactory(), planId, templateId, dayOfWeek, 0),
        )
    }

    override suspend fun clearPlannedWorkout(dayOfWeek: DayOfWeek) {
        val planId = "default-weekly-plan-${requireProfileId()}"
        dao.deletePlannedWorkouts(planId, dayOfWeek)
    }

    override suspend fun replaceWeeklySchedule(assignments: List<Pair<DayOfWeek, String>>) {
        val profileId = requireProfileId()
        require(assignments.isNotEmpty()) { "Choose at least one planned workout." }
        require(assignments.map { it.first }.distinct().size == assignments.size) {
            "A weekday can be assigned only once."
        }
        assignments.forEach { (_, templateId) ->
            requireNotNull(dao.findTemplateForProfile(templateId, profileId)) { "Workout template not found." }
        }
        val now = clock()
        val planId = "default-weekly-plan-$profileId"
        dao.replaceWeeklyScheduleForProfile(
            profileId = profileId,
            plan = WeeklyPlanEntity(
                id = planId,
                name = "Default week",
                startsOn = today().with(DayOfWeek.MONDAY),
                isActive = true,
                createdAt = now,
                updatedAt = now,
                bodyProfileId = profileId,
            ),
            workouts = assignments.mapIndexed { position, (day, templateId) ->
                PlannedWorkoutEntity(idFactory(), planId, templateId, day, position)
            },
        )
    }

    override suspend fun startOrResume(plannedWorkout: PlannedWorkout): String {
        val profileId = requireProfileId()
        val template = requireNotNull(dao.findTemplateDetailsForProfile(plannedWorkout.templateId, profileId))
        val sessionId = idFactory()
        return dao.startSessionIfNoneActiveForProfile(
            profileId = profileId,
            session = WorkoutSessionEntity(
                id = sessionId,
                workoutTemplateId = template.template.id,
                plannedWorkoutId = plannedWorkout.id,
                workoutDate = today(),
                startedAt = clock(),
                completedAt = null,
                notes = null,
                bodyProfileId = profileId,
            ),
            logs = template.exercises.sortedBy { it.templateExercise.position }.mapIndexed { index, item ->
                ExerciseLogEntity(
                    id = idFactory(),
                    workoutSessionId = sessionId,
                    exerciseId = item.templateExercise.exerciseId,
                    position = index,
                    notes = null,
                    targetSets = item.templateExercise.targetSets,
                    targetReps = item.templateExercise.targetReps,
                )
            },
        )
    }

    override suspend fun previewTodayChange(request: TodayChangeRequest): TodayWorkoutPreview {
        validateTodayRequest(request)
        val action = loadTodayAction(request)
        val exercises = proposedExercises(action, request)
        val proposedDescription = when (request.type) {
            TodayChangeType.SHORTEN -> "${exercises.size} exercises with one fewer set where possible"
            TodayChangeType.MINIMUM -> "${exercises.size} essential exercises for up to two sets each"
            TodayChangeType.SUBSTITUTE -> {
                val replacement = requireNotNull(exercises.find { it.exerciseId == request.replacementExerciseId })
                "Swap in ${replacement.exerciseName} for this date only"
            }
            TodayChangeType.RESCHEDULE -> "Move this workout to ${requireNotNull(request.targetDate)}"
            TodayChangeType.SKIP -> "Mark this workout skipped and keep it in your weekly record"
            TodayChangeType.RESTORE -> "Return to the full recurring workout"
        }
        val reason = when (request.type) {
            TodayChangeType.SHORTEN -> "A shorter session keeps the main work while reducing volume."
            TodayChangeType.MINIMUM -> "A minimum session protects the habit on a constrained day."
            TodayChangeType.SUBSTITUTE -> "Only today's exercise changes; your reusable template stays intact."
            TodayChangeType.RESCHEDULE -> "The workout remains available without changing the weekly template."
            TodayChangeType.SKIP -> "Skipping is recorded honestly and can be restored before a session starts."
            TodayChangeType.RESTORE -> "The dated adjustment is removed and the recurring plan becomes active again."
        }
        return TodayWorkoutPreview(
            request = request,
            title = action.title,
            currentDescription = action.describe(),
            proposedDescription = proposedDescription,
            reason = reason,
            exercises = exercises,
        )
    }

    override suspend fun confirmTodayChange(request: TodayChangeRequest) = todayWriteMutex.withLock {
        val profileId = requireProfileId()
        require(dao.findActiveSessionForProfile(profileId) == null) { "Finish the active workout before changing today's plan." }
        val action = loadTodayAction(request)
        if (request.type == TodayChangeType.RESTORE) {
            val occurrenceId = requireNotNull(action.occurrenceId) { "There is no dated change to restore." }
            require(action.plannedWorkoutId != null && dao.findPlannedWorkoutForProfile(action.plannedWorkoutId, profileId) != null) {
                "The original recurring workout is no longer available."
            }
            require(dao.countSessionsForOccurrence(occurrenceId) == 0) {
                "A workout already references this change, so it cannot be restored."
            }
            dao.deleteOccurrence(occurrenceId)
            return@withLock
        }

        val preview = previewTodayChange(request)
        val existing = action.occurrenceId?.let { occurrenceId -> dao.findOccurrenceForProfile(occurrenceId, profileId) }
        val occurrenceId = existing?.id ?: idFactory()
        val timestamp = clock()
        val decision = request.type.toDecision()
        val scheduledDate = when (request.type) {
            TodayChangeType.RESCHEDULE -> requireNotNull(request.targetDate)
            TodayChangeType.SKIP -> action.originalDate
            else -> action.scheduledDate
        }
        dao.replaceOccurrence(
            occurrence = WorkoutOccurrenceEntity(
                id = occurrenceId,
                sourcePlannedWorkoutId = action.plannedWorkoutId,
                sourceTemplateId = action.templateId,
                templateNameSnapshot = action.title,
                originalDate = action.originalDate,
                scheduledDate = scheduledDate,
                decisionType = decision.name,
                createdAt = existing?.createdAt ?: timestamp,
                updatedAt = timestamp,
                bodyProfileId = profileId,
            ),
            exercises = preview.exercises.mapIndexed { index, exercise ->
                WorkoutOccurrenceExerciseEntity(
                    id = idFactory(),
                    workoutOccurrenceId = occurrenceId,
                    sourceTemplateExerciseId = exercise.sourceTemplateExerciseId,
                    exerciseId = exercise.exerciseId,
                    exerciseNameSnapshot = exercise.exerciseName,
                    position = index,
                    targetSets = exercise.targetSets,
                    targetReps = exercise.targetReps,
                )
            },
        )
    }

    override suspend fun startOrResumeToday(action: TodayWorkoutAction): String = todayWriteMutex.withLock {
        val profileId = requireProfileId()
        dao.findActiveSessionForProfile(profileId)?.let { return@withLock it.id }
        require(action.decision != TodayWorkoutDecision.SKIPPED) {
            "Restore this skipped workout before starting it."
        }
        val occurrence = materializeOccurrence(action)
        require(occurrence.occurrence.scheduledDate == today()) {
            "This workout is scheduled for ${occurrence.occurrence.scheduledDate}."
        }
        occurrence.occurrence.id.let { occurrenceId ->
            require(dao.findCompletedSessionForOccurrenceAndProfile(occurrenceId, profileId) == null) {
                "This workout is already complete."
            }
        }
        occurrence.occurrence.sourcePlannedWorkoutId?.let { plannedWorkoutId ->
            require(
                dao.findCompletedSessionForPlanDateAndProfile(
                    plannedWorkoutId,
                    occurrence.occurrence.originalDate,
                    profileId,
                ) == null,
            ) { "This workout is already complete." }
        }
        val sessionId = idFactory()
        dao.startSessionIfNoneActiveForProfile(
            profileId = profileId,
            session = WorkoutSessionEntity(
                id = sessionId,
                workoutTemplateId = occurrence.occurrence.sourceTemplateId,
                plannedWorkoutId = occurrence.occurrence.sourcePlannedWorkoutId,
                workoutDate = today(),
                startedAt = clock(),
                completedAt = null,
                notes = null,
                workoutOccurrenceId = occurrence.occurrence.id,
                sessionVariant = occurrence.occurrence.decisionType,
                bodyProfileId = profileId,
            ),
            logs = occurrence.exercises.sortedBy { it.position }.mapIndexed { index, exercise ->
                ExerciseLogEntity(
                    id = idFactory(),
                    workoutSessionId = sessionId,
                    exerciseId = exercise.exerciseId,
                    position = index,
                    notes = null,
                    targetSets = exercise.targetSets,
                    targetReps = exercise.targetReps,
                )
            },
        )
    }

    private suspend fun loadTodayAction(request: TodayChangeRequest): TodayWorkoutAction {
        val profileId = requireProfileId()
        val occurrenceDetails = request.occurrenceId
            ?.let { occurrenceId -> dao.findOccurrenceDetailsForProfile(occurrenceId, profileId) }
            ?: request.plannedWorkoutId
                ?.let { dao.findOccurrenceForSourceAndProfile(it, request.originalDate, profileId) }
                ?.let { dao.findOccurrenceDetailsForProfile(it.id, profileId) }
        if (occurrenceDetails != null) return occurrenceDetails.toTodayAction()

        val plannedWorkoutId = requireNotNull(request.plannedWorkoutId) {
            "The source workout is no longer available."
        }
        val planned = requireNotNull(dao.findPlannedWorkoutForProfile(plannedWorkoutId, profileId)) {
            "The source workout is no longer available."
        }.toModel()
        val template = requireNotNull(dao.findTemplateDetailsForProfile(planned.templateId, profileId)) {
            "The workout template is no longer available."
        }.toModel()
        return planned.toTodayAction(request.originalDate, template)
    }

    private suspend fun proposedExercises(
        action: TodayWorkoutAction,
        request: TodayChangeRequest,
    ): List<TodayExercise> = when (request.type) {
        TodayChangeType.SHORTEN -> sourceTemplate(action)
            .let { todayRules.createVariant(it, TodayWorkoutVariant.SHORTENED).exercises }
        TodayChangeType.MINIMUM -> sourceTemplate(action)
            .let { todayRules.createVariant(it, TodayWorkoutVariant.MINIMUM).exercises }
        TodayChangeType.SUBSTITUTE -> {
            val sourceExerciseId = requireNotNull(request.sourceExerciseId) { "Choose an exercise to replace." }
            val replacementExerciseId = requireNotNull(request.replacementExerciseId) { "Choose a replacement exercise." }
            require(sourceExerciseId != replacementExerciseId) { "Choose a different replacement exercise." }
            require(action.exercises.any { it.exerciseId == sourceExerciseId }) {
                "The exercise to replace is not in this workout."
            }
            require(action.exercises.none { it.exerciseId == replacementExerciseId }) {
                "That replacement is already in this workout."
            }
            val replacement = requireNotNull(dao.findActiveExercise(replacementExerciseId)) {
                "The replacement exercise is not available."
            }
            action.exercises.map { exercise ->
                if (exercise.exerciseId == sourceExerciseId) {
                    exercise.copy(
                        sourceTemplateExerciseId = null,
                        exerciseId = replacement.id,
                        exerciseName = replacement.name,
                    )
                } else {
                    exercise
                }
            }
        }
        TodayChangeType.RESCHEDULE,
        TodayChangeType.SKIP,
        -> action.exercises
        TodayChangeType.RESTORE -> sourceTemplate(action)
            .let { todayRules.createVariant(it, TodayWorkoutVariant.FULL).exercises }
    }

    private suspend fun sourceTemplate(action: TodayWorkoutAction): WorkoutTemplate {
        val templateId = requireNotNull(action.templateId) { "The source template is no longer available." }
        return requireNotNull(dao.findTemplateDetailsForProfile(templateId, requireProfileId())) {
            "The source template is no longer available."
        }.toModel()
    }

    private suspend fun materializeOccurrence(action: TodayWorkoutAction): WorkoutOccurrenceDetails {
        val profileId = requireProfileId()
        action.occurrenceId?.let { occurrenceId ->
            return requireNotNull(dao.findOccurrenceDetailsForProfile(occurrenceId, profileId)) {
                "This dated workout is no longer available."
            }
        }
        action.plannedWorkoutId?.let { plannedWorkoutId ->
            dao.findOccurrenceForSourceAndProfile(plannedWorkoutId, action.originalDate, profileId)?.let { existing ->
                return requireNotNull(dao.findOccurrenceDetailsForProfile(existing.id, profileId))
            }
        }
        val occurrenceId = idFactory()
        val timestamp = clock()
        dao.replaceOccurrence(
            occurrence = WorkoutOccurrenceEntity(
                id = occurrenceId,
                sourcePlannedWorkoutId = action.plannedWorkoutId,
                sourceTemplateId = action.templateId,
                templateNameSnapshot = action.title,
                originalDate = action.originalDate,
                scheduledDate = action.scheduledDate,
                decisionType = TodayWorkoutDecision.FULL.name,
                createdAt = timestamp,
                updatedAt = timestamp,
                bodyProfileId = profileId,
            ),
            exercises = action.exercises.mapIndexed { index, exercise ->
                WorkoutOccurrenceExerciseEntity(
                    id = idFactory(),
                    workoutOccurrenceId = occurrenceId,
                    sourceTemplateExerciseId = exercise.sourceTemplateExerciseId,
                    exerciseId = exercise.exerciseId,
                    exerciseNameSnapshot = exercise.exerciseName,
                    position = index,
                    targetSets = exercise.targetSets,
                    targetReps = exercise.targetReps,
                )
            },
        )
        return requireNotNull(dao.findOccurrenceDetailsForProfile(occurrenceId, profileId))
    }

    private fun validateTodayRequest(request: TodayChangeRequest) {
        val todayDate = today()
        require(!request.originalDate.isBefore(todayDate.minusDays(7))) {
            "Only workouts from the last seven days can be changed."
        }
        require(!request.originalDate.isAfter(todayDate)) { "Future workouts are not available from Today." }
        if (request.type == TodayChangeType.RESCHEDULE) {
            val targetDate = requireNotNull(request.targetDate) { "Choose a new workout date." }
            require(!targetDate.isBefore(todayDate) && !targetDate.isAfter(todayDate.plusDays(7))) {
                "Choose a date from today through the next seven days."
            }
            require(targetDate != request.originalDate) { "Choose a different workout date." }
        }
    }

    override suspend fun addSet(exerciseLogId: String, input: CompletedSetInput) =
        executionWriteMutex.withLock {
            dao.appendSetToActiveSessionForProfile(
                profileId = requireProfileId(),
                setId = idFactory(),
                exerciseLogId = exerciseLogId,
                repetitions = input.repetitions,
                weightKg = input.weightKg,
            )
        }

    override suspend fun repeatPreviousSet(exerciseLogId: String) = executionWriteMutex.withLock {
        dao.repeatPreviousSetInActiveSessionForProfile(requireProfileId(), idFactory(), exerciseLogId)
    }

    override suspend fun substituteActiveExercise(
        exerciseLogId: String,
        replacementExerciseId: String,
    ) = executionWriteMutex.withLock {
        dao.substituteExerciseInActiveSessionForProfile(requireProfileId(), exerciseLogId, replacementExerciseId)
    }

    override suspend fun convertActiveWorkoutToMinimum() = executionWriteMutex.withLock {
        dao.convertActiveSessionToMinimumForProfile(requireProfileId())
    }

    override suspend fun updateExerciseNotes(exerciseLogId: String, notes: String) =
        require(
            dao.updateExerciseLogNotesForProfile(
                requireProfileId(),
                exerciseLogId,
                notes.trim().ifEmpty { null },
            ) == 1,
        ) { "This exercise is not part of the active profile." }

    override suspend fun completeActiveWorkout(feedback: WorkoutFeedback?) =
        executionWriteMutex.withLock {
            feedback?.energyLevel?.let { require(it in 1..5) { "Energy must be between 1 and 5." } }
            feedback?.difficulty?.let { require(it in 1..5) { "Difficulty must be between 1 and 5." } }
            dao.completeActiveSessionForProfile(
                profileId = requireProfileId(),
                completedAt = clock(),
                energyLevel = feedback?.energyLevel,
                difficulty = feedback?.difficulty,
            )
        }

    private suspend fun requireProfileId(): String =
        requireNotNull(activeProfileStore.observeActiveProfileId().first()) { "Profile missing." }
}

private fun ExerciseDetails.toModel(mediaStore: ExerciseMediaStore) = Exercise(
    id = exercise.id,
    name = exercise.name,
    muscleGroup = exercise.muscleGroup,
    instructions = exercise.instructions,
    notes = exercise.notes,
    isBodyweight = exercise.isBodyweight,
    source = exercise.source,
    sourceId = exercise.sourceId,
    equipment = exercise.equipment,
    targetMuscle = exercise.targetMuscle,
    secondaryMuscles = exercise.secondaryMuscles,
    demo = media?.let { attached ->
        mediaStore.resolve(attached.relativePath)?.let { uri ->
            ExerciseDemo(
                uri = uri.toString(),
                mediaType = attached.mediaType,
                mimeType = attached.mimeType,
            )
        }
    },
)

private fun com.keepfit.core.database.workout.PlannedWorkoutRow.toModel() = PlannedWorkout(
    id = id,
    templateId = workoutTemplateId,
    templateName = templateName,
    dayOfWeek = dayOfWeek,
    position = position,
    planStartsOn = planStartsOn,
)

private fun WorkoutTemplateDetails.toModel() = WorkoutTemplate(
    id = template.id,
    name = template.name,
    notes = template.notes,
    exercises = exercises.sortedBy { it.templateExercise.position }.map { item ->
        TemplateExercise(
            id = item.templateExercise.id,
            exerciseId = item.templateExercise.exerciseId,
            exerciseName = item.exerciseName,
            targetSets = item.templateExercise.targetSets,
            targetReps = item.templateExercise.targetReps,
            notes = item.templateExercise.notes,
        )
    },
)

private fun PlannedWorkout.toTodayAction(
    date: LocalDate,
    template: WorkoutTemplate,
): TodayWorkoutAction = TodayWorkoutAction(
    occurrenceId = null,
    plannedWorkoutId = id,
    templateId = templateId,
    title = templateName,
    originalDate = date,
    scheduledDate = date,
    decision = TodayWorkoutDecision.FULL,
    exercises = TodayWorkoutRules().createVariant(template, TodayWorkoutVariant.FULL).exercises,
)

private fun WorkoutOccurrenceDetails.toTodayAction(): TodayWorkoutAction = TodayWorkoutAction(
    occurrenceId = occurrence.id,
    plannedWorkoutId = occurrence.sourcePlannedWorkoutId,
    templateId = occurrence.sourceTemplateId,
    title = occurrence.templateNameSnapshot,
    originalDate = occurrence.originalDate,
    scheduledDate = occurrence.scheduledDate,
    decision = TodayWorkoutDecision.valueOf(occurrence.decisionType),
    exercises = exercises.sortedBy { it.position }.map { exercise ->
        TodayExercise(
            sourceTemplateExerciseId = exercise.sourceTemplateExerciseId,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseNameSnapshot,
            targetSets = exercise.targetSets,
            targetReps = exercise.targetReps,
        )
    },
)

private fun TodayWorkoutAction.describe(): String {
    val exerciseLabel = if (exercises.size == 1) "1 exercise" else "${exercises.size} exercises"
    return when {
        decision == TodayWorkoutDecision.SKIPPED -> "Skipped for $originalDate"
        scheduledDate != originalDate -> "$exerciseLabel, moved to $scheduledDate"
        else -> "$exerciseLabel planned for $scheduledDate"
    }
}

private fun TodayChangeType.toDecision(): TodayWorkoutDecision = when (this) {
    TodayChangeType.SHORTEN -> TodayWorkoutDecision.SHORTENED
    TodayChangeType.MINIMUM -> TodayWorkoutDecision.MINIMUM
    TodayChangeType.SUBSTITUTE -> TodayWorkoutDecision.SUBSTITUTED
    TodayChangeType.RESCHEDULE -> TodayWorkoutDecision.RESCHEDULED
    TodayChangeType.SKIP -> TodayWorkoutDecision.SKIPPED
    TodayChangeType.RESTORE -> TodayWorkoutDecision.FULL
}
