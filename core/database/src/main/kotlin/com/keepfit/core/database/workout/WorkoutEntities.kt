package com.keepfit.core.database.workout

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.DayOfWeek
import java.time.LocalDate

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val instructions: String?,
    val notes: String?,
    val isBodyweight: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val source: String? = null,
    val sourceId: String? = null,
    val equipment: String? = null,
    val targetMuscle: String? = null,
    val secondaryMuscles: String? = null,
)

@Entity(
    tableName = "exercise_media",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exerciseId"], unique = true)],
)
data class ExerciseMediaEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val mediaType: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

data class ExerciseDetails(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val media: ExerciseMediaEntity?,
)

@Entity(
    tableName = "workout_templates",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [Index("bodyProfileId")],
)
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
    val origin: String = "CUSTOM",
    val bodyProfileId: String = "",
)

@Entity(
    tableName = "workout_template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
        ),
    ],
    indices = [Index("workoutTemplateId"), Index("exerciseId")],
)
data class WorkoutTemplateExerciseEntity(
    @PrimaryKey val id: String,
    val workoutTemplateId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val targetReps: String?,
    val notes: String?,
)

@Entity(
    tableName = "weekly_plans",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [Index("bodyProfileId")],
)
data class WeeklyPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startsOn: LocalDate,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val bodyProfileId: String = "",
)

@Entity(
    tableName = "planned_workouts",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["weeklyPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
        ),
    ],
    indices = [Index("weeklyPlanId"), Index("workoutTemplateId")],
)
data class PlannedWorkoutEntity(
    @PrimaryKey val id: String,
    val weeklyPlanId: String,
    val workoutTemplateId: String,
    val dayOfWeek: DayOfWeek,
    val position: Int,
)

@Entity(
    tableName = "workout_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = PlannedWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourcePlannedWorkoutId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [
        Index("sourcePlannedWorkoutId"),
        Index("originalDate"),
        Index("scheduledDate"),
        Index("bodyProfileId"),
        Index(value = ["bodyProfileId", "sourcePlannedWorkoutId", "originalDate"], unique = true),
    ],
)
data class WorkoutOccurrenceEntity(
    @PrimaryKey val id: String,
    val sourcePlannedWorkoutId: String?,
    val sourceTemplateId: String?,
    val templateNameSnapshot: String,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val decisionType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val bodyProfileId: String = "",
)

@Entity(
    tableName = "workout_occurrence_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutOccurrenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutOccurrenceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
        ),
    ],
    indices = [Index("workoutOccurrenceId"), Index("exerciseId")],
)
data class WorkoutOccurrenceExerciseEntity(
    @PrimaryKey val id: String,
    val workoutOccurrenceId: String,
    val sourceTemplateExerciseId: String?,
    val exerciseId: String,
    val exerciseNameSnapshot: String,
    val position: Int,
    val targetSets: Int,
    val targetReps: String?,
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
        ),
        ForeignKey(
            entity = PlannedWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["plannedWorkoutId"],
        ),
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [
        Index("workoutTemplateId"),
        Index("plannedWorkoutId"),
        Index("workoutOccurrenceId"),
        Index("bodyProfileId"),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val workoutTemplateId: String?,
    val plannedWorkoutId: String?,
    val workoutDate: LocalDate,
    val startedAt: Long,
    val completedAt: Long?,
    val notes: String?,
    val workoutOccurrenceId: String? = null,
    val sessionVariant: String = "FULL",
    val energyLevel: Int? = null,
    val difficulty: Int? = null,
    val bodyProfileId: String = "",
)

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
        ),
    ],
    indices = [Index("workoutSessionId"), Index("exerciseId")],
)
data class ExerciseLogEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val position: Int,
    val notes: String?,
    val targetSets: Int? = null,
    val targetReps: String? = null,
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseLogId")],
)
data class SetLogEntity(
    @PrimaryKey val id: String,
    val exerciseLogId: String,
    val position: Int,
    val repetitions: Int,
    val weightKg: Double,
    val isCompleted: Boolean,
)

data class TemplateExerciseWithExercise(
    @Embedded val templateExercise: WorkoutTemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
) {
    val exerciseName: String get() = exercise.name
}

data class WorkoutTemplateDetails(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        entity = WorkoutTemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutTemplateId",
    )
    val exercises: List<TemplateExerciseWithExercise>,
)

data class ExerciseLogWithSets(
    @Embedded val exerciseLog: ExerciseLogEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseLogId")
    val sets: List<SetLogEntity>,
) {
    val exerciseName: String get() = exercise.name
}

data class WorkoutSessionDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseLogEntity::class,
        parentColumn = "id",
        entityColumn = "workoutSessionId",
    )
    val exercises: List<ExerciseLogWithSets>,
)

data class PlannedWorkoutRow(
    val id: String,
    val weeklyPlanId: String,
    val workoutTemplateId: String,
    val dayOfWeek: DayOfWeek,
    val position: Int,
    val templateName: String,
    val planStartsOn: LocalDate,
)

data class WorkoutOccurrenceDetails(
    @Embedded val occurrence: WorkoutOccurrenceEntity,
    @Relation(
        entity = WorkoutOccurrenceExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutOccurrenceId",
    )
    val exercises: List<WorkoutOccurrenceExerciseEntity>,
)

data class PersonalRecordRow(
    val exerciseId: String,
    val exerciseName: String,
    val highestWeightKg: Double,
    val highestRepetitions: Int,
)

data class PreviousSetRow(
    val repetitions: Int,
    val weightKg: Double,
)

data class CompletedWorkoutDayRow(
    val workoutDate: LocalDate,
    val completedCount: Int,
)

data class ExercisePerformanceRow(
    val exerciseId: String,
    val highestWeightKg: Double,
    val highestRepetitions: Int,
)
