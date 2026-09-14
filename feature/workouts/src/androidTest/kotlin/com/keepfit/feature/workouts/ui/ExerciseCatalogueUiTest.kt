package com.keepfit.feature.workouts.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.keepfit.feature.workouts.LiveCatalogUiState
import com.keepfit.feature.workouts.catalog.CatalogExercise
import com.keepfit.feature.workouts.catalog.CatalogFailureKind
import com.keepfit.feature.workouts.catalog.CatalogOrigin
import com.keepfit.feature.workouts.catalog.CatalogQuery
import com.keepfit.feature.workouts.catalog.CatalogSearchResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExerciseCatalogueUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sourceRailShowsOwnershipAndChangesSource() {
        var selected = ExerciseSource.PERSONAL
        composeRule.setContent {
            MaterialTheme {
                ExerciseSourceRail(
                    selected = selected,
                    personalCount = 3,
                    onSelect = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("3 SAVED").assertIsDisplayed()
        composeRule.onNodeWithText("40 OWNED").assertIsDisplayed()
        composeRule.onNodeWithText("VIEW-ONLY").assertIsDisplayed()
        composeRule.onNodeWithText("Live demos").performClick()

        composeRule.runOnIdle { assertEquals(ExerciseSource.LIVE, selected) }
    }

    @Test
    fun offlineGuideSearchesAndAddsAnOwnedExercise() {
        var added: CatalogExercise? = null
        composeRule.setContent {
            MaterialTheme {
                OfflineExerciseGuide(
                    personalExerciseNames = emptySet(),
                    onAdd = { added = it },
                )
            }
        }

        composeRule.onNodeWithText("Search the offline guide").performTextInput("Goblet")
        composeRule.onNodeWithText("1 guides").assertIsDisplayed()
        composeRule.onNodeWithText("Goblet Squat").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Add")[0].performClick()

        composeRule.runOnIdle { assertEquals("Goblet Squat", added?.name) }
    }

    @Test
    fun liveIdleAndFailureStatesKeepPrivacyAndRecoveryClear() {
        composeRule.setContent {
            MaterialTheme {
                LiveExerciseCatalogue(
                    state = LiveCatalogUiState(
                        query = CatalogQuery(text = "row"),
                        result = CatalogSearchResult.Failure(
                            CatalogFailureKind.OFFLINE,
                            "No network connection. The offline guide is still available.",
                        ),
                    ),
                    onSearch = {},
                )
            }
        }

        composeRule.onNodeWithText("LIVE / VIEW-ONLY PROTOTYPE").assertIsDisplayed()
        composeRule.onNodeWithText("You are offline").assertIsDisplayed()
        composeRule.onNodeWithText("No network connection. The offline guide is still available.")
            .assertIsDisplayed()
    }

    @Test
    fun liveSearchSendsOnlyEnteredCatalogueFilters() {
        var submitted: CatalogQuery? = null
        composeRule.setContent {
            MaterialTheme {
                LiveExerciseCatalogue(
                    state = LiveCatalogUiState(),
                    onSearch = { submitted = it },
                )
            }
        }

        composeRule.onNodeWithText("Exercise name").performTextInput("bench")
        composeRule.onNodeWithText("Filter body area, muscle, equipment").performClick()
        composeRule.onNodeWithText("Body area, e.g. chest").performTextInput("chest")
        composeRule.onNodeWithText("Target muscle, e.g. pectorals").performTextInput("pectorals")
        composeRule.onNodeWithText("Equipment, e.g. dumbbell").performTextInput("barbell")
        composeRule.onNodeWithText("Search live demos").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(
                CatalogQuery("bench", "chest", "pectorals", "barbell"),
                submitted,
            )
        }
    }

    @Test
    fun liveMissingMediaStillOpensAttributedInstructions() {
        val exercise = CatalogExercise(
            id = "exercisedb:row",
            name = "Cable Row",
            movementArea = "Back",
            bodyParts = listOf("Back"),
            targetMuscles = listOf("Lats"),
            secondaryMuscles = emptyList(),
            equipment = listOf("Cable"),
            instructions = listOf("Sit tall.", "Pull toward the ribs."),
            isBodyweight = false,
            demoUrl = null,
            origin = CatalogOrigin.EXERCISE_DB_LIVE,
            attribution = "ExerciseDB by AscendAPI",
        )
        composeRule.setContent {
            MaterialTheme {
                LiveExerciseCatalogue(
                    state = LiveCatalogUiState(
                        result = CatalogSearchResult.Success(listOf(exercise), 1, false),
                    ),
                    onSearch = {},
                )
            }
        }

        composeRule.onNodeWithText("Instructions").performScrollTo().performClick()
        composeRule.onNodeWithText("LIVE DEMO • VIEW-ONLY").assertIsDisplayed()
        composeRule.onNodeWithText("Pull toward the ribs.").assertIsDisplayed()
        composeRule.onNodeWithText("ExerciseDB by AscendAPI. Loaded live without memory or disk caching. General form reference.")
            .assertIsDisplayed()
    }
}
