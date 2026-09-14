package com.keepfit.feature.workouts

import com.keepfit.feature.workouts.catalog.CatalogExercise
import com.keepfit.feature.workouts.catalog.CatalogLibraryService
import com.keepfit.feature.workouts.catalog.CatalogOrigin
import com.keepfit.feature.workouts.catalog.CatalogQuery
import com.keepfit.feature.workouts.catalog.CatalogSearchResult
import com.keepfit.feature.workouts.catalog.ExerciseCatalogProvider
import com.keepfit.feature.workouts.catalog.ExerciseLibraryGateway
import com.keepfit.feature.workouts.catalog.NewCatalogExercise
import com.keepfit.feature.workouts.data.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCatalogViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun liveSearchRetainsSubmittedQueryAndResult() = runTest(dispatcher) {
        val query = CatalogQuery(text = "row", movementArea = "back")
        val expected = CatalogSearchResult.Success(emptyList(), 0, false)
        val viewModel = ExerciseCatalogViewModel(
            provider = ExerciseCatalogProvider { submitted ->
                assertEquals(query, submitted)
                expected
            },
            libraryService = CatalogLibraryService(FakeGateway()),
        )

        viewModel.searchLive(query)
        advanceUntilIdle()

        assertEquals(query, viewModel.liveState.value.query)
        assertEquals(expected, viewModel.liveState.value.result)
        assertEquals(false, viewModel.liveState.value.isLoading)
    }

    @Test
    fun duplicateOfflineAdditionExplainsThatTheGuideAlreadyExists() = runTest(dispatcher) {
        val guide = guide()
        val gateway = FakeGateway(
            active = listOf(
                Exercise("existing", guide.name.uppercase(), "Custom", null, null, false),
            ),
        )
        val viewModel = ExerciseCatalogViewModel(
            provider = ExerciseCatalogProvider { CatalogSearchResult.Success(emptyList(), 0, false) },
            libraryService = CatalogLibraryService(gateway),
        )

        viewModel.addOfflineGuide(guide)
        advanceUntilIdle()

        assertEquals("Guide Exercise is already in your library.", viewModel.message.value)
        assertEquals(emptyList<NewCatalogExercise>(), gateway.added)
    }

    private class FakeGateway(
        private val active: List<Exercise> = emptyList(),
    ) : ExerciseLibraryGateway {
        val added = mutableListOf<NewCatalogExercise>()

        override suspend fun activeExercises(): List<Exercise> = active

        override suspend fun add(exercise: NewCatalogExercise) {
            added += exercise
        }
    }

    private fun guide() = CatalogExercise(
        id = "keepfit:guide",
        name = "Guide Exercise",
        movementArea = "Push",
        bodyParts = listOf("Chest"),
        targetMuscles = listOf("Chest"),
        secondaryMuscles = emptyList(),
        equipment = listOf("Bodyweight"),
        instructions = listOf("Move with control."),
        isBodyweight = true,
        demoUrl = null,
        origin = CatalogOrigin.KEEPFIT_OFFLINE,
        attribution = "Original Keepfit guide",
    )
}
