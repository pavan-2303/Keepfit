package com.keepfit.core.database.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Query(
        """
        SELECT * FROM exercises
        WHERE archivedAt IS NULL AND name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeExercises(query: String): Flow<List<ExerciseEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM exercises
        WHERE archivedAt IS NULL AND name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeExerciseDetails(query: String): Flow<List<ExerciseDetails>>

    @Query("UPDATE exercises SET archivedAt = :archivedAt, updatedAt = :archivedAt WHERE id = :id")
    suspend fun archiveExercise(id: String, archivedAt: Long)

    @Query("SELECT * FROM exercises WHERE archivedAt IS NULL AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findActiveExerciseByName(name: String): ExerciseEntity?

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Upsert
    suspend fun upsertExerciseMedia(media: ExerciseMediaEntity)

    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun findExerciseMedia(exerciseId: String): ExerciseMediaEntity?

    @Query("SELECT COUNT(*) FROM workout_template_exercises WHERE exerciseId = :exerciseId")
    suspend fun countExerciseTemplateUsage(exerciseId: String): Int

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE exerciseId = :exerciseId")
    suspend fun countExerciseLogUsage(exerciseId: String): Int

    @Query("SELECT COUNT(*) FROM workout_occurrence_exercises WHERE exerciseId = :exerciseId")
    suspend fun countExerciseOccurrenceUsage(exerciseId: String): Int

    @Upsert
    suspend fun upsertTemplate(template: WorkoutTemplateEntity)

    @Query("SELECT * FROM workout_templates WHERE archivedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id LIMIT 1")
    suspend fun findTemplate(id: String): WorkoutTemplateEntity?

    @Query("SELECT * FROM workout_templates WHERE origin = :origin ORDER BY createdAt")
    suspend fun findTemplatesByOrigin(origin: String): List<WorkoutTemplateEntity>

    @Query(
        "UPDATE workout_templates SET archivedAt = :archivedAt, updatedAt = :archivedAt " +
            "WHERE origin = :origin AND archivedAt IS NULL",
    )
    suspend fun archiveTemplatesByOrigin(origin: String, archivedAt: Long)

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE archivedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeTemplateDetails(): Flow<List<WorkoutTemplateDetails>>

    @Query("DELETE FROM workout_template_exercises WHERE workoutTemplateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE workoutTemplateId = :templateId")
    suspend fun countTemplateSessionUsage(templateId: String): Int

    @Query("SELECT COUNT(*) FROM workout_occurrences WHERE sourceTemplateId = :templateId")
    suspend fun countTemplateOccurrenceUsage(templateId: String): Int

    @Query("DELETE FROM planned_workouts WHERE workoutTemplateId = :templateId")
    suspend fun deletePlannedWorkoutsForTemplate(templateId: String)

    @Insert
    suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>)

    @Transaction
    suspend fun replaceTemplateExercises(
        templateId: String,
        exercises: List<WorkoutTemplateExerciseEntity>,
    ) {
        deleteTemplateExercises(templateId)
        insertTemplateExercises(exercises)
    }

    @Upsert
    suspend fun upsertWeeklyPlan(plan: WeeklyPlanEntity)

    @Query("UPDATE weekly_plans SET isActive = 0")
    suspend fun deactivateWeeklyPlans()

    @Query("DELETE FROM planned_workouts WHERE weeklyPlanId = :planId AND dayOfWeek = :dayOfWeek")
    suspend fun deletePlannedWorkouts(planId: String, dayOfWeek: DayOfWeek)

    @Query("DELETE FROM planned_workouts WHERE weeklyPlanId = :planId")
    suspend fun deleteAllPlannedWorkouts(planId: String)

    @Upsert
    suspend fun upsertPlannedWorkout(workout: PlannedWorkoutEntity)

    @Transaction
    suspend fun replacePlannedWorkout(workout: PlannedWorkoutEntity) {
        deletePlannedWorkouts(workout.weeklyPlanId, workout.dayOfWeek)
        upsertPlannedWorkout(workout)
    }

    @Transaction
    suspend fun replaceWeeklySchedule(
        plan: WeeklyPlanEntity,
        workouts: List<PlannedWorkoutEntity>,
    ) {
        deactivateWeeklyPlans()
        upsertWeeklyPlan(plan)
        deleteAllPlannedWorkouts(plan.id)
        workouts.forEach { upsertPlannedWorkout(it) }
    }

    @Query(
        """
        SELECT planned_workouts.*, workout_templates.name AS templateName,
               weekly_plans.startsOn AS planStartsOn
        FROM planned_workouts
        JOIN weekly_plans ON weekly_plans.id = planned_workouts.weeklyPlanId
        JOIN workout_templates ON workout_templates.id = planned_workouts.workoutTemplateId
        WHERE weekly_plans.isActive = 1 AND planned_workouts.dayOfWeek = :dayOfWeek
        ORDER BY planned_workouts.position
        """,
    )
    fun observePlannedWorkouts(dayOfWeek: DayOfWeek): Flow<List<PlannedWorkoutRow>>

    @Query(
        """
        SELECT planned_workouts.*, workout_templates.name AS templateName,
               weekly_plans.startsOn AS planStartsOn
        FROM planned_workouts
        JOIN weekly_plans ON weekly_plans.id = planned_workouts.weeklyPlanId
        JOIN workout_templates ON workout_templates.id = planned_workouts.workoutTemplateId
        WHERE weekly_plans.isActive = 1
        ORDER BY planned_workouts.dayOfWeek, planned_workouts.position
        """,
    )
    fun observeWeeklySchedule(): Flow<List<PlannedWorkoutRow>>

    @Query(
        """
        SELECT planned_workouts.*, workout_templates.name AS templateName,
               weekly_plans.startsOn AS planStartsOn
        FROM planned_workouts
        JOIN weekly_plans ON weekly_plans.id = planned_workouts.weeklyPlanId
        JOIN workout_templates ON workout_templates.id = planned_workouts.workoutTemplateId
        WHERE planned_workouts.id = :id
        LIMIT 1
        """,
    )
    suspend fun findPlannedWorkout(id: String): PlannedWorkoutRow?

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId LIMIT 1")
    suspend fun findTemplateDetails(templateId: String): WorkoutTemplateDetails?

    @Query("SELECT * FROM exercises WHERE id = :id AND archivedAt IS NULL LIMIT 1")
    suspend fun findActiveExercise(id: String): ExerciseEntity?

    @Upsert
    suspend fun upsertOccurrence(occurrence: WorkoutOccurrenceEntity)

    @Query("SELECT * FROM workout_occurrences WHERE id = :id LIMIT 1")
    suspend fun findOccurrence(id: String): WorkoutOccurrenceEntity?

    @Query(
        """
        SELECT * FROM workout_occurrences
        WHERE sourcePlannedWorkoutId = :plannedWorkoutId AND originalDate = :originalDate
        LIMIT 1
        """,
    )
    suspend fun findOccurrenceForSource(
        plannedWorkoutId: String,
        originalDate: java.time.LocalDate,
    ): WorkoutOccurrenceEntity?

    @Transaction
    @Query("SELECT * FROM workout_occurrences WHERE id = :id LIMIT 1")
    suspend fun findOccurrenceDetails(id: String): WorkoutOccurrenceDetails?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_occurrences
        WHERE originalDate BETWEEN :startDate AND :endDate
           OR scheduledDate BETWEEN :startDate AND :endDate
        ORDER BY scheduledDate, createdAt
        """,
    )
    fun observeOccurrenceDetails(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ): Flow<List<WorkoutOccurrenceDetails>>

    @Query("DELETE FROM workout_occurrence_exercises WHERE workoutOccurrenceId = :occurrenceId")
    suspend fun deleteOccurrenceExercises(occurrenceId: String)

    @Insert
    suspend fun insertOccurrenceExercises(exercises: List<WorkoutOccurrenceExerciseEntity>)

    @Transaction
    suspend fun replaceOccurrence(
        occurrence: WorkoutOccurrenceEntity,
        exercises: List<WorkoutOccurrenceExerciseEntity>,
    ) {
        upsertOccurrence(occurrence)
        deleteOccurrenceExercises(occurrence.id)
        insertOccurrenceExercises(exercises)
    }

    @Query("DELETE FROM workout_occurrences WHERE id = :id")
    suspend fun deleteOccurrence(id: String)

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE workoutOccurrenceId = :occurrenceId")
    suspend fun countSessionsForOccurrence(occurrenceId: String): Int

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NULL LIMIT 1")
    suspend fun findActiveSession(): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE workoutDate BETWEEN :startDate AND :endDate
        ORDER BY startedAt
        """,
    )
    fun observeSessionsBetween(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ): Flow<List<WorkoutSessionEntity>>

    @Query(
        """
        SELECT exercise_logs.exerciseId AS exerciseId,
               MAX(set_logs.weightKg) AS highestWeightKg,
               MAX(set_logs.repetitions) AS highestRepetitions
        FROM workout_sessions
        JOIN exercise_logs ON exercise_logs.workoutSessionId = workout_sessions.id
        JOIN set_logs ON set_logs.exerciseLogId = exercise_logs.id
        WHERE workout_sessions.completedAt IS NOT NULL
          AND workout_sessions.workoutDate BETWEEN :startDate AND :endDate
          AND set_logs.isCompleted = 1
        GROUP BY exercise_logs.exerciseId
        """,
    )
    suspend fun findExercisePerformanceBetween(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ): List<ExercisePerformanceRow>

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE workoutOccurrenceId = :occurrenceId AND completedAt IS NOT NULL
        LIMIT 1
        """,
    )
    suspend fun findCompletedSessionForOccurrence(occurrenceId: String): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE plannedWorkoutId = :plannedWorkoutId
          AND workoutDate = :workoutDate
          AND completedAt IS NOT NULL
        LIMIT 1
        """,
    )
    suspend fun findCompletedSessionForPlanAndDate(
        plannedWorkoutId: String,
        workoutDate: java.time.LocalDate,
    ): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NULL LIMIT 1")
    suspend fun findActiveSessionDetails(): WorkoutSessionDetails?

    @Query(
        """
        UPDATE workout_sessions
        SET completedAt = :completedAt, energyLevel = :energyLevel, difficulty = :difficulty
        WHERE id = :sessionId AND completedAt IS NULL
        """,
    )
    suspend fun completeSession(
        sessionId: String,
        completedAt: Long,
        energyLevel: Int?,
        difficulty: Int?,
    ): Int

    @Query("UPDATE workout_sessions SET sessionVariant = :variant WHERE id = :sessionId")
    suspend fun updateSessionVariant(sessionId: String, variant: String)

    @Insert
    suspend fun insertExerciseLog(log: ExerciseLogEntity)

    @Insert
    suspend fun insertExerciseLogs(logs: List<ExerciseLogEntity>)

    @Transaction
    suspend fun startSessionIfNoneActive(
        session: WorkoutSessionEntity,
        logs: List<ExerciseLogEntity>,
    ): String {
        findActiveSession()?.let { return it.id }
        insertSession(session)
        insertExerciseLogs(logs)
        return session.id
    }

    @Insert
    suspend fun insertSetLog(log: SetLogEntity)

    @Query("UPDATE exercise_logs SET notes = :notes WHERE id = :exerciseLogId")
    suspend fun updateExerciseLogNotes(exerciseLogId: String, notes: String?)

    @Query("UPDATE exercise_logs SET exerciseId = :exerciseId WHERE id = :exerciseLogId")
    suspend fun updateExerciseLogExercise(exerciseLogId: String, exerciseId: String)

    @Query("UPDATE exercise_logs SET targetSets = :targetSets WHERE id = :exerciseLogId")
    suspend fun updateExerciseLogTargetSets(exerciseLogId: String, targetSets: Int?)

    @Query("DELETE FROM exercise_logs WHERE id IN (:exerciseLogIds)")
    suspend fun deleteExerciseLogs(exerciseLogIds: List<String>)

    @Transaction
    suspend fun appendSetToActiveSession(
        setId: String,
        exerciseLogId: String,
        repetitions: Int,
        weightKg: Double,
    ) {
        val active = requireNotNull(findActiveSessionDetails()) { "There is no active workout." }
        val exercise = requireNotNull(
            active.exercises.find { it.exerciseLog.id == exerciseLogId },
        ) { "This exercise is not part of the active workout." }
        insertSetLog(
            SetLogEntity(
                id = setId,
                exerciseLogId = exerciseLogId,
                position = exercise.sets.size,
                repetitions = repetitions,
                weightKg = weightKg,
                isCompleted = true,
            ),
        )
    }

    @Transaction
    suspend fun repeatPreviousSetInActiveSession(setId: String, exerciseLogId: String) {
        val active = requireNotNull(findActiveSessionDetails()) { "There is no active workout." }
        val exercise = requireNotNull(
            active.exercises.find { it.exerciseLog.id == exerciseLogId },
        ) { "This exercise is not part of the active workout." }
        val previousSets = findPreviousSets(exercise.exerciseLog.exerciseId)
        val previous = previousSets.getOrNull(exercise.sets.size) ?: previousSets.lastOrNull()
        requireNotNull(previous) { "There is no previous set to repeat." }
        insertSetLog(
            SetLogEntity(
                id = setId,
                exerciseLogId = exerciseLogId,
                position = exercise.sets.size,
                repetitions = previous.repetitions,
                weightKg = previous.weightKg,
                isCompleted = true,
            ),
        )
    }

    @Transaction
    suspend fun substituteExerciseInActiveSession(
        exerciseLogId: String,
        replacementExerciseId: String,
    ) {
        val active = requireNotNull(findActiveSessionDetails()) { "There is no active workout." }
        val source = requireNotNull(
            active.exercises.find { it.exerciseLog.id == exerciseLogId },
        ) { "This exercise is not part of the active workout." }
        require(source.sets.isEmpty()) { "An exercise with logged sets cannot be substituted." }
        requireNotNull(findActiveExercise(replacementExerciseId)) {
            "The replacement exercise is not available."
        }
        require(
            active.exercises.none {
                it.exerciseLog.id != exerciseLogId && it.exerciseLog.exerciseId == replacementExerciseId
            },
        ) { "That replacement is already in this workout." }
        updateExerciseLogExercise(exerciseLogId, replacementExerciseId)
    }

    @Transaction
    suspend fun convertActiveSessionToMinimum() {
        val active = requireNotNull(findActiveSessionDetails()) { "There is no active workout." }
        val ordered = active.exercises.sortedBy { it.exerciseLog.position }
        val removed = ordered.drop(2)
        require(removed.all { it.sets.isEmpty() }) {
            "Minimum mode cannot remove an exercise with logged sets."
        }
        val removedIds = removed.map { it.exerciseLog.id }
        if (removedIds.isNotEmpty()) {
            deleteExerciseLogs(removedIds)
        }
        ordered.take(2).forEach { exercise ->
            updateExerciseLogTargetSets(
                exercise.exerciseLog.id,
                exercise.exerciseLog.targetSets?.coerceAtMost(2),
            )
        }
        updateSessionVariant(active.session.id, "MINIMUM")
    }

    @Transaction
    suspend fun completeActiveSession(
        completedAt: Long,
        energyLevel: Int?,
        difficulty: Int?,
    ) {
        val active = requireNotNull(findActiveSession()) { "There is no active workout." }
        check(completeSession(active.id, completedAt, energyLevel, difficulty) == 1) {
            "The active workout could not be completed."
        }
    }

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun observeSessionHistory(): Flow<List<WorkoutSessionDetails>>

    @Query(
        """
        SELECT workoutDate, COUNT(*) AS completedCount
        FROM workout_sessions
        WHERE completedAt IS NOT NULL
        GROUP BY workoutDate
        ORDER BY workoutDate DESC
        """,
    )
    fun observeCompletedWorkoutDays(): Flow<List<CompletedWorkoutDayRow>>

    @Query(
        """
        SELECT exercise_logs.exerciseId,
               exercises.name AS exerciseName,
               MAX(set_logs.weightKg) AS highestWeightKg,
               MAX(set_logs.repetitions) AS highestRepetitions
        FROM set_logs
        JOIN exercise_logs ON exercise_logs.id = set_logs.exerciseLogId
        JOIN exercises ON exercises.id = exercise_logs.exerciseId
        JOIN workout_sessions ON workout_sessions.id = exercise_logs.workoutSessionId
        WHERE set_logs.isCompleted = 1 AND workout_sessions.completedAt IS NOT NULL
        GROUP BY exercise_logs.exerciseId, exercises.name
        ORDER BY exercises.name COLLATE NOCASE
        """,
    )
    fun observePersonalRecords(): Flow<List<PersonalRecordRow>>

    @Query(
        """
        SELECT set_logs.repetitions, set_logs.weightKg
        FROM set_logs
        JOIN exercise_logs ON exercise_logs.id = set_logs.exerciseLogId
        JOIN workout_sessions ON workout_sessions.id = exercise_logs.workoutSessionId
        WHERE exercise_logs.exerciseId = :exerciseId
          AND set_logs.isCompleted = 1
          AND workout_sessions.id = (
              SELECT previous_sessions.id
              FROM workout_sessions AS previous_sessions
              JOIN exercise_logs AS previous_logs
                ON previous_logs.workoutSessionId = previous_sessions.id
              WHERE previous_logs.exerciseId = :exerciseId
                AND previous_sessions.completedAt IS NOT NULL
              ORDER BY previous_sessions.completedAt DESC
              LIMIT 1
          )
        ORDER BY set_logs.position
        """,
    )
    suspend fun findPreviousSets(exerciseId: String): List<PreviousSetRow>
}
