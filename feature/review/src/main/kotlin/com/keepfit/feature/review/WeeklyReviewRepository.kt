package com.keepfit.feature.review

import androidx.room.withTransaction
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.review.WeeklyReviewOutcomeEntity
import com.keepfit.core.database.workout.PlannedWorkoutRow
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.database.workout.WorkoutOccurrenceEntity
import com.keepfit.core.database.workout.WorkoutOccurrenceExerciseEntity
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateDetails
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.core.preferences.NutritionTrackingDepth
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class ReviewOutcomeStatus {
    APPROVED,
    DISMISSED,
}

data class ReviewOutcome(
    val status: ReviewOutcomeStatus,
    val draftType: ReviewDraftType?,
    val sourceDate: LocalDate?,
    val targetDate: LocalDate?,
)

sealed interface WeeklyReviewSnapshot {
    data object Paused : WeeklyReviewSnapshot
    data class Insufficient(val result: WeeklyReviewResult) : WeeklyReviewSnapshot
    data class Ready(val result: WeeklyReviewResult) : WeeklyReviewSnapshot
    data class Decided(val result: WeeklyReviewResult, val outcome: ReviewOutcome) : WeeklyReviewSnapshot
}

interface WeeklyActivityProvider {
    suspend fun loadSteps(window: WeeklyReviewWindow): ReviewStepsSignal?
}

interface WeeklyReviewRepository {
    suspend fun load(): WeeklyReviewSnapshot
    suspend fun approve(draft: ReviewDraft)
    suspend fun dismiss()
    suspend fun setPaused(paused: Boolean)
}

class RoomWeeklyReviewRepository @Inject constructor(
    private val database: KeepfitDatabase,
    private val workoutDao: WorkoutDao,
    private val nutritionDao: NutritionDao,
    private val settingsRepository: AppSettingsRepository,
    private val activeProfileStore: ActiveProfileStore,
    private val activityProvider: WeeklyActivityProvider,
    private val rules: WeeklyReviewRules = WeeklyReviewRules(),
    private val today: () -> LocalDate = LocalDate::now,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : WeeklyReviewRepository {
    override suspend fun load(): WeeklyReviewSnapshot {
        val profileId = requireProfileId()
        val settings = settingsRepository.observeSettings().first()
        if (settings.weeklyReviewPaused) {
            return WeeklyReviewSnapshot.Paused
        }
        val window = rules.windowFor(today())
        val schedule = workoutDao.observeWeeklyScheduleForProfile(profileId).first()
        val occurrences = workoutDao.observeOccurrenceDetailsForProfile(profileId, window.reviewStart, window.reviewEnd).first()
        val sessions = workoutDao.observeSessionsBetweenForProfile(profileId, window.comparisonStart.minusDays(14), window.reviewEnd).first()
        val completed = sessions.filter { it.completedAt != null }
        val workoutDays = resolveWorkoutDays(window, schedule, occurrences, completed)
        val nutrition = nutritionSignal(profileId, window, settings.nutritionTrackingDepth)
        val currentPerformance = workoutDao.findExercisePerformanceBetweenForProfile(profileId, window.reviewStart, window.reviewEnd)
            .associateBy { it.exerciseId }
        val comparisonPerformance = workoutDao.findExercisePerformanceBetweenForProfile(profileId, window.comparisonStart, window.comparisonEnd)
            .associateBy { it.exerciseId }
        val improved = currentPerformance.any { (exerciseId, current) ->
            comparisonPerformance[exerciseId]?.let { prior ->
                current.highestWeightKg > prior.highestWeightKg ||
                    current.highestRepetitions > prior.highestRepetitions
            } == true
        }
        val evaluated = rules.evaluate(
            WeeklyReviewInput(
                window = window,
                workoutDays = workoutDays,
                comparisonCompletions = completed.count { it.workoutDate in window.comparisonStart..window.comparisonEnd },
                olderCompletions = completed.count {
                    it.workoutDate in window.comparisonStart.minusDays(14)..window.comparisonStart.minusDays(1)
                },
                performanceImproved = improved,
                nutrition = nutrition,
                steps = runCatching { activityProvider.loadSteps(window) }.getOrNull(),
            ),
        )
        val result = evaluated.copy(
            drafts = evaluated.drafts.filterNot { draft ->
                draft.type == ReviewDraftType.ADD_ONE_SET && firstExerciseIsAtSetCap(draft)
            },
        )
        val outcome = database.weeklyReviewDao().findForProfileAndWeek(profileId, window.reviewStart)
        return when {
            outcome != null -> WeeklyReviewSnapshot.Decided(result, outcome.toModel())
            !result.hasEnoughData -> WeeklyReviewSnapshot.Insufficient(result)
            else -> WeeklyReviewSnapshot.Ready(result)
        }
    }

    override suspend fun approve(draft: ReviewDraft) {
        val profileId = requireProfileId()
        val window = rules.windowFor(today())
        require(draft.sourceDate in window.nextWeekStart..window.nextWeekEnd) { "Choose a workout in the coming week." }
        require(draft.targetDate in window.nextWeekStart..window.nextWeekEnd) { "Choose a date in the coming week." }
        database.withTransaction {
            database.weeklyReviewDao().findForProfileAndWeek(profileId, window.reviewStart)?.let { return@withTransaction }
            val planned = requireNotNull(workoutDao.findPlannedWorkoutForProfile(draft.sourcePlannedWorkoutId, profileId)) {
                "That planned workout is no longer available."
            }
            require(planned.dayOfWeek == draft.sourceDate.dayOfWeek) { "The selected workout does not match that date." }
            if (draft.type != ReviewDraftType.MOVE_SESSION) {
                require(draft.sourceDate == draft.targetDate) { "Only a moved session can use another date." }
            } else {
                require(draft.sourceDate != draft.targetDate) { "Choose a different day for the moved session." }
                val schedule = workoutDao.observeWeeklyScheduleForProfile(profileId).first()
                require(schedule.none { it.dayOfWeek == draft.targetDate.dayOfWeek }) {
                    "That day already has a planned workout."
                }
            }
            require(workoutDao.findOccurrenceForSourceAndProfile(planned.id, draft.sourceDate, profileId) == null) {
                "That workout already has a one-week adjustment."
            }
            val template = requireNotNull(workoutDao.findTemplateDetailsForProfile(planned.workoutTemplateId, profileId)) {
                "That workout template is no longer available."
            }
            if (draft.type == ReviewDraftType.ADD_ONE_SET) {
                require(
                    template.exercises.minByOrNull { it.templateExercise.position }
                        ?.templateExercise
                        ?.targetSets
                        ?.let { it < 6 } == true,
                ) { "The first exercise is already at the six-set review limit." }
            }
            val occurrenceId = idFactory()
            val timestamp = clock()
            val exercises = adjustedExercises(draft.type, template, occurrenceId)
            require(exercises.isNotEmpty()) { "That workout has no exercises to adjust." }
            workoutDao.replaceOccurrence(
                occurrence = WorkoutOccurrenceEntity(
                    id = occurrenceId,
                    sourcePlannedWorkoutId = planned.id,
                    sourceTemplateId = planned.workoutTemplateId,
                    templateNameSnapshot = planned.templateName,
                    originalDate = draft.sourceDate,
                    scheduledDate = draft.targetDate,
                    decisionType = when (draft.type) {
                        ReviewDraftType.MINIMUM_SESSION -> "MINIMUM"
                        ReviewDraftType.SHORTEN_SESSION -> "SHORTENED"
                        ReviewDraftType.MOVE_SESSION, ReviewDraftType.ADD_ONE_SET -> "FULL"
                    },
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    bodyProfileId = profileId,
                ),
                exercises = exercises,
            )
            database.weeklyReviewDao().upsert(
                WeeklyReviewOutcomeEntity(
                    id = idFactory(),
                    weekStart = window.reviewStart,
                    status = ReviewOutcomeStatus.APPROVED.name,
                    draftType = draft.type.name,
                    sourcePlannedWorkoutId = planned.id,
                    sourceDate = draft.sourceDate,
                    targetDate = draft.targetDate,
                    occurrenceId = occurrenceId,
                    decidedAt = timestamp,
                    bodyProfileId = profileId,
                ),
            )
        }
    }

    override suspend fun dismiss() {
        val profileId = requireProfileId()
        val window = rules.windowFor(today())
        database.withTransaction {
            if (database.weeklyReviewDao().findForProfileAndWeek(profileId, window.reviewStart) != null) return@withTransaction
            database.weeklyReviewDao().upsert(
                WeeklyReviewOutcomeEntity(
                    id = idFactory(),
                    weekStart = window.reviewStart,
                    status = ReviewOutcomeStatus.DISMISSED.name,
                    draftType = null,
                    sourcePlannedWorkoutId = null,
                    sourceDate = null,
                    targetDate = null,
                    occurrenceId = null,
                    decidedAt = clock(),
                    bodyProfileId = profileId,
                ),
            )
        }
    }

    override suspend fun setPaused(paused: Boolean) = settingsRepository.updateWeeklyReviewPaused(paused)

    private suspend fun nutritionSignal(
        profileId: String,
        window: WeeklyReviewWindow,
        depth: NutritionTrackingDepth,
    ): ReviewNutritionSignal? {
        if (depth == NutritionTrackingDepth.DISABLED) return null
        if (depth == NutritionTrackingDepth.MEAL_QUALITY) {
            val current = nutritionDao.getMealQualityCheckInsForProfile(profileId, window.reviewStart, window.reviewEnd)
            val prior = nutritionDao.getMealQualityCheckInsForProfile(profileId, window.comparisonStart, window.comparisonEnd)
            if (current.isEmpty() && prior.isEmpty()) return null
            val loggedDays = current.map { it.diaryDate }.distinct().size
            val balanced = current.count { it.quality == com.keepfit.core.database.nutrition.MealQuality.BALANCED }
            return ReviewNutritionSignal(
                loggedDays = loggedDays,
                averageProteinGrams = 0.0,
                comparisonLoggedDays = prior.map { it.diaryDate }.distinct().size,
                comparisonAverageProteinGrams = 0.0,
                summary = "Meal check-ins were made on $loggedDays of 7 days, including $balanced balanced choices.",
            )
        }
        val totals = nutritionDao.observeAllDailyTotalsForProfile(profileId).first()
        val current = totals.filter { it.diaryDate in window.reviewStart..window.reviewEnd }
        val prior = totals.filter { it.diaryDate in window.comparisonStart..window.comparisonEnd }
        if (current.isEmpty() && prior.isEmpty()) return null
        return ReviewNutritionSignal(
            loggedDays = current.size,
            averageProteinGrams = current.map { it.proteinGrams }.averageOrZero(),
            comparisonLoggedDays = prior.size,
            comparisonAverageProteinGrams = prior.map { it.proteinGrams }.averageOrZero(),
        )
    }

    private fun resolveWorkoutDays(
        window: WeeklyReviewWindow,
        schedule: List<PlannedWorkoutRow>,
        occurrences: List<com.keepfit.core.database.workout.WorkoutOccurrenceDetails>,
        sessions: List<WorkoutSessionEntity>,
    ): List<ReviewWorkoutDay> = (0L..6L).flatMap { offset ->
        val date = window.reviewStart.plusDays(offset)
        schedule.filter { it.dayOfWeek == date.dayOfWeek && !date.isBefore(it.planStartsOn) }.map { planned ->
            val occurrence = occurrences.firstOrNull {
                it.occurrence.sourcePlannedWorkoutId == planned.id && it.occurrence.originalDate == date
            }
            val completion = sessions.firstOrNull { session ->
                session.completedAt != null && if (occurrence != null) {
                    session.workoutOccurrenceId == occurrence.occurrence.id
                } else {
                    session.plannedWorkoutId == planned.id && session.workoutDate == date
                }
            }
            ReviewWorkoutDay(
                date = date,
                plannedWorkoutId = planned.id,
                templateId = planned.workoutTemplateId,
                title = occurrence?.occurrence?.templateNameSnapshot ?: planned.templateName,
                planned = true,
                completed = completion != null,
                energyLevel = completion?.energyLevel,
                difficulty = completion?.difficulty,
                movedTo = occurrence?.occurrence?.scheduledDate?.takeIf { it != date },
                skipped = occurrence?.occurrence?.decisionType == "SKIPPED",
            )
        }
    }

    private fun adjustedExercises(
        type: ReviewDraftType,
        template: WorkoutTemplateDetails,
        occurrenceId: String,
    ): List<WorkoutOccurrenceExerciseEntity> {
        val original = template.exercises.sortedBy { it.templateExercise.position }
        val selected = when (type) {
            ReviewDraftType.MINIMUM_SESSION -> original.take(2)
            ReviewDraftType.SHORTEN_SESSION -> original.take(((original.size * 2 + 2) / 3).coerceAtLeast(minOf(2, original.size)))
            ReviewDraftType.MOVE_SESSION, ReviewDraftType.ADD_ONE_SET -> original
        }
        return selected.mapIndexed { index, item ->
            val targetSets = when (type) {
                ReviewDraftType.MINIMUM_SESSION -> minOf(2, item.templateExercise.targetSets)
                ReviewDraftType.SHORTEN_SESSION -> (item.templateExercise.targetSets - 1)
                    .coerceAtLeast(minOf(2, item.templateExercise.targetSets))
                ReviewDraftType.ADD_ONE_SET -> if (index == 0) {
                    (item.templateExercise.targetSets + 1).coerceAtMost(6)
                } else item.templateExercise.targetSets
                ReviewDraftType.MOVE_SESSION -> item.templateExercise.targetSets
            }
            WorkoutOccurrenceExerciseEntity(
                id = idFactory(),
                workoutOccurrenceId = occurrenceId,
                sourceTemplateExerciseId = item.templateExercise.id,
                exerciseId = item.exercise.id,
                exerciseNameSnapshot = item.exercise.name,
                position = index,
                targetSets = targetSets,
                targetReps = item.templateExercise.targetReps,
            )
        }
    }

    private suspend fun firstExerciseIsAtSetCap(draft: ReviewDraft): Boolean {
        val profileId = requireProfileId()
        val planned = workoutDao.findPlannedWorkoutForProfile(draft.sourcePlannedWorkoutId, profileId) ?: return true
        val firstExercise = workoutDao.findTemplateDetailsForProfile(planned.workoutTemplateId, profileId)
            ?.exercises
            ?.minByOrNull { it.templateExercise.position }
            ?: return true
        return firstExercise.templateExercise.targetSets >= 6
    }

    private suspend fun requireProfileId(): String =
        requireNotNull(activeProfileStore.observeActiveProfileId().first()) { "Profile missing." }
}

private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

private fun WeeklyReviewOutcomeEntity.toModel() = ReviewOutcome(
    status = ReviewOutcomeStatus.valueOf(status),
    draftType = draftType?.let(ReviewDraftType::valueOf),
    sourceDate = sourceDate,
    targetDate = targetDate,
)
