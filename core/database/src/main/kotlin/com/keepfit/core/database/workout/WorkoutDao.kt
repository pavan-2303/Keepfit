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

    @Query("UPDATE exercises SET archivedAt = :archivedAt, updatedAt = :archivedAt WHERE id = :id")
    suspend fun archiveExercise(id: String, archivedAt: Long)

    @Upsert
    suspend fun upsertExerciseMedia(media: ExerciseMediaEntity)

    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun findExerciseMedia(exerciseId: String): ExerciseMediaEntity?

    @Upsert
    suspend fun upsertTemplate(template: WorkoutTemplateEntity)

    @Query("SELECT * FROM workout_templates WHERE archivedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE archivedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeTemplateDetails(): Flow<List<WorkoutTemplateDetails>>

    @Query("DELETE FROM workout_template_exercises WHERE workoutTemplateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

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

    @Upsert
    suspend fun upsertPlannedWorkout(workout: PlannedWorkoutEntity)

    @Transaction
    suspend fun replacePlannedWorkout(workout: PlannedWorkoutEntity) {
        deletePlannedWorkouts(workout.weeklyPlanId, workout.dayOfWeek)
        upsertPlannedWorkout(workout)
    }

    @Query(
        """
        SELECT planned_workouts.*, workout_templates.name AS templateName
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
        SELECT planned_workouts.*, workout_templates.name AS templateName
        FROM planned_workouts
        JOIN weekly_plans ON weekly_plans.id = planned_workouts.weeklyPlanId
        JOIN workout_templates ON workout_templates.id = planned_workouts.workoutTemplateId
        WHERE weekly_plans.isActive = 1
        ORDER BY planned_workouts.dayOfWeek, planned_workouts.position
        """,
    )
    fun observeWeeklySchedule(): Flow<List<PlannedWorkoutRow>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId LIMIT 1")
    suspend fun findTemplateDetails(templateId: String): WorkoutTemplateDetails?

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NULL LIMIT 1")
    suspend fun findActiveSession(): WorkoutSessionEntity?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionDetails?>

    @Query("UPDATE workout_sessions SET completedAt = :completedAt WHERE id = :sessionId")
    suspend fun completeSession(sessionId: String, completedAt: Long)

    @Insert
    suspend fun insertExerciseLog(log: ExerciseLogEntity)

    @Insert
    suspend fun insertSetLog(log: SetLogEntity)

    @Query("SELECT COUNT(*) FROM set_logs WHERE exerciseLogId = :exerciseLogId")
    suspend fun countSets(exerciseLogId: String): Int

    @Query("UPDATE exercise_logs SET notes = :notes WHERE id = :exerciseLogId")
    suspend fun updateExerciseLogNotes(exerciseLogId: String, notes: String?)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun observeSessionHistory(): Flow<List<WorkoutSessionDetails>>

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
          AND workout_sessions.completedAt IS NOT NULL
        ORDER BY workout_sessions.completedAt DESC, set_logs.position
        LIMIT 1
        """,
    )
    suspend fun findPreviousSet(exerciseId: String): PreviousSetRow?
}
