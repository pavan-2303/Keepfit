package com.keepfit.feature.workouts.catalog

import com.keepfit.feature.workouts.data.Exercise
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogLibraryServiceTest {
    @Test
    fun offlineGuideIsAddedAsAnIndependentEditableExercise() = runBlocking {
        val gateway = FakeExerciseLibraryGateway()
        val service = CatalogLibraryService(gateway)
        val guide = OfflineExerciseCatalog.items.first()

        val result = service.addOfflineGuide(guide)

        assertEquals(CatalogAddOutcome.ADDED, result)
        assertEquals(1, gateway.added.size)
        assertEquals(guide.name, gateway.added.single().name)
        assertEquals(guide.instructions.joinToString("\n"), gateway.added.single().instructions)
    }

    @Test
    fun activeExerciseNamePreventsCaseInsensitiveDuplicate() = runBlocking {
        val guide = OfflineExerciseCatalog.items.first()
        val gateway = FakeExerciseLibraryGateway(
            active = listOf(
                Exercise(
                    id = "existing",
                    name = guide.name.lowercase(),
                    muscleGroup = "Custom",
                    instructions = null,
                    notes = null,
                    isBodyweight = false,
                ),
            ),
        )
        val service = CatalogLibraryService(gateway)

        assertEquals(CatalogAddOutcome.ALREADY_PRESENT, service.addOfflineGuide(guide))
        assertEquals(emptyList<NewCatalogExercise>(), gateway.added)
    }

    @Test(expected = IllegalArgumentException::class)
    fun liveProviderRowsCannotBeAdded() {
        runBlocking {
            val live = OfflineExerciseCatalog.items.first().copy(origin = CatalogOrigin.EXERCISE_DB_LIVE)
            CatalogLibraryService(FakeExerciseLibraryGateway()).addOfflineGuide(live)
        }
    }

    private class FakeExerciseLibraryGateway(
        private val active: List<Exercise> = emptyList(),
    ) : ExerciseLibraryGateway {
        val added = mutableListOf<NewCatalogExercise>()

        override suspend fun activeExercises(): List<Exercise> = active

        override suspend fun add(exercise: NewCatalogExercise) {
            added += exercise
        }
    }
}
