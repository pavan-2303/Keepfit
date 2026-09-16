package com.keepfit.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.keepfit.core.model.calculateAge
import com.keepfit.core.model.calculateBodyMassIndex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    validationMessage: String?,
    onSave: (displayName: String, heightCm: String, birthDate: LocalDate?, startingWeightKg: String) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var heightCm by rememberSaveable { mutableStateOf("") }
    var startingWeightKg by rememberSaveable { mutableStateOf("") }
    var birthDateValue by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val birthDate = birthDateValue.takeIf(String::isNotBlank)?.let(LocalDate::parse)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProfileProgress(step = step)
        if (step == 0) {
            IdentityStep(
                displayName = displayName,
                onDisplayNameChanged = { displayName = it },
                onContinue = { step = 1 },
            )
        } else {
            BaselineStep(
                birthDate = birthDate,
                heightCm = heightCm,
                startingWeightKg = startingWeightKg,
                onChooseBirthDate = { showDatePicker = true },
                onClearBirthDate = { birthDateValue = "" },
                onHeightChanged = { heightCm = it },
                onWeightChanged = { startingWeightKg = it },
            )
        }

        validationMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(4.dp))
        if (step == 1) {
            TextButton(
                onClick = { onSave(displayName, "", null, "") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Skip optional details") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { onSave(displayName, heightCm, birthDate, startingWeightKg) },
                    modifier = Modifier.weight(1f),
                ) { Text("Save and continue") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "  Stored privately on this device. No account required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            birthDateValue = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showDatePicker = false
                    },
                ) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ProfileProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PROFILE SETUP  ·  ${step + 1} OF 2",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(progress = { (step + 1) / 2f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun IdentityStep(
    displayName: String,
    onDisplayNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Text("Your profile", style = MaterialTheme.typography.headlineMedium)
    Text(
        "What should Keepfit call you? This profile keeps its workouts, meals, and progress separate.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = displayName,
        onValueChange = onDisplayNameChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (displayName.isNotBlank()) onContinue() }),
    )
    Button(
        onClick = onContinue,
        enabled = displayName.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Continue") }
}

@Composable
private fun BaselineStep(
    birthDate: LocalDate?,
    heightCm: String,
    startingWeightKg: String,
    onChooseBirthDate: () -> Unit,
    onClearBirthDate: () -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
) {
    val bmi = calculateBodyMassIndex(heightCm.toDoubleOrNull(), startingWeightKg.toDoubleOrNull())
    val age = calculateAge(birthDate, LocalDate.now())

    Text("Your baseline", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Optional. Add only what feels useful; you can change it later in your profile and Progress.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onChooseBirthDate, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
        Text(
            if (birthDate == null) "  Add birth date"
            else "  ${birthDate.format(DateTimeFormatter.ofPattern("d MMM uuuu"))}",
        )
    }
    if (birthDate != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Used to show age context${age?.let { ": $it years" }.orEmpty()}.")
            TextButton(onClick = onClearBirthDate) { Text("Remove") }
        }
    }
    OutlinedTextField(
        value = heightCm,
        onValueChange = onHeightChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Height in cm") },
        supportingText = { Text("Used with weight to calculate BMI locally.") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    )
    OutlinedTextField(
        value = startingWeightKg,
        onValueChange = onWeightChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Starting weight in kg") },
        supportingText = { Text("Saved as your first dated Progress measurement.") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
    )
    if (bmi != null) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("BMI $bmi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                Text(
                    "A general reference calculated on this device—not a diagnosis or a complete picture of health.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
