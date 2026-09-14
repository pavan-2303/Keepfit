package com.keepfit.feature.assistant.data

import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal

interface AssistantRepository {
    suspend fun testConnection(config: AssistantRuntimeConfig): Result<Unit>

    suspend fun sendChatTurn(
        config: AssistantRuntimeConfig,
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage>

    suspend fun generateProgressSummary(
        config: AssistantRuntimeConfig,
    ): Result<AssistantProgressSummary>

    suspend fun requestDraftPlan(
        config: AssistantRuntimeConfig,
        input: AssistantDraftInput,
    ): Result<AssistantDraftWorkoutPlan>

    suspend fun requestCoachingProposal(
        config: AssistantRuntimeConfig,
        intent: CoachingIntent,
        userRequest: String,
    ): Result<CoachingProposal> = Result.failure(UnsupportedOperationException("Structured coaching is unavailable."))
}
