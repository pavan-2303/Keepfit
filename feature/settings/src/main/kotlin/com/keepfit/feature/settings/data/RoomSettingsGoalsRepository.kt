package com.keepfit.feature.settings.data

import com.keepfit.core.database.profile.BodyProfileDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSettingsGoalsRepository(
    private val bodyProfileDao: BodyProfileDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : SettingsGoalsRepository {
    override fun observeNutritionGoals(): Flow<NutritionGoalSettings> =
        bodyProfileDao.observeLocalProfile().map { profile ->
            NutritionGoalSettings(
                calorieGoal = profile?.dailyCalorieGoal,
                proteinGoalGrams = profile?.dailyProteinGoalGrams,
                carbohydrateGoalGrams = profile?.dailyCarbohydrateGoalGrams,
                fatGoalGrams = profile?.dailyFatGoalGrams,
            )
        }

    override suspend fun updateNutritionGoals(
        dailyCalorieGoal: Double?,
        dailyProteinGoalGrams: Double?,
        dailyCarbohydrateGoalGrams: Double?,
        dailyFatGoalGrams: Double?,
    ) {
        val profile = requireNotNull(bodyProfileDao.findLocalProfile()) { "Profile missing." }
        bodyProfileDao.updateNutritionGoals(
            profileId = profile.id,
            dailyCalorieGoal = dailyCalorieGoal,
            dailyProteinGoalGrams = dailyProteinGoalGrams,
            dailyCarbohydrateGoalGrams = dailyCarbohydrateGoalGrams,
            dailyFatGoalGrams = dailyFatGoalGrams,
            updatedAt = clock(),
        )
    }
}
