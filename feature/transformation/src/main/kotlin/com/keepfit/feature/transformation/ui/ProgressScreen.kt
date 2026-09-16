package com.keepfit.feature.transformation.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.model.TransformationPose
import com.keepfit.core.model.TransformationPoseGroup
import com.keepfit.feature.transformation.TransformationViewModel
import com.keepfit.feature.transformation.data.BodyMeasurement
import com.keepfit.feature.transformation.data.CurrentProgressOverview
import com.keepfit.feature.transformation.data.TransformationCycle
import com.keepfit.feature.transformation.data.TransformationCycleDay
import com.keepfit.feature.transformation.data.TransformationPhoto
import com.keepfit.feature.transformation.data.TransformationTimeline
import com.keepfit.feature.transformation.data.calculatePoseCaptureProgress
import com.keepfit.feature.transformation.data.formatMetric
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: TransformationViewModel = hiltViewModel(),
) {
    val currentOverview by viewModel.currentOverview.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val enabledPoses by viewModel.enabledPoses.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMeasurementEditor by remember { mutableStateOf(false) }
    var showPoseSelector by remember { mutableStateOf(false) }
    var showAlignmentGuide by rememberSaveable { mutableStateOf(true) }
    var selectedCaptureDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var comparePose by rememberSaveable { mutableStateOf(TransformationPose.FRONT_RELAXED) }
    var leftCaptureDate by rememberSaveable { mutableStateOf<String?>(null) }
    var rightCaptureDate by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImportPose by remember { mutableStateOf<TransformationPose?>(null) }
    val activeCycle = timeline.activeCycle
    val comparisonCycle = activeCycle ?: timeline.history.firstOrNull()
    val currentDay = activeCycle?.days?.firstOrNull { it.captureDate == selectedCaptureDate }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val pose = pendingImportPose
        if (uri != null && pose != null) {
            viewModel.importPhoto(selectedCaptureDate, pose, uri)
        }
        pendingImportPose = null
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }
    LaunchedEffect(activeCycle?.id, activeCycle?.latestCaptureDate) {
        activeCycle?.latestCaptureDate?.let { selectedCaptureDate = it }
    }
    LaunchedEffect(comparisonCycle?.id) {
        val comparison = comparisonCycle?.defaultComparison
        leftCaptureDate = comparison?.leftDay?.captureDate?.toString()
        rightCaptureDate = comparison?.rightDay?.captureDate?.toString()
    }
    LaunchedEffect(enabledPoses, comparisonCycle?.id) {
        val comparisonPoses = comparisonCycle.availableComparisonPoses(enabledPoses)
        if (comparePose !in comparisonPoses) {
            comparePose = comparisonPoses.firstOrNull() ?: TransformationPose.FRONT_RELAXED
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
                text = "TRANSFORMATION CYCLE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            CycleEditorCard(
                selectedCaptureDate = selectedCaptureDate,
                activeCycle = activeCycle,
                currentDay = currentDay,
                enabledPoses = enabledPoses,
                showAlignmentGuide = showAlignmentGuide,
                onPreviousDay = { selectedCaptureDate = selectedCaptureDate.minusDays(1) },
                onNextDay = { selectedCaptureDate = selectedCaptureDate.plusDays(1) },
                onSaveNotes = viewModel::saveCycleNotes,
                onCloseCycle = viewModel::closeActiveCycle,
                onManagePoses = { showPoseSelector = true },
                onShowAlignmentGuide = { showAlignmentGuide = it },
                onImportPose = { pose ->
                    pendingImportPose = pose
                    launcher.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            CycleHistoryList(
                timeline = timeline,
                onReopen = viewModel::reopenCycle,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "COMPARE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            ComparisonCard(
                cycle = comparisonCycle,
                leftCaptureDate = leftCaptureDate,
                rightCaptureDate = rightCaptureDate,
                pose = comparePose,
                availablePoses = comparisonCycle.availableComparisonPoses(enabledPoses),
                onPreviousLeft = {
                    leftCaptureDate = cycleDayDate(comparisonCycle?.days.orEmpty(), leftCaptureDate, -1)?.toString()
                },
                onNextLeft = {
                    leftCaptureDate = cycleDayDate(comparisonCycle?.days.orEmpty(), leftCaptureDate, 1)?.toString()
                },
                onPreviousRight = {
                    rightCaptureDate = cycleDayDate(comparisonCycle?.days.orEmpty(), rightCaptureDate, -1)?.toString()
                },
                onNextRight = {
                    rightCaptureDate = cycleDayDate(comparisonCycle?.days.orEmpty(), rightCaptureDate, 1)?.toString()
                },
                onSelectPose = { comparePose = it },
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
    if (showPoseSelector) {
        PoseSelectorDialog(
            enabledPoses = enabledPoses,
            onToggle = viewModel::setOptionalPoseEnabled,
            onDismiss = { showPoseSelector = false },
        )
    }
}

@Composable
fun TodayProgressSection(
    modifier: Modifier = Modifier,
    onOpenProgress: () -> Unit = {},
    viewModel: TransformationViewModel = hiltViewModel(),
) {
    val overview by viewModel.currentOverview.collectAsStateWithLifecycle()
    Card(
        onClick = onOpenProgress,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            val latest = overview.latestMeasurement
            Column(Modifier.weight(1f)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (latest?.weightKg == null) "No measurements yet"
                    else "${latest.weightKg.formatMetric()} kg · BMI ${overview.bmi?.formatMetric() ?: "--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
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
private fun CycleEditorCard(
    selectedCaptureDate: LocalDate,
    activeCycle: TransformationCycle?,
    currentDay: TransformationCycleDay?,
    enabledPoses: List<TransformationPose>,
    showAlignmentGuide: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onCloseCycle: () -> Unit,
    onManagePoses: () -> Unit,
    onShowAlignmentGuide: (Boolean) -> Unit,
    onImportPose: (TransformationPose) -> Unit,
) {
    var notes by remember(activeCycle?.id, activeCycle?.notes) {
        mutableStateOf(activeCycle?.notes.orEmpty())
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousDay) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous cycle day")
                }
                Text(
                    selectedCaptureDate.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNextDay) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next cycle day")
                }
            }
            if (activeCycle == null) {
                Text(
                    "No active transformation cycle. Import a photo on the selected date to start one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val selectedDayNumber = currentDay?.dayNumber
                    ?: ChronoUnit.DAYS.between(activeCycle.startDate, selectedCaptureDate).toInt().coerceAtLeast(0)
                Text(
                    "Cycle start ${activeCycle.startDate} • Day $selectedDayNumber • Latest update ${activeCycle.latestCaptureDate}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cycle notes") },
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onSaveNotes(notes) }) {
                        Text("Save cycle")
                    }
                    OutlinedButton(onClick = onCloseCycle) {
                        Text("Close cycle")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val progress = calculatePoseCaptureProgress(currentDay, enabledPoses)
                Column(modifier = Modifier.weight(1f)) {
                    Text("${progress.captured} / ${progress.enabled} poses captured", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Incomplete check-ins are okay. Add only the views useful to you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onManagePoses) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Choose transformation poses")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Alignment guide", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = showAlignmentGuide,
                    onCheckedChange = onShowAlignmentGuide,
                    modifier = Modifier.semantics { contentDescription = "Show camera alignment guide" },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                enabledPoses.forEach { pose ->
                    val photo = currentDay?.photos?.firstOrNull { it.pose == pose }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.width(156.dp).padding(12.dp)) {
                            PoseGuideIllustration(pose = pose, showAlignment = showAlignmentGuide)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(pose.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                pose.group.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                pose.guidance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (photo == null) {
                                Text("No photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Imported", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = { onImportPose(pose) }) {
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
private fun CycleHistoryList(
    timeline: TransformationTimeline,
    onReopen: (String) -> Unit,
) {
    val history = timeline.history
    if (history.isEmpty()) {
        Text("No closed transformation cycles yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    history.forEach { cycle ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Cycle from ${cycle.startDate}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Last update ${cycle.latestCaptureDate} • ${cycle.days.size} logged day(s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                cycle.notes?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricPill("Days", cycle.days.size.toString())
                    MetricPill("Photos", cycle.days.sumOf { it.photos.size }.toString())
                    MetricPill("Workouts", cycle.summary.workoutsCompleted.toString())
                    MetricPill("Avg kcal", cycle.summary.averageCalories?.formatMetric() ?: "--")
                    MetricPill(
                        "Weight delta",
                        cycle.summary.weightChangeKg?.let { "${if (it > 0) "+" else ""}${it.formatMetric()} kg" } ?: "--",
                    )
                }
                if (cycle.canReopen) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = { onReopen(cycle.id) }) {
                        Text("Reopen cycle")
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    cycle: TransformationCycle?,
    leftCaptureDate: String?,
    rightCaptureDate: String?,
    pose: TransformationPose,
    availablePoses: List<TransformationPose>,
    onPreviousLeft: () -> Unit,
    onNextLeft: () -> Unit,
    onPreviousRight: () -> Unit,
    onNextRight: () -> Unit,
    onSelectPose: (TransformationPose) -> Unit,
) {
    if (cycle == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                "No transformation cycle to compare yet.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val leftDay = cycle.days.firstOrNull { it.captureDate.toString() == leftCaptureDate } ?: cycle.defaultComparison.leftDay
    val rightDay = cycle.days.firstOrNull { it.captureDate.toString() == rightCaptureDate } ?: cycle.defaultComparison.rightDay
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availablePoses.forEach { candidate ->
                    FilledTonalButton(
                        onClick = { onSelectPose(candidate) },
                        enabled = candidate != pose,
                    ) { Text(candidate.label) }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ComparisonPane(leftDay, pose, onPreviousLeft, onNextLeft, Modifier.weight(1f))
                ComparisonPane(rightDay, pose, onPreviousRight, onNextRight, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ComparisonPane(
    day: TransformationCycleDay,
    pose: TransformationPose,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photo = day.photos.firstOrNull { it.pose == pose }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous comparison day")
            }
            Text(
                "Day ${day.dayNumber} • ${day.captureDate}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next comparison day")
            }
        }
        PhotoPreview(
            photo = photo,
            emptyLabel = "${pose.label} is missing for ${day.captureDate}.",
        )
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
            val unavailableLabel = if (photo == null) {
                emptyLabel
            } else {
                "Photo unavailable on this device. Restore an encrypted Keepfit backup to recover it."
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(unavailableLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
internal fun PoseGuideIllustration(
    pose: TransformationPose,
    showAlignment: Boolean,
    modifier: Modifier = Modifier,
) {
    val figureColor = MaterialTheme.colorScheme.onSurfaceVariant
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val isSide = pose in sidePoses
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .semantics { contentDescription = "Neutral reference for ${pose.label}" },
    ) {
        if (showAlignment) {
            drawLine(guideColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 1.dp.toPx())
            drawLine(guideColor, Offset(0f, size.height * 0.32f), Offset(size.width, size.height * 0.32f), 1.dp.toPx())
            drawLine(guideColor, Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.72f), 1.dp.toPx())
        }

        val centerX = size.width / 2f
        val headY = size.height * 0.16f
        val shoulderY = size.height * 0.32f
        val hipY = size.height * 0.67f
        val shoulderHalf = size.width * if (isSide) 0.08f else 0.18f
        val hipHalf = size.width * if (isSide) 0.055f else 0.11f
        val stroke = 6.dp.toPx()
        fun limb(startX: Float, startY: Float, endX: Float, endY: Float) {
            drawLine(
                color = figureColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(figureColor, radius = size.minDimension * 0.075f, center = Offset(centerX, headY))
        limb(centerX, headY + size.height * 0.08f, centerX, hipY)
        limb(centerX - shoulderHalf, shoulderY, centerX + shoulderHalf, shoulderY)
        limb(centerX - hipHalf, hipY, centerX + hipHalf, hipY)
        limb(centerX - hipHalf, hipY, centerX - size.width * 0.12f, size.height * 0.94f)
        limb(centerX + hipHalf, hipY, centerX + size.width * 0.12f, size.height * 0.94f)

        when (pose) {
            TransformationPose.FRONT_DOUBLE_BICEPS,
            TransformationPose.BACK_DOUBLE_BICEPS,
            -> {
                limb(centerX - shoulderHalf, shoulderY, centerX - size.width * 0.34f, size.height * 0.24f)
                limb(centerX - size.width * 0.34f, size.height * 0.24f, centerX - size.width * 0.25f, size.height * 0.10f)
                limb(centerX + shoulderHalf, shoulderY, centerX + size.width * 0.34f, size.height * 0.24f)
                limb(centerX + size.width * 0.34f, size.height * 0.24f, centerX + size.width * 0.25f, size.height * 0.10f)
            }
            TransformationPose.ABS_AND_CORE -> {
                limb(centerX - shoulderHalf, shoulderY, centerX - size.width * 0.24f, size.height * 0.12f)
                limb(centerX - size.width * 0.24f, size.height * 0.12f, centerX - size.width * 0.09f, size.height * 0.05f)
                limb(centerX + shoulderHalf, shoulderY, centerX + size.width * 0.24f, size.height * 0.12f)
                limb(centerX + size.width * 0.24f, size.height * 0.12f, centerX + size.width * 0.09f, size.height * 0.05f)
            }
            TransformationPose.FRONT_HANDS_ON_HIPS,
            TransformationPose.BACK_LAT_SPREAD,
            -> {
                val elbowOffset = if (pose == TransformationPose.BACK_LAT_SPREAD) 0.34f else 0.28f
                limb(centerX - shoulderHalf, shoulderY, centerX - size.width * elbowOffset, size.height * 0.49f)
                limb(centerX - size.width * elbowOffset, size.height * 0.49f, centerX - hipHalf, hipY)
                limb(centerX + shoulderHalf, shoulderY, centerX + size.width * elbowOffset, size.height * 0.49f)
                limb(centerX + size.width * elbowOffset, size.height * 0.49f, centerX + hipHalf, hipY)
            }
            TransformationPose.CHEST_FOCUSED,
            TransformationPose.RIGHT_SIDE_FLEXED,
            TransformationPose.LEFT_SIDE_FLEXED,
            TransformationPose.SIDE_CHEST_RIGHT,
            TransformationPose.SIDE_CHEST_LEFT,
            -> {
                limb(centerX - shoulderHalf, shoulderY, centerX - size.width * 0.22f, size.height * 0.50f)
                limb(centerX - size.width * 0.22f, size.height * 0.50f, centerX, size.height * 0.58f)
                limb(centerX + shoulderHalf, shoulderY, centerX + size.width * 0.22f, size.height * 0.50f)
                limb(centerX + size.width * 0.22f, size.height * 0.50f, centerX, size.height * 0.58f)
            }
            else -> {
                limb(centerX - shoulderHalf, shoulderY, centerX - size.width * 0.23f, size.height * 0.63f)
                limb(centerX + shoulderHalf, shoulderY, centerX + size.width * 0.23f, size.height * 0.63f)
            }
        }
    }
}

@Composable
internal fun PoseSelectorDialog(
    enabledPoses: List<TransformationPose>,
    onToggle: (TransformationPose, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose transformation poses") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Four relaxed views stay enabled for consistent basic check-ins.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TransformationPose.defaultPoses.forEach { pose ->
                    Text("• ${pose.label}", style = MaterialTheme.typography.bodyMedium)
                }
                TransformationPoseGroup.entries.filterNot { it == TransformationPoseGroup.BASIC }.forEach { group ->
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(group.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    TransformationPose.entries.filter { it.group == group }.forEach { pose ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pose.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    pose.guidance,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = pose in enabledPoses,
                                onCheckedChange = { onToggle(pose, it) },
                                modifier = Modifier.semantics {
                                    contentDescription = "Enable ${pose.label}"
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        },
    )
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

private fun cycleDayDate(days: List<TransformationCycleDay>, currentCaptureDate: String?, delta: Int): LocalDate? {
    if (days.isEmpty()) {
        return null
    }
    val currentIndex = days.indexOfFirst { it.captureDate.toString() == currentCaptureDate }.takeIf { it >= 0 } ?: 0
    val nextIndex = (currentIndex + delta).mod(days.size)
    return days[nextIndex].captureDate
}

private fun TransformationCycle?.availableComparisonPoses(
    enabledPoses: List<TransformationPose>,
): List<TransformationPose> {
    val captured = this?.days.orEmpty().flatMap(TransformationCycleDay::photos).map(TransformationPhoto::pose).toSet()
    return TransformationPose.entries.filter { it in enabledPoses || it in captured }
}

private val sidePoses = setOf(
    TransformationPose.RIGHT_SIDE_RELAXED,
    TransformationPose.LEFT_SIDE_RELAXED,
    TransformationPose.RIGHT_SIDE_FLEXED,
    TransformationPose.LEFT_SIDE_FLEXED,
    TransformationPose.SIDE_CHEST_RIGHT,
    TransformationPose.SIDE_CHEST_LEFT,
)
