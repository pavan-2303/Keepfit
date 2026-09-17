package com.keepfit.feature.settings.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
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
    credentialRecoveryEnabled: Boolean = false,
    credentialRecoveryAvailable: Boolean = false,
    onCredentialRecoveryChanged: (Boolean) -> Unit = {},
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
    var exportPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (selectedSection == null) {
                Text(
                    "Daily actions stay in the main tabs. Setup, reminders, connections, and private data live here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsSection.entries.forEach { section ->
                    SettingsSectionRow(section) { selectedSection = section }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                }
                return@Column
            }
            TextButton(onClick = { selectedSection = null }) { Text("Back to settings") }
            Text(text = selectedSection!!.title, style = MaterialTheme.typography.headlineSmall)
            Text(selectedSection!!.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(18.dp))
            if (selectedSection == SettingsSection.APPEARANCE) SettingsCard("Motion") {
                MotionPreferenceRow(
                    reduceMotion = appSettings.reduceMotion,
                    onReduceMotionChanged = viewModel::saveReduceMotion,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (selectedSection == SettingsSection.GOALS) SettingsCard("Nutrition goals") {
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
            if (selectedSection == SettingsSection.TRAINING) SettingsCard("Units") {
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
            if (selectedSection == SettingsSection.TRAINING) SettingsCard("Workout timer") {
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
            if (selectedSection == SettingsSection.TRAINING) SettingsCard("Workout reminder") {
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
            if (selectedSection == SettingsSection.TRAINING) SettingsCard("Progress reminder") {
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
            if (selectedSection == SettingsSection.INTEGRATIONS) AssistantSettingsCard(
                onTestAssistantConnection = onTestAssistantConnection,
                onOpenAssistant = onOpenAssistant,
                credentialRecoveryEnabled = credentialRecoveryEnabled,
                credentialRecoveryAvailable = credentialRecoveryAvailable,
                onCredentialRecoveryChanged = onCredentialRecoveryChanged,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (selectedSection == SettingsSection.DATA) SettingsCard("Backup and restore") {
                BackupRecoveryOverview()
                Spacer(modifier = Modifier.height(18.dp))
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
            if (selectedSection == SettingsSection.ABOUT) SettingsCard("Keepfit") {
                Text("Private fitness tracking without an account, backend, ads, or analytics.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Fitness and Coach responses are general information, not medical advice.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selectedSection == SettingsSection.ABOUT) {
                Spacer(modifier = Modifier.height(12.dp))
                ExerciseCatalogueLegalNotice()
            }
        }
    }
}

@Composable
internal fun ExerciseCatalogueLegalNotice() {
    var detailsVisible by remember { mutableStateOf(false) }
    SettingsCard("Open-source licences") {
        Text(
            "Third-party catalogue and library notices.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { detailsVisible = !detailsVisible }) {
            Text(if (detailsVisible) "Hide details" else "View details")
        }
        if (detailsVisible) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                "Exercises Dataset by Hasan Emir Yıldırım. Metadata and English instructions are included under the MIT License.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Pinned source revision: 7455efae41b3",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Copyright (c) 2026 Hasan Emir Yıldırım. Gym visual images and GIFs are not included.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "25 original Keepfit movement figures are included as code-native artwork. They do not reuse the dataset's Gym visual media.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun AssistantSettingsCard(
    onTestAssistantConnection: () -> Unit,
    onOpenAssistant: () -> Unit,
    credentialRecoveryEnabled: Boolean = false,
    credentialRecoveryAvailable: Boolean = false,
    onCredentialRecoveryChanged: (Boolean) -> Unit = {},
) {
    SettingsCard("OpenRouter assistant") {
        Text(
            "Coach uses the OpenRouter account you connect from its tab. There is no Keepfit daily request cap and no pasted API key.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "OpenRouter and the selected model provider control free-tier, rate, and credit limits.",
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Recover access after reinstall", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (credentialRecoveryAvailable) {
                        "Optional. Google Block Store keeps the OpenRouter token separate from fitness backup and Keepfit verifies it before reuse."
                    } else {
                        "Unavailable on this device. You can always reconnect OpenRouter from Coach."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = credentialRecoveryEnabled,
                onCheckedChange = onCredentialRecoveryChanged,
                enabled = credentialRecoveryAvailable || credentialRecoveryEnabled,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow {
            FilledTonalButton(onClick = onTestAssistantConnection) {
                Text("Check saved access")
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onOpenAssistant,
            ) {
                Text("Open assistant")
            }
        }
    }
}

private enum class SettingsSection(
    val title: String,
    val description: String,
) {
    APPEARANCE("Appearance and accessibility", "Motion and visual comfort"),
    GOALS("Goals and nutrition", "Daily calorie and macro targets"),
    TRAINING("Training preferences", "Units, rest timer, and reminders"),
    INTEGRATIONS("Connections", "Coach and optional device services"),
    DATA("Data and backup", "Automatic recovery and complete encrypted backup"),
    ABOUT("About and safety", "Privacy and guidance boundaries"),
}

@Composable
internal fun MotionPreferenceRow(
    reduceMotion: Boolean,
    onReduceMotionChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .toggleable(
                value = reduceMotion,
                role = Role.Switch,
                onValueChange = onReduceMotionChanged,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Reduce motion", style = MaterialTheme.typography.titleSmall)
            Text(
                "Keeps every state change visible while removing nonessential transitions and celebrations.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = reduceMotion, onCheckedChange = null)
    }
}

@Composable
internal fun BackupRecoveryOverview() {
    Text(
        "Automatic recovery",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Android may restore your fitness records and ordinary settings after reinstall when device backup is available. Timing and restore are controlled by Android and are not guaranteed.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        "Private photos, imported exercise media, and OpenRouter access are not included. Use an encrypted Keepfit backup for complete recovery.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsSectionRow(section: SettingsSection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            Text(section.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
