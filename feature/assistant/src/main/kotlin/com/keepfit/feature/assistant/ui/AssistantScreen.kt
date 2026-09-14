package com.keepfit.feature.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.AssistantAccessStatus
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    uiState: AssistantUiState,
    isEnabled: Boolean,
    validationMessage: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onConnect: () -> Unit = {},
    onConfirmConnect: () -> Unit = {},
    onDismissDisclosure: () -> Unit = {},
    onCancelAuthorization: () -> Unit = {},
    onInspectConnection: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val hasAccess = uiState.accessState.status == AssistantAccessStatus.CONNECTED
    val canSend = isEnabled && validationMessage == null && hasAccess &&
        uiState.draftMessage.isNotBlank() && !uiState.isWorking

    if (uiState.showPrivacyDisclosure) {
        OpenRouterDisclosureDialog(onConfirmConnect, onDismissDisclosure)
    }

    LaunchedEffect(uiState.messages.size, uiState.isWorking) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Coach")
                        Text(
                            "Ask anything. Your logs are used only when relevant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            CompactAccessRow(
                accessState = uiState.accessState,
                onCancelAuthorization = onCancelAuthorization,
                onInspectConnection = onInspectConnection,
                onDisconnect = onDisconnect,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            when {
                !isEnabled || validationMessage != null -> CoachUnavailable(
                    title = "Coach needs attention",
                    detail = validationMessage ?: "Open Profile and settings to enable Coach.",
                )
                !hasAccess -> CoachUnavailable(
                    title = "Ask with your own OpenRouter account",
                    detail = "Connect once in the browser. Keepfit never asks you to paste a key or pays for requests on your behalf.",
                    action = if (uiState.accessState.status == AssistantAccessStatus.CONNECTING) null else "Connect OpenRouter",
                    onAction = onConnect,
                )
                else -> {
                    Box(Modifier.weight(1f)) {
                        if (uiState.messages.isEmpty()) {
                            EmptyConversation(onPrompt = onDraftChange)
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(uiState.messages, key = { it.id }) { MessageBubble(it) }
                                if (uiState.lastResponseUsedLocalContext) {
                                    item {
                                        Text(
                                            "Recent Keepfit activity was included in the last answer.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    uiState.safetyMessage?.let { InlineNotice(it) }
                    uiState.errorMessage?.let { error ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry, enabled = uiState.draftMessage.isNotBlank()) { Text("Retry") }
                        }
                    }
                    OutlinedTextField(
                        value = uiState.draftMessage,
                        onValueChange = onDraftChange,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        placeholder = { Text("Ask about training, nutrition, or your progress") },
                        minLines = 1,
                        maxLines = 4,
                        enabled = !uiState.isWorking,
                        trailingIcon = {
                            IconButton(onClick = onSend, enabled = canSend) {
                                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send question")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactAccessRow(
    accessState: AssistantAccessState,
    onCancelAuthorization: () -> Unit,
    onInspectConnection: () -> Unit,
    onDisconnect: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val (label, color) = when (accessState.status) {
        AssistantAccessStatus.CONNECTED -> "OpenRouter connected" to MaterialTheme.colorScheme.primary
        AssistantAccessStatus.CONNECTING -> "Waiting for browser" to MaterialTheme.colorScheme.secondary
        AssistantAccessStatus.PROVIDER_LIMIT_REACHED -> "Provider limit reached" to MaterialTheme.colorScheme.error
        AssistantAccessStatus.INVALID -> "Connection invalid" to MaterialTheme.colorScheme.error
        AssistantAccessStatus.REVOKED -> "Access revoked" to MaterialTheme.colorScheme.error
        AssistantAccessStatus.PROVIDER_UNAVAILABLE -> "Provider unavailable" to MaterialTheme.colorScheme.error
        AssistantAccessStatus.DISCONNECTED -> "Not connected" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = color, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(8.dp)) {}
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (accessState.status != AssistantAccessStatus.DISCONNECTED) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Coach connection options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (accessState.status == AssistantAccessStatus.CONNECTING) {
                        DropdownMenuItem(
                            text = { Text("Cancel connection") },
                            onClick = { menuOpen = false; onCancelAuthorization() },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Check connection") },
                            onClick = { menuOpen = false; onInspectConnection() },
                        )
                        DropdownMenuItem(
                            text = { Text("Disconnect") },
                            onClick = { menuOpen = false; onDisconnect() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversation(onPrompt: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        Text("What would you like to understand?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask a general question, or ask about your own recent training and nutrition.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        FilledTonalButton(onClick = { onPrompt("How has my training consistency changed recently?") }) {
            Text("Review my consistency")
        }
        TextButton(onClick = { onPrompt("What is progressive overload?") }) {
            Text("Explain progressive overload")
        }
    }
}

@Composable
private fun CoachUnavailable(
    title: String,
    detail: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.let {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction) { Text(it) }
        }
    }
}

@Composable
private fun InlineNotice(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun MessageBubble(message: AssistantChatMessage) {
    val isUser = message.role == AssistantMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = if (isUser) MaterialTheme.shapes.large else MaterialTheme.shapes.small,
        ) {
            Text(
                message.content,
                modifier = Modifier.fillMaxWidth(0.86f).padding(horizontal = 14.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OpenRouterDisclosureDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before you connect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OpenRouter and the selected model provider receive each question you choose to send.")
                Text("For questions about your progress, Keepfit may add compact workout, nutrition, and step summaries. The conversation shows when this happened.")
                Text("Weight, BMI, height, transformation photos, measurements, identifiers, private notes, paths, and raw database records are excluded.")
                Text("Your OpenRouter account controls provider limits and credits. Keepfit does not impose its own daily request cap.")
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Continue to OpenRouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
