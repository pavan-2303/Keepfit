package com.keepfit.feature.nutrition.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.feature.nutrition.data.DailyNutritionSummary
import com.keepfit.feature.nutrition.data.NutritionGoals
import com.keepfit.feature.nutrition.data.NutritionTotals
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NutritionModeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calorieProteinTodayCardUsesOneCompactSummary() {
        composeRule.setContent {
            MaterialTheme {
                TodayNutritionCard(
                    NutritionTrackingDepth.CALORIES_PROTEIN,
                    10,
                    summary(),
                    emptyList(),
                    {},
                )
            }
        }

        composeRule.onNodeWithText("1850 kcal · 105 g protein").assertIsDisplayed()
        composeRule.onNodeWithText("Log").assertIsDisplayed()
        composeRule.onNodeWithText("Carbs").assertDoesNotExist()
        composeRule.onNodeWithText("Fat").assertDoesNotExist()
    }

    @Test
    fun detailedModeStillKeepsTodayCardCompact() {
        composeRule.setContent {
            MaterialTheme {
                TodayNutritionCard(NutritionTrackingDepth.DETAILED_MACROS, 10, summary(), emptyList(), {})
            }
        }
        composeRule.onNodeWithText("1850 kcal · 105 g protein").assertIsDisplayed()
        composeRule.onNodeWithText("Carbs").assertDoesNotExist()
        composeRule.onNodeWithText("Fat").assertDoesNotExist()
    }

    @Test
    fun qualityPanelRecordsTheChosenMealAndQuality() {
        var selected: Pair<MealType, MealQuality>? = null
        composeRule.setContent {
            MaterialTheme {
                MealQualityCheckInPanel(emptyList()) { meal, quality -> selected = meal to quality }
            }
        }

        composeRule.onAllNodesWithText("Balanced")[0].performClick()
        assertEquals(MealType.BREAKFAST to MealQuality.BALANCED, selected)
    }

    @Test
    fun nutritionLensSwitchesToDisabled() {
        var disabled = false
        composeRule.setContent {
            MaterialTheme {
                NutritionLensSelector(
                    NutritionTrackingDepth.DETAILED_MACROS,
                    10,
                    { disabled = it == NutritionTrackingDepth.DISABLED },
                    {},
                )
            }
        }

        composeRule.onNodeWithText("Off").performClick()
        assertTrue(disabled)
    }

    @Test
    fun disabledStateExplainsThatHistoryIsPreserved() {
        composeRule.setContent {
            MaterialTheme { NutritionDisabledCard {} }
        }

        composeRule.onNodeWithText("Nutrition is off").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Your foods, saved meals, goals, check-ins, and diary history are still here.",
        ).assertIsDisplayed()
    }

    @Test
    fun servingPresetChangesThePendingAmount() {
        var value = "1"
        composeRule.setContent {
            MaterialTheme { ServingPresetRow(value) { value = it } }
        }

        composeRule.onNodeWithText("1.5x").performClick()
        assertEquals("1.5", value)
    }

    private fun summary() = DailyNutritionSummary(
        date = LocalDate.parse("2026-09-13"),
        totals = NutritionTotals(1_850.0, 105.0, 190.0, 60.0),
        goals = NutritionGoals(2_000.0, 120.0, 220.0, 65.0),
        hasEntries = true,
    )
}
