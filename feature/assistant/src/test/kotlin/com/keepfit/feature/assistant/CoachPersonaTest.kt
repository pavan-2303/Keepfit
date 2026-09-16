package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.conversation.CoachPersona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachPersonaTest {
    @Test
    fun builtInCoachesHaveStableDistinctInstructionsAndSharedSafetyBoundary() {
        val coaches = CoachPersona.entries

        assertEquals(listOf("mira", "rook", "atlas"), coaches.map { it.id })
        assertEquals(coaches.size, coaches.map { it.systemInstruction }.toSet().size)
        coaches.forEach { coach ->
            assertTrue(coach.systemInstruction.contains("general fitness", ignoreCase = true))
            assertTrue(coach.systemInstruction.contains("Do not diagnose", ignoreCase = true))
            assertTrue(coach.systemInstruction.contains("respect", ignoreCase = true))
            assertTrue(coach.systemInstruction.contains("Never shame or humiliate", ignoreCase = true))
        }
    }

    @Test
    fun unknownPersistedCoachFallsBackToWarmCoach() {
        assertEquals(CoachPersona.MIRA, CoachPersona.fromId("removed-coach"))
    }
}
