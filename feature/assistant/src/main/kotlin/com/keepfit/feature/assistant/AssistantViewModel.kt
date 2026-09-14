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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository,
    private val planApplier: AssistantPlanApplier,
    private val accessController: AssistantAccessController,
    private val coachingProposalApplier: CoachingProposalApplier,
    private val assistantDraftStore: AssistantDraftStore,
    private val contextPolicy: AssistantContextPolicy = AssistantContextPolicy(),
) : ViewModel() {
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
        viewModelScope.launch {
            accessController.state.collect { accessState ->
                _uiState.value = _uiState.value.copy(accessState = accessState)
            }
        }
        viewModelScope.launch {
            accessController.resumePendingAuthorization()
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
        accessController.disconnect()
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
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + confirmation,
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
            repository.sendChatTurn(
                config = config,
                history = _uiState.value.messages,
                userMessage = draftMessage,
            ).onSuccess { assistantReply ->
                val userMessage = AssistantChatMessage(
                    id = "user-${System.currentTimeMillis()}",
                    role = AssistantMessageRole.USER,
                    content = draftMessage,
                    createdAtUtcEpochMillis = System.currentTimeMillis(),
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage + assistantReply,
                    draftMessage = "",
                    isWorking = false,
                    errorMessage = null,
                    lastResponseUsedLocalContext = usesLocalContext,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    errorMessage = it.message ?: "Assistant request failed.",
                )
            }
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
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
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
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
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
}
