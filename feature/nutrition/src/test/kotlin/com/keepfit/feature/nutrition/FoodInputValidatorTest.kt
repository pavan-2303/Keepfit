package com.keepfit.feature.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodInputValidatorTest {
    @Test
    fun trimsAndNormalizesValidInput() {
        val result = FoodInputValidator.validate(
            name = "  Oats  ",
            servingLabel = " 100 g ",
            servingAmount = "100",
            calories = "389",
            proteinGrams = "16.9",
            carbohydrateGrams = "66.3",
            fatGrams = "6.9",
        )

        assertTrue(result is FoodValidationResult.Valid)
        assertEquals(
            FoodInput(
                name = "Oats",
                servingLabel = "100 g",
                servingAmount = 100.0,
                calories = 389.0,
                proteinGrams = 16.9,
                carbohydrateGrams = 66.3,
                fatGrams = 6.9,
            ),
            (result as FoodValidationResult.Valid).input,
        )
    }

    @Test
    fun rejectsBlankFoodName() {
        assertEquals(
            FoodValidationResult.Invalid("Enter a food name."),
            FoodInputValidator.validate("", "100 g", "100", "389", "16", "66", "6"),
        )
    }

    @Test
    fun rejectsBlankServingLabel() {
        assertEquals(
            FoodValidationResult.Invalid("Enter a serving label."),
            FoodInputValidator.validate("Oats", "", "100", "389", "16", "66", "6"),
        )
    }

    @Test
    fun rejectsNonPositiveServingAmount() {
        assertEquals(
            FoodValidationResult.Invalid("Enter a serving amount greater than 0."),
            FoodInputValidator.validate("Oats", "100 g", "0", "389", "16", "66", "6"),
        )
    }

    @Test
    fun rejectsNegativeMacroValues() {
        assertEquals(
            FoodValidationResult.Invalid("Protein must be 0 or more."),
            FoodInputValidator.validate("Oats", "100 g", "100", "389", "-1", "66", "6"),
        )
    }
}
