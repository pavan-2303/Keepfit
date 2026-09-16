package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.conversation.ConversationMemoryBuilder
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMemoryBuilderTest {
    @Test
    fun keepsTwelveRecentMessagesAndCapsDeterministicEarlierMemory() {
        val messages = (1..20).map { index ->
            AssistantChatMessage(
                id = index.toString(),
                role = if (index % 2 == 0) AssistantMessageRole.ASSISTANT else AssistantMessageRole.USER,
                content = "Message $index ${"detail ".repeat(30)}",
                createdAtUtcEpochMillis = index.toLong(),
            )
        }

        val result = ConversationMemoryBuilder.build(messages)

        assertEquals((9..20).map(Int::toString), result.recentMessages.map { it.id })
        assertTrue(result.summary.orEmpty().length <= ConversationMemoryBuilder.MAX_SUMMARY_CHARACTERS)
        assertTrue(result.summary.orEmpty().contains("Message 8"))
        assertEquals(result, ConversationMemoryBuilder.build(messages))
    }

    @Test
    fun memoryClearTimestampExcludesEarlierTranscriptFromSummaryAndRecentWindow() {
        val messages = (1..16).map { index ->
            AssistantChatMessage(
                id = index.toString(),
                role = AssistantMessageRole.USER,
                content = "Message $index",
                createdAtUtcEpochMillis = index.toLong(),
            )
        }

        val result = ConversationMemoryBuilder.build(messages, memoryClearedAtUtcEpochMillis = 13L)

        assertEquals(listOf("14", "15", "16"), result.recentMessages.map { it.id })
        assertFalse(result.summary.orEmpty().contains("Message 13"))
    }
}
