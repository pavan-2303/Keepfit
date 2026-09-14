package com.keepfit.feature.nutrition.data

import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.feature.nutrition.FoodInput
import com.keepfit.feature.nutrition.SavedMealInput
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun observeFoods(query: String): Flow<List<Food>>
    fun observeFavoriteFoods(): Flow<List<Food>>
    fun observeRecentFoods(limit: Int): Flow<List<Food>>
    fun observeSavedMeals(): Flow<List<SavedMeal>>
    fun observeDiaryEntries(date: LocalDate): Flow<List<DiaryEntry>>
    fun observeDailySummary(date: LocalDate): Flow<DailyNutritionSummary>
    fun observeMealQualityCheckIns(date: LocalDate): Flow<List<MealQualityCheckIn>>
    fun observePreviousMealTypes(date: LocalDate): Flow<List<MealType>>

    suspend fun saveFood(id: String?, input: FoodInput)
    suspend fun toggleFavorite(foodId: String, isFavorite: Boolean)
    suspend fun archiveFood(foodId: String)
    suspend fun createSavedMeal(input: SavedMealInput)
    suspend fun addFoodToDiary(date: LocalDate, mealType: MealType, foodId: String, servings: Double)
    suspend fun addFoodsToDiary(date: LocalDate, mealType: MealType, items: List<FoodDiaryAddition>) {
        items.forEach { item -> addFoodToDiary(date, mealType, item.foodId, item.servings) }
    }
    suspend fun addSavedMealToDiary(date: LocalDate, mealType: MealType, savedMealId: String, multiplier: Double)
    suspend fun deleteDiaryEntry(entryId: String)
    suspend fun duplicatePreviousDay(targetDate: LocalDate)
    suspend fun repeatPreviousMeal(targetDate: LocalDate, mealType: MealType)
    suspend fun setMealQuality(date: LocalDate, mealType: MealType, quality: MealQuality)
}

data class FoodDiaryAddition(val foodId: String, val servings: Double)
