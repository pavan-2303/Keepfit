package com.keepfit.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.feature.assistant.data.AssistantRepository
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.validateAssistantRuntimeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssistantLaunchState(
    val isEnabled: Boolean = false,
    val config: AssistantRuntimeConfig? = null,
    val validationMessage: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeShellViewModel @Inject constructor(
    settingsRepository: AppSettingsRepository,
    private val buildTimeConfig: AssistantRuntimeConfig,
    private val assistantRepository: AssistantRepository,
) : ViewModel() {
    val assistantLaunchState: StateFlow<AssistantLaunchState> = settingsRepository.observeSettings()
        .mapLatest {
            validateAssistantRuntimeConfig(
                baseUrl = buildTimeConfig.baseUrl,
                generalChatModelName = buildTimeConfig.generalChatModelName,
                reasoningModelName = buildTimeConfig.reasoningModelName,
                apiKey = buildTimeConfig.apiKey,
            ).fold(
                onSuccess = {
                    AssistantLaunchState(
                        isEnabled = true,
                        config = it,
                    )
                },
                onFailure = {
                    AssistantLaunchState(
                        isEnabled = true,
                        validationMessage = it.message ?: "Assistant settings are invalid.",
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AssistantLaunchState(isEnabled = true),
        )

    private val _assistantConnectionMessage = MutableStateFlow<String?>(null)
    val assistantConnectionMessage: StateFlow<String?> = _assistantConnectionMessage.asStateFlow()

    fun testAssistantConnection() {
        val launchState = assistantLaunchState.value
        val config = launchState.config
        if (config == null) {
            _assistantConnectionMessage.value = launchState.validationMessage ?: "Assistant settings are invalid."
            return
        }

        viewModelScope.launch {
            assistantRepository.testConnection(config)
                .onSuccess {
                    _assistantConnectionMessage.value = "Assistant connection successful."
                }
                .onFailure {
                    _assistantConnectionMessage.value = it.message ?: "Assistant connection failed."
                }
        }
    }

    fun dismissAssistantConnectionMessage() {
        _assistantConnectionMessage.value = null
    }
}
