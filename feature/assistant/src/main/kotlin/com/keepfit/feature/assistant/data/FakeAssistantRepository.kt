package com.keepfit.feature.assistant.data

class FakeAssistantRepository : AssistantRepository {
    var lastChatHistory: List<AssistantChatMessage>? = null
    var coachingResult: Result<com.keepfit.feature.assistant.coaching.CoachingProposal> =
        Result.failure(UnsupportedOperationException("No coaching fixture configured."))
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
    ): Result<AssistantChatMessage> {
        lastChatHistory = history
        return chatResult
    }

    override suspend fun generateProgressSummary(
        config: AssistantRuntimeConfig,
    ): Result<AssistantProgressSummary> = summaryResult

    override suspend fun requestDraftPlan(
        config: AssistantRuntimeConfig,
        input: AssistantDraftInput,
    ): Result<AssistantDraftWorkoutPlan> = draftPlanResult

    override suspend fun requestCoachingProposal(
        config: AssistantRuntimeConfig,
        intent: com.keepfit.feature.assistant.coaching.CoachingIntent,
        userRequest: String,
    ): Result<com.keepfit.feature.assistant.coaching.CoachingProposal> = coachingResult
}
