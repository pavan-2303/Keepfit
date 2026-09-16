package com.keepfit.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantConnectionStatus
import com.keepfit.feature.assistant.data.AssistantContextPolicy
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantPlanApplier
import com.keepfit.feature.assistant.data.AssistantRepository
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.AssistantUiState
import com.keepfit.feature.assistant.access.AssistantAccessController
import com.keepfit.feature.assistant.coaching.AssistantDraftSnapshot
import com.keepfit.feature.assistant.coaching.AssistantDraftStore
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposalApplier
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import com.keepfit.feature.assistant.coaching.CoachingSafetyRefusalException
import com.keepfit.feature.assistant.conversation.AssistantConversationRepository
import com.keepfit.feature.assistant.conversation.CoachPersona
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository,
    private val conversationRepository: AssistantConversationRepository,
    private val planApplier: AssistantPlanApplier,
    private val accessController: AssistantAccessController,
    private val coachingProposalApplier: CoachingProposalApplier,
    private val assistantDraftStore: AssistantDraftStore,
    private val contextPolicy: AssistantContextPolicy = AssistantContextPolicy(),
) : ViewModel() {
    private val activeConversationId = MutableStateFlow<String?>(null)
    private val supplementalMessages = mutableMapOf<String?, List<AssistantChatMessage>>()
    private var coachPickerRequested = false
    private val restoredDraft = assistantDraftStore.read()
    private val _uiState = MutableStateFlow(
        AssistantUiState(
            draftMessage = restoredDraft.draftText,
            selectedCoachingIntent = restoredDraft.selectedIntent,
            pendingCoachingProposal = restoredDraft.proposal,
        ),
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        observeConversations()
        observeActiveConversationMessages()
        viewModelScope.launch {
            accessController.state.collect { accessState ->
                _uiState.value = _uiState.value.copy(accessState = accessState)
            }
        }
        viewModelScope.launch {
            accessController.resumePendingAuthorization()
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            conversationRepository.observeConversations().collect { conversations ->
                val state = _uiState.value
                val currentId = activeConversationId.value
                val nextId = currentId?.takeIf { id -> conversations.any { it.id == id } }
                    ?: conversations.firstOrNull()?.id
                activeConversationId.value = nextId
                _uiState.value = state.copy(
                    conversations = conversations,
                    activeConversationId = nextId,
                    showCoachPicker = conversations.isEmpty() || coachPickerRequested,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeActiveConversationMessages() {
        viewModelScope.launch {
            activeConversationId.flatMapLatest { conversationId ->
                if (conversationId == null) flowOf(emptyList())
                else conversationRepository.observeMessages(conversationId)
            }.collect { messages ->
                val visibleMessages = messages + supplementalMessages[activeConversationId.value].orEmpty()
                _uiState.value = _uiState.value.copy(
                    messages = visibleMessages,
                    lastResponseUsedLocalContext = visibleMessages.lastOrNull {
                        it.role == AssistantMessageRole.ASSISTANT
                    }?.includedLocalContext == true,
                )
            }
        }
    }

    fun startNewConversation() {
        coachPickerRequested = true
        _uiState.value = _uiState.value.copy(showCoachPicker = true, errorMessage = null)
    }

    fun dismissCoachPicker() {
        if (_uiState.value.conversations.isNotEmpty()) {
            coachPickerRequested = false
            _uiState.value = _uiState.value.copy(showCoachPicker = false)
        }
    }

    fun chooseCoach(coach: CoachPersona) {
        viewModelScope.launch {
            runCatching { conversationRepository.createConversation(coach) }
                .onSuccess { conversation ->
                    coachPickerRequested = false
                    activeConversationId.value = conversation.id
                    _uiState.value = _uiState.value.copy(
                        activeConversationId = conversation.id,
                        showCoachPicker = false,
                        errorMessage = null,
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "A Coach conversation could not be created.",
                    )
                }
        }
    }

    fun selectConversation(conversationId: String) {
        if (_uiState.value.conversations.none { it.id == conversationId }) return
        activeConversationId.value = conversationId
        _uiState.value = _uiState.value.copy(
            activeConversationId = conversationId,
            showCoachPicker = false,
            errorMessage = null,
        )
        coachPickerRequested = false
    }

    fun renameConversation(conversationId: String, title: String) {
        performConversationMutation("Conversation title could not be changed.") {
            conversationRepository.renameConversation(conversationId, title)
        }
    }

    fun clearConversationMemory(conversationId: String) {
        performConversationMutation("Coach memory could not be cleared.") {
            conversationRepository.clearMemory(conversationId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val wasActive = activeConversationId.value == conversationId
            if (wasActive) {
                activeConversationId.value = null
                _uiState.value = _uiState.value.copy(activeConversationId = null)
            }
            conversationRepository.deleteConversation(conversationId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
                .onFailure { exception ->
                    if (wasActive) activeConversationId.value = conversationId
                    _uiState.value = _uiState.value.copy(
                        activeConversationId = activeConversationId.value,
                        errorMessage = exception.message ?: "Conversation could not be deleted.",
                    )
                }
        }
    }

    private fun performConversationMutation(
        fallbackError: String,
        mutation: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            mutation()
                .onSuccess { _uiState.value = _uiState.value.copy(errorMessage = null) }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(errorMessage = exception.message ?: fallbackError)
                }
        }
    }

    fun requestConnectionDisclosure() {
        _uiState.value = _uiState.value.copy(showPrivacyDisclosure = true, errorMessage = null)
    }

    fun dismissConnectionDisclosure() {
        _uiState.value = _uiState.value.copy(showPrivacyDisclosure = false)
    }

    fun acceptDisclosureAndConnect() {
        accessController.acknowledgeDisclosure()
        _uiState.value = _uiState.value.copy(showPrivacyDisclosure = false, errorMessage = null)
        viewModelScope.launch {
            accessController.beginAuthorization()
                .onSuccess { url ->
                    _uiState.value = _uiState.value.copy(authorizationUrl = url)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = exception.message ?: "OpenRouter authorization could not start.",
                    )
                }
        }
    }

    fun consumeAuthorizationUrl() {
        _uiState.value = _uiState.value.copy(authorizationUrl = null)
    }

    fun authorizationBrowserFailed() {
        accessController.cancelAuthorization("No browser could open OpenRouter. Your local data was not changed.")
        _uiState.value = _uiState.value.copy(authorizationUrl = null)
    }

    fun cancelOpenRouterAuthorization() {
        accessController.cancelAuthorization("OpenRouter connection cancelled. Your local data was not changed.")
        _uiState.value = _uiState.value.copy(authorizationUrl = null)
    }

    fun inspectOpenRouterConnection(config: AssistantRuntimeConfig) {
        testConnection(config)
    }

    fun disconnectOpenRouter() {
        viewModelScope.launch { accessController.disconnect() }
    }

    fun updateDraftMessage(message: String) {
        _uiState.value = _uiState.value.copy(draftMessage = message.take(500), safetyMessage = null)
        persistCoachingDraft()
    }

    fun selectCoachingIntent(intent: CoachingIntent) {
        _uiState.value = _uiState.value.copy(selectedCoachingIntent = intent, safetyMessage = null)
        persistCoachingDraft()
    }

    fun requestCoachingProposal(config: AssistantRuntimeConfig) {
        val state = _uiState.value
        val request = state.draftMessage.trim().ifEmpty { state.selectedCoachingIntent.promptHint }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, errorMessage = null, safetyMessage = null)
            persistCoachingDraft(draftOverride = request)
            repository.requestCoachingProposal(config, state.selectedCoachingIntent, request)
                .onSuccess { proposal ->
                    _uiState.value = _uiState.value.copy(
                        pendingCoachingProposal = proposal,
                        draftMessage = "",
                        isWorking = false,
                        errorMessage = null,
                    )
                    persistCoachingDraft()
                }
                .onFailure { exception ->
                    _uiState.value = if (exception is CoachingSafetyRefusalException) {
                        _uiState.value.copy(isWorking = false, safetyMessage = exception.guidance, errorMessage = null)
                    } else {
                        _uiState.value.copy(
                            isWorking = false,
                            errorMessage = exception.message ?: "Coaching proposal could not be created.",
                        )
                    }
                    persistCoachingDraft()
                }
        }
    }

    fun editCoachingProposal() {
        val proposal = _uiState.value.pendingCoachingProposal ?: return
        _uiState.value = _uiState.value.copy(
            draftMessage = proposal.originalRequest,
            selectedCoachingIntent = proposal.intent,
            pendingCoachingProposal = null,
            errorMessage = null,
        )
        persistCoachingDraft()
    }

    fun dismissCoachingProposal() {
        _uiState.value = _uiState.value.copy(pendingCoachingProposal = null)
        persistCoachingDraft()
    }

    fun applyCoachingProposal() {
        val proposal = _uiState.value.pendingCoachingProposal ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, errorMessage = null)
            coachingProposalApplier.apply(proposal)
                .onSuccess {
                    val confirmation = AssistantChatMessage(
                        id = "assistant-proposal-${System.currentTimeMillis()}",
                        role = AssistantMessageRole.ASSISTANT,
                        content = if (proposal.operation is CoachingProposalOperation.None) {
                            "Review closed without changing local data."
                        } else {
                            "Approved coaching change applied."
                        },
                        createdAtUtcEpochMillis = System.currentTimeMillis(),
                    )
                    appendSupplementalMessage(confirmation)
                    _uiState.value = _uiState.value.copy(
                        pendingCoachingProposal = null,
                        isWorking = false,
                    )
                    persistCoachingDraft()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        errorMessage = exception.message ?: "The reviewed change could not be applied.",
                    )
                    persistCoachingDraft()
                }
        }
    }

    fun testConnection(config: AssistantRuntimeConfig) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = AssistantConnectionStatus.TESTING,
                isWorking = true,
                errorMessage = null,
            )
            repository.testConnection(config)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = AssistantConnectionStatus.CONNECTED,
                        isWorking = false,
                        errorMessage = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = AssistantConnectionStatus.ERROR,
                        isWorking = false,
                        errorMessage = it.message ?: "Assistant connection failed.",
                    )
                }
        }
    }

    fun sendDraftMessage(config: AssistantRuntimeConfig) {
        val draftMessage = _uiState.value.draftMessage.trim()
        if (draftMessage.isEmpty()) return
        val usesLocalContext = contextPolicy.shouldIncludeLocalContext(draftMessage)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                errorMessage = null,
            )
            val conversationId = ensureActiveConversation().getOrElse { exception ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message ?: "A Coach conversation could not be created.",
                )
                return@launch
            }
            val history = conversationRepository.loadRequestHistory(conversationId).getOrElse { exception ->
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    errorMessage = exception.message ?: "Coach memory could not be loaded.",
                )
                return@launch
            }
            val userMessage = AssistantChatMessage(
                id = UUID.randomUUID().toString(),
                role = AssistantMessageRole.USER,
                content = draftMessage,
                createdAtUtcEpochMillis = System.currentTimeMillis(),
            )
            repository.sendChatTurn(
                config = config,
                history = history,
                userMessage = draftMessage,
            ).onSuccess { assistantReply ->
                val persistedReply = assistantReply.copy(
                    id = UUID.randomUUID().toString(),
                    createdAtUtcEpochMillis = maxOf(
                        assistantReply.createdAtUtcEpochMillis,
                        userMessage.createdAtUtcEpochMillis + 1,
                    ),
                    includedLocalContext = usesLocalContext,
                )
                conversationRepository.appendCompletedTurn(conversationId, userMessage, persistedReply)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            draftMessage = "",
                            isWorking = false,
                            errorMessage = null,
                            lastResponseUsedLocalContext = usesLocalContext,
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isWorking = false,
                            errorMessage = exception.message ?: "Coach response could not be saved.",
                        )
                    }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    errorMessage = it.message ?: "Assistant request failed.",
                )
            }
        }
    }

    private suspend fun ensureActiveConversation(): Result<String> {
        activeConversationId.value?.let { return Result.success(it) }
        return runCatching {
            conversationRepository.createConversation(CoachPersona.MIRA)
        }.map { conversation ->
            coachPickerRequested = false
            activeConversationId.value = conversation.id
            _uiState.value = _uiState.value.copy(
                activeConversationId = conversation.id,
                showCoachPicker = false,
            )
            conversation.id
        }
    }

    fun retryLastMessage(config: AssistantRuntimeConfig) {
        sendDraftMessage(config)
    }

    fun generateProgressSummary(config: AssistantRuntimeConfig) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                errorMessage = null,
            )
            repository.generateProgressSummary(config)
                .onSuccess { summary ->
                    val assistantMessage = AssistantChatMessage(
                        id = "assistant-summary-${System.currentTimeMillis()}",
                        role = AssistantMessageRole.ASSISTANT,
                        content = "${summary.title}\n\n${summary.summary}",
                        createdAtUtcEpochMillis = System.currentTimeMillis(),
                    )
                    appendSupplementalMessage(assistantMessage)
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        errorMessage = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        errorMessage = it.message ?: "Assistant request failed.",
                    )
                }
        }
    }

    fun requestDraftPlan(config: AssistantRuntimeConfig) {
        val draftInput = _uiState.value.draftMessage.trim()
        val goal = draftInput.ifEmpty { "Create a balanced weekly workout plan that fits the available local data." }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                errorMessage = null,
            )
            repository.requestDraftPlan(
                config = config,
                input = com.keepfit.feature.assistant.data.AssistantDraftInput(goal = goal),
            ).onSuccess { plan ->
                _uiState.value = _uiState.value.copy(
                    pendingDraftPlan = plan,
                    draftMessage = "",
                    isWorking = false,
                    errorMessage = null,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    errorMessage = it.message ?: "Assistant draft request failed.",
                )
            }
        }
    }

    fun dismissDraftPlan() {
        _uiState.value = _uiState.value.copy(pendingDraftPlan = null)
    }

    fun applyDraftPlan() {
        val draft = _uiState.value.pendingDraftPlan ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWorking = true,
                errorMessage = null,
            )
            planApplier.applyDraftPlan(draft)
                .onSuccess {
                    val assistantMessage = AssistantChatMessage(
                        id = "assistant-apply-${System.currentTimeMillis()}",
                        role = AssistantMessageRole.ASSISTANT,
                        content = "Draft weekly plan applied to Workouts > Plan.",
                        createdAtUtcEpochMillis = System.currentTimeMillis(),
                    )
                    appendSupplementalMessage(assistantMessage)
                    _uiState.value = _uiState.value.copy(
                        pendingDraftPlan = null,
                        isWorking = false,
                        errorMessage = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        errorMessage = it.message ?: "Draft plan apply failed.",
                    )
                }
        }
    }

    private fun persistCoachingDraft(draftOverride: String? = null) {
        val state = _uiState.value
        assistantDraftStore.write(
            AssistantDraftSnapshot(
                draftText = draftOverride ?: state.draftMessage,
                selectedIntent = state.selectedCoachingIntent,
                proposal = state.pendingCoachingProposal,
            ),
        )
    }

    private fun appendSupplementalMessage(message: AssistantChatMessage) {
        val conversationId = activeConversationId.value
        supplementalMessages[conversationId] = supplementalMessages[conversationId].orEmpty() + message
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + message)
    }
}
