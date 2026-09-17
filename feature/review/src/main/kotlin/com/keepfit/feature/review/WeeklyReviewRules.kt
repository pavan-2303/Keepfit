package com.keepfit.feature.review

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class WeeklyReviewWindow(
    val comparisonStart: LocalDate,
    val comparisonEnd: LocalDate,
    val reviewStart: LocalDate,
    val reviewEnd: LocalDate,
    val nextWeekStart: LocalDate,
    val nextWeekEnd: LocalDate,
)

data class ReviewWorkoutDay(
    val date: LocalDate,
    val plannedWorkoutId: String?,
    val templateId: String?,
    val title: String,
    val planned: Boolean,
    val completed: Boolean,
    val energyLevel: Int? = null,
    val difficulty: Int? = null,
    val movedTo: LocalDate? = null,
    val skipped: Boolean = false,
)

data class ReviewNutritionSignal(
    val loggedDays: Int,
    val averageProteinGrams: Double,
    val comparisonLoggedDays: Int,
    val comparisonAverageProteinGrams: Double,
    val summary: String? = null,
)

data class ReviewStepsSignal(
    val averageSteps: Long,
    val comparisonAverageSteps: Long,
)

data class WeeklyReviewInput(
    val window: WeeklyReviewWindow,
    val workoutDays: List<ReviewWorkoutDay>,
    val comparisonCompletions: Int,
    val olderCompletions: Int,
    val performanceImproved: Boolean,
    val nutrition: ReviewNutritionSignal? = null,
    val steps: ReviewStepsSignal? = null,
)

enum class ReviewAchievementKind {
    COMPLETED_PLAN,
    PARTIAL_CONSISTENCY,
    RETURNED,
    IMPROVED,
}

data class ReviewAchievement(
    val kind: ReviewAchievementKind,
    val text: String,
)

enum class ReviewDraftType {
    MINIMUM_SESSION,
    SHORTEN_SESSION,
    MOVE_SESSION,
    ADD_ONE_SET,
}

data class ReviewDraft(
    val type: ReviewDraftType,
    val title: String,
    val current: String,
    val proposed: String,
    val reason: String,
    val sourcePlannedWorkoutId: String,
    val sourceDate: LocalDate,
    val targetDate: LocalDate,
)

data class WeeklyReviewResult(
    val window: WeeklyReviewWindow,
    val workoutDays: List<ReviewWorkoutDay>,
    val plannedCount: Int,
    val completedCount: Int,
    val achievements: List<ReviewAchievement>,
    val supportingSignals: List<String>,
    val motivation: String,
    val drafts: List<ReviewDraft>,
    val hasEnoughData: Boolean,
)

class WeeklyReviewRules {
    fun windowFor(today: LocalDate): WeeklyReviewWindow {
        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val reviewEnd = currentMonday.minusDays(1)
        val reviewStart = reviewEnd.minusDays(6)
        return WeeklyReviewWindow(
            comparisonStart = reviewStart.minusDays(7),
            comparisonEnd = reviewStart.minusDays(1),
            reviewStart = reviewStart,
            reviewEnd = reviewEnd,
            nextWeekStart = currentMonday.plusWeeks(1),
            nextWeekEnd = currentMonday.plusWeeks(1).plusDays(6),
        )
    }

    fun evaluate(input: WeeklyReviewInput): WeeklyReviewResult {
        val plannedDays = input.workoutDays.filter(ReviewWorkoutDay::planned)
        val plannedCount = plannedDays.size
        val completedCount = plannedDays.count(ReviewWorkoutDay::completed)
        val hasEnoughData = plannedCount > 0
        val completionRate = if (plannedCount == 0) 0.0 else completedCount.toDouble() / plannedCount
        val achievements = achievements(input, plannedCount, completedCount)
        val signals = buildList {
            input.nutrition?.let { nutrition ->
                add(nutrition.summary ?: numericNutritionSummary(nutrition))
            }
            input.steps?.let { steps ->
                val direction = when {
                    steps.averageSteps > steps.comparisonAverageSteps -> "up"
                    steps.averageSteps < steps.comparisonAverageSteps -> "down"
                    else -> "steady"
                }
                add("Steps averaged ${"%,d".format(steps.averageSteps)} per day, $direction from the prior week.")
            }
        }
        return WeeklyReviewResult(
            window = input.window,
            workoutDays = input.workoutDays,
            plannedCount = plannedCount,
            completedCount = completedCount,
            achievements = achievements,
            supportingSignals = signals,
            motivation = motivation(plannedCount, completedCount, achievements),
            drafts = if (hasEnoughData) drafts(input, completionRate) else emptyList(),
            hasEnoughData = hasEnoughData,
        )
    }

    private fun numericNutritionSummary(nutrition: ReviewNutritionSignal): String =
        "Nutrition was logged on ${nutrition.loggedDays} of 7 days" +
            if (nutrition.loggedDays > 0) {
                ", averaging ${nutrition.averageProteinGrams.roundToInt()} g protein."
            } else {
                "."
            }

    private fun achievements(
        input: WeeklyReviewInput,
        plannedCount: Int,
        completedCount: Int,
    ): List<ReviewAchievement> = buildList {
        if (plannedCount > 0 && completedCount == plannedCount) {
            add(ReviewAchievement(ReviewAchievementKind.COMPLETED_PLAN, "You completed all $plannedCount planned workouts."))
        } else if (completedCount > 0) {
            add(ReviewAchievement(ReviewAchievementKind.PARTIAL_CONSISTENCY, "You completed $completedCount of $plannedCount planned workouts."))
        }
        if (completedCount > 0 && input.comparisonCompletions == 0 && input.olderCompletions > 0) {
            add(ReviewAchievement(ReviewAchievementKind.RETURNED, "You returned after a week away. That return counts."))
        }
        if (input.performanceImproved) {
            add(ReviewAchievement(ReviewAchievementKind.IMPROVED, "At least one exercise moved forward in weight or repetitions."))
        }
    }

    private fun motivation(
        plannedCount: Int,
        completedCount: Int,
        achievements: List<ReviewAchievement>,
    ): String = when {
        achievements.any { it.kind == ReviewAchievementKind.RETURNED } ->
            "Coming back is useful progress. Keep next week realistic."
        completedCount == plannedCount && plannedCount > 0 ->
            "Your plan matched real life last week. Build only if the next step still feels manageable."
        completedCount > 0 ->
            "$completedCount completed ${if (completedCount == 1) "session" else "sessions"} kept the week moving. A smaller plan can make the next week easier to sustain."
        else -> "The records show an interruption. Adjust the week to fit what is possible now."
    }

    private fun drafts(input: WeeklyReviewInput, completionRate: Double): List<ReviewDraft> {
        val planned = input.workoutDays.filter(ReviewWorkoutDay::planned).sortedBy(ReviewWorkoutDay::date)
        val feedback = planned.filter(ReviewWorkoutDay::completed)
        val lowEnergy = feedback.mapNotNull(ReviewWorkoutDay::energyLevel).averageOrNull()?.let { it <= 2.5 } == true
        val highDifficulty = feedback.mapNotNull(ReviewWorkoutDay::difficulty).averageOrNull()?.let { it >= 4.0 } == true
        val missed = planned.firstOrNull { !it.completed }
        val drafts = mutableListOf<ReviewDraft>()

        if (completionRate <= 0.5 || lowEnergy || highDifficulty) {
            planned.firstOrNull()?.toDraft(
                input.window,
                ReviewDraftType.MINIMUM_SESSION,
                "Make one session a minimum",
                "Full session",
                "First two exercises, up to two sets each",
                "Last week suggests that protecting a small, finishable session would be more useful than adding work.",
            )?.let(drafts::add)
        } else if (completionRate < 0.8) {
            missed?.toDraft(
                input.window,
                ReviewDraftType.SHORTEN_SESSION,
                "Shorten one session",
                "Full session",
                "About two-thirds of the exercises, with one fewer set where possible",
                "You completed part of the plan. A shorter version keeps the same training day with less friction.",
            )?.let(drafts::add)
        }

        if (missed != null) {
            moveDraft(missed, planned, input.window)?.let(drafts::add)
        }

        if (completionRate >= 0.8 && input.performanceImproved && !lowEnergy && !highDifficulty) {
            planned.firstOrNull()?.toDraft(
                input.window,
                ReviewDraftType.ADD_ONE_SET,
                "Add one set to one exercise",
                "Current session volume",
                "One extra set on the first exercise, capped at six",
                "Most of the plan was completed and at least one exercise improved, so a single-set increase is a bounded next step.",
            )?.let(drafts::add)
        }

        return drafts.distinctBy(ReviewDraft::type).take(2)
    }

    private fun ReviewWorkoutDay.toDraft(
        window: WeeklyReviewWindow,
        type: ReviewDraftType,
        draftTitle: String,
        current: String,
        proposed: String,
        reason: String,
    ): ReviewDraft? {
        val plannedId = plannedWorkoutId ?: return null
        val nextDate = window.nextWeekStart.plusDays((date.dayOfWeek.value - 1).toLong())
        return ReviewDraft(
            type = type,
            title = draftTitle,
            current = "$title on ${nextDate.dayOfWeek.displayName()}: $current",
            proposed = proposed,
            reason = reason,
            sourcePlannedWorkoutId = plannedId,
            sourceDate = nextDate,
            targetDate = nextDate,
        )
    }

    private fun moveDraft(
        missed: ReviewWorkoutDay,
        planned: List<ReviewWorkoutDay>,
        window: WeeklyReviewWindow,
    ): ReviewDraft? {
        val plannedId = missed.plannedWorkoutId ?: return null
        val sourceDate = window.nextWeekStart.plusDays((missed.date.dayOfWeek.value - 1).toLong())
        val occupied = planned.map { it.date.dayOfWeek }.toSet()
        val target = (1L..6L)
            .map { sourceDate.plusDays(it) }
            .firstOrNull { it <= window.nextWeekEnd && it.dayOfWeek !in occupied }
            ?: (1L..6L)
                .map { window.nextWeekStart.plusDays(it - 1) }
                .firstOrNull { it.dayOfWeek !in occupied && it != sourceDate }
            ?: return null
        return ReviewDraft(
            type = ReviewDraftType.MOVE_SESSION,
            title = "Move one session",
            current = "${missed.title} on ${sourceDate.dayOfWeek.displayName()}",
            proposed = "Move it to ${target.dayOfWeek.displayName()} for this week only",
            reason = "That training day was interrupted last week, and ${target.dayOfWeek.displayName()} is currently free in the coming week.",
            sourcePlannedWorkoutId = plannedId,
            sourceDate = sourceDate,
            targetDate = target,
        )
    }
}

private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::uppercase)
