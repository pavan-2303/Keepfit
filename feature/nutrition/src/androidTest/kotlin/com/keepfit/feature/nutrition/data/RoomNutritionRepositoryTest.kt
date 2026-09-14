package com.keepfit.feature.nutrition.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.SavedMealItemEntity
import com.keepfit.feature.nutrition.FoodInput
import com.keepfit.feature.nutrition.SavedMealInput
import com.keepfit.feature.nutrition.SavedMealItemInput
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
class RoomNutritionRepositoryTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var repository: RoomNutritionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java).build()
        repository = RoomNutritionRepository(
            dao = database.nutritionDao(),
            bodyProfileDao = database.bodyProfileDao(),
            idFactory = IdFactory(),
            clock = { 100L },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun qualitySelectionReplacesTheSameMeal() = runBlocking {
        val date = LocalDate.parse("2026-09-13")
        repository.setMealQuality(date, MealType.BREAKFAST, MealQuality.BALANCED)
        repository.setMealQuality(date, MealType.BREAKFAST, MealQuality.FLEXIBLE)

        val result = repository.observeMealQualityCheckIns(date).first()
        assertEquals(1, result.size)
        assertEquals(MealQuality.FLEXIBLE, result.single().quality)
    }

    @Test
    fun savedMealExpansionStaysIndependentFromLaterTemplateItems() = runBlocking {
        repository.saveFood(null, FoodInput("Oats", "1 bowl", 1.0, 300.0, 12.0, 50.0, 6.0))
        repository.saveFood(null, FoodInput("Fruit", "1 piece", 1.0, 80.0, 1.0, 20.0, 0.0))
        val foods = repository.observeFoods("").first()
        val oats = foods.first { it.name == "Oats" }
        val fruit = foods.first { it.name == "Fruit" }
        repository.createSavedMeal(SavedMealInput("Breakfast", listOf(SavedMealItemInput(oats.id, 1.0))))
        val meal = repository.observeSavedMeals().first().single()
        val date = LocalDate.parse("2026-09-13")

        repository.addSavedMealToDiary(date, MealType.BREAKFAST, meal.id, 1.0)
        database.nutritionDao().replaceSavedMealItems(
            meal.id,
            listOf(SavedMealItemEntity("replacement", meal.id, fruit.id, 2.0, 0)),
        )

        val diary = repository.observeDiaryEntries(date).first()
        assertEquals(listOf("Oats"), diary.map(DiaryEntry::foodName))
        assertEquals(1.0, diary.single().servings, 0.0)
    }

    @Test
    fun reviewedExistingFoodMealValidatesEverythingBeforeWriting() = runBlocking {
        repository.saveFood(null, FoodInput("Oats", "1 bowl", 1.0, 300.0, 12.0, 50.0, 6.0))
        repository.saveFood(null, FoodInput("Yogurt", "1 cup", 1.0, 120.0, 10.0, 8.0, 4.0))
        val foods = repository.observeFoods("").first()
        val date = LocalDate.parse("2026-09-13")

        repository.addFoodsToDiary(
            date = date,
            mealType = MealType.BREAKFAST,
            items = foods.map { FoodDiaryAddition(it.id, 1.0) },
        )
        val beforeFailure = repository.observeDiaryEntries(date).first()
        assertEquals(2, beforeFailure.size)

        val failure = runCatching {
            repository.addFoodsToDiary(
                date = date,
                mealType = MealType.LUNCH,
                items = listOf(
                    FoodDiaryAddition(foods.first().id, 1.0),
                    FoodDiaryAddition("missing-food", 1.0),
                ),
            )
        }

        assertTrue(failure.isFailure)
        assertEquals(beforeFailure, repository.observeDiaryEntries(date).first())
    }
}

private class IdFactory : () -> String {
    private var next = 0
    override fun invoke(): String = "id-${next++}"
}
