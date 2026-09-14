package com.keepfit.feature.workouts.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepfit.feature.workouts.data.ActiveExercise
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.WorkoutFeedback

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    exercises: List<Exercise>,
    timerSeconds: Int?,
    snackbarHostState: SnackbarHostState,
    isWriting: Boolean,
    onAddSet: (String, String, String) -> Unit,
    onRepeatPrevious: (String) -> Unit,
    onSaveNotes: (String, String) -> Unit,
    onStartTimer: () -> Unit,
    onSubstitute: (String, String) -> Unit,
    onMinimum: () -> Unit,
    onComplete: (WorkoutFeedback?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAdaptMenu by rememberSaveable { mutableStateOf(false) }
    var showMinimumConfirmation by rememberSaveable { mutableStateOf(false) }
    var substitutionSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var substitutionReplacementId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCompletion by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { showCompletion = true },
                    enabled = !isWriting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finish workout")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            SessionHeader(workout, timerSeconds, onStartTimer) { showAdaptMenu = true }
            Spacer(modifier = Modifier.height(16.dp))
            workout.exercises.forEachIndexed { index, exercise ->
                ActiveExerciseCard(
                    exercise = exercise,
                    index = index,
                    enabled = !isWriting,
                    onAddSet = onAddSet,
                    onRepeatPrevious = onRepeatPrevious,
                    onSaveNotes = onSaveNotes,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showAdaptMenu) {
        AdaptSessionDialog(
            workout = workout,
            onDismiss = { showAdaptMenu = false },
            onMinimum = {
                showAdaptMenu = false
                showMinimumConfirmation = true
            },
            onChooseSubstitution = { exerciseLogId ->
                showAdaptMenu = false
                substitutionSourceId = exerciseLogId
            },
        )
    }
    if (showMinimumConfirmation) {
        AlertDialog(
            onDismissRequest = { showMinimumConfirmation = false },
            title = { Text("Keep first 2 exercises") },
            text = {
                Text("This keeps the first two exercises at up to two sets. Logged work is always protected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMinimumConfirmation = false
                        onMinimum()
                    },
                    enabled = !isWriting,
                ) { Text("Confirm minimum session") }
            },
            dismissButton = {
                TextButton(onClick = { showMinimumConfirmation = false }) { Text("Cancel") }
            },
        )
    }
    substitutionSourceId?.let { sourceId ->
        val activeExerciseIds = workout.exercises.map(ActiveExercise::exerciseId).toSet()
        val candidates = exercises.filter { it.id !in activeExerciseIds }
        val replacementId = substitutionReplacementId
        if (replacementId == null) {
            AlertDialog(
                onDismissRequest = { substitutionSourceId = null },
                title = { Text("Choose replacement") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (candidates.isEmpty()) {
                            Text("Add another exercise to your library before substituting.")
                        } else {
                            candidates.forEach { exercise ->
                                OutlinedButton(
                                    onClick = { substitutionReplacementId = exercise.id },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text(exercise.name) }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { substitutionSourceId = null }) { Text("Cancel") }
                },
            )
        } else {
            val source = workout.exercises.find { it.exerciseLogId == sourceId }
            val replacement = exercises.find { it.id == replacementId }
            AlertDialog(
                onDismissRequest = { substitutionReplacementId = null },
                title = { Text("Replace ${source?.exerciseName.orEmpty()}?") },
                text = {
                    Text("Use ${replacement?.name.orEmpty()} for this active session only. Your plan stays unchanged.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            substitutionSourceId = null
                            substitutionReplacementId = null
                            onSubstitute(sourceId, replacementId)
                        },
                        enabled = !isWriting,
                    ) { Text("Confirm substitution") }
                },
                dismissButton = {
                    TextButton(onClick = { substitutionReplacementId = null }) { Text("Back") }
                },
            )
        }
    }
    if (showCompletion) {
        CompletionDialog(
            enabled = !isWriting,
            onDismiss = { showCompletion = false },
            onComplete = {
                showCompletion = false
                onComplete(it)
            },
        )
    }
}

@Composable
private fun SessionHeader(
    workout: ActiveWorkout,
    timerSeconds: Int?,
    onStartTimer: () -> Unit,
    onAdapt: () -> Unit,
) {
    val completed = workout.exercises.sumOf { it.sets.size }
    val target = workout.exercises.sumOf { exercise ->
        exercise.targetSets ?: exercise.sets.size.coerceAtLeast(1)
    }.coerceAtLeast(1)
    val progress = (completed.toFloat() / target).coerceIn(0f, 1f)
    Text(
        if (workout.sessionVariant == "FULL") "ACTIVE WORKOUT" else "${workout.sessionVariant} SESSION",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(workout.templateName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(10.dp))
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "$completed of $target sets complete",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onStartTimer, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.Timer, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(timerSeconds?.toTimerLabel() ?: "Start rest")
        }
        OutlinedButton(onClick = onAdapt, modifier = Modifier.weight(1f)) {
            Text("Adapt session")
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exercise: ActiveExercise,
    index: Int,
    enabled: Boolean,
    onAddSet: (String, String, String) -> Unit,
    onRepeatPrevious: (String) -> Unit,
    onSaveNotes: (String, String) -> Unit,
) {
    val suggestion = exercise.nextSetSuggestion
    var repetitions by remember(exercise.exerciseLogId, exercise.sets.size, suggestion?.repetitions) {
        mutableStateOf(suggestion?.repetitions?.toString().orEmpty())
    }
    var weight by remember(exercise.exerciseLogId, exercise.sets.size, suggestion?.weightKg) {
        mutableStateOf(suggestion?.weightKg?.toInputNumber().orEmpty())
    }
    var noteText by remember(exercise.exerciseLogId, exercise.notes) {
        mutableStateOf(exercise.notes.orEmpty())
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${index + 1}. ${exercise.exerciseName}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        exercise.targetLabel(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    "${exercise.sets.size}/${exercise.targetSets ?: "–"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (exercise.previousSets.isEmpty()) {
                Text("No completed history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(
                    "Previous · " + exercise.previousSets.joinToString("  |  ") {
                        "${it.weightKg.toInputNumber()} kg × ${it.repetitions}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (exercise.sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                exercise.sets.forEachIndexed { setIndex, set ->
                    Text("Set ${setIndex + 1}  ·  ${set.weightKg.toInputNumber()} kg × ${set.repetitions}")
                }
            }
            exercise.progressionSuggestion?.let { progression ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        progression.explanation,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repetitions,
                    onValueChange = { repetitions = it },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Repetitions for ${exercise.exerciseName}" },
                    label = { Text("Reps") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Weight for ${exercise.exerciseName}" },
                    label = { Text("Weight kg") },
                    singleLine = true,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onAddSet(exercise.exerciseLogId, repetitions, weight) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log set")
            }
            OutlinedButton(
                onClick = { onRepeatPrevious(exercise.exerciseLogId) },
                enabled = enabled && suggestion != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Repeat previous") }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exercise notes") },
                minLines = 1,
            )
            TextButton(
                onClick = { onSaveNotes(exercise.exerciseLogId, noteText) },
                enabled = enabled,
            ) { Text("Save notes") }
        }
    }
}

@Composable
private fun AdaptSessionDialog(
    workout: ActiveWorkout,
    onDismiss: () -> Unit,
    onMinimum: () -> Unit,
    onChooseSubstitution: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adapt session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Changes apply only to this active workout.")
                FilledTonalButton(onClick = onMinimum, modifier = Modifier.fillMaxWidth()) {
                    Text("Use minimum session")
                }
                workout.exercises.filter { it.sets.isEmpty() }.forEach { exercise ->
                    OutlinedButton(
                        onClick = { onChooseSubstitution(exercise.exerciseLogId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Replace ${exercise.exerciseName}") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun CompletionDialog(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onComplete: (WorkoutFeedback?) -> Unit,
) {
    var energy by rememberSaveable { mutableStateOf<Int?>(null) }
    var difficulty by rememberSaveable { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finish workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Optional: how did today feel?")
                Text("Energy", style = MaterialTheme.typography.labelLarge)
                RatingChips("Energy", energy) { energy = it }
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                RatingChips("Difficulty", difficulty) { difficulty = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onComplete(WorkoutFeedback(energy, difficulty)) },
                enabled = enabled,
            ) { Text("Save and finish") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { onComplete(null) }, enabled = enabled) {
                    Text("Finish without feedback")
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun RatingChips(label: String, selected: Int?, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..5).forEach { rating ->
            FilterChip(
                selected = selected == rating,
                onClick = { onSelected(rating) },
                label = { Text(rating.toString()) },
                modifier = Modifier.semantics { contentDescription = "$label $rating" },
            )
        }
    }
}

private fun ActiveExercise.targetLabel(): String {
    val sets = targetSets?.let { "$it sets" } ?: "open sets"
    val reps = targetReps?.let { "$it reps" } ?: "open reps"
    return "Target $sets · $reps"
}

private fun Int.toTimerLabel(): String = "Rest ${this / 60}:${(this % 60).toString().padStart(2, '0')}"

private fun Double.toInputNumber(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
