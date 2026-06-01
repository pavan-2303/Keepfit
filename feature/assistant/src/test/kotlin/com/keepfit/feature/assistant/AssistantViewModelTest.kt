package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantConnectionStatus
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutDay
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutExercise
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantPlanApplier
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.FakeAssistantRepository
import java.time.DayOfWeek
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
    private lateinit var planApplier: FakeAssistantPlanApplier

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAssistantRepository()
        planApplier = FakeAssistantPlanApplier()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsConnectedAfterSuccessfulConnectionTest() = runTest(dispatcher) {
        val viewModel = AssistantViewModel(repository, planApplier)

        viewModel.testConnection(sampleConfig())
        advanceUntilIdle()

        assertEquals(AssistantConnectionStatus.CONNECTED, viewModel.uiState.value.connectionStatus)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reportsErrorAfterFailedConnectionTest() = runTest(dispatcher) {
        repository.connectionResult = Result.failure(IllegalStateException("Unable to reach Ollama."))
        val viewModel = AssistantViewModel(repository, planApplier)

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
        val viewModel = AssistantViewModel(repository, planApplier)

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
        val viewModel = AssistantViewModel(repository, planApplier)

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
        val viewModel = AssistantViewModel(repository, planApplier)

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

    @Test
    fun appendsProgressSummaryAsAssistantMessage() = runTest(dispatcher) {
        repository.summaryResult = Result.success(
            com.keepfit.feature.assistant.data.AssistantProgressSummary(
                title = "Progress summary",
                summary = "Training consistency is improving and weight is trending down.",
            ),
        )
        val viewModel = AssistantViewModel(repository, planApplier)

        viewModel.generateProgressSummary(sampleConfig())
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(
            "Progress summary\n\nTraining consistency is improving and weight is trending down.",
            viewModel.uiState.value.messages.single().content,
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun storesDraftPlanForReviewAfterSuccessfulRequest() = runTest(dispatcher) {
        repository.draftPlanResult = Result.success(sampleDraftPlan())
        val viewModel = AssistantViewModel(repository, planApplier)

        viewModel.updateDraftMessage("I want a four day hypertrophy split.")
        viewModel.requestDraftPlan(sampleConfig())
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertEquals("Balanced Week", viewModel.uiState.value.pendingDraftPlan?.name)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun dismissesPendingDraftPlan() = runTest(dispatcher) {
        repository.draftPlanResult = Result.success(sampleDraftPlan())
        val viewModel = AssistantViewModel(repository, planApplier)

        viewModel.requestDraftPlan(sampleConfig())
        advanceUntilIdle()
        viewModel.dismissDraftPlan()

        assertNull(viewModel.uiState.value.pendingDraftPlan)
    }

    @Test
    fun appliesPendingDraftPlanAndAppendsConfirmationMessage() = runTest(dispatcher) {
        repository.draftPlanResult = Result.success(sampleDraftPlan())
        val viewModel = AssistantViewModel(repository, planApplier)

        viewModel.requestDraftPlan(sampleConfig())
        advanceUntilIdle()
        viewModel.applyDraftPlan()
        advanceUntilIdle()

        assertEquals("Balanced Week", planApplier.appliedPlans.single().name)
        assertNull(viewModel.uiState.value.pendingDraftPlan)
        assertTrue(viewModel.uiState.value.messages.last().content.contains("applied"))
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun sampleConfig() = AssistantRuntimeConfig(
        baseUrl = "https://ollama.com/api",
        generalChatModelName = "mistral-large-3:675b",
        reasoningModelName = "qwen3.5:397b",
        apiKey = "secret-token",
    )

    private fun sampleDraftPlan() = AssistantDraftWorkoutPlan(
        name = "Balanced Week",
        overview = "A four-day split with two upper and two lower sessions.",
        days = listOf(
            AssistantDraftWorkoutDay(
                dayOfWeek = DayOfWeek.MONDAY,
                templateName = "Upper A",
                notes = "Start heavy and leave one rep in reserve.",
                exercises = listOf(
                    AssistantDraftWorkoutExercise(
                        name = "Bench Press",
                        targetSets = 4,
                        targetReps = "6-8",
                    ),
                    AssistantDraftWorkoutExercise(
                        name = "Chest-Supported Row",
                        targetSets = 4,
                        targetReps = "8-10",
                    ),
                ),
            ),
        ),
    )

    private class FakeAssistantPlanApplier : AssistantPlanApplier {
        val appliedPlans = mutableListOf<AssistantDraftWorkoutPlan>()
        var applyResult: Result<Unit> = Result.success(Unit)

        override suspend fun applyDraftPlan(draft: AssistantDraftWorkoutPlan): Result<Unit> {
            if (applyResult.isSuccess) {
                appliedPlans += draft
            }
            return applyResult
        }
    }
}
