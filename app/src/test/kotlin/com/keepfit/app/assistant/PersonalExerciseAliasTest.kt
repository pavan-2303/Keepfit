package com.keepfit.app.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PersonalExerciseAliasTest {
    @Test
    fun aliasesAreStableOpaqueAndDistinct() {
        val first = PersonalExerciseAlias.forId("private-room-uuid")

        assertEquals(first, PersonalExerciseAlias.forId("private-room-uuid"))
        assertNotEquals(first, PersonalExerciseAlias.forId("another-room-uuid"))
        assertFalse(first.contains("private-room-uuid"))
        assertEquals(33, first.length)
    }
}
