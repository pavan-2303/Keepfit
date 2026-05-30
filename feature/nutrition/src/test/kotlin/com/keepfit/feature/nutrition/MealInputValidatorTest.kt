package com.keepfit.feature.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealInputValidatorTest {
    @Test
    fun acceptsValidSavedMealDefinition() {
        val result = MealInputValidator.validateSavedMeal(
            name = "  Oats Bowl  ",
            items = listOf("food-oats" to "1", "food-milk" to "0.5"),
        )

        assertTrue(result.isSuccess)
        assertEquals(
            SavedMealInput(
                name = "Oats Bowl",
                items = listOf(
                    SavedMealItemInput("food-oats", 1.0),
                    SavedMealItemInput("food-milk", 0.5),
                ),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun rejectsBlankMealName() {
        val result = MealInputValidator.validateSavedMeal(
            name = "",
            items = listOf("food-oats" to "1"),
        )

        assertEquals("Enter a saved meal name.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsMissingFoods() {
        val result = MealInputValidator.validateSavedMeal(
            name = "Oats Bowl",
            items = emptyList(),
        )

        assertEquals("Choose at least one food.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsNonPositiveMealServings() {
        val result = MealInputValidator.validateSavedMeal(
            name = "Oats Bowl",
            items = listOf("food-oats" to "0"),
        )

        assertEquals("Meal servings must be greater than 0.", result.exceptionOrNull()?.message)
    }

    @Test
    fun validatesDiaryServings() {
        assertEquals(1.5, MealInputValidator.validateServings("1.5").getOrThrow(), 0.0)
    }
}
