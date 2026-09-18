package com.keepfit.feature.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.keepfit.core.designsystem.KeepfitPaceCard
import com.keepfit.core.designsystem.KeepfitStatusMark
import com.keepfit.core.designsystem.LocalKeepfitMotionSettings
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.AssistantAccessStatus
import com.keepfit.feature.assistant.conversation.AssistantConversation
import com.keepfit.feature.assistant.conversation.CoachPersona
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.data.AssistantUiState
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch

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
    onStartNewConversation: () -> Unit = {},
    onDismissCoachPicker: () -> Unit = {},
    onChooseCoach: (CoachPersona) -> Unit = {},
    onSelectConversation: (String) -> Unit = {},
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onClearConversationMemory: (String) -> Unit = {},
    onDeleteConversation: (String) -> Unit = {},
    openPlanner: Boolean = false,
    onPlannerOpened: () -> Unit = {},
    onRequestPlan: () -> Unit = {},
    onApplyPlan: () -> Unit = {},
    onDismissPlan: () -> Unit = {},
    onSelectReview: (CoachingIntent) -> Unit = {},
    onRequestReview: () -> Unit = {},
    onApplyReview: () -> Unit = {},
    onEditReview: () -> Unit = {},
    onDismissReview: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val motionSettings = LocalKeepfitMotionSettings.current
    val hasAccess = uiState.accessState.status == AssistantAccessStatus.CONNECTED
    val canSend = isEnabled && validationMessage == null && hasAccess &&
        uiState.draftMessage.isNotBlank() && !uiState.isWorking
    var renameTarget by remember { mutableStateOf<AssistantConversation?>(null) }
    var clearTarget by remember { mutableStateOf<AssistantConversation?>(null) }
    var deleteTarget by remember { mutableStateOf<AssistantConversation?>(null) }
    var showPlanComposer by remember { mutableStateOf(false) }
    var showReviewComposer by remember { mutableStateOf(false) }

    LaunchedEffect(openPlanner, hasAccess) {
        if (openPlanner && hasAccess) {
            showPlanComposer = true
            onPlannerOpened()
        }
    }
    LaunchedEffect(uiState.pendingDraftPlan) {
        if (uiState.pendingDraftPlan != null) showPlanComposer = false
    }
    LaunchedEffect(uiState.pendingCoachingProposal) {
        if (uiState.pendingCoachingProposal != null) showReviewComposer = false
    }

    if (uiState.showPrivacyDisclosure) OpenRouterDisclosureDialog(onConfirmConnect, onDismissDisclosure)
    renameTarget?.let { conversation ->
        RenameConversationDialog(
            conversation = conversation,
            onConfirm = { title -> onRenameConversation(conversation.id, title); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
    clearTarget?.let { conversation ->
        ConfirmConversationDialog(
            title = "Clear ${conversation.coach.displayName}'s memory?",
            detail = "The transcript stays visible, but future replies will not use this conversation's earlier messages.",
            confirmLabel = "Clear memory",
            onConfirm = { onClearConversationMemory(conversation.id); clearTarget = null },
            onDismiss = { clearTarget = null },
        )
    }
    deleteTarget?.let { conversation ->
        ConfirmConversationDialog(
            title = "Delete conversation?",
            detail = "\"${conversation.title}\" and its local transcript will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm = { onDeleteConversation(conversation.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
    if (showPlanComposer && uiState.pendingDraftPlan == null) {
        PlanComposerDialog(
            request = uiState.draftMessage,
            isWorking = uiState.isWorking,
            onRequestChange = onDraftChange,
            onCreate = onRequestPlan,
            onDismiss = { showPlanComposer = false },
        )
    }
    uiState.pendingDraftPlan?.let { plan ->
        PlanReviewDialog(
            plan = plan,
            isWorking = uiState.isWorking,
            onApply = onApplyPlan,
            onRevise = {
                onDismissPlan()
                showPlanComposer = true
            },
            onDismiss = onDismissPlan,
        )
    }
    if (showReviewComposer && uiState.pendingCoachingProposal == null) {
        ReviewComposerDialog(
            selectedIntent = uiState.selectedCoachingIntent,
            request = uiState.draftMessage,
            isWorking = uiState.isWorking,
            onSelect = onSelectReview,
            onRequestChange = onDraftChange,
            onCreate = onRequestReview,
            onDismiss = { showReviewComposer = false },
        )
    }
    uiState.pendingCoachingProposal?.let { proposal ->
        CoachingProposalDialog(
            proposal = proposal,
            isWorking = uiState.isWorking,
            onApply = onApplyReview,
            onEdit = {
                onEditReview()
                showReviewComposer = true
            },
            onDismiss = onDismissReview,
        )
    }

    LaunchedEffect(uiState.messages.size, uiState.isWorking) {
        if (uiState.messages.isNotEmpty()) {
            if (motionSettings.reduceMotion) listState.scrollToItem(uiState.messages.lastIndex)
            else listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.conversations.isNotEmpty(),
        drawerContent = {
            ConversationDrawer(
                conversations = uiState.conversations,
                activeConversationId = uiState.activeConversationId,
                onNew = {
                    scope.launch { drawerState.close() }
                    onStartNewConversation()
                },
                onSelect = { conversationId ->
                    scope.launch { drawerState.close() }
                    onSelectConversation(conversationId)
                },
                onRename = { renameTarget = it },
                onClearMemory = { clearTarget = it },
                onDelete = { deleteTarget = it },
            )
        },
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CoachTopBar(
                    coach = uiState.selectedCoach,
                    hasConversations = uiState.conversations.isNotEmpty(),
                    onOpenHistory = { scope.launch { drawerState.open() } },
                    onNewConversation = onStartNewConversation,
                    onOpenSettings = onOpenSettings,
                    onBack = onBack,
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
                    uiState.showCoachPicker -> CoachPicker(
                        canDismiss = uiState.conversations.isNotEmpty(),
                        onDismiss = onDismissCoachPicker,
                        onChoose = onChooseCoach,
                    )
                    else -> ConversationContent(
                        uiState = uiState,
                        listState = listState,
                        canSend = canSend,
                        onDraftChange = onDraftChange,
                        onSend = onSend,
                        onRetry = onRetry,
                        onOpenPlan = { showPlanComposer = true },
                        onOpenReview = { showReviewComposer = true },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachTopBar(
    coach: CoachPersona?,
    hasConversations: Boolean,
    onOpenHistory: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)?,
) {
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        coach?.displayName ?: "Coach",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        coach?.styleName ?: "Choose the voice that works for you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
            if (hasConversations) {
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Conversation history")
                }
            }
            IconButton(onClick = onNewConversation) {
                Icon(Icons.Outlined.AddComment, contentDescription = "New Coach conversation")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings")
            }
        },
    )
}

@Composable
private fun ConversationDrawer(
    conversations: List<AssistantConversation>,
    activeConversationId: String?,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (AssistantConversation) -> Unit,
    onClearMemory: (AssistantConversation) -> Unit,
    onDelete: (AssistantConversation) -> Unit,
) {
    ModalDrawerSheet(Modifier.width(320.dp)) {
        Text(
            "Conversations",
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, end = 20.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onNew, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Outlined.AddComment, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New conversation")
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            items(conversations, key = { it.id }) { conversation ->
                var menuOpen by remember(conversation.id) { mutableStateOf(false) }
                NavigationDrawerItem(
                    selected = conversation.id == activeConversationId,
                    onClick = { onSelect(conversation.id) },
                    label = {
                        Column {
                            Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                conversation.coach.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    icon = { CoachInitial(conversation.coach, 34.dp) },
                    badge = {
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Options for ${conversation.title}")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = { menuOpen = false; onRename(conversation) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Coach memory") },
                                    onClick = { menuOpen = false; onClearMemory(conversation) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { menuOpen = false; onDelete(conversation) },
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun CoachPicker(
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onChoose: (CoachPersona) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Choose your Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "The advice boundaries stay the same. Choose how you want it delivered.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(CoachPersona.entries, key = { it.id }) { coach ->
            KeepfitPaceCard(accentColor = coach.accentColor()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoachInitial(coach, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(coach.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(coach.styleName, color = coach.accentColor())
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(coach.description)
                Spacer(Modifier.height(8.dp))
                Text(
                    "\"${coach.sample}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = { onChoose(coach) }, modifier = Modifier.align(Alignment.End)) {
                    Text("Choose ${coach.displayName}")
                }
            }
        }
        if (canDismiss) {
            item {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep current conversation") }
            }
        }
    }
}

@Composable
private fun CoachInitial(coach: CoachPersona, size: Dp) {
    val accent = coach.accentColor()
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.14f),
        contentColor = accent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(coach.displayName.take(1), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ColumnScope.ConversationContent(
    uiState: AssistantUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenReview: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Coach tools", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onOpenPlan, enabled = !uiState.isWorking) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Create plan")
        }
        TextButton(onClick = onOpenReview, enabled = !uiState.isWorking) { Text("Review") }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    Box(Modifier.fillMaxWidth().weight(1f)) {
        if (uiState.messages.isEmpty()) {
            EmptyConversation(uiState.selectedCoach, onDraftChange)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
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
            TextButton(onClick = onRetry, enabled = uiState.canRetryLastMessage) { Text("Retry") }
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

@Composable
private fun ReviewComposerDialog(
    selectedIntent: CoachingIntent,
    request: String,
    isWorking: Boolean,
    onSelect: (CoachingIntent) -> Unit,
    onRequestChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val intents = listOf(CoachingIntent.WEEKLY_SUMMARY, CoachingIntent.WEEKLY_PLAN, CoachingIntent.EXISTING_FOOD_MEAL)
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Review with Coach") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Coach can inspect the selected local records and prepare a reviewable proposal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                intents.forEach { intent ->
                    FilterChip(
                        selected = selectedIntent == intent,
                        onClick = { onSelect(intent) },
                        label = { Text(intent.label) },
                    )
                }
                OutlinedTextField(
                    value = request,
                    onValueChange = { onRequestChange(it.take(500)) },
                    label = { Text("Optional direction") },
                    placeholder = { Text(selectedIntent.promptHint) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isWorking,
                )
                Text("No change is made until you approve the proposal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { Button(onClick = onCreate, enabled = !isWorking) { Text(if (isWorking) "Reviewing…" else "Create review") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") } },
    )
}

@Composable
private fun CoachingProposalDialog(
    proposal: CoachingProposal,
    isWorking: Boolean,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val changesData = proposal.operation !is CoachingProposalOperation.None
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Column {
                Text(proposal.title)
                Text("Review before ${if (changesData) "approval" else "closing"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProposalSection("Observed", proposal.observed)
                ProposalSection("Current", proposal.current)
                ProposalSection("Proposed", proposal.proposed)
                ProposalSection("Why", proposal.reason)
                if (changesData) Text("Your local data remains unchanged until you apply this proposal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onApply, enabled = !isWorking) { Text(if (changesData) "Apply proposal" else "Close review") } },
        dismissButton = {
            Row {
                TextButton(onClick = onEdit, enabled = !isWorking) { Text("Revise") }
                TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Dismiss") }
            }
        },
    )
}

@Composable
private fun ProposalSection(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value)
    }
}

@Composable
private fun PlanComposerDialog(
    request: String,
    isWorking: Boolean,
    onRequestChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Create a workout draft") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Coach uses your saved body, activity, recovery, schedule, equipment, and limitation answers. It can reuse your exercises or propose complete new ones.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = request,
                    onValueChange = { onRequestChange(it.take(500)) },
                    label = { Text("Anything to emphasize?") },
                    placeholder = { Text("For example: keep sessions simple and knee-friendly") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isWorking,
                )
                Text(
                    "You will review every day and exercise before the current plan is replaced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(onClick = onCreate, enabled = !isWorking) {
                Text(if (isWorking) "Creating…" else "Create draft")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") } },
    )
}

@Composable
private fun PlanReviewDialog(
    plan: AssistantDraftWorkoutPlan,
    isWorking: Boolean,
    onApply: () -> Unit,
    onRevise: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = {
            Column {
                Text(plan.name)
                Text("Review before applying", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                plan.overview?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                plan.days.forEachIndexed { index, day ->
                    Row(Modifier.fillMaxWidth()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(day.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(day.templateName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            day.exercises.forEach { exercise ->
                                Text(
                                    "${exercise.name} · ${exercise.targetSets} × ${exercise.targetReps}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (exercise.newExercise != null) {
                                    Text(
                                        "New exercise · added to your catalogue when applied",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    if (index != plan.days.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
                Text(
                    "Applying replaces only this profile's active weekly plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onApply, enabled = !isWorking) { Text(if (isWorking) "Applying…" else "Apply plan") } },
        dismissButton = {
            Row {
                TextButton(onClick = onRevise, enabled = !isWorking) { Text("Revise") }
                TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Dismiss") }
            }
        },
    )
}

@Composable
private fun RenameConversationDialog(
    conversation: AssistantConversation,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(conversation.id) { mutableStateOf(conversation.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename conversation") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Title") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmConversationDialog(
    title: String,
    detail: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
        KeepfitStatusMark(label = label, color = color, modifier = Modifier.weight(1f))
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
private fun EmptyConversation(coach: CoachPersona?, onPrompt: (String) -> Unit) {
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
        Text(
            if (coach == null) "What would you like to understand?" else "Start with ${coach.displayName}",
            style = MaterialTheme.typography.titleLarge,
        )
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
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        KeepfitPaceCard(accentColor = MaterialTheme.colorScheme.secondary) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action?.let {
                Spacer(Modifier.height(18.dp))
                Button(onClick = onAction) { Text(it) }
            }
        }
    }
}

@Composable
private fun CoachPersona.accentColor(): Color = when (this) {
    CoachPersona.MIRA -> MaterialTheme.colorScheme.primary
    CoachPersona.ROOK -> MaterialTheme.colorScheme.secondary
    CoachPersona.ATLAS -> MaterialTheme.colorScheme.tertiary
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
            val bubbleModifier = Modifier.fillMaxWidth(0.86f).padding(horizontal = 14.dp, vertical = 12.dp)
            if (isUser) {
                Text(message.content, modifier = bubbleModifier, style = MaterialTheme.typography.bodyMedium)
            } else {
                Markdown(message.content, modifier = bubbleModifier)
            }
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
                Text("General chat excludes weight, BMI, height, transformation photos, measurements, profile identifiers, private notes, paths, and raw database records.")
                Text("When you explicitly create a workout plan, Coach receives the assessment answers shown in planning—including age, height, current weight, activity, sleep, equipment, and limitations—plus bounded exercise aliases. It may propose validated new exercise definitions, which are saved only after you approve the draft.")
                Text("Your OpenRouter account controls provider limits and credits. Keepfit does not impose its own daily request cap.")
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Continue to OpenRouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
