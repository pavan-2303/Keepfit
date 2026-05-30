package com.keepfit.core.database.nutrition

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun findFood(id: String): FoodEntity?

    @Query(
        """
        SELECT * FROM foods
        WHERE archivedAt IS NULL AND name LIKE '%' || :query || '%'
        ORDER BY isFavorite DESC, name COLLATE NOCASE
        """,
    )
    fun observeFoods(query: String): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT * FROM foods
        WHERE archivedAt IS NULL AND isFavorite = 1
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeFavoriteFoods(): Flow<List<FoodEntity>>

    @Query("UPDATE foods SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE foods SET archivedAt = :archivedAt, updatedAt = :archivedAt WHERE id = :id")
    suspend fun archiveFood(id: String, archivedAt: Long)

    @Upsert
    suspend fun upsertSavedMeal(meal: SavedMealEntity)

    @Query("DELETE FROM saved_meal_items WHERE savedMealId = :mealId")
    suspend fun deleteSavedMealItems(mealId: String)

    @Insert
    suspend fun insertSavedMealItems(items: List<SavedMealItemEntity>)

    @Transaction
    suspend fun replaceSavedMealItems(mealId: String, items: List<SavedMealItemEntity>) {
        deleteSavedMealItems(mealId)
        insertSavedMealItems(items)
    }

    @Transaction
    @Query("SELECT * FROM saved_meals ORDER BY name COLLATE NOCASE")
    fun observeSavedMealDetails(): Flow<List<SavedMealDetails>>

    @Query("SELECT * FROM saved_meals WHERE id = :id LIMIT 1")
    suspend fun findSavedMeal(id: String): SavedMealEntity?

    @Transaction
    @Query("SELECT * FROM saved_meals WHERE id = :id LIMIT 1")
    suspend fun findSavedMealDetails(id: String): SavedMealDetails?

    @Insert
    suspend fun insertDiaryEntry(entry: FoodDiaryEntryEntity)

    @Insert
    suspend fun insertDiaryEntries(entries: List<FoodDiaryEntryEntity>)

    @Query("DELETE FROM food_diary_entries WHERE id = :id")
    suspend fun deleteDiaryEntry(id: String)

    @Query("DELETE FROM food_diary_entries WHERE diaryDate = :date")
    suspend fun deleteDiaryEntriesForDate(date: LocalDate)

    @Transaction
    @Query(
        """
        SELECT * FROM food_diary_entries
        WHERE diaryDate = :date
        ORDER BY
            CASE mealType
                WHEN 'BREAKFAST' THEN 0
                WHEN 'LUNCH' THEN 1
                WHEN 'DINNER' THEN 2
                ELSE 3
            END,
            loggedAt,
            id
        """,
    )
    fun observeDiaryEntries(date: LocalDate): Flow<List<FoodDiaryEntryDetails>>

    @Query(
        """
        SELECT
            COALESCE(SUM(foods.calories * food_diary_entries.servings), 0.0) AS calories,
            COALESCE(SUM(foods.proteinGrams * food_diary_entries.servings), 0.0) AS proteinGrams,
            COALESCE(SUM(foods.carbohydrateGrams * food_diary_entries.servings), 0.0) AS carbohydrateGrams,
            COALESCE(SUM(foods.fatGrams * food_diary_entries.servings), 0.0) AS fatGrams
        FROM food_diary_entries
        JOIN foods ON foods.id = food_diary_entries.foodId
        WHERE food_diary_entries.diaryDate = :date
        """,
    )
    fun observeDailyTotals(date: LocalDate): Flow<DailyNutritionTotalsRow>

    @Query(
        """
        SELECT foods.*, MAX(food_diary_entries.loggedAt) AS lastUsedAt
        FROM food_diary_entries
        JOIN foods ON foods.id = food_diary_entries.foodId
        WHERE foods.archivedAt IS NULL
        GROUP BY foods.id
        ORDER BY lastUsedAt DESC, foods.name COLLATE NOCASE
        LIMIT :limit
        """,
    )
    fun observeRecentFoods(limit: Int): Flow<List<RecentFoodRow>>

    @Query(
        """
        INSERT INTO food_diary_entries (
            id, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
        )
        SELECT
            lower(hex(randomblob(16))),
            :targetDate,
            mealType,
            foodId,
            savedMealId,
            servings,
            :loggedAt
        FROM food_diary_entries
        WHERE diaryDate = :sourceDate
        ORDER BY loggedAt, id
        """,
    )
    suspend fun copyDiaryEntries(sourceDate: LocalDate, targetDate: LocalDate, loggedAt: Long)

    @Transaction
    suspend fun duplicateDiaryEntries(sourceDate: LocalDate, targetDate: LocalDate, loggedAt: Long) {
        deleteDiaryEntriesForDate(targetDate)
        copyDiaryEntries(sourceDate, targetDate, loggedAt)
    }
}
