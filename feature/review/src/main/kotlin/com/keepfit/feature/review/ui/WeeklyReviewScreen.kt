package com.keepfit.feature.review.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.review.ReviewDraft
import com.keepfit.feature.review.ReviewDraftType
import com.keepfit.feature.review.ReviewOutcomeStatus
import com.keepfit.feature.review.ReviewWorkoutDay
import com.keepfit.feature.review.WeeklyReviewResult
import com.keepfit.feature.review.WeeklyReviewSnapshot
import com.keepfit.feature.review.WeeklyReviewUiState
import com.keepfit.feature.review.WeeklyReviewViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val weeklyReviewRoute = "weekly-review"

@Composable
fun TodayWeeklyReviewCard(
    onOpenReview: () -> Unit,
    viewModel: WeeklyReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPaused = state.snapshot is WeeklyReviewSnapshot.Paused
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Weekly review", style = MaterialTheme.typography.titleMedium)
                val detail = when {
                    state.isLoading -> "Preparing your local week…"
                    isPaused -> "Paused"
                    state.snapshot is WeeklyReviewSnapshot.Ready -> {
                        val result = (state.snapshot as WeeklyReviewSnapshot.Ready).result
                        "${result.completedCount} of ${result.plannedCount} planned workouts"
                    }
                    state.snapshot is WeeklyReviewSnapshot.Decided -> {
                        val decided = state.snapshot as WeeklyReviewSnapshot.Decided
                        if (decided.outcome.status == ReviewOutcomeStatus.APPROVED) "Next week adjusted" else "Last week reviewed"
                    }
                    else -> "Build history, then review the week"
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { if (isPaused) viewModel.setPaused(false) else onOpenReview() }) {
                Text(if (isPaused) "Resume" else "Open")
            }
        }
    }
}

@Composable
fun WeeklyReviewRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WeeklyReviewScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onSelectDraft = viewModel::selectDraft,
        onToggleEditing = viewModel::toggleEditing,
        onChooseDate = viewModel::chooseDate,
        onApprove = viewModel::approve,
        onDismiss = viewModel::dismiss,
        onPause = { viewModel.setPaused(true) },
        onResume = { viewModel.setPaused(false) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(
    state: WeeklyReviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectDraft: (ReviewDraft) -> Unit,
    onToggleEditing: () -> Unit,
    onChooseDate: (LocalDate) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Weekly review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.snapshot is WeeklyReviewSnapshot.Paused -> PausedReview(padding, onResume)
            state.snapshot is WeeklyReviewSnapshot.Insufficient -> InsufficientReview(
                padding = padding,
                result = state.snapshot.result,
                onPause = onPause,
            )
            state.snapshot is WeeklyReviewSnapshot.Ready -> ReviewContent(
                padding = padding,
                state = state,
                result = state.snapshot.result,
                onSelectDraft = onSelectDraft,
                onToggleEditing = onToggleEditing,
                onChooseDate = onChooseDate,
                onApprove = onApprove,
                onDismiss = onDismiss,
                onPause = onPause,
            )
            state.snapshot is WeeklyReviewSnapshot.Decided -> DecidedReview(padding, state.snapshot)
            else -> ErrorReview(padding, state.message, onRetry)
        }
    }
}

@Composable
private fun ReviewContent(
    padding: PaddingValues,
    state: WeeklyReviewUiState,
    result: WeeklyReviewResult,
    onSelectDraft: (ReviewDraft) -> Unit,
    onToggleEditing: () -> Unit,
    onChooseDate: (LocalDate) -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit,
    onPause: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "${result.window.reviewStart.format(shortDate)} – ${result.window.reviewEnd.format(shortDate)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Your week, without a score", style = MaterialTheme.typography.headlineMedium)
        Text(result.motivation, style = MaterialTheme.typography.bodyLarge)
        WeekRibbon(result)
        result.achievements.forEach { achievement -> EvidenceLine(achievement.text) }
        result.supportingSignals.forEach { signal -> EvidenceLine(signal) }
        if (result.drafts.isEmpty()) {
            Card { Text("No change is suggested. Keeping a manageable plan is a valid choice.", Modifier.padding(18.dp)) }
        } else {
            Text("One small choice for next week", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                result.drafts.forEach { draft ->
                    FilterChip(
                        selected = state.selectedDraft?.type == draft.type,
                        onClick = { onSelectDraft(draft) },
                        label = { Text(draft.type.shortLabel()) },
                    )
                }
            }
            state.selectedDraft?.let { draft ->
                DraftCard(
                    draft = draft,
                    result = result,
                    isEditing = state.isEditing,
                    isWorking = state.isWorking,
                    onToggleEditing = onToggleEditing,
                    onChooseDate = onChooseDate,
                    onApprove = onApprove,
                )
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onDismiss, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss this review")
        }
        TextButton(onClick = onPause, enabled = !state.isWorking, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Outlined.PauseCircle, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Pause weekly reviews")
        }
    }
}

@Composable
private fun WeekRibbon(result: WeeklyReviewResult) {
    val byDate = result.workoutDays.associateBy(ReviewWorkoutDay::date)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            (0L..6L).forEach { offset ->
                val date = result.window.reviewStart.plusDays(offset)
                val day = byDate[date]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier.size(32.dp).background(day.ribbonColor(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            day?.completed == true -> Icon(Icons.Outlined.Check, "Completed ${date.dayOfWeek}", Modifier.size(18.dp))
                            day?.movedTo != null -> Icon(Icons.Outlined.EditCalendar, "Moved ${date.dayOfWeek}", Modifier.size(18.dp))
                            day != null -> Text("•", fontWeight = FontWeight.Bold)
                            else -> Text("–")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewWorkoutDay?.ribbonColor(): Color = when {
    this?.completed == true -> MaterialTheme.colorScheme.primaryContainer
    this?.movedTo != null -> MaterialTheme.colorScheme.secondaryContainer
    this != null -> MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.surface
}

@Composable
private fun EvidenceLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.AutoMirrored.Outlined.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(10.dp))
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DraftCard(
    draft: ReviewDraft,
    result: WeeklyReviewResult,
    isEditing: Boolean,
    isWorking: Boolean,
    onToggleEditing: () -> Unit,
    onChooseDate: (LocalDate) -> Unit,
    onApprove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(draft.title, style = MaterialTheme.typography.titleLarge)
            Text("Current", style = MaterialTheme.typography.labelMedium)
            Text(draft.current)
            Text("Proposed", style = MaterialTheme.typography.labelMedium)
            Text(draft.proposed, fontWeight = FontWeight.SemiBold)
            Text(draft.reason, color = MaterialTheme.colorScheme.onPrimaryContainer)
            TextButton(onClick = onToggleEditing) { Text(if (isEditing) "Keep selected day" else "Edit day") }
            if (isEditing) {
                val plannedWeekdays = result.workoutDays.map { it.date.dayOfWeek }.toSet()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    (0L..6L).forEach { offset ->
                        val date = result.window.nextWeekStart.plusDays(offset)
                        val valid = if (draft.type == ReviewDraftType.MOVE_SESSION) {
                            date.dayOfWeek !in plannedWeekdays && date != draft.sourceDate
                        } else date.dayOfWeek in plannedWeekdays
                        if (valid) {
                            FilterChip(
                                selected = date == draft.targetDate,
                                onClick = { onChooseDate(date) },
                                label = { Text(date.dayOfWeek.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                            )
                        }
                    }
                }
            }
            Button(onClick = onApprove, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                Text(if (isWorking) "Applying…" else "Apply for next week")
            }
        }
    }
}

@Composable
private fun PausedReview(padding: PaddingValues, onResume: () -> Unit) {
    CenteredState(padding, "Reviews are paused", "Your tracking continues normally. Resume whenever a weekly check-in would be useful.") {
        Button(onClick = onResume) { Text("Resume weekly reviews") }
    }
}

@Composable
private fun InsufficientReview(padding: PaddingValues, result: WeeklyReviewResult, onPause: () -> Unit) {
    CenteredState(padding, "Not enough workout history yet", "Add a weekly plan and complete or adapt a few sessions. Nutrition and steps stay optional.") {
        TextButton(onClick = onPause) { Text("Pause weekly reviews") }
    }
}

@Composable
private fun DecidedReview(padding: PaddingValues, snapshot: WeeklyReviewSnapshot.Decided) {
    val approved = snapshot.outcome.status == ReviewOutcomeStatus.APPROVED
    CenteredState(
        padding,
        if (approved) "Your choice is ready" else "Review dismissed",
        if (approved) {
            "Only the selected workout in the coming week was adjusted. Your recurring plan and templates are unchanged."
        } else {
            "Nothing in your plan was changed. A fresh review will be available for the next completed week."
        },
    ) { WeekRibbon(snapshot.result) }
}

@Composable
private fun ErrorReview(padding: PaddingValues, message: String?, onRetry: () -> Unit) {
    CenteredState(padding, "Review unavailable", message ?: "Your local records are safe. Try loading the review again.") {
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun CenteredState(
    padding: PaddingValues,
    title: String,
    description: String,
    action: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        action()
    }
}

private fun ReviewDraftType.shortLabel(): String = when (this) {
    ReviewDraftType.MINIMUM_SESSION -> "Minimum"
    ReviewDraftType.SHORTEN_SESSION -> "Shorter"
    ReviewDraftType.MOVE_SESSION -> "Move"
    ReviewDraftType.ADD_ONE_SET -> "+1 set"
}

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
