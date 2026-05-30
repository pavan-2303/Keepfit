package com.keepfit.feature.workouts.data

import java.time.DayOfWeek
import java.time.LocalDate

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val instructions: String?,
    val notes: String?,
    val isBodyweight: Boolean,
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
)

data class LoggedSet(
    val id: String,
    val repetitions: Int,
    val weightKg: Double,
)

data class ActiveExercise(
    val exerciseLogId: String,
    val exerciseName: String,
    val notes: String?,
    val previousSet: LoggedSet?,
    val sets: List<LoggedSet>,
)

data class ActiveWorkout(
    val sessionId: String,
    val templateName: String,
    val workoutDate: LocalDate,
    val exercises: List<ActiveExercise>,
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

