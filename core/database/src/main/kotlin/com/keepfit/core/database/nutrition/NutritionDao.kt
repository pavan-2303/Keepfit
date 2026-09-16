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
    @Transaction
    @Query("SELECT * FROM saved_meals WHERE bodyProfileId = :profileId ORDER BY name COLLATE NOCASE")
    fun observeSavedMealDetailsForProfile(profileId: String): Flow<List<SavedMealDetails>>

    @Transaction
    @Query("SELECT * FROM saved_meals WHERE id = :id AND bodyProfileId = :profileId LIMIT 1")
    suspend fun findSavedMealDetailsForProfile(id: String, profileId: String): SavedMealDetails?

    @Transaction
    @Query("SELECT * FROM food_diary_entries WHERE bodyProfileId = :profileId AND diaryDate = :date ORDER BY CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END, loggedAt, id")
    fun observeDiaryEntriesForProfile(profileId: String, date: LocalDate): Flow<List<FoodDiaryEntryDetails>>

    @Query("SELECT DISTINCT mealType FROM food_diary_entries WHERE bodyProfileId = :profileId AND diaryDate = :date")
    fun observeLoggedMealTypesForProfile(profileId: String, date: LocalDate): Flow<List<MealType>>

    @Query("SELECT * FROM meal_quality_check_ins WHERE bodyProfileId = :profileId AND diaryDate = :date ORDER BY CASE mealType WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END")
    fun observeMealQualityCheckInsForProfile(profileId: String, date: LocalDate): Flow<List<MealQualityCheckInEntity>>

    @Query("SELECT * FROM meal_quality_check_ins WHERE bodyProfileId = :profileId AND diaryDate BETWEEN :startDate AND :endDate ORDER BY diaryDate, loggedAt")
    suspend fun getMealQualityCheckInsForProfile(
        profileId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MealQualityCheckInEntity>

    @Query(
        """
        SELECT COALESCE(SUM(foods.calories * food_diary_entries.servings), 0.0) AS calories,
          COALESCE(SUM(foods.proteinGrams * food_diary_entries.servings), 0.0) AS proteinGrams,
          COALESCE(SUM(foods.carbohydrateGrams * food_diary_entries.servings), 0.0) AS carbohydrateGrams,
          COALESCE(SUM(foods.fatGrams * food_diary_entries.servings), 0.0) AS fatGrams
        FROM food_diary_entries JOIN foods ON foods.id = food_diary_entries.foodId
        WHERE food_diary_entries.bodyProfileId = :profileId AND food_diary_entries.diaryDate = :date
        """,
    )
    fun observeDailyTotalsForProfile(profileId: String, date: LocalDate): Flow<DailyNutritionTotalsRow>

    @Query(
        """
        SELECT food_diary_entries.diaryDate AS diaryDate,
          COALESCE(SUM(foods.calories * food_diary_entries.servings), 0.0) AS calories,
          COALESCE(SUM(foods.proteinGrams * food_diary_entries.servings), 0.0) AS proteinGrams,
          COALESCE(SUM(foods.carbohydrateGrams * food_diary_entries.servings), 0.0) AS carbohydrateGrams,
          COALESCE(SUM(foods.fatGrams * food_diary_entries.servings), 0.0) AS fatGrams
        FROM food_diary_entries JOIN foods ON foods.id = food_diary_entries.foodId
        WHERE food_diary_entries.bodyProfileId = :profileId
        GROUP BY food_diary_entries.diaryDate ORDER BY food_diary_entries.diaryDate DESC
        """,
    )
    fun observeAllDailyTotalsForProfile(profileId: String): Flow<List<DailyNutritionTotalsByDateRow>>

    @Query(
        """
        SELECT foods.*, MAX(food_diary_entries.loggedAt) AS lastUsedAt
        FROM food_diary_entries JOIN foods ON foods.id = food_diary_entries.foodId
        WHERE food_diary_entries.bodyProfileId = :profileId AND foods.archivedAt IS NULL
        GROUP BY foods.id ORDER BY lastUsedAt DESC, foods.name COLLATE NOCASE LIMIT :limit
        """,
    )
    fun observeRecentFoodsForProfile(profileId: String, limit: Int): Flow<List<RecentFoodRow>>

    @Query("DELETE FROM food_diary_entries WHERE id = :id AND bodyProfileId = :profileId")
    suspend fun deleteDiaryEntryForProfile(id: String, profileId: String)

    @Query("DELETE FROM meal_quality_check_ins WHERE bodyProfileId = :profileId AND diaryDate = :date AND mealType = :mealType")
    suspend fun deleteMealQualityCheckInForProfile(profileId: String, date: LocalDate, mealType: MealType)

    @Transaction
    suspend fun upsertMealQualityCheckInForProfile(profileId: String, checkIn: MealQualityCheckInEntity) {
        require(checkIn.bodyProfileId == profileId)
        deleteMealQualityCheckInForProfile(profileId, checkIn.diaryDate, checkIn.mealType)
        insertMealQualityCheckIn(checkIn)
    }

    @Insert
    suspend fun insertMealQualityCheckIn(checkIn: MealQualityCheckInEntity)

    @Query("DELETE FROM meal_quality_check_ins WHERE diaryDate = :date AND mealType = :mealType")
    suspend fun deleteMealQualityCheckIn(date: LocalDate, mealType: MealType)

    @Transaction
    suspend fun upsertMealQualityCheckIn(checkIn: MealQualityCheckInEntity) {
        deleteMealQualityCheckIn(checkIn.diaryDate, checkIn.mealType)
        insertMealQualityCheckIn(checkIn)
    }

    @Query(
        """
        SELECT * FROM meal_quality_check_ins
        WHERE diaryDate = :date
        ORDER BY CASE mealType
            WHEN 'BREAKFAST' THEN 0
            WHEN 'LUNCH' THEN 1
            WHEN 'DINNER' THEN 2
            ELSE 3
        END
        """,
    )
    fun observeMealQualityCheckIns(date: LocalDate): Flow<List<MealQualityCheckInEntity>>

    @Query(
        """
        SELECT * FROM meal_quality_check_ins
        WHERE diaryDate BETWEEN :startDate AND :endDate
        ORDER BY diaryDate, loggedAt
        """,
    )
    suspend fun getMealQualityCheckIns(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MealQualityCheckInEntity>

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

    @Query("SELECT DISTINCT mealType FROM food_diary_entries WHERE diaryDate = :date")
    fun observeLoggedMealTypes(date: LocalDate): Flow<List<MealType>>

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
        SELECT
            food_diary_entries.diaryDate AS diaryDate,
            COALESCE(SUM(foods.calories * food_diary_entries.servings), 0.0) AS calories,
            COALESCE(SUM(foods.proteinGrams * food_diary_entries.servings), 0.0) AS proteinGrams,
            COALESCE(SUM(foods.carbohydrateGrams * food_diary_entries.servings), 0.0) AS carbohydrateGrams,
            COALESCE(SUM(foods.fatGrams * food_diary_entries.servings), 0.0) AS fatGrams
        FROM food_diary_entries
        JOIN foods ON foods.id = food_diary_entries.foodId
        GROUP BY food_diary_entries.diaryDate
        ORDER BY food_diary_entries.diaryDate DESC
        """,
    )
    fun observeAllDailyTotals(): Flow<List<DailyNutritionTotalsByDateRow>>

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
            id, bodyProfileId, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
        )
        SELECT
            lower(hex(randomblob(16))),
            bodyProfileId,
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

    @Query("DELETE FROM food_diary_entries WHERE diaryDate = :date AND mealType = :mealType")
    suspend fun deleteDiaryMeal(date: LocalDate, mealType: MealType)

    @Query(
        """
        INSERT INTO food_diary_entries (
            id, bodyProfileId, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
        )
        SELECT
            lower(hex(randomblob(16))),
            bodyProfileId,
            :targetDate,
            mealType,
            foodId,
            savedMealId,
            servings,
            :loggedAt
        FROM food_diary_entries
        WHERE diaryDate = :sourceDate AND mealType = :mealType
        ORDER BY loggedAt, id
        """,
    )
    suspend fun copyDiaryMeal(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        mealType: MealType,
        loggedAt: Long,
    )

    @Query("SELECT COUNT(*) FROM food_diary_entries WHERE diaryDate = :date AND mealType = :mealType")
    suspend fun countDiaryMeal(date: LocalDate, mealType: MealType): Int

    @Transaction
    suspend fun repeatDiaryMeal(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        mealType: MealType,
        loggedAt: Long,
    ) {
        if (countDiaryMeal(sourceDate, mealType) == 0) return
        deleteDiaryMeal(targetDate, mealType)
        copyDiaryMeal(sourceDate, targetDate, mealType, loggedAt)
    }

    @Query("DELETE FROM food_diary_entries WHERE bodyProfileId = :profileId AND diaryDate = :date")
    suspend fun deleteDiaryEntriesForProfileAndDate(profileId: String, date: LocalDate)

    @Query(
        """
        INSERT INTO food_diary_entries (
            id, bodyProfileId, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
        )
        SELECT lower(hex(randomblob(16))), :profileId, :targetDate, mealType, foodId,
               savedMealId, servings, :loggedAt
        FROM food_diary_entries
        WHERE bodyProfileId = :profileId AND diaryDate = :sourceDate
        ORDER BY loggedAt, id
        """,
    )
    suspend fun copyDiaryEntriesForProfile(
        profileId: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        loggedAt: Long,
    )

    @Transaction
    suspend fun duplicateDiaryEntriesForProfile(
        profileId: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        loggedAt: Long,
    ) {
        deleteDiaryEntriesForProfileAndDate(profileId, targetDate)
        copyDiaryEntriesForProfile(profileId, sourceDate, targetDate, loggedAt)
    }

    @Query("DELETE FROM food_diary_entries WHERE bodyProfileId = :profileId AND diaryDate = :date AND mealType = :mealType")
    suspend fun deleteDiaryMealForProfile(profileId: String, date: LocalDate, mealType: MealType)

    @Query("SELECT COUNT(*) FROM food_diary_entries WHERE bodyProfileId = :profileId AND diaryDate = :date AND mealType = :mealType")
    suspend fun countDiaryMealForProfile(profileId: String, date: LocalDate, mealType: MealType): Int

    @Query(
        """
        INSERT INTO food_diary_entries (
            id, bodyProfileId, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
        )
        SELECT lower(hex(randomblob(16))), :profileId, :targetDate, mealType, foodId,
               savedMealId, servings, :loggedAt
        FROM food_diary_entries
        WHERE bodyProfileId = :profileId AND diaryDate = :sourceDate AND mealType = :mealType
        ORDER BY loggedAt, id
        """,
    )
    suspend fun copyDiaryMealForProfile(
        profileId: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        mealType: MealType,
        loggedAt: Long,
    )

    @Transaction
    suspend fun repeatDiaryMealForProfile(
        profileId: String,
        sourceDate: LocalDate,
        targetDate: LocalDate,
        mealType: MealType,
        loggedAt: Long,
    ) {
        if (countDiaryMealForProfile(profileId, sourceDate, mealType) == 0) return
        deleteDiaryMealForProfile(profileId, targetDate, mealType)
        copyDiaryMealForProfile(profileId, sourceDate, targetDate, mealType, loggedAt)
    }
}
