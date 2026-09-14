package com.keepfit.feature.assistant.coaching

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidatedCoachingProposalApplier @Inject constructor(
    private val gateway: CoachingCommandGateway,
) : CoachingProposalApplier {
    override suspend fun apply(proposal: CoachingProposal): Result<Unit> = runCatching {
        when (val operation = proposal.operation) {
            CoachingProposalOperation.None -> Unit
            is CoachingProposalOperation.ReplaceWeeklySchedule ->
                gateway.replaceWeeklySchedule(operation.assignments)
            is CoachingProposalOperation.AdjustTodayWorkout ->
                gateway.adjustTodayWorkout(operation)
            is CoachingProposalOperation.AddExistingFoodMeal ->
                gateway.addExistingFoodMeal(operation)
        }
    }
}
