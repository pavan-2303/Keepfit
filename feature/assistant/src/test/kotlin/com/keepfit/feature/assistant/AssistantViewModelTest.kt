package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantConnectionStatus
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.FakeAssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAssistantRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAssistantRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsConnectedAfterSuccessfulConnectionTest() = runTest(dispatcher) {
        val viewModel = AssistantViewModel(repository)

        viewModel.testConnection(sampleConfig())
        advanceUntilIdle()

        assertEquals(AssistantConnectionStatus.CONNECTED, viewModel.uiState.value.connectionStatus)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reportsErrorAfterFailedConnectionTest() = runTest(dispatcher) {
        repository.connectionResult = Result.failure(IllegalStateException("Unable to reach Ollama."))
        val viewModel = AssistantViewModel(repository)

        viewModel.testConnection(sampleConfig())
        advanceUntilIdle()

        assertEquals(AssistantConnectionStatus.ERROR, viewModel.uiState.value.connectionStatus)
        assertEquals("Unable to reach Ollama.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun appendsUserAndAssistantMessagesAfterSuccessfulSend() = runTest(dispatcher) {
        repository.chatResult = Result.success(
            AssistantChatMessage(
                id = "assistant-1",
                role = AssistantMessageRole.ASSISTANT,
                content = "Your recent training looks consistent.",
                createdAtUtcEpochMillis = 2L,
            ),
        )
        val viewModel = AssistantViewModel(repository)

        viewModel.updateDraftMessage("How am I doing?")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals(AssistantMessageRole.USER, viewModel.uiState.value.messages.first().role)
        assertEquals(AssistantMessageRole.ASSISTANT, viewModel.uiState.value.messages.last().role)
    }

    @Test
    fun keepsDraftMessageWhenSendFails() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("Endpoint unavailable."))
        val viewModel = AssistantViewModel(repository)

        viewModel.updateDraftMessage("Summarize this week.")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals("Summarize this week.", viewModel.uiState.value.draftMessage)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("Endpoint unavailable.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun retriesFailedDraftWithoutRequiringRetyping() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("Endpoint unavailable."))
        val viewModel = AssistantViewModel(repository)

        viewModel.updateDraftMessage("Try again with last draft.")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        repository.chatResult = Result.success(
            AssistantChatMessage(
                id = "assistant-2",
                role = AssistantMessageRole.ASSISTANT,
                content = "Retry succeeded.",
                createdAtUtcEpochMillis = 3L,
            ),
        )

        viewModel.retryLastMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun sampleConfig() = AssistantRuntimeConfig(
        baseUrl = "https://ollama.com/api",
        generalChatModelName = "mistral-large-3:675b",
        reasoningModelName = "qwen3.5:397b",
        apiKey = "secret-token",
    )
}
