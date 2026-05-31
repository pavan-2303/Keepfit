package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.validateAssistantRuntimeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantSettingsValidationTest {
    @Test
    fun rejectsBlankGeneralChatModelName() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "https://ollama.com/api",
            generalChatModelName = "   ",
            reasoningModelName = "qwen3.5:397b",
            apiKey = "secret-token",
        )

        assertTrue(result.isFailure)
        assertEquals("Assistant chat model name is required.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsMalformedUrl() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "not-a-url",
            generalChatModelName = "mistral-large-3:675b",
            reasoningModelName = "qwen3.5:397b",
            apiKey = "secret-token",
        )

        assertTrue(result.isFailure)
        assertEquals("Assistant endpoint must be a valid http or https URL.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsOllamaCloudEndpointWithoutApiKey() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "https://ollama.com/api",
            generalChatModelName = "mistral-large-3:675b",
            reasoningModelName = "qwen3.5:397b",
            apiKey = "",
        )

        assertTrue(result.isFailure)
        assertEquals("Ollama Cloud requires an API key.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsBlankReasoningModelName() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "https://ollama.com/api",
            generalChatModelName = "mistral-large-3:675b",
            reasoningModelName = "   ",
            apiKey = "secret-token",
        )

        assertTrue(result.isFailure)
        assertEquals("Assistant reasoning model name is required.", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsCloudSuffixModelForDirectOllamaCloudApi() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "https://ollama.com/api",
            generalChatModelName = "mistral-large-3:675b-cloud",
            reasoningModelName = "qwen3.5:397b",
            apiKey = "secret-token",
        )

        assertTrue(result.isFailure)
        assertEquals(
            "Use the direct Ollama Cloud model name without '-cloud'.",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun acceptsOllamaCloudEndpointWithApiKeyAndTwoModelRoles() {
        val result = validateAssistantRuntimeConfig(
            baseUrl = "https://ollama.com/api",
            generalChatModelName = "mistral-large-3:675b",
            reasoningModelName = "qwen3.5:397b",
            apiKey = "  secret-token  ",
        )

        assertEquals(
            Result.success(
                AssistantRuntimeConfig(
                    baseUrl = "https://ollama.com/api",
                    generalChatModelName = "mistral-large-3:675b",
                    reasoningModelName = "qwen3.5:397b",
                    apiKey = "secret-token",
                ),
            ),
            result,
        )
    }
}
