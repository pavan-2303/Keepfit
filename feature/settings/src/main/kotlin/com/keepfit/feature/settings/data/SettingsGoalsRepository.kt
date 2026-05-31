package com.keepfit.feature.settings.data

import kotlinx.coroutines.flow.Flow

interface SettingsGoalsRepository {
    fun observeNutritionGoals(): Flow<NutritionGoalSettings>

    suspend fun updateNutritionGoals(
        dailyCalorieGoal: Double?,
        dailyProteinGoalGrams: Double?,
        dailyCarbohydrateGoalGrams: Double?,
        dailyFatGoalGrams: Double?,
    )
}
