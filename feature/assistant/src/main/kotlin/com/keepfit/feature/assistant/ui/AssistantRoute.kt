package com.keepfit.feature.assistant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.assistant.AssistantViewModel
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig

const val assistantRoute = "assistant"

@Composable
fun AssistantRoute(
    isEnabled: Boolean,
    config: AssistantRuntimeConfig?,
    validationMessage: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    AssistantScreen(
        uiState = uiState,
        isEnabled = isEnabled,
        validationMessage = validationMessage,
        onBack = onBack,
        onDraftChange = viewModel::updateDraftMessage,
        onSend = {
            config?.let(viewModel::sendDraftMessage)
        },
        onSummarizeProgress = {
            config?.let(viewModel::generateProgressSummary)
        },
        onDraftWeeklyPlan = {
            config?.let(viewModel::requestDraftPlan)
        },
        onApplyDraftPlan = viewModel::applyDraftPlan,
        onDismissDraftPlan = viewModel::dismissDraftPlan,
        onRetry = {
            config?.let(viewModel::retryLastMessage)
        },
        modifier = modifier,
    )
}
