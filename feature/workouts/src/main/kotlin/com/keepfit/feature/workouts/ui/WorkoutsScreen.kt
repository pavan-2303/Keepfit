package com.keepfit.feature.workouts.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.workouts.WorkoutViewModel
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.PlannedWorkout
import com.keepfit.feature.workouts.data.TemplateExercise
import com.keepfit.feature.workouts.data.WorkoutTemplate
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayChangeType
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun WorkoutsScreen(
    modifier: Modifier = Modifier,
    onOpenStarterPlan: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val workoutWriteInProgress by viewModel.workoutWriteInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }
    if (activeWorkout != null) {
        ActiveWorkoutScreen(
            workout = requireNotNull(activeWorkout),
            exercises = exercises,
            timerSeconds = timerSeconds,
            snackbarHostState = snackbarHostState,
            isWriting = workoutWriteInProgress,
            onAddSet = viewModel::addSet,
            onRepeatPrevious = viewModel::repeatPreviousSet,
            onSaveNotes = viewModel::updateExerciseNotes,
            onStartTimer = viewModel::startRestTimer,
            onSubstitute = viewModel::substituteActiveExercise,
            onMinimum = viewModel::convertActiveWorkoutToMinimum,
            onComplete = { feedback -> viewModel.completeWorkout(feedback) },
            modifier = modifier,
        )
        return
    }

    var destination by rememberSaveable { mutableStateOf(WorkoutDestination.PLAN) }
    var showExerciseEditor by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (destination == WorkoutDestination.EXERCISES) {
                FloatingActionButton(onClick = { showExerciseEditor = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add exercise")
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (destination) {
                WorkoutDestination.PLAN -> PlanOverview(
                    templates = templates,
                    exercises = exercises,
                    schedule = schedule,
                    onAssign = viewModel::assignTemplate,
                    onClear = viewModel::clearPlannedWorkout,
                    onOpenStarterPlan = onOpenStarterPlan,
                    onOpenTemplates = { destination = WorkoutDestination.TEMPLATES },
                    onOpenExercises = { destination = WorkoutDestination.EXERCISES },
                    onOpenHistory = { destination = WorkoutDestination.HISTORY },
                )
                WorkoutDestination.TEMPLATES -> TemplateLibrary(
                    exercises = exercises,
                    templates = templates,
                    onCreate = viewModel::createTemplate,
                    onRename = viewModel::renameTemplate,
                    onAddExercises = viewModel::addTemplateExercises,
                    onUpdateExercise = viewModel::updateTemplateExercise,
                    onRemoveExercise = viewModel::removeTemplateExercise,
                    onDeleteTemplates = viewModel::deleteTemplates,
                    onBack = { destination = WorkoutDestination.PLAN },
                )
                WorkoutDestination.EXERCISES -> Column(Modifier.fillMaxSize()) {
                    SubpageHeader(
                        title = "Exercise catalogue",
                        supportingText = "Build and maintain your own exercise library.",
                        onBack = { destination = WorkoutDestination.PLAN },
                    )
                    ExerciseLibrary(
                        exercises = exercises,
                        onSearch = viewModel::search,
                        onSave = viewModel::saveExercise,
                        onArchive = viewModel::archiveExercise,
                        onDelete = viewModel::deleteExercise,
                    )
                }
                WorkoutDestination.HISTORY -> Column(Modifier.fillMaxSize()) {
                    SubpageHeader(
                        title = "Workout history",
                        supportingText = "Completed sessions and personal records.",
                        onBack = { destination = WorkoutDestination.PLAN },
                    )
                    WorkoutHistory(history = history, records = records)
                }
            }
        }
    }

    if (showExerciseEditor) {
        ExerciseEditor(
            exercise = null,
            onDismiss = { showExerciseEditor = false },
            onSave = { id, name, muscleGroup, equipment, target, secondary, instructions, notes, bodyweight, media ->
                viewModel.saveExercise(
                    id,
                    name,
                    muscleGroup,
                    equipment,
                    target,
                    secondary,
                    instructions,
                    notes,
                    bodyweight,
                    media,
                )
                showExerciseEditor = false
            },
        )
    }
}

@Composable
fun TodayWorkoutSection(
    onOpenWorkout: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val todayWorkout by viewModel.todayWorkout.collectAsStateWithLifecycle()
    val preview by viewModel.todayPreview.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val isWorking by viewModel.todayChangeInProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    DecisiveTodayCard(
        state = todayWorkout,
        preview = preview,
        exercises = exercises,
        isWorking = isWorking,
        message = message,
        onStart = { action -> viewModel.startTodayWorkout(action, onOpenWorkout) },
        onResume = onOpenWorkout,
        onPreviewChange = viewModel::previewTodayChange,
        onConfirmPreview = viewModel::confirmTodayChange,
        onDismissPreview = viewModel::dismissTodayPreview,
        onDismissMessage = viewModel::dismissMessage,
        onRestore = { action ->
            viewModel.previewTodayChange(
                TodayChangeRequest(
                    plannedWorkoutId = action.plannedWorkoutId,
                    occurrenceId = action.occurrenceId,
                    originalDate = action.originalDate,
                    type = TodayChangeType.RESTORE,
                ),
            )
        },
        onOpenWorkouts = onOpenWorkout,
    )
}

@Composable
internal fun ExerciseLibrary(
    exercises: List<Exercise>,
    onSearch: (String) -> Unit,
    onSave: (String?, String, String, String, String, String, String, String, Boolean, Uri?) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var deletingExercise by remember { mutableStateOf<Exercise?>(null) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "exercise-search") {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search your exercises") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = NumberFormat.getIntegerInstance().format(exercises.size) +
                        if (exercises.size == 1) " exercise" else " exercises",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (exercises.isEmpty()) {
            item(key = "exercise-empty") {
                EmptyMessage(
                    if (query.isBlank()) "No exercises yet" else "No exercise matches",
                    if (query.isBlank()) "Add your first exercise with the + button." else "Try a broader name, muscle, or equipment search.",
                )
            }
        }
        items(exercises, key = Exercise::id) { exercise ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clickable { viewingExercise = exercise },
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                        Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val metadata = listOfNotNull(exercise.equipment, exercise.targetMuscle)
                        if (metadata.isNotEmpty()) {
                            Text(
                                metadata.joinToString(" • ") { it.catalogueLabel() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (exercise.demo != null) {
                            Text(
                                "Private demo attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { editingExercise = exercise }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${exercise.name}")
                    }
                    IconButton(onClick = { deletingExercise = exercise }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${exercise.name}")
                    }
                    IconButton(onClick = { onArchive(exercise.id) }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Archive ${exercise.name}")
                    }
                }
            }
        }
    }
    editingExercise?.let { exercise ->
        ExerciseEditor(
            exercise = exercise,
            onDismiss = { editingExercise = null },
            onSave = { id, name, group, equipment, target, secondary, instructions, notes, bodyweight, media ->
                onSave(id, name, group, equipment, target, secondary, instructions, notes, bodyweight, media)
                editingExercise = null
            },
        )
    }
    viewingExercise?.let { exercise ->
        PersonalExerciseDetailDialog(
            exercise = exercise,
            onEdit = {
                viewingExercise = null
                editingExercise = exercise
            },
            onDismiss = { viewingExercise = null },
        )
    }
    deletingExercise?.let { exercise ->
        ConfirmDeleteDialog(
            title = "Delete ${exercise.name}?",
            description = "This only works when the exercise is not already used in templates or workout history.",
            onDismiss = { deletingExercise = null },
            onConfirm = {
                onDelete(exercise.id)
                deletingExercise = null
            },
        )
    }
}

@Composable
internal fun ExerciseEditor(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, String, String, String, Boolean, Uri?) -> Unit,
) {
    var name by remember { mutableStateOf(exercise?.name.orEmpty()) }
    var group by remember { mutableStateOf(exercise?.muscleGroup.orEmpty()) }
    var equipment by remember { mutableStateOf(exercise?.equipment.orEmpty()) }
    var targetMuscle by remember { mutableStateOf(exercise?.targetMuscle.orEmpty()) }
    var secondaryMuscles by remember { mutableStateOf(exercise?.secondaryMuscles.orEmpty()) }
    var instructions by remember { mutableStateOf(exercise?.instructions.orEmpty()) }
    var notes by remember { mutableStateOf(exercise?.notes.orEmpty()) }
    var bodyweight by remember { mutableStateOf(exercise?.isBodyweight == true) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        mediaUri = it
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) "Add exercise" else "Edit exercise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exercise name") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Body area") },
                    supportingText = { Text("For example: chest, back, legs, core, or mobility") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Equipment (optional)") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetMuscle,
                    onValueChange = { targetMuscle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Primary target (optional)") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = secondaryMuscles,
                    onValueChange = { secondaryMuscles = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Secondary targets (optional)") },
                    supportingText = { Text("Separate multiple targets with commas") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Instructions or description") },
                    supportingText = { Text("Use one step per line for clearer exercise details") },
                    minLines = 4,
                    maxLines = 8,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Personal notes (optional)") },
                    minLines = 3,
                    maxLines = 6,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Bodyweight exercise", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Allows sets with no added weight.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = bodyweight, onCheckedChange = { bodyweight = it })
                }
                OutlinedButton(onClick = { launcher.launch(arrayOf("video/mp4", "video/webm", "image/gif")) }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text(if (mediaUri == null) "Attach demo" else "Demo selected")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && group.isNotBlank(),
                onClick = {
                    onSave(
                        exercise?.id,
                        name,
                        group,
                        equipment,
                        targetMuscle,
                        secondaryMuscles,
                        instructions,
                        notes,
                        bodyweight,
                        mediaUri,
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun TemplateLibrary(
    exercises: List<Exercise>,
    templates: List<WorkoutTemplate>,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onAddExercises: (String, List<String>) -> Unit,
    onUpdateExercise: (String, String, Int, String?) -> Unit,
    onRemoveExercise: (String, String) -> Unit,
    onDeleteTemplates: (Set<String>) -> Unit,
    onBack: () -> Unit,
) {
    var showCreateEditor by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var renamingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var addingToTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var editingExercise by remember { mutableStateOf<TemplateExercise?>(null) }
    var removingExercise by remember { mutableStateOf<TemplateExercise?>(null) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }
    var deletingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedTemplateIds = remember { mutableStateListOf<String>() }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    val currentTemplate = selectedTemplate?.id?.let { id -> templates.firstOrNull { it.id == id } }

    LaunchedEffect(templates.map(WorkoutTemplate::id)) {
        selectedTemplateIds.retainAll(templates.map(WorkoutTemplate::id).toSet())
        if (selectionMode && selectedTemplateIds.isEmpty()) selectionMode = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubpageHeader(
            title = when {
                currentTemplate != null -> currentTemplate.name
                selectionMode -> "${selectedTemplateIds.size} selected"
                else -> "Templates"
            },
            supportingText = when {
                currentTemplate != null -> "${currentTemplate.exercises.size} exercises"
                selectionMode -> "Choose templates to delete"
                else -> "Reusable workouts for your weekly plan."
            },
            onBack = when {
                currentTemplate != null -> ({ selectedTemplate = null })
                selectionMode -> ({
                    selectionMode = false
                    selectedTemplateIds.clear()
                })
                else -> onBack
            },
            actions = {
                if (currentTemplate != null) {
                    IconButton(onClick = { renamingTemplate = currentTemplate }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Rename ${currentTemplate.name}")
                    }
                    IconButton(onClick = { addingToTemplate = currentTemplate }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add exercises to ${currentTemplate.name}")
                    }
                    IconButton(onClick = { deletingTemplate = currentTemplate }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${currentTemplate.name}")
                    }
                } else if (selectionMode) {
                    val allSelected = selectedTemplateIds.size == templates.size
                    IconButton(
                        onClick = {
                            if (allSelected) selectedTemplateIds.clear()
                            else {
                                selectedTemplateIds.clear()
                                selectedTemplateIds.addAll(templates.map(WorkoutTemplate::id))
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.SelectAll,
                            contentDescription = if (allSelected) "Clear template selection" else "Select all templates",
                        )
                    }
                    IconButton(
                        enabled = selectedTemplateIds.isNotEmpty(),
                        onClick = { showBulkDeleteConfirmation = true },
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete ${selectedTemplateIds.size} selected templates",
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showCreateEditor = true },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Create template")
                    }
                    IconButton(
                        enabled = templates.isNotEmpty(),
                        onClick = { selectionMode = true },
                    ) {
                        Icon(Icons.Outlined.Checklist, contentDescription = "Select templates")
                    }
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (currentTemplate != null) {
                if (currentTemplate.exercises.isEmpty()) {
                    EmptyMessage(
                        "No exercises yet",
                        "Use the add action above to build this workout from your exercise catalogue.",
                    )
                    FilledTonalButton(
                        onClick = { addingToTemplate = currentTemplate },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add exercises")
                    }
                }
                currentTemplate.exercises.forEachIndexed { index, exercise ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable {
                                    viewingExercise = exercises.firstOrNull { it.id == exercise.exerciseId }
                                }
                                .semantics { contentDescription = "View ${exercise.exerciseName} details" }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${exercise.targetSets} sets · ${exercise.targetReps ?: "Open reps"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editingExercise = exercise }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit targets for ${exercise.exerciseName}",
                            )
                        }
                        IconButton(onClick = { removingExercise = exercise }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove ${exercise.exerciseName} from ${currentTemplate.name}",
                            )
                        }
                    }
                    HorizontalDivider()
                }
            } else {
                if (templates.isEmpty()) {
                    EmptyMessage(
                        "No templates yet",
                        "Use the add action above to create a reusable workout.",
                    )
                }
                templates.forEach { template ->
                    val selected = template.id in selectedTemplateIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .animateContentSize()
                            .clickable {
                                if (selectionMode) {
                                    if (selected) selectedTemplateIds.remove(template.id)
                                    else selectedTemplateIds.add(template.id)
                                } else {
                                    selectedTemplate = template
                                }
                            },
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selectionMode) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedTemplateIds.add(template.id)
                                        else selectedTemplateIds.remove(template.id)
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = if (selected) {
                                            "Deselect ${template.name}"
                                        } else {
                                            "Select ${template.name}"
                                        }
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${template.exercises.size} exercises",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!selectionMode) {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = "Open ${template.name}")
                            }
                        }
                    }
                }
            }
        }
    }
    if (showCreateEditor) {
        TemplateEditor({ showCreateEditor = false }) { name ->
            onCreate(name)
            showCreateEditor = false
        }
    }
    renamingTemplate?.let { template ->
        RenameTemplateDialog(
            template = template,
            onDismiss = { renamingTemplate = null },
            onSave = { name ->
                onRename(template.id, name)
                renamingTemplate = null
            },
        )
    }
    addingToTemplate?.let { template ->
        AddTemplateExercisesDialog(
            template = template,
            exercises = exercises,
            onDismiss = { addingToTemplate = null },
            onAdd = { exerciseIds ->
                onAddExercises(template.id, exerciseIds)
                addingToTemplate = null
            },
        )
    }
    editingExercise?.let { exercise ->
        EditTemplateExerciseDialog(
            exercise = exercise,
            onDismiss = { editingExercise = null },
            onSave = { targetSets, targetReps ->
                currentTemplate?.let { template ->
                    onUpdateExercise(template.id, exercise.id, targetSets, targetReps)
                }
                editingExercise = null
            },
        )
    }
    viewingExercise?.let { exercise ->
        PersonalExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { viewingExercise = null },
        )
    }
    removingExercise?.let { exercise ->
        ConfirmDeleteDialog(
            title = "Remove ${exercise.exerciseName}?",
            description = "This removes the exercise from ${currentTemplate?.name ?: "this template"}. Workout history is unchanged.",
            onDismiss = { removingExercise = null },
            onConfirm = {
                currentTemplate?.let { template -> onRemoveExercise(template.id, exercise.id) }
                removingExercise = null
            },
        )
    }
    deletingTemplate?.let { template ->
        ConfirmDeleteDialog(
            title = "Delete ${template.name}?",
            description = "This removes it from future weekly plans. Templates already used in workout history stay protected.",
            onDismiss = { deletingTemplate = null },
            onConfirm = {
                onDeleteTemplates(setOf(template.id))
                deletingTemplate = null
            },
        )
    }
    if (showBulkDeleteConfirmation) {
        val count = selectedTemplateIds.size
        ConfirmDeleteDialog(
            title = "Delete $count templates?",
            description = "This removes the selected templates from future plans. If any is protected by workout history, nothing will be deleted.",
            onDismiss = { showBulkDeleteConfirmation = false },
            onConfirm = {
                onDeleteTemplates(selectedTemplateIds.toSet())
                showBulkDeleteConfirmation = false
            },
        )
    }
}

@Composable
private fun TemplateEditor(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create template") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Template name") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Create the template first, then add exercises from its detail page.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Create") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameTemplateDialog(
    template: WorkoutTemplate,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(template.id, template.name) { mutableStateOf(template.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename template") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Template name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddTemplateExercisesDialog(
    template: WorkoutTemplate,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    var query by remember(template.id) { mutableStateOf("") }
    val selectedIds = remember(template.id) { mutableStateListOf<String>() }
    val existingIds = remember(template.id, template.exercises) {
        template.exercises.map(TemplateExercise::exerciseId).toSet()
    }
    val available = remember(exercises, existingIds, query) {
        exercises.filter { exercise ->
            exercise.id !in existingIds && (
                query.isBlank() || listOfNotNull(
                    exercise.name,
                    exercise.muscleGroup,
                    exercise.equipment,
                    exercise.targetMuscle,
                ).any { it.contains(query, ignoreCase = true) }
                )
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercises") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Find exercises") },
                        singleLine = true,
                    )
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (available.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (query.isBlank()) "Every available exercise is already included."
                            else "No exercises match your search.",
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(available, key = Exercise::id) { exercise ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exercise.id in selectedIds,
                            onCheckedChange = { checked ->
                                if (checked) selectedIds.add(exercise.id)
                                else selectedIds.remove(exercise.id)
                            },
                        )
                        Text(exercise.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedIds.isNotEmpty(),
                onClick = { onAdd(selectedIds.toList()) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EditTemplateExerciseDialog(
    exercise: TemplateExercise,
    onDismiss: () -> Unit,
    onSave: (Int, String?) -> Unit,
) {
    var targetSets by remember(exercise.id, exercise.targetSets) {
        mutableStateOf(exercise.targetSets.toString())
    }
    var targetReps by remember(exercise.id, exercise.targetReps) {
        mutableStateOf(exercise.targetReps.orEmpty())
    }
    val parsedSets = targetSets.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${exercise.exerciseName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = { targetSets = it.filter(Char::isDigit) },
                    label = { Text("Target sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = targetSets.isNotEmpty() && (parsedSets == null || parsedSets < 1),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = { targetReps = it },
                    label = { Text("Target reps") },
                    supportingText = { Text("Examples: 8-10, 12, or 30 sec") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = parsedSets != null && parsedSets > 0,
                onClick = { onSave(requireNotNull(parsedSets), targetReps.ifBlank { null }) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PlanOverview(
    templates: List<WorkoutTemplate>,
    exercises: List<Exercise>,
    schedule: List<PlannedWorkout>,
    onAssign: (DayOfWeek, String) -> Unit,
    onClear: (DayOfWeek) -> Unit,
    onOpenStarterPlan: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenExercises: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var clearingDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var viewingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("This week", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenStarterPlan) {
                Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Starter setup")
            }
        }
        Spacer(Modifier.height(4.dp))
        if (templates.isEmpty()) {
            EmptyMessage("Create a template first", "Weekly planning uses your saved workout templates.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenTemplates) { Text("Create template") }
        } else {
            DayOfWeek.entries.forEach { day ->
                val planned = schedule.firstOrNull { it.dayOfWeek == day }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(day.getDisplayName(TextStyle.FULL, locale), style = MaterialTheme.typography.titleMedium)
                        Text(planned?.templateName ?: "Rest day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (planned != null) {
                        IconButton(
                            onClick = {
                                viewingTemplate = templates.firstOrNull { it.id == planned.templateId }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = "View ${day.getDisplayName(TextStyle.FULL, locale)} workout",
                            )
                        }
                        IconButton(onClick = { clearingDay = day }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear ${day.name}")
                        }
                    }
                    OutlinedButton(onClick = { selectedDay = day }) {
                        Text(if (planned == null) "Assign" else "Change")
                    }
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("Plan tools", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        PlanToolRow(
            icon = Icons.Outlined.ViewWeek,
            title = "Templates",
            detail = "Create and edit reusable workouts",
            onClick = onOpenTemplates,
        )
        PlanToolRow(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            title = "Exercise catalogue",
            detail = "Build exercises and review movement guidance",
            onClick = onOpenExercises,
        )
        PlanToolRow(
            icon = Icons.Outlined.History,
            title = "Workout history",
            detail = "Review completed sessions and records",
            onClick = onOpenHistory,
        )
    }
    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("Assign ${day.getDisplayName(TextStyle.FULL, locale)}") },
            text = {
                Column {
                    val readyTemplates = templates.filter { it.exercises.isNotEmpty() }
                    if (readyTemplates.isEmpty()) {
                        Text(
                            "Add at least one exercise to a template before assigning it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    readyTemplates.forEach { template ->
                        FilledTonalButton(
                            onClick = {
                                onAssign(day, template.id)
                                selectedDay = null
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { OutlinedButton(onClick = { selectedDay = null }) { Text("Cancel") } },
        )
    }
    clearingDay?.let { day ->
        ConfirmDeleteDialog(
            title = "Clear ${day.getDisplayName(TextStyle.FULL, locale)}?",
            description = "This removes the planned workout for that day and leaves it as a rest day.",
            onDismiss = { clearingDay = null },
            onConfirm = {
                onClear(day)
                clearingDay = null
            },
        )
    }
    viewingTemplate?.let { template ->
        PlanTemplateDialog(
            template = template,
            onExerciseClick = { templateExercise ->
                viewingTemplate = null
                viewingExercise = exercises.firstOrNull { it.id == templateExercise.exerciseId }
            },
            onDismiss = { viewingTemplate = null },
        )
    }
    viewingExercise?.let { exercise ->
        PersonalExerciseDetailDialog(
            exercise = exercise,
            onDismiss = { viewingExercise = null },
        )
    }
}

@Composable
private fun PlanTemplateDialog(
    template: WorkoutTemplate,
    onExerciseClick: (TemplateExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.name) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(template.exercises, key = TemplateExercise::id) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExerciseClick(exercise) }
                            .semantics { contentDescription = "View ${exercise.exerciseName} details" }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${exercise.targetSets} sets · ${exercise.targetReps ?: "Open reps"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun PlanToolRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = "Open $title")
    }
    HorizontalDivider()
}

@Composable
private fun SubpageHeader(
    title: String,
    supportingText: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to plan")
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
private fun WorkoutHistory(
    history: List<com.keepfit.feature.workouts.data.WorkoutHistory>,
    records: List<com.keepfit.feature.workouts.data.PersonalRecord>,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("PERSONAL RECORDS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        if (records.isEmpty()) Text("Complete a workout to establish records.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        records.forEach { record ->
            Text("${record.exerciseName}: ${record.highestWeightKg} kg / ${record.highestRepetitions} reps")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("COMPLETED SESSIONS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        if (history.isEmpty()) EmptyMessage("No completed sessions", "Finished workouts will appear as calendar markers here.")
        history.forEach { session ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(session.workoutDate.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(session.exerciseNames.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(4.dp))
    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(description) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

internal fun String.catalogueLabel(): String = replaceFirstChar { character ->
    if (character.isLowerCase()) character.titlecase() else character.toString()
}

private enum class WorkoutDestination { PLAN, TEMPLATES, EXERCISES, HISTORY }
