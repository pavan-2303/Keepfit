package com.keepfit.feature.steps

import com.keepfit.feature.steps.data.StepsSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepsSummaryTest {
    @Test
    fun computesSevenDayAverageFromTotal() {
        val summary = StepsSummary(todaySteps = 6_500, sevenDayTotal = 49_000)

        assertEquals(7_000, summary.sevenDayAverage)
    }

    @Test
    fun reportsNoDataWhenTotalsAreZero() {
        val summary = StepsSummary(todaySteps = 0, sevenDayTotal = 0)

        assertFalse(summary.hasData)
    }

    @Test
    fun reportsDataWhenTodayHasSteps() {
        val summary = StepsSummary(todaySteps = 3_200, sevenDayTotal = 3_200)

        assertTrue(summary.hasData)
    }
}
