package com.keepfit.feature.workouts.planning

interface StarterPlanRepository {
    suspend fun loadPreferences(): StarterPlanInput?

    suspend fun savePreferences(input: StarterPlanInput)

    suspend fun apply(input: StarterPlanInput, draft: StarterWeekDraft)
}
