package com.keepfit.app.ui.home

enum class HomeDestination(
    val route: String,
    val label: String,
) {
    TODAY("today", "Today"),
    WORKOUTS("workouts", "Workouts"),
    NUTRITION("nutrition", "Nutrition"),
    PROGRESS("progress", "Progress"),
    SETTINGS("settings", "Settings"),
}
