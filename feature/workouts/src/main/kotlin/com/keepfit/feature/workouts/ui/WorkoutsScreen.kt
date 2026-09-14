package com.keepfit.feature.workouts.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.workouts.WorkoutViewModel
import com.keepfit.feature.workouts.ExerciseCatalogViewModel
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.PlannedWorkout
import com.keepfit.feature.workouts.data.WorkoutTemplate
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayChangeType
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun WorkoutsScreen(
    modifier: Modifier = Modifier,
    onOpenStarterPlan: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel(),
    catalogViewModel: ExerciseCatalogViewModel = hiltViewModel(),
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
    val liveCatalogState by catalogViewModel.liveState.collectAsStateWithLifecycle()
    val catalogMessage by catalogViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }
    LaunchedEffect(catalogMessage) {
        catalogMessage?.let {
            snackbarHostState.showSnackbar(it)
            catalogViewModel.dismissMessage()
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

    var selectedTab by rememberSaveable { mutableStateOf(WorkoutTab.EXERCISES) }
    var selectedExerciseSource by rememberSaveable { mutableStateOf(ExerciseSource.PERSONAL) }
    var showExerciseEditor by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == WorkoutTab.EXERCISES && selectedExerciseSource == ExerciseSource.PERSONAL) {
                FloatingActionButton(onClick = { showExerciseEditor = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add exercise")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                SectionHero(
                    label = "TRAINING",
                    title = "Workouts",
                    description = "Build your exercise library, reusable templates, and a weekly rhythm that is easy to maintain.",
                )
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = onOpenStarterPlan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build my starter week")
                }
            }
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                WorkoutTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label) },
                    )
                }
            }
            when (selectedTab) {
                WorkoutTab.EXERCISES -> Column(modifier = Modifier.fillMaxSize()) {
                    ExerciseSourceRail(
                        selected = selectedExerciseSource,
                        personalCount = exercises.size,
                        onSelect = { source ->
                            viewModel.search("")
                            selectedExerciseSource = source
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedExerciseSource) {
                            ExerciseSource.PERSONAL -> ExerciseLibrary(
                                exercises = exercises,
                                onSearch = viewModel::search,
                                onSave = viewModel::saveExercise,
                                onArchive = viewModel::archiveExercise,
                                onDelete = viewModel::deleteExercise,
                            )
                            ExerciseSource.OFFLINE -> OfflineExerciseGuide(
                                personalExerciseNames = exercises.map { it.name }.toSet(),
                                onAdd = catalogViewModel::addOfflineGuide,
                            )
                            ExerciseSource.LIVE -> LiveExerciseCatalogue(
                                state = liveCatalogState,
                                onSearch = catalogViewModel::searchLive,
                            )
                        }
                    }
                }
                WorkoutTab.TEMPLATES -> TemplateLibrary(
                    exercises = exercises,
                    templates = templates,
                    onCreate = viewModel::createTemplate,
                    onDelete = viewModel::deleteTemplate,
                )
                WorkoutTab.PLAN -> WeeklyPlan(
                    templates = templates,
                    schedule = schedule,
                    onAssign = viewModel::assignTemplate,
                    onClear = viewModel::clearPlannedWorkout,
                )
                WorkoutTab.HISTORY -> WorkoutHistory(
                    history = history,
                    records = records,
                )
            }
        }
    }

    if (showExerciseEditor) {
        ExerciseEditor(
            exercise = null,
            onDismiss = { showExerciseEditor = false },
            onSave = { id, name, muscleGroup, instructions, notes, bodyweight, media ->
                viewModel.saveExercise(id, name, muscleGroup, instructions, notes, bodyweight, media)
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
private fun ExerciseLibrary(
    exercises: List<Exercise>,
    onSearch: (String) -> Unit,
    onSave: (String?, String, String, String, String, Boolean, Uri?) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var deletingExercise by remember { mutableStateOf<Exercise?>(null) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search exercises") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (exercises.isEmpty()) {
            EmptyMessage("No exercises yet", "Add your first exercise with the + button.")
        }
        exercises.forEach { exercise ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
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
            onSave = { id, name, group, instructions, notes, bodyweight, media ->
                onSave(id, name, group, instructions, notes, bodyweight, media)
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
private fun ExerciseEditor(
    exercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, Boolean, Uri?) -> Unit,
) {
    var name by remember { mutableStateOf(exercise?.name.orEmpty()) }
    var group by remember { mutableStateOf(exercise?.muscleGroup.orEmpty()) }
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
                OutlinedTextField(name, { name = it }, label = { Text("Exercise name") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(group, { group = it }, label = { Text("Muscle group") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(instructions, { instructions = it }, label = { Text("Instructions") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bodyweight, onCheckedChange = { bodyweight = it })
                    Text("Bodyweight exercise")
                }
                OutlinedButton(onClick = { launcher.launch(arrayOf("video/mp4", "video/webm", "image/gif")) }) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null)
                    Text(if (mediaUri == null) "Attach demo" else "Demo selected")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(exercise?.id, name, group, instructions, notes, bodyweight, mediaUri) }) {
                Text("Save")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TemplateLibrary(
    exercises: List<Exercise>,
    templates: List<WorkoutTemplate>,
    onCreate: (String, List<String>) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var deletingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Button(onClick = { showEditor = true }, enabled = exercises.isNotEmpty()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("New template")
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (templates.isEmpty()) EmptyMessage("No templates yet", "Create a reusable workout from your exercise library.")
        templates.forEach { template ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).animateContentSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            template.exercises.joinToString { "${it.exerciseName} ${it.targetSets}x${it.targetReps}" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { deletingTemplate = template }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${template.name}")
                    }
                }
            }
        }
    }
    if (showEditor) {
        TemplateEditor(exercises, { showEditor = false }) { name, ids ->
            onCreate(name, ids)
            showEditor = false
        }
    }
    deletingTemplate?.let { template ->
        ConfirmDeleteDialog(
            title = "Delete ${template.name}?",
            description = "This removes it from future weekly plans. Templates that already exist in workout history stay protected.",
            onDismiss = { deletingTemplate = null },
            onConfirm = {
                onDelete(template.id)
                deletingTemplate = null
            },
        )
    }
}

@Composable
private fun TemplateEditor(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New workout template") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Template name") }, singleLine = true)
                Spacer(modifier = Modifier.height(10.dp))
                exercises.forEach { exercise ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exercise.id in selectedIds,
                            onCheckedChange = { checked ->
                                if (checked) selectedIds += exercise.id else selectedIds -= exercise.id
                            },
                        )
                        Text(exercise.name)
                    }
                }
                Text("Selected exercises start at 3 sets of 8-10 reps.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, selectedIds.toList()) }) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun WeeklyPlan(
    templates: List<WorkoutTemplate>,
    schedule: List<PlannedWorkout>,
    onAssign: (DayOfWeek, String) -> Unit,
    onClear: (DayOfWeek) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var clearingDay by remember { mutableStateOf<DayOfWeek?>(null) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (templates.isEmpty()) EmptyMessage("Create a template first", "Weekly planning uses your saved workout templates.")
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
                    IconButton(onClick = { clearingDay = day }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear ${day.name}")
                    }
                }
                OutlinedButton(onClick = { selectedDay = day }, enabled = templates.isNotEmpty()) {
                    Text(if (planned == null) "Assign" else "Change")
                }
            }
            HorizontalDivider()
        }
    }
    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("Assign ${day.getDisplayName(TextStyle.FULL, locale)}") },
            text = {
                Column {
                    templates.forEach { template ->
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

@Composable
private fun SectionHero(
    label: String,
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                AssistChip(onClick = {}, label = { Text(label) })
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class WorkoutTab(val label: String) {
    EXERCISES("Exercises"),
    TEMPLATES("Templates"),
    PLAN("Plan"),
    HISTORY("History"),
}
