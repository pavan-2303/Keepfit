package com.keepfit.feature.workouts.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keepfit.feature.workouts.data.ActiveWorkout

@Composable
fun ActiveWorkoutScreen(
    workout: ActiveWorkout,
    timerSeconds: Int?,
    snackbarHostState: SnackbarHostState,
    onAddSet: (String, String, String) -> Unit,
    onSaveNotes: (String, String) -> Unit,
    onStartTimer: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("ACTIVE WORKOUT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(workout.templateName, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onStartTimer) {
                Icon(Icons.Outlined.Timer, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(timerSeconds?.let { "Rest timer ${it}s" } ?: "Start 90s rest timer")
            }
            Spacer(modifier = Modifier.height(16.dp))
            workout.exercises.forEach { exercise ->
                ActiveExerciseCard(
                    exerciseLogId = exercise.exerciseLogId,
                    exerciseName = exercise.exerciseName,
                    notes = exercise.notes.orEmpty(),
                    previous = exercise.previousSet?.let { "${it.weightKg} kg x ${it.repetitions}" },
                    sets = exercise.sets.map { "${it.weightKg} kg x ${it.repetitions}" },
                    onAddSet = onAddSet,
                    onSaveNotes = onSaveNotes,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Complete workout")
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exerciseLogId: String,
    exerciseName: String,
    notes: String,
    previous: String?,
    sets: List<String>,
    onAddSet: (String, String, String) -> Unit,
    onSaveNotes: (String, String) -> Unit,
) {
    var repetitions by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var noteText by remember(notes) { mutableStateOf(notes) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(exerciseName, style = MaterialTheme.typography.titleLarge)
            Text(
                previous?.let { "Previous: $it" } ?: "No previous logged set",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                sets.forEachIndexed { index, set -> Text("Set ${index + 1}: $set") }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repetitions,
                    onValueChange = { repetitions = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Reps") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Weight kg") },
                    singleLine = true,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    onAddSet(exerciseLogId, repetitions, weight)
                    repetitions = ""
                    weight = ""
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Add set")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exercise notes") },
            )
            OutlinedButton(onClick = { onSaveNotes(exerciseLogId, noteText) }) {
                Text("Save notes")
            }
        }
    }
}

