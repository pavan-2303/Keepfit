package com.keepfit.feature.workouts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.feature.workouts.planning.EquipmentOption
import com.keepfit.feature.workouts.planning.ExperienceLevel
import com.keepfit.feature.workouts.planning.JourneyGoal
import com.keepfit.feature.workouts.planning.StarterExerciseCatalog
import com.keepfit.feature.workouts.planning.StarterExerciseDefinition
import com.keepfit.feature.workouts.planning.StarterPlanStage
import com.keepfit.feature.workouts.planning.StarterPlanUiState
import com.keepfit.feature.workouts.planning.StarterPlanViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle

const val starterPlanRoute = "starter-plan"

@Composable
fun StarterPlanRoute(
    onBack: () -> Unit,
    onPlanManually: () -> Unit,
    onAskCoach: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarterPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StarterPlanScreen(
        state = state,
        onGoalSelected = viewModel::selectGoal,
        onExperienceSelected = viewModel::selectExperience,
        onDayToggled = viewModel::toggleDay,
        onDurationSelected = viewModel::selectDuration,
        onEquipmentToggled = viewModel::toggleEquipment,
        onAvoidToggled = viewModel::toggleAvoidedExercise,
        onNutritionDepthSelected = viewModel::selectNutritionDepth,
        onChoosePlanningRoute = viewModel::showRouteChoice,
        onCreateDraft = viewModel::createDraft,
        onPlanManually = onPlanManually,
        onAskCoach = onAskCoach,
        onEditSetup = viewModel::editSetup,
        onTemplateNameChanged = viewModel::renameDay,
        onRemoveExercise = viewModel::removeExercise,
        onAddExercise = viewModel::addExercise,
        onApply = viewModel::applyWeek,
        onDismissMessage = viewModel::dismissMessage,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StarterPlanScreen(
    state: StarterPlanUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onGoalSelected: (JourneyGoal) -> Unit = {},
    onExperienceSelected: (ExperienceLevel) -> Unit = {},
    onDayToggled: (DayOfWeek) -> Unit = {},
    onDurationSelected: (Int) -> Unit = {},
    onEquipmentToggled: (EquipmentOption) -> Unit = {},
    onAvoidToggled: (String) -> Unit = {},
    onNutritionDepthSelected: (NutritionTrackingDepth) -> Unit = {},
    onChoosePlanningRoute: () -> Unit = {},
    onCreateDraft: () -> Unit = {},
    onPlanManually: () -> Unit = {},
    onAskCoach: () -> Unit = {},
    onEditSetup: () -> Unit = {},
    onTemplateNameChanged: (Int, String) -> Unit = { _, _ -> },
    onRemoveExercise: (Int, String) -> Unit = { _, _ -> },
    onAddExercise: (Int, StarterExerciseDefinition) -> Unit = { _, _ -> },
    onApply: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.stage) {
                            StarterPlanStage.PREVIEW -> "Review your week"
                            StarterPlanStage.ROUTE_CHOICE -> "Choose your path"
                            else -> "Plan your start"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = if (
                            state.stage == StarterPlanStage.PREVIEW || state.stage == StarterPlanStage.ROUTE_CHOICE
                        ) onEditSetup else onBack,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state.stage) {
            StarterPlanStage.SETUP -> SetupContent(
                state = state,
                onGoalSelected = onGoalSelected,
                onExperienceSelected = onExperienceSelected,
                onDayToggled = onDayToggled,
                onDurationSelected = onDurationSelected,
                onEquipmentToggled = onEquipmentToggled,
                onAvoidToggled = onAvoidToggled,
                onNutritionDepthSelected = onNutritionDepthSelected,
                onChoosePlanningRoute = onChoosePlanningRoute,
                modifier = Modifier.padding(padding),
            )
            StarterPlanStage.ROUTE_CHOICE -> PlanningRouteContent(
                onCreateDraft = onCreateDraft,
                onPlanManually = onPlanManually,
                onAskCoach = onAskCoach,
                modifier = Modifier.padding(padding),
            )
            StarterPlanStage.PREVIEW -> PreviewContent(
                state = state,
                onTemplateNameChanged = onTemplateNameChanged,
                onRemoveExercise = onRemoveExercise,
                onAddExercise = onAddExercise,
                onApply = onApply,
                modifier = Modifier.padding(padding),
            )
            StarterPlanStage.APPLIED -> AppliedContent(onBack, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupContent(
    state: StarterPlanUiState,
    onGoalSelected: (JourneyGoal) -> Unit,
    onExperienceSelected: (ExperienceLevel) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onEquipmentToggled: (EquipmentOption) -> Unit,
    onAvoidToggled: (String) -> Unit,
    onNutritionDepthSelected: (NutritionTrackingDepth) -> Unit,
    onChoosePlanningRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var step by rememberSaveable { mutableIntStateOf(0) }
    val stepCount = 7
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Training setup · ${step + 1} of $stepCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(progress = { (step + 1f) / stepCount }, modifier = Modifier.fillMaxWidth())
        when (step) {
            0 -> {
                SectionLabel("What do you want to work toward?", "This changes the balance of your starter week.")
                ChoiceSection("Choose one goal", JourneyGoal.entries, state.input.goal, { it.label }, onGoalSelected)
            }
            1 -> {
                SectionLabel("How familiar is training?", "We use this to keep the starting volume realistic.")
                ChoiceSection("Experience", ExperienceLevel.entries, state.input.experienceLevel, { it.label }, onExperienceSelected)
            }
            2 -> {
                SectionLabel("Which days can you usually protect?", "Choose 1–4 realistic days. You can move a session later.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.input.preferredDays,
                            onClick = { onDayToggled(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
            }
            3 -> {
                SectionLabel("How much time fits on a normal day?", "A repeatable 30 minutes is better than an unrealistic hour.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        FilterChip(
                            selected = state.input.sessionMinutes == minutes,
                            onClick = { onDurationSelected(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
            }
            4 -> {
                SectionLabel("What can you train with?", "Bodyweight is always included. Select everything reliably available.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentOption.entries.forEach { option ->
                        FilterChip(
                            selected = option in state.input.equipment,
                            onClick = { onEquipmentToggled(option) },
                            enabled = option != EquipmentOption.BODYWEIGHT,
                            label = { Text(option.label) },
                        )
                    }
                }
            }
            5 -> {
                SectionLabel("Anything you prefer not to do?", "Optional. This is a planning preference, not injury treatment.")
                MovementConstraintPicker(
                    selectedKeys = state.input.avoidedExerciseKeys,
                    onToggle = onAvoidToggled,
                )
            }
            else -> {
                SectionLabel(
                    "How much nutrition detail helps you?",
                    "Choose the lightest approach you can maintain. You can change this later in Settings.",
                )
                ChoiceSection(
                    title = "Nutrition tracking",
                    choices = NutritionTrackingDepth.entries,
                    selected = state.nutritionTrackingDepth,
                    label = {
                        when (it) {
                            NutritionTrackingDepth.DETAILED_MACROS -> "Detailed macros"
                            NutritionTrackingDepth.CALORIES_PROTEIN -> "Calories + protein"
                            NutritionTrackingDepth.MEAL_QUALITY -> "Simple meal check-ins"
                            NutritionTrackingDepth.DISABLED -> "Not right now"
                        }
                    },
                    onSelect = onNutritionDepthSelected,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { if (step == stepCount - 1) onChoosePlanningRoute() else step++ },
                enabled = step != 2 || state.input.preferredDays.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text(if (step == stepCount - 1) "Choose how to plan" else "Continue") }
        }
        Text(
            "Your answers stay on this device. The starter week is created offline and remains editable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovementConstraintPicker(
    selectedKeys: Set<String>,
    onToggle: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = StarterExerciseCatalog.exercises.filter { it.key in selectedKeys }

    if (selected.isNotEmpty()) {
        Text("Avoiding ${selected.size}", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.forEach { exercise ->
                FilterChip(
                    selected = true,
                    onClick = { onToggle(exercise.key) },
                    label = { Text(exercise.name) },
                )
            }
        }
    }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Text(if (expanded) "Hide exercise list" else "Choose exercises to avoid")
    }
    if (expanded) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search exercises") },
            singleLine = true,
        )
        val matches = StarterExerciseCatalog.exercises.filter { exercise ->
            query.isBlank() || exercise.name.contains(query, ignoreCase = true) ||
                exercise.muscleGroup.contains(query, ignoreCase = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            matches.forEach { exercise ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = exercise.key in selectedKeys,
                        onCheckedChange = { onToggle(exercise.key) },
                    )
                    Column {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (matches.isEmpty()) {
                Text("No matching starter exercises.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanningRouteContent(
    onCreateDraft: () -> Unit,
    onPlanManually: () -> Unit,
    onAskCoach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Choose your next step", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your answers are saved. Pick the amount of help you want right now.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PlanningRouteButton(
            title = "Build an offline starter week",
            supporting = "Preview a simple, editable week created entirely on this device.",
            icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null) },
            onClick = onCreateDraft,
        )
        PlanningRouteButton(
            title = "Plan it myself",
            supporting = "Go straight to Plan and build your own exercises, templates, and week.",
            icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
            onClick = onPlanManually,
        )
        PlanningRouteButton(
            title = "Let AI personalize a draft",
            supporting = "Open Coach to create a catalogue-backed plan from your saved answers. OpenRouter connection is required.",
            icon = { Icon(Icons.Outlined.QuestionAnswer, contentDescription = null) },
            onClick = onAskCoach,
        )
        Text(
            "The AI draft is only a preview. Your current plan stays unchanged until you review and apply it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlanningRouteButton(
    title: String,
    supporting: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            icon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceSection(
    title: String,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SectionLabel(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(selected = choice == selected, onClick = { onSelect(choice) }, label = { Text(label(choice)) })
        }
    }
}

@Composable
private fun SectionLabel(title: String, supporting: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (supporting != null) Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewContent(
    state: StarterPlanUiState,
    onTemplateNameChanged: (Int, String) -> Unit,
    onRemoveExercise: (Int, String) -> Unit,
    onAddExercise: (Int, StarterExerciseDefinition) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft ?: return
    val locale = LocalConfiguration.current.locales[0]
    var addingToDay by remember { mutableStateOf<Int?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(draft.rationale, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Nothing changes until you tap Use this week.", fontWeight = FontWeight.SemiBold)
        }
        itemsIndexed(draft.days) { dayIndex, day ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.FULL, locale).uppercase(locale),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                        value = day.templateName,
                        onValueChange = { onTemplateNameChanged(dayIndex, it) },
                        label = { Text("Workout name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    day.exercises.forEach { exercise ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                Text("${exercise.targetSets} sets x ${exercise.targetReps} reps · ${exercise.muscleGroup}")
                            }
                            IconButton(
                                onClick = { onRemoveExercise(dayIndex, exercise.key) },
                                enabled = day.exercises.size > 1,
                            ) { Icon(Icons.Outlined.Close, contentDescription = "Remove ${exercise.name}") }
                        }
                        HorizontalDivider()
                    }
                    OutlinedButton(onClick = { addingToDay = dayIndex }) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Add exercise")
                    }
                }
            }
        }
        item {
            Button(
                onClick = onApply,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isSaving) "Saving…" else "Use this week") }
        }
    }
    addingToDay?.let { dayIndex ->
        val existingKeys = draft.days[dayIndex].exercises.map { it.key }.toSet()
        AlertDialog(
            onDismissRequest = { addingToDay = null },
            title = { Text("Add an exercise") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val availableEquipment = state.input.equipment + EquipmentOption.BODYWEIGHT
                    StarterExerciseCatalog.exercises.filter { exercise ->
                        exercise.key !in existingKeys &&
                            exercise.key !in state.input.avoidedExerciseKeys &&
                            exercise.equipment.any { it in availableEquipment }
                    }.forEach { exercise ->
                        OutlinedButton(
                            onClick = {
                                onAddExercise(dayIndex, exercise)
                                addingToDay = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(exercise.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { addingToDay = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AppliedContent(onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Your starter week is ready", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("You can start from Today or adjust individual days in Workouts.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onDone) { Text("Back to workouts") }
    }
}
