package com.keepfit.feature.nutrition

data class FoodInput(
    val name: String,
    val servingLabel: String,
    val servingAmount: Double,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
)

sealed interface FoodValidationResult {
    data class Valid(val input: FoodInput) : FoodValidationResult
    data class Invalid(val message: String) : FoodValidationResult
}

object FoodInputValidator {
    fun validate(
        name: String,
        servingLabel: String,
        servingAmount: String,
        calories: String,
        proteinGrams: String,
        carbohydrateGrams: String,
        fatGrams: String,
    ): FoodValidationResult {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            return FoodValidationResult.Invalid("Enter a food name.")
        }
        val normalizedServingLabel = servingLabel.trim()
        if (normalizedServingLabel.isEmpty()) {
            return FoodValidationResult.Invalid("Enter a serving label.")
        }
        val normalizedServingAmount = servingAmount.toPositiveDoubleOrNull()
            ?: return FoodValidationResult.Invalid("Enter a serving amount greater than 0.")
        val normalizedCalories = calories.toNonNegativeDoubleOrNull()
            ?: return FoodValidationResult.Invalid("Calories must be 0 or more.")
        val normalizedProtein = proteinGrams.toNonNegativeDoubleOrNull()
            ?: return FoodValidationResult.Invalid("Protein must be 0 or more.")
        val normalizedCarbohydrates = carbohydrateGrams.toNonNegativeDoubleOrNull()
            ?: return FoodValidationResult.Invalid("Carbohydrates must be 0 or more.")
        val normalizedFat = fatGrams.toNonNegativeDoubleOrNull()
            ?: return FoodValidationResult.Invalid("Fat must be 0 or more.")
        return FoodValidationResult.Valid(
            FoodInput(
                name = normalizedName,
                servingLabel = normalizedServingLabel,
                servingAmount = normalizedServingAmount,
                calories = normalizedCalories,
                proteinGrams = normalizedProtein,
                carbohydrateGrams = normalizedCarbohydrates,
                fatGrams = normalizedFat,
            ),
        )
    }
}

internal fun String.toPositiveDoubleOrNull(): Double? =
    trim().takeIf(String::isNotEmpty)?.toDoubleOrNull()?.takeIf { it > 0.0 }

internal fun String.toNonNegativeDoubleOrNull(): Double? =
    trim().takeIf(String::isNotEmpty)?.toDoubleOrNull()?.takeIf { it >= 0.0 }
