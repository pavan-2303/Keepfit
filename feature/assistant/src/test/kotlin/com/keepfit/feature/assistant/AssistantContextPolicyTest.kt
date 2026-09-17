package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantContextPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantContextPolicyTest {
    private val policy = AssistantContextPolicy()

    @Test
    fun personalProgressQuestionUsesKeepfitContext() {
        assertTrue(policy.shouldIncludeLocalContext("How has my workout consistency changed this month?"))
        assertTrue(policy.shouldIncludeLocalContext("Am I getting stronger on my recent exercises?"))
        assertTrue(policy.shouldIncludeLocalContext("What do my nutrition logs suggest?"))
    }

    @Test
    fun generalKnowledgeQuestionDoesNotSendLocalContext() {
        assertFalse(policy.shouldIncludeLocalContext("What is progressive overload?"))
        assertFalse(policy.shouldIncludeLocalContext("Explain the difference between sets and reps"))
        assertFalse(policy.shouldIncludeLocalContext("Give me a bodyweight warm-up"))
    }
}
