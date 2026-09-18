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
import androidx.compose.material3.TextButton
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
import com.keepfit.feature.workouts.planning.ActivityLevel
import com.keepfit.feature.workouts.planning.CurrentBuild
import com.keepfit.feature.workouts.planning.EquipmentOption
import com.keepfit.feature.workouts.planning.ExperienceLevel
import com.keepfit.feature.workouts.planning.JourneyGoal
import com.keepfit.feature.workouts.planning.LimitationArea
import com.keepfit.feature.workouts.planning.RoutineChallenge
import com.keepfit.feature.workouts.planning.SleepDuration
import com.keepfit.feature.workouts.planning.SleepSchedule
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
        onActivitySelected = viewModel::selectActivity,
        onSleepDurationSelected = viewModel::selectSleepDuration,
        onSleepScheduleSelected = viewModel::selectSleepSchedule,
        onCurrentBuildSelected = viewModel::selectCurrentBuild,
        onRoutineChallengeToggled = viewModel::toggleRoutineChallenge,
        onLimitationAreaToggled = viewModel::toggleLimitationArea,
        onLimitationNotesChanged = viewModel::updateLimitationNotes,
        onDayToggled = viewModel::toggleDay,
        onDurationSelected = viewModel::selectDuration,
        onEquipmentToggled = viewModel::toggleEquipment,
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
    onActivitySelected: (ActivityLevel) -> Unit = {},
    onSleepDurationSelected: (SleepDuration) -> Unit = {},
    onSleepScheduleSelected: (SleepSchedule) -> Unit = {},
    onCurrentBuildSelected: (CurrentBuild) -> Unit = {},
    onRoutineChallengeToggled: (RoutineChallenge) -> Unit = {},
    onLimitationAreaToggled: (LimitationArea) -> Unit = {},
    onLimitationNotesChanged: (String) -> Unit = {},
    onDayToggled: (DayOfWeek) -> Unit = {},
    onDurationSelected: (Int) -> Unit = {},
    onEquipmentToggled: (EquipmentOption) -> Unit = {},
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
                onActivitySelected = onActivitySelected,
                onSleepDurationSelected = onSleepDurationSelected,
                onSleepScheduleSelected = onSleepScheduleSelected,
                onCurrentBuildSelected = onCurrentBuildSelected,
                onRoutineChallengeToggled = onRoutineChallengeToggled,
                onLimitationAreaToggled = onLimitationAreaToggled,
                onLimitationNotesChanged = onLimitationNotesChanged,
                onDayToggled = onDayToggled,
                onDurationSelected = onDurationSelected,
                onEquipmentToggled = onEquipmentToggled,
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
    onActivitySelected: (ActivityLevel) -> Unit,
    onSleepDurationSelected: (SleepDuration) -> Unit,
    onSleepScheduleSelected: (SleepSchedule) -> Unit,
    onCurrentBuildSelected: (CurrentBuild) -> Unit,
    onRoutineChallengeToggled: (RoutineChallenge) -> Unit,
    onLimitationAreaToggled: (LimitationArea) -> Unit,
    onLimitationNotesChanged: (String) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onEquipmentToggled: (EquipmentOption) -> Unit,
    onChoosePlanningRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    var step by rememberSaveable { mutableIntStateOf(0) }
    val stepCount = 9
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Personal assessment · ${step + 1} of $stepCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(progress = { (step + 1f) / stepCount }, modifier = Modifier.fillMaxWidth())
        when (step) {
            0 -> {
                SectionLabel("What result matters most right now?", "Choose the outcome that should lead your first plan.")
                ChoiceSection("Primary goal", JourneyGoal.entries, state.input.goal, { it.label }, onGoalSelected)
            }
            1 -> {
                SectionLabel(
                    "Where are you starting from?",
                    "Build is optional context, not a diagnosis or a fixed body type.",
                )
                ChoiceSection("Current build", CurrentBuild.entries, state.input.currentBuild, { it.label }, onCurrentBuildSelected)
                ChoiceSection("Typical daily activity", ActivityLevel.entries, state.input.activityLevel, { it.label }, onActivitySelected)
            }
            2 -> {
                SectionLabel("How familiar is structured training?", "This keeps exercise complexity and starting volume realistic.")
                ChoiceSection("Experience", ExperienceLevel.entries, state.input.experienceLevel, { it.label }, onExperienceSelected)
            }
            3 -> {
                SectionLabel("How does recovery usually look?", "Sleep is planning context, not a score. Choose the closest normal week.")
                ChoiceSection("Sleep duration", SleepDuration.entries, state.input.sleepDuration, { it.label }, onSleepDurationSelected)
                ChoiceSection("Sleep schedule", SleepSchedule.entries, state.input.sleepSchedule, { it.label }, onSleepScheduleSelected)
            }
            4 -> {
                SectionLabel("What regularly gets in the way?", "Optional. Select anything a realistic plan should work around.")
                MultiChoiceSection(
                    choices = RoutineChallenge.entries,
                    selected = state.input.routineChallenges,
                    label = { it.label },
                    onToggle = onRoutineChallengeToggled,
                )
            }
            5 -> {
                SectionLabel(
                    "Any pain, injuries, or movement limits?",
                    "Optional planning context only. Keepfit cannot diagnose or prescribe rehabilitation.",
                )
                MultiChoiceSection(
                    choices = LimitationArea.entries,
                    selected = state.input.limitationAreas,
                    label = { it.label },
                    onToggle = onLimitationAreaToggled,
                )
                OutlinedTextField(
                    value = state.input.limitationNotes.orEmpty(),
                    onValueChange = onLimitationNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Movements or advice to avoid") },
                    placeholder = { Text("For example: avoid jumping and deep knee bends") },
                    minLines = 3,
                    maxLines = 6,
                )
            }
            6 -> {
                SectionLabel("What can your week actually support?", "Choose 1–4 realistic days and a repeatable session length.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.input.preferredDays,
                            onClick = { onDayToggled(day) },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                        )
                    }
                }
                Text("Session length", style = MaterialTheme.typography.labelLarge)
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
            7 -> {
                SectionLabel("What can you reliably train with?", "Bodyweight is always available. Select only equipment you can use most weeks.")
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
            else -> {
                SectionLabel("Review your starting point", "These answers guide planning. You can return and change them before creating anything.")
                AssessmentSummary(state)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(
                onClick = { if (step == stepCount - 1) onChoosePlanningRoute() else step++ },
                enabled = step != 6 || state.input.preferredDays.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) { Text(if (step == stepCount - 1) "Choose how to plan" else "Continue") }
        }
        if (step < stepCount - 1) {
            TextButton(onClick = onChoosePlanningRoute, modifier = Modifier.fillMaxWidth()) {
                Text("Skip assessment for now")
            }
        }
        Text(
            "Your answers stay on this device unless you explicitly ask Coach to create a plan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChoiceSection(
    choices: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice in selected,
                onClick = { onToggle(choice) },
                label = { Text(label(choice)) },
            )
        }
    }
}

@Composable
private fun AssessmentSummary(state: StarterPlanUiState) {
    val locale = LocalConfiguration.current.locales[0]
    val rows = listOf(
        "Goal" to state.input.goal.label,
        "Starting point" to "${state.input.currentBuild.label}; ${state.input.activityLevel.label}",
        "Experience" to state.input.experienceLevel.label,
        "Recovery" to "${state.input.sleepDuration.label}; ${state.input.sleepSchedule.label}",
        "Training days" to state.input.preferredDays.sorted().joinToString { it.getDisplayName(TextStyle.SHORT, locale) },
        "Session" to "${state.input.sessionMinutes} minutes",
        "Equipment" to state.input.equipment.joinToString { it.label },
        "Limitations" to when {
            state.input.limitationAreas.isEmpty() && state.input.limitationNotes.isNullOrBlank() -> "None shared"
            else -> state.input.limitationAreas.joinToString { it.label }
                .ifBlank { "Details provided" }
        },
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(value.ifBlank { "Not selected" }, style = MaterialTheme.typography.bodyLarge)
                }
                if (index != rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
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
            supporting = "Connect OpenRouter, choose a Coach, and review a plan that can also add the exercises it needs to your catalogue.",
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
