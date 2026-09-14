package com.keepfit.feature.assistant.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.authorizationUrl) {
        uiState.authorizationUrl?.let { url ->
            val launchResult = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            if (launchResult.isSuccess) {
                viewModel.consumeAuthorizationUrl()
            } else {
                viewModel.authorizationBrowserFailed()
            }
        }
    }

    AssistantScreen(
        uiState = uiState,
        isEnabled = isEnabled,
        validationMessage = validationMessage,
        onBack = onBack,
        onDraftChange = viewModel::updateDraftMessage,
        onSend = {
            config?.let(viewModel::sendDraftMessage)
        },
        onRetry = {
            config?.let(viewModel::retryLastMessage)
        },
        onConnect = viewModel::requestConnectionDisclosure,
        onConfirmConnect = viewModel::acceptDisclosureAndConnect,
        onDismissDisclosure = viewModel::dismissConnectionDisclosure,
        onCancelAuthorization = viewModel::cancelOpenRouterAuthorization,
        onInspectConnection = {
            config?.let(viewModel::inspectOpenRouterConnection)
        },
        onDisconnect = viewModel::disconnectOpenRouter,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}
