package com.keepfit.feature.assistant.data

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
}
