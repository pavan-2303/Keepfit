package com.keepfit.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantConnectionStatus
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantPlanApplier
import com.keepfit.feature.assistant.data.AssistantRepository
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.AssistantUiState
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun updateDraftMessage(message: String) {
        _uiState.value = _uiState.value.copy(draftMessage = message)
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
}
