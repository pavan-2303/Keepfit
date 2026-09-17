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
import com.keepfit.feature.assistant.access.AssistantAccessController
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.OpenRouterKeyMetadata
import com.keepfit.feature.assistant.coaching.AssistantDraftSnapshot
import com.keepfit.feature.assistant.coaching.AssistantDraftStore
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal
import com.keepfit.feature.assistant.coaching.CoachingProposalApplier
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import com.keepfit.feature.assistant.conversation.AssistantConversation
import com.keepfit.feature.assistant.conversation.AssistantConversationRepository
import com.keepfit.feature.assistant.conversation.CoachPersona
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
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
    private lateinit var conversationRepository: FakeConversationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAssistantRepository()
        planApplier = FakeAssistantPlanApplier()
        conversationRepository = FakeConversationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsConnectedAfterSuccessfulConnectionTest() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.testConnection(sampleConfig())
        advanceUntilIdle()

        assertEquals(AssistantConnectionStatus.CONNECTED, viewModel.uiState.value.connectionStatus)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun reportsErrorAfterFailedConnectionTest() = runTest(dispatcher) {
        repository.connectionResult = Result.failure(IllegalStateException("Unable to reach Ollama."))
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.updateDraftMessage("How am I doing?")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals(AssistantMessageRole.USER, viewModel.uiState.value.messages.first().role)
        assertEquals(AssistantMessageRole.ASSISTANT, viewModel.uiState.value.messages.last().role)
    }

    @Test
    fun clearsComposerWhenSendFailsAndKeepsRetrySeparate() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("Endpoint unavailable."))
        val viewModel = viewModel()

        viewModel.updateDraftMessage("Summarize this week.")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.draftMessage)
        assertTrue(viewModel.uiState.value.canRetryLastMessage)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("Endpoint unavailable.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun retriesFailedDraftWithoutRequiringRetyping() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("Endpoint unavailable."))
        val viewModel = viewModel()

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
    fun choosingCoachDoesNotCreateConversationUntilFirstQuestion() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.chooseCoach(CoachPersona.ROOK)
        advanceUntilIdle()

        assertEquals(CoachPersona.ROOK, viewModel.uiState.value.selectedCoach)
        assertEquals(false, viewModel.uiState.value.showCoachPicker)
        assertTrue(conversationRepository.createdCoaches.isEmpty())

        viewModel.updateDraftMessage("What should I train today?")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals(CoachPersona.ROOK, conversationRepository.createdCoaches.single())
        assertEquals(CoachPersona.ROOK, viewModel.uiState.value.activeConversation?.coach)
    }

    @Test
    fun legacyProposalDraftDoesNotPopulateGeneralChatComposer() = runTest(dispatcher) {
        val store = FakeDraftStore(
            AssistantDraftSnapshot("Old proposal prompt", CoachingIntent.WEEKLY_PLAN, null),
        )

        val viewModel = viewModel(draftStore = store)

        assertEquals("", viewModel.uiState.value.draftMessage)
    }

    @Test
    fun successfulSendUsesBoundedRepositoryHistoryAndPersistsCompletedTurn() = runTest(dispatcher) {
        conversationRepository.requestHistory = listOf(
            AssistantChatMessage("system-rook", AssistantMessageRole.SYSTEM, "Rook persona", 0L),
        )
        repository.chatResult = Result.success(
            AssistantChatMessage("coach-1", AssistantMessageRole.ASSISTANT, "Do the next useful thing.", 2L),
        )
        val viewModel = viewModel()
        viewModel.chooseCoach(CoachPersona.ROOK)
        advanceUntilIdle()

        viewModel.updateDraftMessage("What should I do today?")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()

        assertEquals(conversationRepository.requestHistory, repository.lastChatHistory)
        assertEquals(1, conversationRepository.savedTurns.size)
        assertEquals("What should I do today?", conversationRepository.savedTurns.single().second.content)
    }

    @Test
    fun managesConversationTitleMemoryAndDeletionThroughRepository() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.chooseCoach(CoachPersona.ATLAS)
        viewModel.updateDraftMessage("Start an Atlas conversation")
        viewModel.sendDraftMessage(sampleConfig())
        advanceUntilIdle()
        val conversationId = requireNotNull(viewModel.uiState.value.activeConversationId)

        viewModel.renameConversation(conversationId, "Strength block")
        viewModel.clearConversationMemory(conversationId)
        viewModel.deleteConversation(conversationId)
        advanceUntilIdle()

        assertEquals(listOf(conversationId to "Strength block"), conversationRepository.renamed)
        assertEquals(listOf(conversationId), conversationRepository.cleared)
        assertEquals(listOf(conversationId), conversationRepository.deleted)
        assertTrue(viewModel.uiState.value.showCoachPicker)
    }

    @Test
    fun appendsProgressSummaryAsAssistantMessage() = runTest(dispatcher) {
        repository.summaryResult = Result.success(
            com.keepfit.feature.assistant.data.AssistantProgressSummary(
                title = "Progress summary",
                summary = "Training consistency is improving and weight is trending down.",
            ),
        )
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.requestDraftPlan(sampleConfig())
        advanceUntilIdle()
        viewModel.dismissDraftPlan()

        assertNull(viewModel.uiState.value.pendingDraftPlan)
    }

    @Test
    fun appliesPendingDraftPlanAndAppendsConfirmationMessage() = runTest(dispatcher) {
        repository.draftPlanResult = Result.success(sampleDraftPlan())
        val viewModel = viewModel()

        viewModel.requestDraftPlan(sampleConfig())
        advanceUntilIdle()
        viewModel.applyDraftPlan()
        advanceUntilIdle()

        assertEquals("Balanced Week", planApplier.appliedPlans.single().name)
        assertNull(viewModel.uiState.value.pendingDraftPlan)
        assertTrue(viewModel.uiState.value.messages.last().content.contains("applied"))
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun coachingPreviewDoesNotApplyUntilExplicitApproval() = runTest(dispatcher) {
        val applier = FakeCoachingProposalApplier()
        repository.coachingResult = Result.success(sampleCoachingProposal())
        val viewModel = viewModel(coachingApplier = applier)

        viewModel.selectCoachingIntent(CoachingIntent.WEEKLY_PLAN)
        viewModel.updateDraftMessage("Keep Monday simple")
        viewModel.requestCoachingProposal(sampleConfig())
        advanceUntilIdle()

        assertEquals("A simpler week", viewModel.uiState.value.pendingCoachingProposal?.title)
        assertTrue(applier.applied.isEmpty())

        viewModel.applyCoachingProposal()
        advanceUntilIdle()

        assertEquals(1, applier.applied.size)
        assertNull(viewModel.uiState.value.pendingCoachingProposal)
    }

    @Test
    fun restoredProposalCanBeEditedWithoutApplying() = runTest(dispatcher) {
        val restored = sampleCoachingProposal()
        val store = FakeDraftStore(AssistantDraftSnapshot("", CoachingIntent.WEEKLY_PLAN, restored))
        val applier = FakeCoachingProposalApplier()
        val viewModel = viewModel(coachingApplier = applier, draftStore = store)

        viewModel.editCoachingProposal()

        assertEquals("Keep Monday simple", viewModel.uiState.value.draftMessage)
        assertNull(viewModel.uiState.value.pendingCoachingProposal)
        assertTrue(applier.applied.isEmpty())
    }

    @Test
    fun failedApprovalKeepsTheReviewedProposalRecoverable() = runTest(dispatcher) {
        val applier = FakeCoachingProposalApplier().apply {
            applyResult = Result.failure(IllegalStateException("Local plan changed"))
        }
        repository.coachingResult = Result.success(sampleCoachingProposal())
        val store = FakeDraftStore()
        val viewModel = viewModel(coachingApplier = applier, draftStore = store)
        viewModel.requestCoachingProposal(sampleConfig())
        advanceUntilIdle()

        viewModel.applyCoachingProposal()
        advanceUntilIdle()

        assertEquals("A simpler week", viewModel.uiState.value.pendingCoachingProposal?.title)
        assertEquals("Local plan changed", viewModel.uiState.value.errorMessage)
        assertEquals("A simpler week", store.read().proposal?.title)
    }

    private fun sampleConfig() = AssistantRuntimeConfig(
        baseUrl = "https://ollama.com/api",
        generalChatModelName = "mistral-large-3:675b",
        reasoningModelName = "qwen3.5:397b",
        apiKey = "secret-token",
    )

    private fun viewModel(
        coachingApplier: CoachingProposalApplier = FakeCoachingProposalApplier(),
        draftStore: AssistantDraftStore = FakeDraftStore(),
    ) = AssistantViewModel(
        repository = repository,
        conversationRepository = conversationRepository,
        planApplier = planApplier,
        accessController = FakeAccessController(),
        coachingProposalApplier = coachingApplier,
        assistantDraftStore = draftStore,
    )

    private fun sampleCoachingProposal() = CoachingProposal(
        id = "proposal-1",
        intent = CoachingIntent.WEEKLY_PLAN,
        title = "A simpler week",
        observed = "Three recent workouts were completed.",
        current = "Monday and Friday are planned.",
        proposed = "Keep Monday and move Friday.",
        reason = "This matches the requested schedule.",
        operation = CoachingProposalOperation.None,
        originalRequest = "Keep Monday simple",
        generatedAtUtcEpochMillis = 1L,
        model = "model",
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

    private class FakeConversationRepository : AssistantConversationRepository {
        private val conversations = MutableStateFlow<List<AssistantConversation>>(emptyList())
        private val messages = mutableMapOf<String, MutableStateFlow<List<AssistantChatMessage>>>()
        val createdCoaches = mutableListOf<CoachPersona>()
        val savedTurns = mutableListOf<Triple<String, AssistantChatMessage, AssistantChatMessage>>()
        val renamed = mutableListOf<Pair<String, String>>()
        val cleared = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        var requestHistory: List<AssistantChatMessage> = emptyList()

        override fun observeConversations(): Flow<List<AssistantConversation>> = conversations

        override fun observeMessages(conversationId: String): Flow<List<AssistantChatMessage>> =
            messages.getOrPut(conversationId) { MutableStateFlow(emptyList()) }

        override suspend fun createConversation(coach: CoachPersona): AssistantConversation {
            createdCoaches += coach
            val conversation = AssistantConversation(
                id = "conversation-${createdCoaches.size}",
                coach = coach,
                title = "New conversation",
                createdAtUtcEpochMillis = createdCoaches.size.toLong(),
                updatedAtUtcEpochMillis = createdCoaches.size.toLong(),
            )
            messages[conversation.id] = MutableStateFlow(emptyList())
            conversations.value = listOf(conversation) + conversations.value
            return conversation
        }

        override suspend fun loadRequestHistory(conversationId: String): Result<List<AssistantChatMessage>> =
            Result.success(requestHistory)

        override suspend fun appendCompletedTurn(
            conversationId: String,
            userMessage: AssistantChatMessage,
            assistantMessage: AssistantChatMessage,
        ): Result<Unit> {
            savedTurns += Triple(conversationId, userMessage, assistantMessage)
            messages.getValue(conversationId).value += listOf(userMessage, assistantMessage)
            return Result.success(Unit)
        }

        override suspend fun renameConversation(conversationId: String, title: String): Result<Unit> {
            renamed += conversationId to title
            conversations.value = conversations.value.map {
                if (it.id == conversationId) it.copy(title = title) else it
            }
            return Result.success(Unit)
        }

        override suspend fun clearMemory(conversationId: String): Result<Unit> {
            cleared += conversationId
            return Result.success(Unit)
        }

        override suspend fun deleteConversation(conversationId: String): Result<Unit> {
            deleted += conversationId
            conversations.value = conversations.value.filterNot { it.id == conversationId }
            messages.remove(conversationId)
            return Result.success(Unit)
        }
    }

    private class FakeCoachingProposalApplier : CoachingProposalApplier {
        val applied = mutableListOf<CoachingProposal>()
        var applyResult: Result<Unit> = Result.success(Unit)
        override suspend fun apply(proposal: CoachingProposal): Result<Unit> {
            applied += proposal
            return applyResult
        }
    }

    private class FakeDraftStore(
        private var snapshot: AssistantDraftSnapshot = AssistantDraftSnapshot(),
    ) : AssistantDraftStore {
        override fun read(): AssistantDraftSnapshot = snapshot
        override fun write(snapshot: AssistantDraftSnapshot) {
            this.snapshot = snapshot
        }
        override fun clear() {
            snapshot = AssistantDraftSnapshot()
        }
    }

    private class FakeAccessController : AssistantAccessController {
        override fun synchronizeCredentialState() = Unit
        override val state = MutableStateFlow(
            AssistantAccessState(),
        )

        override fun acknowledgeDisclosure() = Unit
        override suspend fun beginAuthorization(): Result<String> = Result.success("https://openrouter.ai/auth")
        override suspend fun resumePendingAuthorization() = Unit
        override suspend fun inspectConnection(): Result<OpenRouterKeyMetadata> =
            Result.success(OpenRouterKeyMetadata(null, true, null))
        override fun cancelAuthorization(message: String) = Unit
        override suspend fun disconnect() = Unit
        override fun reserveInferenceRequest(): Result<String> = Result.success("test-token")
        override fun recordProviderSuccess() = Unit
        override fun recordProviderFailure(exception: Throwable) = Unit
    }
}
