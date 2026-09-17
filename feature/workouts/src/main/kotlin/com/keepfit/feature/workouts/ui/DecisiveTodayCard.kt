package com.keepfit.feature.workouts.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.core.designsystem.KeepfitPaceCard
import com.keepfit.core.designsystem.KeepfitStatusMark
import com.keepfit.core.designsystem.LocalKeepfitMotionSettings
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayChangeType
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutDecision
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutState
import com.keepfit.feature.workouts.today.TodayWorkoutStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DecisiveTodayCard(
    state: TodayWorkoutState,
    preview: TodayWorkoutPreview? = null,
    exercises: List<Exercise> = emptyList(),
    today: LocalDate = LocalDate.now(),
    isWorking: Boolean = false,
    message: String? = null,
    onStart: (TodayWorkoutAction) -> Unit = {},
    onResume: () -> Unit = {},
    onPreviewChange: (TodayChangeRequest) -> Unit = {},
    onConfirmPreview: () -> Unit = {},
    onDismissPreview: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    onRestore: (TodayWorkoutAction) -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
) {
    var dialog by rememberSaveable { mutableStateOf<TodayActionDialog?>(null) }
    val primary = state.primary
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val motionSettings = LocalKeepfitMotionSettings.current
    val hapticFeedback = LocalHapticFeedback.current
    var previousStatus by rememberSaveable { mutableStateOf(state.status) }

    LaunchedEffect(state.status) {
        if (previousStatus != TodayWorkoutStatus.COMPLETED && state.status == TodayWorkoutStatus.COMPLETED) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        previousStatus = state.status
    }

    KeepfitPaceCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = when (state.status) {
            TodayWorkoutStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
            TodayWorkoutStatus.RESUME -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        TodayCardHeader(status = state.status)
        Spacer(modifier = Modifier.height(if (largeText) 10.dp else 18.dp))
        if (motionSettings.reduceMotion) {
            TodayCardCopy(state = state, largeText = largeText)
        } else {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(100))
                },
                label = "Today workout state",
            ) { animatedState ->
                TodayCardCopy(state = animatedState, largeText = largeText)
            }
        }
        Spacer(modifier = Modifier.height(if (largeText) 10.dp else 18.dp))
        TodayPrimaryAction(
            state = state,
            isWorking = isWorking,
            onStart = onStart,
            onResume = onResume,
            onRestore = onRestore,
            onOpenWorkouts = onOpenWorkouts,
        )
        if (state.status == TodayWorkoutStatus.PLANNED && primary != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { dialog = TodayActionDialog.ADAPT },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Adapt today")
            }
        }
        state.missed?.let { missed ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "A session to recover",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = missed.originalDate.format(dayFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onPreviewChange(
                                missed.request(
                                    type = TodayChangeType.RESCHEDULE,
                                    targetDate = today,
                                ),
                            )
                        },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recover ${missed.title}")
                    }
                }
            }
        }
        message?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onDismissMessage) {
                Text("Dismiss")
            }
        }
    }

    when (dialog) {
        TodayActionDialog.ADAPT -> AdaptTodayDialog(
            action = requireNotNull(primary),
            onDismiss = { dialog = null },
            onChoose = { request ->
                dialog = null
                onPreviewChange(request)
            },
            onOpenSubstitution = { dialog = TodayActionDialog.SUBSTITUTE },
            onOpenReschedule = { dialog = TodayActionDialog.RESCHEDULE },
        )
        TodayActionDialog.SUBSTITUTE -> SubstituteExerciseDialog(
            action = requireNotNull(primary),
            exercises = exercises,
            onDismiss = { dialog = null },
            onChoose = { sourceId, replacementId ->
                dialog = null
                onPreviewChange(
                    primary.request(
                        type = TodayChangeType.SUBSTITUTE,
                        sourceExerciseId = sourceId,
                        replacementExerciseId = replacementId,
                    ),
                )
            },
        )
        TodayActionDialog.RESCHEDULE -> RescheduleWorkoutDialog(
            today = today,
            onDismiss = { dialog = null },
            onChoose = { targetDate ->
                dialog = null
                onPreviewChange(primary!!.request(TodayChangeType.RESCHEDULE, targetDate = targetDate))
            },
        )
        null -> Unit
    }

    preview?.let {
        TodayChangePreviewDialog(
            preview = it,
            isWorking = isWorking,
            onConfirm = onConfirmPreview,
            onDismiss = onDismissPreview,
        )
    }
}

@Composable
private fun TodayCardHeader(status: TodayWorkoutStatus) {
    Column {
        KeepfitStatusMark(
            label = "Today's move",
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayCardCopy(state: TodayWorkoutState, largeText: Boolean) {
    val action = state.primary
    val title = when (state.status) {
        TodayWorkoutStatus.LOADING -> "Finding your next move"
        TodayWorkoutStatus.RESUME -> state.activeWorkout?.templateName ?: "Workout in progress"
        TodayWorkoutStatus.COMPLETED -> "Workout complete"
        TodayWorkoutStatus.PLANNED -> action?.title ?: "Workout ready"
        TodayWorkoutStatus.RESCHEDULED -> action?.title ?: "Workout moved"
        TodayWorkoutStatus.SKIPPED -> action?.title ?: "Workout skipped"
        TodayWorkoutStatus.REST_DAY -> "Recovery day"
    }
    val description = when (state.status) {
        TodayWorkoutStatus.LOADING -> "Checking your local plan."
        TodayWorkoutStatus.RESUME -> "Continue where you left off. Your logged work is safe."
        TodayWorkoutStatus.COMPLETED -> "Done for today. The rest of the day can stay simple."
        TodayWorkoutStatus.PLANNED -> action?.plannedDescription() ?: "Your workout is ready."
        TodayWorkoutStatus.RESCHEDULED -> "Moved to ${action?.scheduledDate?.format(dayFormatter)}."
        TodayWorkoutStatus.SKIPPED -> "Skipped for today"
        TodayWorkoutStatus.REST_DAY -> "No workout planned. Adjust the week if needed."
    }
    Column {
        Text(
            text = title,
            style = if (largeText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(if (largeText) 2.dp else 6.dp))
        Text(
            text = description,
            style = if (largeText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayPrimaryAction(
    state: TodayWorkoutState,
    isWorking: Boolean,
    onStart: (TodayWorkoutAction) -> Unit,
    onResume: () -> Unit,
    onRestore: (TodayWorkoutAction) -> Unit,
    onOpenWorkouts: () -> Unit,
) {
    val action = state.primary
    when (state.status) {
        TodayWorkoutStatus.LOADING -> Unit
        TodayWorkoutStatus.RESUME -> Button(
            onClick = onResume,
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Resume workout")
        }
        TodayWorkoutStatus.PLANNED -> Button(
            onClick = { onStart(requireNotNull(action)) },
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Outlined.Bolt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start now")
        }
        TodayWorkoutStatus.SKIPPED,
        TodayWorkoutStatus.RESCHEDULED,
        -> FilledTonalButton(
            onClick = { onRestore(requireNotNull(action)) },
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Restore workout")
        }
        TodayWorkoutStatus.COMPLETED,
        TodayWorkoutStatus.REST_DAY,
        -> OutlinedButton(
            onClick = onOpenWorkouts,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (state.status == TodayWorkoutStatus.COMPLETED) "View workouts" else "Adjust plan")
        }
    }
}

@Composable
private fun AdaptTodayDialog(
    action: TodayWorkoutAction,
    onDismiss: () -> Unit,
    onChoose: (TodayChangeRequest) -> Unit,
    onOpenSubstitution: () -> Unit,
    onOpenReschedule: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adapt today's workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose the version that fits today. Your reusable template will not change.")
                OutlinedButton(
                    onClick = { onChoose(action.request(TodayChangeType.SHORTEN)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Shorter workout") }
                OutlinedButton(
                    onClick = { onChoose(action.request(TodayChangeType.MINIMUM)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Minimum session") }
                OutlinedButton(onClick = onOpenSubstitution, modifier = Modifier.fillMaxWidth()) {
                    Text("Swap an exercise")
                }
                OutlinedButton(onClick = onOpenReschedule, modifier = Modifier.fillMaxWidth()) {
                    Text("Move to another day")
                }
                TextButton(
                    onClick = { onChoose(action.request(TodayChangeType.SKIP)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip today") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SubstituteExerciseDialog(
    action: TodayWorkoutAction,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onChoose: (String, String) -> Unit,
) {
    var sourceExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val replacements = exercises.filter { candidate -> action.exercises.none { it.exerciseId == candidate.id } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Swap one exercise") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Text("1. Choose what to replace", fontWeight = FontWeight.Bold) }
                items(action.exercises, key = { "source-${it.exerciseId}" }) { exercise ->
                    OutlinedButton(
                        onClick = { sourceExerciseId = exercise.exerciseId },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (sourceExerciseId == exercise.exerciseId) "Selected: ${exercise.exerciseName}" else exercise.exerciseName)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("2. Choose the replacement", fontWeight = FontWeight.Bold)
                }
                if (replacements.isEmpty()) {
                    item { Text("Add another exercise to your library before making a swap.") }
                }
                items(replacements, key = { "replacement-${it.id}" }) { exercise ->
                    FilledTonalButton(
                        onClick = { sourceExerciseId?.let { onChoose(it, exercise.id) } },
                        enabled = sourceExerciseId != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(exercise.name) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RescheduleWorkoutDialog(
    today: LocalDate,
    onDismiss: () -> Unit,
    onChoose: (LocalDate) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move this workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a day in the next week. Existing workouts on that day stay in place.")
                (1L..7L).forEach { offset ->
                    val date = today.plusDays(offset)
                    OutlinedButton(onClick = { onChoose(date) }, modifier = Modifier.fillMaxWidth()) {
                        Text(date.format(fullDayFormatter))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TodayChangePreviewDialog(
    preview: TodayWorkoutPreview,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.EditCalendar, contentDescription = null) },
        title = { Text("Review today's change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(preview.title, style = MaterialTheme.typography.titleMedium)
                PreviewLine("Current", preview.currentDescription)
                PreviewLine("Proposed", preview.proposedDescription)
                PreviewLine("Why", preview.reason)
                Text(
                    text = "Nothing changes until you confirm.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isWorking) { Text("Confirm change") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Keep current") }
        },
    )
}

@Composable
private fun PreviewLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun TodayWorkoutAction.request(
    type: TodayChangeType,
    sourceExerciseId: String? = null,
    replacementExerciseId: String? = null,
    targetDate: LocalDate? = null,
) = TodayChangeRequest(
    plannedWorkoutId = plannedWorkoutId,
    occurrenceId = occurrenceId,
    originalDate = originalDate,
    type = type,
    sourceExerciseId = sourceExerciseId,
    replacementExerciseId = replacementExerciseId,
    targetDate = targetDate,
)

private fun TodayWorkoutAction.plannedDescription(): String {
    val exerciseCount = if (exercises.size == 1) "1 exercise" else "${exercises.size} exercises"
    val version = when (decision) {
        TodayWorkoutDecision.FULL -> "Full session"
        TodayWorkoutDecision.SHORTENED -> "Shorter session"
        TodayWorkoutDecision.MINIMUM -> "Minimum session"
        TodayWorkoutDecision.SUBSTITUTED -> "Adjusted session"
        TodayWorkoutDecision.RESCHEDULED -> "Recovered session"
        TodayWorkoutDecision.SKIPPED -> "Skipped"
    }
    return "$version • $exerciseCount"
}

private fun TodayWorkoutStatus.label(): String = when (this) {
    TodayWorkoutStatus.LOADING -> "Checking plan"
    TodayWorkoutStatus.RESUME -> "In progress"
    TodayWorkoutStatus.COMPLETED -> "Done"
    TodayWorkoutStatus.PLANNED -> "Ready"
    TodayWorkoutStatus.RESCHEDULED -> "Moved"
    TodayWorkoutStatus.SKIPPED -> "Skipped"
    TodayWorkoutStatus.REST_DAY -> "Recovery"
}

private enum class TodayActionDialog {
    ADAPT,
    SUBSTITUTE,
    RESCHEDULE,
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val fullDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
