package com.keepfit.feature.nutrition

import com.keepfit.core.preferences.NutritionTrackingDepth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionSummaryRulesTest {
    private val rules = NutritionSummaryRules()

    @Test
    fun eachDepthExposesOnlyItsRelevantMetrics() {
        assertEquals(
            listOf(NutritionMetric.CALORIES, NutritionMetric.PROTEIN, NutritionMetric.CARBOHYDRATES, NutritionMetric.FAT),
            rules.visibleMetrics(NutritionTrackingDepth.DETAILED_MACROS),
        )
        assertEquals(
            listOf(NutritionMetric.CALORIES, NutritionMetric.PROTEIN),
            rules.visibleMetrics(NutritionTrackingDepth.CALORIES_PROTEIN),
        )
        assertEquals(emptyList<NutritionMetric>(), rules.visibleMetrics(NutritionTrackingDepth.MEAL_QUALITY))
        assertEquals(emptyList<NutritionMetric>(), rules.visibleMetrics(NutritionTrackingDepth.DISABLED))
    }

    @Test
    fun supportedFlexibilityProducesBoundedRanges() {
        assertEquals(NutritionTargetRange(1_900.0, 2_100.0), rules.targetRange(2_000.0, 5))
        assertEquals(NutritionTargetRange(1_800.0, 2_200.0), rules.targetRange(2_000.0, 10))
        assertEquals(NutritionTargetRange(1_700.0, 2_300.0), rules.targetRange(2_000.0, 15))
    }

    @Test
    fun unsupportedFlexibilityIsClampedAndMissingGoalHasNoRange() {
        assertEquals(NutritionTargetRange(85.0, 115.0), rules.targetRange(100.0, 99))
        assertEquals(NutritionTargetRange(90.0, 110.0), rules.targetRange(100.0, 8))
        assertNull(rules.targetRange(null, 10))
        assertNull(rules.targetRange(0.0, 10))
    }
}
