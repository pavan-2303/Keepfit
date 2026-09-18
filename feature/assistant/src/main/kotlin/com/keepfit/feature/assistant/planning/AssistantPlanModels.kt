package com.keepfit.feature.assistant.planning

import java.time.DayOfWeek

data class AssistantPlanExerciseOption(
    val id: String,
    val name: String,
    val equipment: String?,
    val targetMuscle: String?,
)

data class AssistantPlanContext(
    val goal: String,
    val experience: String,
    val sessionMinutes: Int,
    val preferredDays: Set<DayOfWeek>,
    val equipment: Set<String>,
    val exercises: List<AssistantPlanExerciseOption>,
    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val activityLevel: String = "",
    val sleepDuration: String = "",
    val sleepSchedule: String = "",
    val currentBuild: String = "",
    val routineChallenges: Set<String> = emptySet(),
    val limitationAreas: Set<String> = emptySet(),
    val limitationNotes: String? = null,
)

fun interface AssistantPlanContextDataSource {
    suspend fun loadContext(): AssistantPlanContext
}
