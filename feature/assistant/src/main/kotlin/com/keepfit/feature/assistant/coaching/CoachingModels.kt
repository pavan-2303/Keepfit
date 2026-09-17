package com.keepfit.feature.assistant.coaching

import java.time.LocalDate

enum class CoachingIntent(val label: String, val promptHint: String) {
    WEEKLY_SUMMARY("Review my week", "Summarize the evidence and suggest one practical focus."),
    WEEKLY_PLAN("Plan next week", "Build a week from my existing workout templates."),
    SCHEDULE_CHANGE("Move a workout", "Suggest a safer date for today's workout."),
    WORKOUT_SHORTENING("Shorten today", "Make today's workout fit the time I have."),
    EXERCISE_SUBSTITUTION("Swap an exercise", "Replace one exercise using my existing library."),
    EXISTING_FOOD_MEAL("Suggest a meal", "Build one meal using foods already in my library."),
}

data class CoachingTemplateOption(
    val alias: String,
    val localId: String,
    val name: String,
)

data class CoachingWorkoutOption(
    val alias: String,
    val plannedWorkoutId: String?,
    val occurrenceId: String?,
    val title: String,
    val originalDate: LocalDate,
    val scheduledDate: LocalDate,
    val exerciseAliases: Map<String, String>,
    val exerciseNames: Map<String, String> = emptyMap(),
)

data class CoachingFoodOption(
    val alias: String,
    val localId: String,
    val name: String,
    val servingLabel: String,
)

data class CoachingContext(
    val generatedOn: LocalDate,
    val evidence: List<String>,
    val templates: List<CoachingTemplateOption>,
    val todayWorkout: CoachingWorkoutOption?,
    val replacementExercises: Map<String, Pair<String, String>>,
    val foods: List<CoachingFoodOption>,
    val currentSchedule: Map<String, String>,
)

data class CoachingToolCall(
    val name: String,
    val arguments: String,
)

data class CoachingProposal(
    val id: String,
    val intent: CoachingIntent,
    val title: String,
    val observed: String,
    val current: String,
    val proposed: String,
    val reason: String,
    val operation: CoachingProposalOperation,
    val originalRequest: String,
    val generatedAtUtcEpochMillis: Long,
    val model: String,
)

sealed interface CoachingProposalOperation {
    data object None : CoachingProposalOperation

    data class ReplaceWeeklySchedule(
        val assignments: List<ScheduleAssignment>,
    ) : CoachingProposalOperation

    data class AdjustTodayWorkout(
        val plannedWorkoutId: String?,
        val occurrenceId: String?,
        val originalDate: LocalDate,
        val type: TodayAdjustmentType,
        val targetDate: LocalDate? = null,
        val sourceExerciseId: String? = null,
        val replacementExerciseId: String? = null,
    ) : CoachingProposalOperation

    data class AddExistingFoodMeal(
        val date: LocalDate,
        val mealType: String,
        val items: List<FoodAmount>,
    ) : CoachingProposalOperation
}

data class ScheduleAssignment(
    val dayOfWeek: String,
    val templateId: String,
    val templateName: String,
)

enum class TodayAdjustmentType {
    SHORTENED,
    MINIMUM,
    RESCHEDULED,
    SUBSTITUTED,
}

data class FoodAmount(
    val foodId: String,
    val foodName: String,
    val servings: Double,
)

fun interface CoachingContextDataSource {
    suspend fun loadContext(): CoachingContext
}

interface CoachingProposalApplier {
    suspend fun apply(proposal: CoachingProposal): Result<Unit>
}

interface CoachingCommandGateway {
    suspend fun replaceWeeklySchedule(assignments: List<ScheduleAssignment>)
    suspend fun adjustTodayWorkout(operation: CoachingProposalOperation.AdjustTodayWorkout)
    suspend fun addExistingFoodMeal(operation: CoachingProposalOperation.AddExistingFoodMeal)
}
