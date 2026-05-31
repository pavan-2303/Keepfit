package com.keepfit.feature.transformation.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.feature.transformation.TransformationViewModel
import com.keepfit.feature.transformation.data.BodyMeasurement
import com.keepfit.feature.transformation.data.CurrentProgressOverview
import com.keepfit.feature.transformation.data.TransformationPhoto
import com.keepfit.feature.transformation.data.TransformationWeek
import com.keepfit.feature.transformation.data.formatMetric
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: TransformationViewModel = hiltViewModel(),
) {
    val currentOverview by viewModel.currentOverview.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val weeks by viewModel.weeks.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMeasurementEditor by remember { mutableStateOf(false) }
    var selectedWeekStart by rememberSaveable { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }
    var compareAngle by rememberSaveable { mutableStateOf(TransformationPhotoAngle.FRONT) }
    var leftWeekId by rememberSaveable { mutableStateOf<String?>(null) }
    var rightWeekId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportAngle by remember { mutableStateOf<TransformationPhotoAngle?>(null) }
    val currentWeek = weeks.firstOrNull { it.weekStartDate == selectedWeekStart }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val angle = pendingImportAngle
        if (uri != null && angle != null) {
            viewModel.importPhoto(selectedWeekStart, angle, uri)
        }
        pendingImportAngle = null
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }
    LaunchedEffect(weeks) {
        if (leftWeekId == null) {
            leftWeekId = weeks.firstOrNull()?.id
        }
        if (rightWeekId == null) {
            rightWeekId = weeks.drop(1).firstOrNull()?.id ?: weeks.firstOrNull()?.id
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
                text = "PROGRESS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "Transformation", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(18.dp))
            CurrentMetricsCard(currentOverview)
            Spacer(modifier = Modifier.height(18.dp))
            SectionHeader(
                title = "Measurements",
                actionLabel = "Log",
                onAction = { showMeasurementEditor = true },
            )
            Spacer(modifier = Modifier.height(10.dp))
            MeasurementHistory(measurements)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "WEEKLY PHOTOS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            WeekEditorCard(
                selectedWeekStart = selectedWeekStart,
                currentWeek = currentWeek,
                onPreviousWeek = { selectedWeekStart = selectedWeekStart.minusWeeks(1) },
                onNextWeek = { selectedWeekStart = selectedWeekStart.plusWeeks(1) },
                onSaveNotes = viewModel::saveWeekNotes,
                onImportAngle = { angle ->
                    pendingImportAngle = angle
                    launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeekList(weeks)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "COMPARE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ComparisonCard(
                weeks = weeks,
                leftWeekId = leftWeekId,
                rightWeekId = rightWeekId,
                angle = compareAngle,
                onPreviousLeft = { leftWeekId = cycleWeekId(weeks, leftWeekId, -1) },
                onNextLeft = { leftWeekId = cycleWeekId(weeks, leftWeekId, 1) },
                onPreviousRight = { rightWeekId = cycleWeekId(weeks, rightWeekId, -1) },
                onNextRight = { rightWeekId = cycleWeekId(weeks, rightWeekId, 1) },
                onSelectAngle = { compareAngle = it },
            )
        }
    }

    if (showMeasurementEditor) {
        MeasurementEditorDialog(
            onDismiss = { showMeasurementEditor = false },
            onSave = { date, weight, waist, chest, hips, leftArm, rightArm, leftThigh, rightThigh, notes ->
                viewModel.saveMeasurement(date, weight, waist, chest, hips, leftArm, rightArm, leftThigh, rightThigh, notes)
                showMeasurementEditor = false
            },
        )
    }
}

@Composable
fun TodayProgressSection(
    modifier: Modifier = Modifier,
    viewModel: TransformationViewModel = hiltViewModel(),
) {
    val overview by viewModel.currentOverview.collectAsStateWithLifecycle()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val latest = overview.latestMeasurement
            if (latest?.weightKg == null) {
                Text("No measurements yet", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Weekly progress summaries will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("${latest.weightKg.formatMetric()} kg", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "BMI ${overview.bmi?.formatMetric() ?: "--"}  •  ${latest.measurementDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CurrentMetricsCard(overview: CurrentProgressOverview) {
    val latest = overview.latestMeasurement
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Current snapshot", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            if (latest == null) {
                Text("No body measurements logged yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricPill("Weight", latest.weightKg?.formatMetric()?.plus(" kg") ?: "--")
                MetricPill("BMI", overview.bmi?.formatMetric() ?: "--")
                latest.waistCm?.let { MetricPill("Waist", "${it.formatMetric()} cm") }
                latest.chestCm?.let { MetricPill("Chest", "${it.formatMetric()} cm") }
                latest.hipsCm?.let { MetricPill("Hips", "${it.formatMetric()} cm") }
            }
            latest.notes?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(actionLabel)
        }
    }
}

@Composable
private fun MeasurementHistory(measurements: List<BodyMeasurement>) {
    if (measurements.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                "No entries yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    measurements.forEach { measurement ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(measurement.measurementDate.toString(), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    measurement.weightKg?.let { MetricPill("Weight", "${it.formatMetric()} kg") }
                    measurement.waistCm?.let { MetricPill("Waist", "${it.formatMetric()} cm") }
                    measurement.chestCm?.let { MetricPill("Chest", "${it.formatMetric()} cm") }
                    measurement.hipsCm?.let { MetricPill("Hips", "${it.formatMetric()} cm") }
                }
                measurement.notes?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WeekEditorCard(
    selectedWeekStart: LocalDate,
    currentWeek: TransformationWeek?,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSaveNotes: (LocalDate, String) -> Unit,
    onImportAngle: (TransformationPhotoAngle) -> Unit,
) {
    var notes by remember(selectedWeekStart, currentWeek?.notes) {
        mutableStateOf(currentWeek?.notes.orEmpty())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous week")
                }
                Text(
                    selectedWeekStart.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNextWeek) {
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Next week")
                }
            }
            Text(
                "Week of ${selectedWeekStart}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Week notes") },
            )
            Spacer(modifier = Modifier.height(10.dp))
            FilledTonalButton(onClick = { onSaveNotes(selectedWeekStart, notes) }) {
                Text("Save week")
            }
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TransformationPhotoAngle.entries.forEach { angle ->
                    val photo = currentWeek?.photos?.firstOrNull { it.angle == angle }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.width(156.dp).padding(12.dp)) {
                            Text(angle.label, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (photo == null) {
                                Text("No photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Imported", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = { onImportAngle(angle) }) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (photo == null) "Import" else "Replace")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekList(weeks: List<TransformationWeek>) {
    if (weeks.isEmpty()) {
        Text("No saved weeks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    weeks.forEach { week ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Week of ${week.weekStartDate}", style = MaterialTheme.typography.titleMedium)
                week.notes?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Photos", week.photos.size.toString())
                    MetricPill("Workouts", week.summary.workoutsCompleted.toString())
                    MetricPill("Avg kcal", week.summary.averageCalories?.formatMetric() ?: "--")
                    MetricPill("Avg protein", week.summary.averageProteinGrams?.formatMetric()?.plus(" g") ?: "--")
                    MetricPill("Weight delta", week.summary.weightChangeKg?.let { "${if (it > 0) "+" else ""}${it.formatMetric()} kg" } ?: "--")
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    weeks: List<TransformationWeek>,
    leftWeekId: String?,
    rightWeekId: String?,
    angle: TransformationPhotoAngle,
    onPreviousLeft: () -> Unit,
    onNextLeft: () -> Unit,
    onPreviousRight: () -> Unit,
    onNextRight: () -> Unit,
    onSelectAngle: (TransformationPhotoAngle) -> Unit,
) {
    val leftWeek = weeks.firstOrNull { it.id == leftWeekId }
    val rightWeek = weeks.firstOrNull { it.id == rightWeekId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TransformationPhotoAngle.entries.forEach { candidate ->
                    FilledTonalButton(
                        onClick = { onSelectAngle(candidate) },
                        enabled = candidate != angle,
                    ) { Text(candidate.label) }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ComparisonPane(leftWeek, angle, onPreviousLeft, onNextLeft, Modifier.weight(1f))
                ComparisonPane(rightWeek, angle, onPreviousRight, onNextRight, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonPane(
    week: TransformationWeek?,
    angle: TransformationPhotoAngle,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = week?.photos?.firstOrNull { it.angle == angle }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous comparison week")
            }
            Text(
                week?.weekStartDate?.toString() ?: "No week",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Next comparison week")
            }
        }
        PhotoPreview(photo = photo, emptyLabel = "No ${angle.label.lowercase()} photo")
    }
}

@Composable
private fun PhotoPreview(photo: TransformationPhoto?, emptyLabel: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        val bitmap = remember(photo?.absolutePath) {
            photo?.absolutePath?.let(BitmapFactory::decodeFile)
        }
        if (bitmap == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(emptyLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MeasurementEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String, String) -> Unit,
) {
    var measurementDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var weightKg by remember { mutableStateOf("") }
    var waistCm by remember { mutableStateOf("") }
    var chestCm by remember { mutableStateOf("") }
    var hipsCm by remember { mutableStateOf("") }
    var leftArmCm by remember { mutableStateOf("") }
    var rightArmCm by remember { mutableStateOf("") }
    var leftThighCm by remember { mutableStateOf("") }
    var rightThighCm by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log measurement") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(measurementDate, { measurementDate = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(weightKg, { weightKg = it }, label = { Text("Weight (kg)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(waistCm, { waistCm = it }, label = { Text("Waist (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(chestCm, { chestCm = it }, label = { Text("Chest (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(hipsCm, { hipsCm = it }, label = { Text("Hips (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(leftArmCm, { leftArmCm = it }, label = { Text("Left arm (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(rightArmCm, { rightArmCm = it }, label = { Text("Right arm (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(leftThighCm, { leftThighCm = it }, label = { Text("Left thigh (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(rightThighCm, { rightThighCm = it }, label = { Text("Right thigh (cm)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        measurementDate,
                        weightKg,
                        waistCm,
                        chestCm,
                        hipsCm,
                        leftArmCm,
                        rightArmCm,
                        leftThighCm,
                        rightThighCm,
                        notes,
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun cycleWeekId(weeks: List<TransformationWeek>, currentWeekId: String?, delta: Int): String? {
    if (weeks.isEmpty()) {
        return null
    }
    val currentIndex = weeks.indexOfFirst { it.id == currentWeekId }.takeIf { it >= 0 } ?: 0
    val nextIndex = (currentIndex + delta).mod(weeks.size)
    return weeks[nextIndex].id
}

private val TransformationPhotoAngle.label: String
    get() = when (this) {
        TransformationPhotoAngle.FRONT -> "Front"
        TransformationPhotoAngle.LEFT -> "Left"
        TransformationPhotoAngle.RIGHT -> "Right"
        TransformationPhotoAngle.BACK -> "Back"
        TransformationPhotoAngle.LEGS -> "Legs"
    }
