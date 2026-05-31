package com.keepfit.feature.steps

import com.keepfit.feature.steps.data.StepsSummary
import com.keepfit.feature.steps.data.StepsSnapshot
import com.keepfit.feature.steps.data.StepsStatus
import com.keepfit.feature.steps.data.stepsUiStateForError
import com.keepfit.feature.steps.data.stepsUiStateForSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun mapsConnectedSnapshotToConnectedUiState() {
        val summary = StepsSummary(todaySteps = 9_400, sevenDayTotal = 58_000)

        val uiState = stepsUiStateForSnapshot(StepsSnapshot.Connected(summary))

        assertEquals(StepsStatus.CONNECTED, uiState.status)
        assertEquals(summary, uiState.summary)
        assertNull(uiState.message)
    }

    @Test
    fun mapsPermissionRequiredSnapshotWithoutStaleSummary() {
        val uiState = stepsUiStateForSnapshot(StepsSnapshot.PermissionRequired)

        assertEquals(StepsStatus.PERMISSION_REQUIRED, uiState.status)
        assertNull(uiState.summary)
        assertNull(uiState.message)
    }

    @Test
    fun mapsBlankErrorMessageToDefaultStepsMessage() {
        val uiState = stepsUiStateForError(IllegalStateException(""))

        assertEquals(StepsStatus.ERROR, uiState.status)
        assertNull(uiState.summary)
        assertEquals("Health Connect steps could not be loaded.", uiState.message)
    }
}
