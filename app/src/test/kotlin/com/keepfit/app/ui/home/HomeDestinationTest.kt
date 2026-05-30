package com.keepfit.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDestinationTest {
    @Test
    fun bottomNavigationUsesFocusedFiveDestinationOrder() {
        assertEquals(
            listOf("Today", "Workouts", "Nutrition", "Progress", "Settings"),
            HomeDestination.entries.map(HomeDestination::label),
        )
    }

    @Test
    fun destinationsExposeStableNavigationRoutes() {
        assertEquals(
            listOf("today", "workouts", "nutrition", "progress", "settings"),
            HomeDestination.entries.map(HomeDestination::route),
        )
    }
}
