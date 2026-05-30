package com.keepfit.core.database.nutrition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: NutritionDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.nutritionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun nutritionLifecycleSupportsFavoritesMealsDiaryTotalsAndArchiveSafeHistory() = runBlocking {
        val oats = FoodEntity(
            id = "food-oats",
            name = "Oats",
            servingLabel = "100 g",
            servingAmount = 100.0,
            calories = 389.0,
            proteinGrams = 16.9,
            carbohydrateGrams = 66.3,
            fatGrams = 6.9,
            isFavorite = true,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
        )
        val milk = FoodEntity(
            id = "food-milk",
            name = "Milk",
            servingLabel = "250 ml",
            servingAmount = 250.0,
            calories = 150.0,
            proteinGrams = 8.0,
            carbohydrateGrams = 12.0,
            fatGrams = 8.0,
            isFavorite = false,
            createdAt = 2L,
            updatedAt = 2L,
            archivedAt = null,
        )
        dao.upsertFood(oats)
        dao.upsertFood(milk)

        assertEquals(listOf("Oats"), dao.observeFoods("oa").first().map(FoodEntity::name))
        assertEquals(listOf("Oats"), dao.observeFavoriteFoods().first().map(FoodEntity::name))

        dao.upsertSavedMeal(
            SavedMealEntity(
                id = "meal",
                name = "Oats Bowl",
                createdAt = 3L,
                updatedAt = 3L,
            ),
        )
        dao.replaceSavedMealItems(
            mealId = "meal",
            items = listOf(
                SavedMealItemEntity(
                    id = "meal-item-1",
                    savedMealId = "meal",
                    foodId = oats.id,
                    servings = 1.0,
                    position = 0,
                ),
                SavedMealItemEntity(
                    id = "meal-item-2",
                    savedMealId = "meal",
                    foodId = milk.id,
                    servings = 1.0,
                    position = 1,
                ),
            ),
        )

        assertEquals(
            listOf("Oats", "Milk"),
            dao.observeSavedMealDetails().first().single().items.map { it.foodName },
        )

        dao.insertDiaryEntry(
            FoodDiaryEntryEntity(
                id = "entry-1",
                diaryDate = LocalDate.parse("2026-05-31"),
                mealType = MealType.LUNCH,
                foodId = oats.id,
                savedMealId = null,
                servings = 2.0,
                loggedAt = 10L,
            ),
        )
        dao.insertDiaryEntries(
            listOf(
                FoodDiaryEntryEntity(
                    id = "entry-2",
                    diaryDate = LocalDate.parse("2026-05-30"),
                    mealType = MealType.DINNER,
                    foodId = oats.id,
                    savedMealId = "meal",
                    servings = 1.0,
                    loggedAt = 8L,
                ),
                FoodDiaryEntryEntity(
                    id = "entry-3",
                    diaryDate = LocalDate.parse("2026-05-30"),
                    mealType = MealType.DINNER,
                    foodId = milk.id,
                    savedMealId = "meal",
                    servings = 1.0,
                    loggedAt = 8L,
                ),
            ),
        )

        val lunchRows = dao.observeDiaryEntries(LocalDate.parse("2026-05-31")).first()
        assertEquals(1, lunchRows.size)
        assertEquals("Oats", lunchRows.single().foodName)

        val totals = dao.observeDailyTotals(LocalDate.parse("2026-05-31")).first()
        assertEquals(778.0, totals?.calories ?: 0.0, 0.0)
        assertEquals(33.8, totals?.proteinGrams ?: 0.0, 0.0)

        assertEquals(
            listOf("Oats", "Milk"),
            dao.observeRecentFoods(limit = 5).first().map { it.food.name },
        )

        dao.duplicateDiaryEntries(
            sourceDate = LocalDate.parse("2026-05-30"),
            targetDate = LocalDate.parse("2026-06-01"),
            loggedAt = 20L,
        )

        val duplicatedDinner = dao.observeDiaryEntries(LocalDate.parse("2026-06-01")).first()
        assertEquals(2, duplicatedDinner.size)
        assertTrue(duplicatedDinner.all { it.mealType == MealType.DINNER })

        dao.archiveFood(oats.id, 30L)

        assertEquals(listOf("Milk"), dao.observeFoods("").first().map(FoodEntity::name))
        assertEquals(
            listOf("Oats"),
            dao.observeDiaryEntries(LocalDate.parse("2026-05-31")).first().map { it.foodName },
        )
    }
}
