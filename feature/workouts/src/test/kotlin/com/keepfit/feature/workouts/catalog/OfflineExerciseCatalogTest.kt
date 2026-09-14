package com.keepfit.feature.workouts.catalog

import com.keepfit.feature.workouts.planning.EquipmentOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineExerciseCatalogTest {
    @Test
    fun catalogueContainsFortyUniqueOwnedGuidesAndPreservesOriginalKeys() {
        val items = OfflineExerciseCatalog.items

        assertEquals(40, items.size)
        assertEquals(40, items.map(CatalogExercise::id).distinct().size)
        assertEquals(40, items.map { it.name.lowercase() }.distinct().size)
        assertTrue(items.all { item ->
            item.origin == CatalogOrigin.KEEPFIT_OFFLINE &&
                item.instructions.isNotEmpty() &&
                item.instructions.none(String::isBlank) &&
                item.equipment.isNotEmpty() &&
                item.demoUrl == null
        })
        assertTrue(
            ORIGINAL_KEYS.all { key ->
                items.any { it.id == "keepfit:$key" }
            },
        )
    }

    @Test
    fun searchMatchesNameInstructionsAndMovementAreaCaseInsensitively() {
        val nameMatch = OfflineExerciseCatalog.search(CatalogQuery(text = "GOBLET"))
        val instructionMatch = OfflineExerciseCatalog.search(CatalogQuery(text = "anchor the band securely"))
        val movementMatch = OfflineExerciseCatalog.search(CatalogQuery(movementArea = "core"))

        assertEquals(listOf("Goblet Squat"), nameMatch.map(CatalogExercise::name))
        assertTrue(instructionMatch.any { it.name == "Resistance-Band Row" })
        assertTrue(movementMatch.isNotEmpty())
        assertTrue(movementMatch.all { it.movementArea.equals("Core", ignoreCase = true) })
    }

    @Test
    fun equipmentFilterReturnsOnlyCompatibleGuides() {
        val results = OfflineExerciseCatalog.search(
            CatalogQuery(equipment = EquipmentOption.RESISTANCE_BANDS.label),
        )

        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all { item ->
                item.equipment.any { it.equals(EquipmentOption.RESISTANCE_BANDS.label, ignoreCase = true) }
            },
        )
    }

    @Test
    fun blankFiltersReturnAllGuidesInStableOrder() {
        assertEquals(
            OfflineExerciseCatalog.items.map(CatalogExercise::id),
            OfflineExerciseCatalog.search(CatalogQuery()).map(CatalogExercise::id),
        )
    }

    private companion object {
        val ORIGINAL_KEYS = setOf(
            "chair-squat",
            "bodyweight-squat",
            "reverse-lunge",
            "goblet-squat",
            "leg-press",
            "glute-bridge",
            "dumbbell-romanian-deadlift",
            "incline-push-up",
            "dumbbell-bench-press",
            "dumbbell-overhead-press",
            "one-arm-dumbbell-row",
            "resistance-band-row",
            "lat-pulldown",
            "prone-w-raise",
            "dead-bug",
            "bird-dog",
        )
    }
}
