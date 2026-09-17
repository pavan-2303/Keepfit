package com.keepfit.core.media

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreExerciseGuidanceCatalogTest {
    @Test
    fun packHasTwentyFiveUniqueRightsCompleteCatalogueMappings() {
        val entries = CoreExerciseGuidanceCatalog.entries

        assertEquals(25, entries.size)
        assertEquals(entries.size, entries.map(ExerciseGuidance::exerciseId).distinct().size)
        assertEquals(entries.size, entries.map(ExerciseGuidance::sourceId).distinct().size)
        entries.forEach { guidance ->
            assertEquals(guidance.exerciseId, UUID.fromString(guidance.exerciseId).toString())
            assertTrue(guidance.exerciseName.isNotBlank())
            assertTrue(guidance.primaryCue.isNotBlank())
            assertTrue(guidance.safetyCue.isNotBlank())
            assertEquals("Keepfit movement figures v1", guidance.rights.artworkFamily)
            assertEquals("Keepfit", guidance.rights.creator)
            assertEquals("Original code-native artwork", guidance.rights.rightsBasis)
            assertEquals("2026-09-15", guidance.rights.reviewedOn)
        }
    }

    @Test
    fun lookupIsStableAndUnknownExercisesStayTextOnly() {
        val benchPress = CoreExerciseGuidanceCatalog.find(
            "5ca9f46f-1ae9-5ff8-9627-32cd73c56a13",
        )

        assertNotNull(benchPress)
        assertEquals("Barbell bench press", benchPress?.exerciseName)
        assertEquals(GuidanceMotion.BENCH_PRESS, benchPress?.motion)
        assertEquals(null, CoreExerciseGuidanceCatalog.find("unknown"))
    }

    @Test
    fun everyMotionHasBoundedStartAndFinishPoses() {
        CoreExerciseGuidanceCatalog.entries.forEach { guidance ->
            listOf(guidance.startPose, guidance.finishPose).forEach { pose ->
                pose.joints.forEach { point ->
                    assertTrue(point.x in 0f..1f)
                    assertTrue(point.y in 0f..1f)
                }
            }
        }
    }

    @Test
    fun poseInterpolationIsBoundedAndClampsProgress() {
        val guidance = CoreExerciseGuidanceCatalog.entries.first()

        val beforeStart = guidance.startPose.interpolateTo(guidance.finishPose, -1f)
        val midpoint = guidance.startPose.interpolateTo(guidance.finishPose, .5f)
        val afterFinish = guidance.startPose.interpolateTo(guidance.finishPose, 2f)

        assertEquals(guidance.startPose, beforeStart)
        assertEquals(guidance.finishPose, afterFinish)
        midpoint.joints.forEach { point ->
            assertTrue(point.x in 0f..1f)
            assertTrue(point.y in 0f..1f)
        }
    }
}
