package com.keepfit.feature.settings.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.settings.SettingsViewModel
import com.keepfit.feature.settings.data.BackupPreview
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenAssistant: () -> Unit = {},
    externalMessage: String? = null,
    onExternalMessageShown: () -> Unit = {},
    onTestAssistantConnection: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val nutritionGoals by viewModel.nutritionGoals.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val backupPreview by viewModel.backupPreview.collectAsStateWithLifecycle()
    val restartRequired by viewModel.restartRequired.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var calorieGoal by remember(nutritionGoals.calorieGoal) { mutableStateOf(nutritionGoals.calorieGoal?.toInt()?.toString().orEmpty()) }
    var proteinGoal by remember(nutritionGoals.proteinGoalGrams) { mutableStateOf(nutritionGoals.proteinGoalGrams?.toString().orEmpty()) }
    var carbohydrateGoal by remember(nutritionGoals.carbohydrateGoalGrams) { mutableStateOf(nutritionGoals.carbohydrateGoalGrams?.toString().orEmpty()) }
    var fatGoal by remember(nutritionGoals.fatGoalGrams) { mutableStateOf(nutritionGoals.fatGoalGrams?.toString().orEmpty()) }
    var restTimerSeconds by remember(appSettings.restTimerSeconds) { mutableStateOf(appSettings.restTimerSeconds.toString()) }
    var workoutReminderEnabled by remember(appSettings.workoutReminder.enabled) { mutableStateOf(appSettings.workoutReminder.enabled) }
    var workoutHour by remember(appSettings.workoutReminder.hour) { mutableStateOf(appSettings.workoutReminder.hour.toString()) }
    var workoutMinute by remember(appSettings.workoutReminder.minute) { mutableStateOf(appSettings.workoutReminder.minute.toString()) }
    var transformationReminderEnabled by remember(appSettings.transformationReminder.enabled) { mutableStateOf(appSettings.transformationReminder.enabled) }
    var transformationDay by remember(appSettings.transformationReminder.dayOfWeek) { mutableStateOf(appSettings.transformationReminder.dayOfWeek) }
    var transformationHour by remember(appSettings.transformationReminder.hour) { mutableStateOf(appSettings.transformationReminder.hour.toString()) }
    var transformationMinute by remember(appSettings.transformationReminder.minute) { mutableStateOf(appSettings.transformationReminder.minute.toString()) }
    var assistantEnabled by remember(appSettings.assistant.enabled) { mutableStateOf(appSettings.assistant.enabled) }
    var exportPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val canOpenAssistant = appSettings.assistant.enabled

    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.exportBackup(it, exportPassphrase) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectedRestoreUri = uri
            viewModel.clearBackupPreview()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    LaunchedEffect(externalMessage) {
        externalMessage?.let {
            snackbarHostState.showSnackbar(it)
            onExternalMessageShown()
        }
    }

    LaunchedEffect(restartRequired) {
        if (!restartRequired) return@LaunchedEffect
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
        (context as? Activity)?.finishAffinity()
        exitProcess(0)
    }

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(18.dp))
            SettingsCard("Nutrition goals") {
                OutlinedTextField(calorieGoal, { calorieGoal = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(proteinGoal, { proteinGoal = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(carbohydrateGoal, { carbohydrateGoal = it }, label = { Text("Carbs (g)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(fatGoal, { fatGoal = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { viewModel.saveNutritionGoals(calorieGoal, proteinGoal, carbohydrateGoal, fatGoal) }) {
                    Text("Save goals")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard("Units") {
                Text("Weight unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow {
                    FilledTonalButton(
                        onClick = { viewModel.saveUnits(WeightUnit.KG, appSettings.measurementUnit) },
                        enabled = appSettings.weightUnit != WeightUnit.KG,
                    ) { Text("KG") }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.saveUnits(WeightUnit.LB, appSettings.measurementUnit) },
                        enabled = appSettings.weightUnit != WeightUnit.LB,
                    ) { Text("LB") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Measurement unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow {
                    FilledTonalButton(
                        onClick = { viewModel.saveUnits(appSettings.weightUnit, MeasurementUnit.CM) },
                        enabled = appSettings.measurementUnit != MeasurementUnit.CM,
                    ) { Text("CM") }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.saveUnits(appSettings.weightUnit, MeasurementUnit.IN) },
                        enabled = appSettings.measurementUnit != MeasurementUnit.IN,
                    ) { Text("IN") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard("Workout timer") {
                OutlinedTextField(
                    value = restTimerSeconds,
                    onValueChange = { restTimerSeconds = it },
                    label = { Text("Rest timer seconds") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { viewModel.saveRestTimer(restTimerSeconds) }) {
                    Text("Save timer")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard("Workout reminder") {
                ReminderToggle("Enable daily workout reminder", workoutReminderEnabled) {
                    workoutReminderEnabled = it
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(workoutHour, { workoutHour = it }, label = { Text("Hour (0-23)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(workoutMinute, { workoutMinute = it }, label = { Text("Minute (0-59)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { viewModel.saveWorkoutReminder(workoutReminderEnabled, workoutHour, workoutMinute) }) {
                    Text("Save workout reminder")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard("Progress reminder") {
                ReminderToggle("Enable weekly progress reminder", transformationReminderEnabled) {
                    transformationReminderEnabled = it
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Weekday", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow {
                    DayOfWeek.entries.forEach { day ->
                        FilledTonalButton(
                            onClick = { transformationDay = day },
                            enabled = transformationDay != day,
                        ) { Text(day.name.take(3)) }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(transformationHour, { transformationHour = it }, label = { Text("Hour (0-23)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(transformationMinute, { transformationMinute = it }, label = { Text("Minute (0-59)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.saveTransformationReminder(
                            transformationReminderEnabled,
                            transformationDay.value,
                            transformationHour,
                            transformationMinute,
                        )
                    },
                ) {
                    Text("Save progress reminder")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AssistantSettingsCard(
                assistantEnabled = assistantEnabled,
                canOpenAssistant = canOpenAssistant,
                onAssistantEnabledChange = { assistantEnabled = it },
                onSaveAssistant = {
                    viewModel.saveAssistantSettings(enabled = assistantEnabled)
                },
                onTestAssistantConnection = onTestAssistantConnection,
                onOpenAssistant = onOpenAssistant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCard("Backup and restore") {
                Text(
                    "Encrypted backup",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exportPassphrase,
                    onValueChange = { exportPassphrase = it },
                    label = { Text("Export passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        exportLauncher.launch("keepfit-backup-${java.time.LocalDate.now()}.kfit")
                    },
                ) {
                    Text("Export backup")
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "Restore backup",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = restorePassphrase,
                    onValueChange = {
                        restorePassphrase = it
                        viewModel.clearBackupPreview()
                    },
                    label = { Text("Restore passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(onClick = { restoreLauncher.launch(arrayOf("*/*")) }) {
                    Text(if (selectedRestoreUri == null) "Choose backup file" else "Change backup file")
                }
                selectedRestoreUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selected: ${uri.lastPathSegment ?: uri.toString()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { viewModel.previewBackup(uri, restorePassphrase) }) {
                        Text("Preview restore")
                    }
                }
                backupPreview?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    BackupPreviewCard(preview = it)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { viewModel.restoreBackup(selectedRestoreUri!!, restorePassphrase) }) {
                        Text("Restore and replace local data")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This replaces the current local database, private media, and settings.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AssistantSettingsCard(
    assistantEnabled: Boolean,
    canOpenAssistant: Boolean,
    onAssistantEnabledChange: (Boolean) -> Unit,
    onSaveAssistant: () -> Unit,
    onTestAssistantConnection: () -> Unit,
    onOpenAssistant: () -> Unit,
) {
    SettingsCard("Ollama Cloud assistant") {
        ReminderToggle("Enable optional Ollama Cloud assistant", assistantEnabled, onAssistantEnabledChange)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This build uses an Ollama Cloud configuration injected during app build, not entered at runtime.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "General chat uses mistral-large-3:675b. Deeper planning and analysis use qwen3.5:397b.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Assistant replies are general fitness guidance, not medical advice.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow {
            Button(onClick = onSaveAssistant) {
                Text("Save assistant")
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onTestAssistantConnection) {
                Text("Test connection")
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onOpenAssistant,
                enabled = canOpenAssistant,
            ) {
                Text("Open assistant")
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BackupPreviewCard(preview: BackupPreview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Backup preview", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Exported: ${preview.exportedAtUtcEpochMillis.toLocalDateTimeLabel()}")
            Text("Schema version: ${preview.databaseSchemaVersion}")
            Text("Media size: ${preview.mediaSizeBytes.toReadableSize()}")
            Spacer(modifier = Modifier.height(8.dp))
            preview.recordCounts.forEach { (label, count) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(count.toString())
                }
            }
        }
    }
}

@Composable
private fun ReminderToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Long.toReadableSize(): String = when {
    this >= 1_048_576 -> String.format("%.1f MB", this / 1_048_576.0)
    this >= 1_024 -> String.format("%.1f KB", this / 1_024.0)
    else -> "$this B"
}

private fun Long.toLocalDateTimeLabel(): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime())
