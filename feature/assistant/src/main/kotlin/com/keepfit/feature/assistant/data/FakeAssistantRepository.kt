package com.keepfit.feature.assistant.data

class FakeAssistantRepository : AssistantRepository {
    var connectionResult: Result<Unit> = Result.success(Unit)
    var chatResult: Result<AssistantChatMessage> = Result.success(
        AssistantChatMessage(
            id = "assistant-default",
            role = AssistantMessageRole.ASSISTANT,
            content = "Assistant is ready.",
            createdAtUtcEpochMillis = 0L,
        ),
    )

    var summaryResult: Result<AssistantProgressSummary> = Result.success(
        AssistantProgressSummary(
            title = "Progress summary",
            summary = "No summary yet.",
        ),
    )

    var draftPlanResult: Result<AssistantDraftWorkoutPlan> = Result.success(
        AssistantDraftWorkoutPlan(
            name = "Draft plan",
            days = emptyList(),
        ),
    )

    override suspend fun testConnection(config: AssistantRuntimeConfig): Result<Unit> =
        connectionResult

    override suspend fun sendChatTurn(
        config: AssistantRuntimeConfig,
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage> = chatResult

    override suspend fun generateProgressSummary(
        config: AssistantRuntimeConfig,
    ): Result<AssistantProgressSummary> = summaryResult

    override suspend fun requestDraftPlan(
        config: AssistantRuntimeConfig,
        input: AssistantDraftInput,
    ): Result<AssistantDraftWorkoutPlan> = draftPlanResult
}
