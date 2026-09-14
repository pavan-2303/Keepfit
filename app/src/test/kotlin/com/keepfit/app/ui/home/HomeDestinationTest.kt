package com.keepfit.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDestinationTest {
    @Test
    fun bottomNavigationUsesFocusedFiveDestinationOrder() {
        assertEquals(
            listOf("Today", "Plan", "Log", "Progress", "Coach"),
            HomeDestination.entries.map(HomeDestination::label),
        )
    }

    @Test
    fun destinationsExposeStableNavigationRoutes() {
        assertEquals(
            listOf("today", "plan", "log", "progress", "coach"),
            HomeDestination.entries.map(HomeDestination::route),
        )
    }

    @Test
    fun largeTextUsesAccessibleIconOnlyNavigation() {
        assertEquals(true, shouldShowNavigationLabels(fontScale = 1.0f))
        assertEquals(false, shouldShowNavigationLabels(fontScale = 2.0f))
    }
}
