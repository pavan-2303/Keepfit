package com.keepfit.feature.assistant.data

interface AssistantPlanApplier {
    suspend fun applyDraftPlan(draft: AssistantDraftWorkoutPlan): Result<Unit>
}
