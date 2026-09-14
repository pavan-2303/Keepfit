package com.keepfit.feature.nutrition.data

import com.keepfit.core.database.nutrition.DailyNutritionTotalsRow
import com.keepfit.core.database.nutrition.FoodDiaryEntryDetails
import com.keepfit.core.database.nutrition.FoodDiaryEntryEntity
import com.keepfit.core.database.nutrition.FoodEntity
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.database.nutrition.MealQualityCheckInEntity
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.nutrition.SavedMealDetails
import com.keepfit.core.database.nutrition.SavedMealEntity
import com.keepfit.core.database.nutrition.SavedMealItemEntity
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.feature.nutrition.FoodInput
import com.keepfit.feature.nutrition.SavedMealInput
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomNutritionRepository(
    private val dao: NutritionDao,
    private val bodyProfileDao: BodyProfileDao,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) : NutritionRepository {
    override fun observeFoods(query: String): Flow<List<Food>> =
        dao.observeFoods(query).map { foods -> foods.map(FoodEntity::toModel) }

    override fun observeFavoriteFoods(): Flow<List<Food>> =
        dao.observeFavoriteFoods().map { foods -> foods.map(FoodEntity::toModel) }

    override fun observeRecentFoods(limit: Int): Flow<List<Food>> =
        dao.observeRecentFoods(limit).map { rows -> rows.map { it.food.toModel() } }

    override fun observeSavedMeals(): Flow<List<SavedMeal>> =
        dao.observeSavedMealDetails().map { meals ->
            meals.map(SavedMealDetails::toModel)
        }

    override fun observeDiaryEntries(date: LocalDate): Flow<List<DiaryEntry>> =
        dao.observeDiaryEntries(date).map { rows -> rows.map(FoodDiaryEntryDetails::toModel) }

    override fun observeDailySummary(date: LocalDate): Flow<DailyNutritionSummary> =
        combine(
            dao.observeDailyTotals(date),
            dao.observeDiaryEntries(date),
            bodyProfileDao.observeLocalProfile(),
        ) { totals, entries, profile ->
            DailyNutritionSummary(
                date = date,
                totals = totals.toModel(),
                goals = profile?.let {
                    NutritionGoals(
                        calorieGoal = it.dailyCalorieGoal,
                        proteinGoalGrams = it.dailyProteinGoalGrams,
                        carbohydrateGoalGrams = it.dailyCarbohydrateGoalGrams,
                        fatGoalGrams = it.dailyFatGoalGrams,
                    )
                },
                hasEntries = entries.isNotEmpty(),
            )
        }

    override fun observeMealQualityCheckIns(date: LocalDate): Flow<List<MealQualityCheckIn>> =
        dao.observeMealQualityCheckIns(date).map { rows -> rows.map(MealQualityCheckInEntity::toModel) }

    override fun observePreviousMealTypes(date: LocalDate): Flow<List<MealType>> =
        dao.observeLoggedMealTypes(date.minusDays(1))

    override suspend fun saveFood(id: String?, input: FoodInput) {
        val existing = id?.let { dao.findFood(it) }
        val now = clock()
        dao.upsertFood(
            FoodEntity(
                id = existing?.id ?: id ?: idFactory(),
                name = input.name,
                servingLabel = input.servingLabel,
                servingAmount = input.servingAmount,
                calories = input.calories,
                proteinGrams = input.proteinGrams,
                carbohydrateGrams = input.carbohydrateGrams,
                fatGrams = input.fatGrams,
                isFavorite = existing?.isFavorite ?: false,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                archivedAt = existing?.archivedAt,
            ),
        )
    }

    override suspend fun toggleFavorite(foodId: String, isFavorite: Boolean) {
        dao.updateFavorite(foodId, isFavorite, clock())
    }

    override suspend fun archiveFood(foodId: String) {
        dao.archiveFood(foodId, clock())
    }

    override suspend fun createSavedMeal(input: SavedMealInput) {
        val mealId = idFactory()
        val now = clock()
        dao.upsertSavedMeal(
            SavedMealEntity(
                id = mealId,
                name = input.name,
                createdAt = now,
                updatedAt = now,
            ),
        )
        dao.replaceSavedMealItems(
            mealId = mealId,
            items = input.items.mapIndexed { index, item ->
                SavedMealItemEntity(
                    id = idFactory(),
                    savedMealId = mealId,
                    foodId = item.foodId,
                    servings = item.servings,
                    position = index,
                )
            },
        )
    }

    override suspend fun addFoodToDiary(
        date: LocalDate,
        mealType: MealType,
        foodId: String,
        servings: Double,
    ) {
        dao.insertDiaryEntry(
            FoodDiaryEntryEntity(
                id = idFactory(),
                diaryDate = date,
                mealType = mealType,
                foodId = foodId,
                savedMealId = null,
                servings = servings,
                loggedAt = clock(),
            ),
        )
    }

    override suspend fun addFoodsToDiary(
        date: LocalDate,
        mealType: MealType,
        items: List<FoodDiaryAddition>,
    ) {
        require(items.isNotEmpty()) { "Choose at least one food." }
        require(items.size <= 6 && items.map { it.foodId }.distinct().size == items.size)
        items.forEach { item ->
            require(item.servings.isFinite() && item.servings in 0.25..5.0)
            requireNotNull(dao.findFood(item.foodId)) { "Food not found." }
        }
        val now = clock()
        dao.insertDiaryEntries(
            items.map { item ->
                FoodDiaryEntryEntity(
                    id = idFactory(),
                    diaryDate = date,
                    mealType = mealType,
                    foodId = item.foodId,
                    savedMealId = null,
                    servings = item.servings,
                    loggedAt = now,
                )
            },
        )
    }

    override suspend fun addSavedMealToDiary(
        date: LocalDate,
        mealType: MealType,
        savedMealId: String,
        multiplier: Double,
    ) {
        val meal = requireNotNull(dao.findSavedMealDetails(savedMealId)) { "Saved meal not found." }
        val now = clock()
        dao.insertDiaryEntries(
            meal.items
                .sortedBy { it.item.position }
                .map { item ->
                    FoodDiaryEntryEntity(
                        id = idFactory(),
                        diaryDate = date,
                        mealType = mealType,
                        foodId = item.item.foodId,
                        savedMealId = savedMealId,
                        servings = item.item.servings * multiplier,
                        loggedAt = now,
                    )
                },
        )
    }

    override suspend fun deleteDiaryEntry(entryId: String) {
        dao.deleteDiaryEntry(entryId)
    }

    override suspend fun duplicatePreviousDay(targetDate: LocalDate) {
        dao.duplicateDiaryEntries(
            sourceDate = targetDate.minusDays(1),
            targetDate = targetDate,
            loggedAt = clock(),
        )
    }

    override suspend fun repeatPreviousMeal(targetDate: LocalDate, mealType: MealType) {
        dao.repeatDiaryMeal(
            sourceDate = targetDate.minusDays(1),
            targetDate = targetDate,
            mealType = mealType,
            loggedAt = clock(),
        )
    }

    override suspend fun setMealQuality(
        date: LocalDate,
        mealType: MealType,
        quality: MealQuality,
    ) {
        dao.upsertMealQualityCheckIn(
            MealQualityCheckInEntity(
                id = idFactory(),
                diaryDate = date,
                mealType = mealType,
                quality = quality,
                loggedAt = clock(),
            ),
        )
    }
}

private fun FoodEntity.toModel() = Food(
    id = id,
    name = name,
    servingLabel = servingLabel,
    servingAmount = servingAmount,
    calories = calories,
    proteinGrams = proteinGrams,
    carbohydrateGrams = carbohydrateGrams,
    fatGrams = fatGrams,
    isFavorite = isFavorite,
)

private fun SavedMealDetails.toModel() = SavedMeal(
    id = meal.id,
    name = meal.name,
    items = items
        .sortedBy { it.item.position }
        .map {
            SavedMealItem(
                id = it.item.id,
                foodId = it.item.foodId,
                foodName = it.foodName,
                servings = it.item.servings,
            )
        },
)

private fun FoodDiaryEntryDetails.toModel() = DiaryEntry(
    id = entry.id,
    diaryDate = entry.diaryDate,
    mealType = entry.mealType,
    foodId = entry.foodId,
    foodName = food.name,
    servingLabel = food.servingLabel,
    servings = entry.servings,
    savedMealId = entry.savedMealId,
    calories = food.calories * entry.servings,
    proteinGrams = food.proteinGrams * entry.servings,
    carbohydrateGrams = food.carbohydrateGrams * entry.servings,
    fatGrams = food.fatGrams * entry.servings,
)

private fun DailyNutritionTotalsRow.toModel() = NutritionTotals(
    calories = calories,
    proteinGrams = proteinGrams,
    carbohydrateGrams = carbohydrateGrams,
    fatGrams = fatGrams,
)

private fun MealQualityCheckInEntity.toModel() = MealQualityCheckIn(
    id = id,
    date = diaryDate,
    mealType = mealType,
    quality = quality,
)
