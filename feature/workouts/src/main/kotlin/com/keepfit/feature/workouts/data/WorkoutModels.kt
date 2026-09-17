package com.keepfit.feature.workouts.data

import com.keepfit.feature.workouts.execution.ProgressionSuggestion
import java.time.DayOfWeek
import java.time.LocalDate

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val instructions: String?,
    val notes: String?,
    val isBodyweight: Boolean,
    val demo: ExerciseDemo? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val equipment: String? = null,
    val targetMuscle: String? = null,
    val secondaryMuscles: String? = null,
)

data class ExerciseDemo(
    val uri: String,
    val mediaType: String,
    val mimeType: String,
)

data class TemplateExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: String?,
    val notes: String?,
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val notes: String?,
    val exercises: List<TemplateExercise>,
)

data class PlannedWorkout(
    val id: String,
    val templateId: String,
    val templateName: String,
    val dayOfWeek: DayOfWeek,
    val position: Int = 0,
    val planStartsOn: LocalDate = LocalDate.MIN,
)

data class LoggedSet(
    val id: String,
    val repetitions: Int,
    val weightKg: Double,
)

data class ActiveExercise(
    val exerciseLogId: String,
    val exerciseId: String,
    val exerciseName: String,
    val notes: String?,
    val targetSets: Int?,
    val targetReps: String?,
    val previousSets: List<LoggedSet>,
    val nextSetSuggestion: LoggedSet?,
    val progressionSuggestion: ProgressionSuggestion?,
    val sets: List<LoggedSet>,
)

data class ActiveWorkout(
    val sessionId: String,
    val templateName: String,
    val workoutDate: LocalDate,
    val exercises: List<ActiveExercise>,
    val sessionVariant: String = "FULL",
)

data class WorkoutFeedback(
    val energyLevel: Int? = null,
    val difficulty: Int? = null,
)

data class WorkoutHistory(
    val sessionId: String,
    val workoutDate: LocalDate,
    val completedAt: Long,
    val exerciseNames: List<String>,
)

data class PersonalRecord(
    val exerciseName: String,
    val highestWeightKg: Double,
    val highestRepetitions: Int,
)
