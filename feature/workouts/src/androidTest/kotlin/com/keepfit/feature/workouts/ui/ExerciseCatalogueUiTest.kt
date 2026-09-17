package com.keepfit.feature.workouts.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.keepfit.core.media.CoreExerciseGuidanceCatalog
import com.keepfit.feature.workouts.data.Exercise
import java.io.FileInputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExerciseCatalogueUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exerciseEditorShowsEverySupportedField() {
        composeRule.setContent {
            MaterialTheme {
                ExerciseEditor(
                    exercise = null,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _, _ -> },
                )
            }
        }

        listOf(
            "Exercise name",
            "Body area",
            "Equipment (optional)",
            "Primary target (optional)",
            "Secondary targets (optional)",
            "Instructions or description",
            "Personal notes (optional)",
            "Bodyweight exercise",
            "Attach demo",
        ).forEach { label -> composeRule.onNodeWithText(label).fetchSemanticsNode() }
    }

    @Test
    fun personalLibrarySearchesExerciseMetadataLocally() {
        val dumbbellCurl = exercise(
            id = "curl",
            name = "Dumbbell curl",
            equipment = "dumbbell",
            targetMuscle = "biceps",
        )
        val squat = exercise(id = "squat", name = "Bodyweight squat", equipment = "body weight")
        var visible by mutableStateOf(listOf(dumbbellCurl, squat))
        var submittedQuery = ""
        composeRule.setContent {
            MaterialTheme {
                ExerciseLibrary(
                    exercises = visible,
                    onSearch = { query ->
                        submittedQuery = query
                        visible = if (query.isBlank()) listOf(dumbbellCurl, squat) else listOf(dumbbellCurl)
                    },
                    onSave = { _, _, _, _, _, _, _, _, _, _ -> },
                    onArchive = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Search your exercises").performTextInput("dumbbell")
        composeRule.onNodeWithText("1 exercise").assertIsDisplayed()
        composeRule.onNodeWithText("Dumbbell curl").assertIsDisplayed()
        composeRule.onNodeWithText("Dumbbell • Biceps").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("dumbbell", submittedQuery) }
    }

    @Test
    fun exerciseDetailsPrioritizeStructuredMovementGuidance() {
        composeRule.setContent {
            MaterialTheme {
                PersonalExerciseDetailDialog(
                    exercise = exercise(
                        id = "row",
                        name = "Cable row",
                        equipment = "cable",
                        targetMuscle = "lats",
                        secondaryMuscles = "biceps, rear delts",
                    ),
                    onEdit = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Exercise details").assertIsDisplayed()
        composeRule.onNodeWithText("Movement profile").assertIsDisplayed()
        composeRule.onNodeWithText("Body area").assertIsDisplayed()
        composeRule.onNodeWithText("Upper arms").assertIsDisplayed()
        composeRule.onNodeWithText("Equipment").assertIsDisplayed()
        composeRule.onNodeWithText("Cable").assertIsDisplayed()
        composeRule.onNodeWithText("Primary target").assertIsDisplayed()
        composeRule.onNodeWithText("Lats").assertIsDisplayed()
        composeRule.onNodeWithText("Also works").assertIsDisplayed()
        composeRule.onNodeWithText("Biceps, rear delts").assertIsDisplayed()
        composeRule.onNodeWithText("How to perform").assertIsDisplayed()
        composeRule.onNodeWithText("Exercises Dataset • MIT metadata and instructions").assertDoesNotExist()
        composeRule.onNodeWithText("Movement guide").assertDoesNotExist()
    }

    @Test
    fun exerciseDetailsKeepTechniqueReadableAtCompactWidthWithLargeText() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        PersonalExerciseDetailDialog(
                            exercise = exercise(
                                id = "row",
                                name = "Cable row",
                                equipment = "cable",
                                targetMuscle = "lats",
                                secondaryMuscles = "biceps, rear delts",
                            ),
                            onEdit = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("How to perform").assertIsDisplayed()
        composeRule.onNodeWithText("Move with control.").assertIsDisplayed()
        composeRule.onNodeWithText("Edit exercise").assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun supportedExerciseDetailsShowOriginalReplayableGuidance() {
        composeRule.setContent {
            MaterialTheme {
                PersonalExerciseDetailDialog(
                    exercise = exercise(
                        id = "5ca9f46f-1ae9-5ff8-9627-32cd73c56a13",
                        name = "Barbell bench press",
                        equipment = "barbell",
                        targetMuscle = "pectorals",
                    ),
                    onEdit = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Movement guide").assertIsDisplayed()
        composeRule.onNodeWithText("Play movement").assertIsDisplayed()
        composeRule.onNodeWithText("Play movement").performClick()
        composeRule.onNodeWithContentDescription("Barbell bench press movement diagram").assertIsDisplayed()
        composeRule.onNodeWithText("Original Keepfit movement figure").assertIsDisplayed()
    }

    @Test
    fun guideRemainsUsableAtCompactWidthWithLargeText() {
        val guidance = CoreExerciseGuidanceCatalog.entries.first()
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        ExerciseMovementGuide(guidance)
                    }
                }
            }
        }

        composeRule.onNodeWithText("Movement guide").assertIsDisplayed()
        composeRule.onNodeWithText("Play movement").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Barbell bench press movement diagram",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Original Keepfit movement figure").assertIsDisplayed()
    }

    @Test
    fun replayRetainsStaticGuidanceWhenSystemAnimationsAreDisabled() {
        val previousScale = shellCommand("settings get global animator_duration_scale").trim()
        try {
            shellCommand("settings put global animator_duration_scale 0")
            composeRule.setContent {
                MaterialTheme {
                    ExerciseMovementGuide(CoreExerciseGuidanceCatalog.entries.first())
                }
            }

            composeRule.onNodeWithText("Play movement").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Start in ink · finish in outline").assertIsDisplayed()
            composeRule.onNodeWithContentDescription(
                "Barbell bench press movement diagram",
            ).assertIsDisplayed()
        } finally {
            shellCommand("settings put global animator_duration_scale $previousScale")
        }
    }

    @Test
    fun largePersonalCatalogueCanScrollToTheLastLazyRow() {
        val exercises = (0 until 1316).map { index ->
            exercise(id = index.toString(), name = "Exercise $index")
        }
        composeRule.setContent {
            MaterialTheme {
                ExerciseLibrary(
                    exercises = exercises,
                    onSearch = {},
                    onSave = { _, _, _, _, _, _, _, _, _, _ -> },
                    onArchive = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("1,316 exercises").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Exercise 1315"))
        composeRule.onNodeWithText("Exercise 1315").assertIsDisplayed()
    }

    private fun exercise(
        id: String,
        name: String,
        equipment: String? = null,
        targetMuscle: String? = null,
        secondaryMuscles: String? = null,
    ) = Exercise(
        id = id,
        name = name,
        muscleGroup = "upper arms",
        instructions = "Move with control.",
        notes = null,
        isBodyweight = equipment == "body weight",
        source = null,
        sourceId = null,
        equipment = equipment,
        targetMuscle = targetMuscle,
        secondaryMuscles = secondaryMuscles,
    )

    private fun shellCommand(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }
}
