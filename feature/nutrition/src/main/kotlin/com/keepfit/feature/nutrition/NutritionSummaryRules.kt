package com.keepfit.feature.nutrition

import com.keepfit.core.preferences.NutritionTrackingDepth
import kotlin.math.round

enum class NutritionMetric {
    CALORIES,
    PROTEIN,
    CARBOHYDRATES,
    FAT,
}

data class NutritionTargetRange(
    val minimum: Double,
    val maximum: Double,
)

class NutritionSummaryRules {
    fun visibleMetrics(depth: NutritionTrackingDepth): List<NutritionMetric> = when (depth) {
        NutritionTrackingDepth.DETAILED_MACROS -> NutritionMetric.entries
        NutritionTrackingDepth.CALORIES_PROTEIN -> listOf(NutritionMetric.CALORIES, NutritionMetric.PROTEIN)
        NutritionTrackingDepth.MEAL_QUALITY,
        NutritionTrackingDepth.DISABLED,
        -> emptyList()
    }

    fun targetRange(goal: Double?, flexibilityPercent: Int): NutritionTargetRange? {
        if (goal == null || goal <= 0.0) return null
        val supportedPercent = when {
            flexibilityPercent <= 7 -> 5
            flexibilityPercent <= 12 -> 10
            else -> 15
        }
        val fraction = supportedPercent / 100.0
        return NutritionTargetRange(
            minimum = (goal * (1.0 - fraction)).toDisplayPrecision(),
            maximum = (goal * (1.0 + fraction)).toDisplayPrecision(),
        )
    }
}

private fun Double.toDisplayPrecision(): Double = round(this * 10.0) / 10.0
