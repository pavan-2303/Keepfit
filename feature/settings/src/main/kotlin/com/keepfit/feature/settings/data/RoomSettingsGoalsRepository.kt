package com.keepfit.feature.settings.data

import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.preferences.ActiveProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class RoomSettingsGoalsRepository(
    private val bodyProfileDao: BodyProfileDao,
    private val activeProfileStore: ActiveProfileStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : SettingsGoalsRepository {
    override fun observeNutritionGoals(): Flow<NutritionGoalSettings> =
        activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest(bodyProfileDao::observeProfile).map { profile ->
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
        val profileId = requireNotNull(activeProfileStore.observeActiveProfileId().first()) { "Profile missing." }
        val profile = requireNotNull(bodyProfileDao.findProfile(profileId)) { "Profile missing." }
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
