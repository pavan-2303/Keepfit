package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.coaching.CoachingCommandGateway
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import com.keepfit.feature.assistant.coaching.FoodAmount
import com.keepfit.feature.assistant.coaching.ScheduleAssignment
import com.keepfit.feature.assistant.coaching.ValidatedCoachingProposalApplier
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatedCoachingProposalApplierTest {
    @Test
    fun previewDoesNotWriteAndApprovalInvokesOnlyReviewedCommand() = runBlocking {
        val gateway = RecordingGateway()
        val applier = ValidatedCoachingProposalApplier(gateway)
        val proposal = proposal(
            CoachingProposalOperation.ReplaceWeeklySchedule(
                listOf(ScheduleAssignment("MONDAY", "template-id", "Upper")),
            ),
        )

        assertEquals(0, gateway.writeCount)
        applier.apply(proposal).getOrThrow()

        assertEquals(1, gateway.writeCount)
        assertEquals("template-id", gateway.schedule.single().templateId)
    }

    @Test
    fun readOnlySummaryApprovalPerformsNoDomainWrite() = runBlocking {
        val gateway = RecordingGateway()
        val applier = ValidatedCoachingProposalApplier(gateway)

        applier.apply(proposal(CoachingProposalOperation.None)).getOrThrow()

        assertEquals(0, gateway.writeCount)
    }

    private fun proposal(operation: CoachingProposalOperation) = CoachingProposal(
        id = "proposal-1",
        intent = CoachingIntent.WEEKLY_PLAN,
        title = "Plan",
        observed = "Evidence",
        current = "Current",
        proposed = "Proposed",
        reason = "Reason",
        operation = operation,
        originalRequest = "Plan my week",
        generatedAtUtcEpochMillis = 1L,
        model = "model",
    )

    private class RecordingGateway : CoachingCommandGateway {
        var writeCount = 0
        var schedule = emptyList<ScheduleAssignment>()
        override suspend fun replaceWeeklySchedule(assignments: List<ScheduleAssignment>) {
            writeCount++
            schedule = assignments
        }
        override suspend fun adjustTodayWorkout(operation: CoachingProposalOperation.AdjustTodayWorkout) {
            writeCount++
        }
        override suspend fun addExistingFoodMeal(operation: CoachingProposalOperation.AddExistingFoodMeal) {
            writeCount++
        }
    }
}
