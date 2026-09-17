package com.keepfit.feature.workouts.today

import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.WorkoutTemplate
import java.time.LocalDate

class TodayWorkoutRules {
    fun createVariant(
        template: WorkoutTemplate,
        variant: TodayWorkoutVariant,
    ): TodayVariantResult {
        require(template.exercises.isNotEmpty()) { "This workout has no exercises." }
        val full = template.exercises.map { exercise ->
            TodayExercise(
                sourceTemplateExerciseId = exercise.id,
                exerciseId = exercise.exerciseId,
                exerciseName = exercise.exerciseName,
                targetSets = exercise.targetSets,
                targetReps = exercise.targetReps,
            )
        }
        return when (variant) {
            TodayWorkoutVariant.FULL -> TodayVariantResult(
                exercises = full,
                explanation = "Keep the full workout exactly as planned.",
            )

            TodayWorkoutVariant.SHORTENED -> {
                val exerciseCount = ((full.size * 2 + 2) / 3)
                    .coerceAtLeast(minOf(2, full.size))
                    .coerceAtMost(full.size)
                TodayVariantResult(
                    exercises = full.take(exerciseCount).map { exercise ->
                        exercise.copy(targetSets = reducedSetCount(exercise.targetSets))
                    },
                    explanation = "Keep the first $exerciseCount exercises and trim one set where possible.",
                )
            }

            TodayWorkoutVariant.MINIMUM -> {
                val exerciseCount = minOf(2, full.size)
                TodayVariantResult(
                    exercises = full.take(exerciseCount).map { exercise ->
                        exercise.copy(targetSets = minOf(2, exercise.targetSets))
                    },
                    explanation = "Do the first $exerciseCount exercises for up to two sets each.",
                )
            }
        }
    }

    private fun reducedSetCount(targetSets: Int): Int = (targetSets - 1)
        .coerceAtLeast(minOf(2, targetSets))
        .coerceAtMost(targetSets)
}

class TodayWorkoutResolver {
    fun resolve(
        today: LocalDate,
        activeWorkout: ActiveWorkout?,
        candidates: List<TodayWorkoutAction>,
        sessions: List<TodayWorkoutSession>,
    ): TodayWorkoutState {
        if (activeWorkout != null) {
            return TodayWorkoutState(
                status = TodayWorkoutStatus.RESUME,
                activeWorkout = activeWorkout,
            )
        }

        val effectiveCandidates = candidates.filterNot { candidate ->
            candidate.isRecurringPlanOverriddenBy(candidates)
        }
        val completedToday = effectiveCandidates.firstOrNull { candidate ->
            candidate.appliesTo(today) && sessions.any { session ->
                session.isCompleted && session.matches(candidate)
            }
        }
        if (completedToday != null) {
            return TodayWorkoutState(
                status = TodayWorkoutStatus.COMPLETED,
                primary = completedToday,
                missed = findMissed(today, effectiveCandidates, sessions),
            )
        }

        val todayCandidate = effectiveCandidates
            .filter { candidate ->
                candidate.appliesTo(today)
            }
            .sortedWith(
                compareByDescending<TodayWorkoutAction> { it.occurrenceId != null }
                    .thenByDescending { it.scheduledDate == today },
            )
            .firstOrNull()
        if (todayCandidate != null) {
            return TodayWorkoutState(
                status = todayCandidate.statusFor(today),
                primary = todayCandidate,
                missed = findMissed(today, effectiveCandidates, sessions),
            )
        }

        return TodayWorkoutState(
            status = TodayWorkoutStatus.REST_DAY,
            missed = findMissed(today, effectiveCandidates, sessions),
        )
    }

    private fun findMissed(
        today: LocalDate,
        candidates: List<TodayWorkoutAction>,
        sessions: List<TodayWorkoutSession>,
    ): TodayWorkoutAction? {
        val firstRecoverableDate = today.minusDays(7)
        return candidates
            .asSequence()
            .filter { it.originalDate in firstRecoverableDate..<today }
            .filter { it.decision != TodayWorkoutDecision.SKIPPED }
            .filter { candidate ->
                candidate.scheduledDate < today && sessions.none { session ->
                    session.isCompleted && session.matches(candidate)
                }
            }
            .maxByOrNull(TodayWorkoutAction::originalDate)
    }

    private fun TodayWorkoutSession.matches(candidate: TodayWorkoutAction): Boolean =
        (candidate.occurrenceId != null && occurrenceId == candidate.occurrenceId) ||
            (plannedWorkoutId != null &&
                plannedWorkoutId == candidate.plannedWorkoutId &&
                workoutDate == candidate.originalDate)

    private fun TodayWorkoutAction.appliesTo(date: LocalDate): Boolean =
        originalDate == date || scheduledDate == date

    private fun TodayWorkoutAction.isRecurringPlanOverriddenBy(
        candidates: List<TodayWorkoutAction>,
    ): Boolean = occurrenceId == null && candidates.any { occurrence ->
        occurrence.occurrenceId != null &&
            occurrence.plannedWorkoutId == plannedWorkoutId &&
            occurrence.originalDate == originalDate
    }

    private fun TodayWorkoutAction.statusFor(today: LocalDate): TodayWorkoutStatus = when {
        decision == TodayWorkoutDecision.SKIPPED -> TodayWorkoutStatus.SKIPPED
        scheduledDate != originalDate && scheduledDate != today -> {
            TodayWorkoutStatus.RESCHEDULED
        }
        else -> TodayWorkoutStatus.PLANNED
    }
}
