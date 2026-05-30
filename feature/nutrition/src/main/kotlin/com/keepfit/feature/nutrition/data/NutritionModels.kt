package com.keepfit.feature.nutrition.data

import com.keepfit.core.database.nutrition.MealType
import java.time.LocalDate

data class Food(
    val id: String,
    val name: String,
    val servingLabel: String,
    val servingAmount: Double,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val isFavorite: Boolean,
)

data class SavedMealItem(
    val id: String,
    val foodId: String,
    val foodName: String,
    val servings: Double,
)

data class SavedMeal(
    val id: String,
    val name: String,
    val items: List<SavedMealItem>,
) {
    val summary: String
        get() = items.joinToString { "${it.foodName} x${it.servings.formatQuantity()}" }
}

data class DiaryEntry(
    val id: String,
    val diaryDate: LocalDate,
    val mealType: MealType,
    val foodId: String,
    val foodName: String,
    val servingLabel: String,
    val servings: Double,
    val savedMealId: String?,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
)

data class NutritionTotals(
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
)

data class NutritionGoals(
    val calorieGoal: Double?,
    val proteinGoalGrams: Double?,
    val carbohydrateGoalGrams: Double?,
    val fatGoalGrams: Double?,
)

data class DailyNutritionSummary(
    val date: LocalDate,
    val totals: NutritionTotals,
    val goals: NutritionGoals?,
    val hasEntries: Boolean,
)

internal fun Double.formatQuantity(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
