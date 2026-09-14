package com.keepfit.feature.workouts.catalog

import com.keepfit.feature.workouts.planning.MovementPattern
import com.keepfit.feature.workouts.planning.StarterExerciseCatalog

object OfflineExerciseCatalog {
    val items: List<CatalogExercise> = StarterExerciseCatalog.exercises.map { definition ->
        CatalogExercise(
            id = "keepfit:${definition.key}",
            name = definition.name,
            movementArea = definition.movementPattern.label,
            bodyParts = listOf(definition.muscleGroup),
            targetMuscles = listOf(definition.muscleGroup),
            secondaryMuscles = emptyList(),
            equipment = definition.equipment.map { it.label }.sorted(),
            instructions = listOf(definition.instructions),
            isBodyweight = definition.isBodyweight,
            demoUrl = null,
            origin = CatalogOrigin.KEEPFIT_OFFLINE,
            attribution = "Original Keepfit guide",
        )
    }

    val movementAreas: List<String> = items.map(CatalogExercise::movementArea).distinct()
    val equipment: List<String> = items.flatMap(CatalogExercise::equipment).distinct().sorted()

    fun search(query: CatalogQuery): List<CatalogExercise> {
        val text = query.text.trim()
        val movementArea = query.movementArea?.trim().orEmpty()
        val equipment = query.equipment?.trim().orEmpty()
        return items.filter { item ->
            val textMatches = text.isBlank() || listOf(
                item.name,
                item.movementArea,
                item.bodyParts.joinToString(),
                item.instructions.joinToString(),
            ).any { it.contains(text, ignoreCase = true) }
            val movementMatches = movementArea.isBlank() ||
                item.movementArea.equals(movementArea, ignoreCase = true)
            val equipmentMatches = equipment.isBlank() ||
                item.equipment.any { it.equals(equipment, ignoreCase = true) }
            textMatches && movementMatches && equipmentMatches
        }
    }
}

private val MovementPattern.label: String
    get() = when (this) {
        MovementPattern.SQUAT -> "Squat"
        MovementPattern.SINGLE_LEG -> "Single leg"
        MovementPattern.HINGE -> "Hinge"
        MovementPattern.PUSH -> "Push"
        MovementPattern.PULL -> "Pull"
        MovementPattern.CORE -> "Core"
    }
