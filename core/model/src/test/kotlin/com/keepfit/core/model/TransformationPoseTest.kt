package com.keepfit.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformationPoseTest {
    @Test
    fun catalogueHasFifteenStableUniqueKeysAndFourBasicDefaults() {
        assertEquals(15, TransformationPose.entries.size)
        assertEquals(15, TransformationPose.entries.map(TransformationPose::key).distinct().size)
        assertEquals(
            listOf(
                "front_relaxed",
                "right_side_relaxed",
                "back_relaxed",
                "left_side_relaxed",
            ),
            TransformationPose.defaultPoses.map(TransformationPose::key),
        )
        assertTrue(TransformationPose.defaultPoses.all { it.group == TransformationPoseGroup.BASIC })
    }

    @Test
    fun lookupUsesPersistedKeyWithoutGuessingUnknownValues() {
        assertEquals(TransformationPose.SIDE_CHEST_RIGHT, TransformationPose.fromKey("side_chest_right"))
        assertNull(TransformationPose.fromKey("FRONT"))
        assertNull(TransformationPose.fromKey("unknown"))
    }
}
