package com.keepfit.feature.workouts.today

import com.keepfit.feature.workouts.data.ActiveWorkout
import java.time.LocalDate

enum class TodayWorkoutDecision {
    FULL,
    SHORTENED,
    MINIMUM,
    SUBSTITUTED,
    RESCHEDULED,
    SKIPPED,
}

enum class TodayWorkoutVariant {
    FULL,
    SHORTENED,
    MINIMUM,
}

enum class TodayWorkoutStatus {
    LOADING,
    RESUME,
    COMPLETED,
    PLANNED,
    RESCHEDULED,
    SKIPPED,
    REST_DAY,
}

data class TodayExercise(
    val sourceTemplateExerciseId: String?,
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: String?,
)

data class TodayWorkoutAction(
    val occurrenceId: String?,
    val plannedWorkoutId: String?,
    val templateId: String?,
    val title: String,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val decision: TodayWorkoutDecision,
    val exercises: List<TodayExercise>,
)

data class TodayWorkoutSession(
    val id: String,
    val occurrenceId: String?,
    val plannedWorkoutId: String?,
    val workoutDate: LocalDate,
    val isCompleted: Boolean,
)

data class TodayWorkoutState(
    val status: TodayWorkoutStatus = TodayWorkoutStatus.LOADING,
    val primary: TodayWorkoutAction? = null,
    val missed: TodayWorkoutAction? = null,
    val activeWorkout: ActiveWorkout? = null,
) {
    val activeSessionId: String? get() = activeWorkout?.sessionId
}

enum class TodayChangeType {
    SHORTEN,
    MINIMUM,
    SUBSTITUTE,
    RESCHEDULE,
    SKIP,
    RESTORE,
}

data class TodayChangeRequest(
    val plannedWorkoutId: String?,
    val occurrenceId: String?,
    val originalDate: LocalDate,
    val type: TodayChangeType,
    val sourceExerciseId: String? = null,
    val replacementExerciseId: String? = null,
    val targetDate: LocalDate? = null,
)

data class TodayWorkoutPreview(
    val request: TodayChangeRequest,
    val title: String,
    val currentDescription: String,
    val proposedDescription: String,
    val reason: String,
    val exercises: List<TodayExercise>,
)

data class TodayVariantResult(
    val exercises: List<TodayExercise>,
    val explanation: String,
)
