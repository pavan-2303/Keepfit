package com.keepfit.core.model

import java.time.LocalDate

data class BodyProfile(
    val id: String,
    val displayName: String,
    val heightCm: Double?,
    val birthDate: LocalDate?,
    val dailyCalorieGoal: Double? = null,
    val dailyProteinGoalGrams: Double? = null,
    val dailyCarbohydrateGoalGrams: Double? = null,
    val dailyFatGoalGrams: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
