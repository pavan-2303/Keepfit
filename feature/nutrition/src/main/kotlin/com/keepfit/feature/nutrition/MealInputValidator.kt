package com.keepfit.feature.nutrition

data class SavedMealItemInput(
    val foodId: String,
    val servings: Double,
)

data class SavedMealInput(
    val name: String,
    val items: List<SavedMealItemInput>,
)

object MealInputValidator {
    fun validateSavedMeal(name: String, items: List<Pair<String, String>>): Result<SavedMealInput> {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a saved meal name."))
        }
        val normalizedItems = items.mapNotNull { (foodId, servingsText) ->
            val servings = servingsText.toPositiveDoubleOrNull()
                ?: return Result.failure(IllegalArgumentException("Meal servings must be greater than 0."))
            if (foodId.isBlank()) {
                return Result.failure(IllegalArgumentException("Choose at least one food."))
            }
            SavedMealItemInput(foodId = foodId, servings = servings)
        }
        if (normalizedItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Choose at least one food."))
        }
        return Result.success(SavedMealInput(normalizedName, normalizedItems))
    }

    fun validateServings(servings: String): Result<Double> =
        servings.toPositiveDoubleOrNull()
            ?.let(Result.Companion::success)
            ?: Result.failure(IllegalArgumentException("Servings must be greater than 0."))
}
