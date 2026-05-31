package com.keepfit.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.settings.SettingsViewModel
import java.time.DayOfWeek

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val nutritionGoals by viewModel.nutritionGoals.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
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

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
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
private fun ReminderToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
