package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.parseOllamaChatResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaResponseParsingTest {
    @Test
    fun parsesAssistantReplyFromChatPayload() {
        val json = """
            {
              "message": {
                "role": "assistant",
                "content": "Keep your volume steady this week."
              }
            }
        """.trimIndent()

        val result = parseOllamaChatResponse(json, createdAtUtcEpochMillis = 42L)

        assertTrue(result.isSuccess)
        val message = result.getOrThrow()
        assertEquals(AssistantMessageRole.ASSISTANT, message.role)
        assertEquals("Keep your volume steady this week.", message.content)
        assertEquals(42L, message.createdAtUtcEpochMillis)
    }

    @Test
    fun rejectsChatPayloadWithoutAssistantContent() {
        val json = """
            {
              "message": {
                "role": "assistant",
                "content": ""
              }
            }
        """.trimIndent()

        val result = parseOllamaChatResponse(json, createdAtUtcEpochMillis = 42L)

        assertTrue(result.isFailure)
        assertEquals(
            "Assistant response did not include a message.",
            result.exceptionOrNull()?.message,
        )
    }
}
