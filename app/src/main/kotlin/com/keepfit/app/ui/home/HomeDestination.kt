package com.keepfit.app.ui.home

enum class HomeDestination(
    val route: String,
    val label: String,
) {
    TODAY("today", "Today"),
    PLAN("plan", "Plan"),
    LOG("log", "Log"),
    PROGRESS("progress", "Progress"),
    COACH("coach", "Coach"),
}
