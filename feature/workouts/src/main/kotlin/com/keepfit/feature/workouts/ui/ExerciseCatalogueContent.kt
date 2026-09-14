package com.keepfit.feature.workouts.ui

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.keepfit.feature.workouts.LiveCatalogUiState
import com.keepfit.feature.workouts.catalog.CatalogExercise
import com.keepfit.feature.workouts.catalog.CatalogOrigin
import com.keepfit.feature.workouts.catalog.CatalogQuery
import com.keepfit.feature.workouts.catalog.CatalogSearchResult
import com.keepfit.feature.workouts.catalog.OfflineExerciseCatalog
import com.keepfit.feature.workouts.data.Exercise

enum class ExerciseSource(
    val title: String,
    val status: String,
) {
    PERSONAL("My library", "PRIVATE"),
    OFFLINE("Offline guide", "40 OWNED"),
    LIVE("Live demos", "VIEW-ONLY"),
}

@Composable
fun ExerciseSourceRail(
    selected: ExerciseSource,
    personalCount: Int,
    onSelect: (ExerciseSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExerciseSource.entries.forEach { source ->
                val isSelected = source == selected
                val status = if (source == ExerciseSource.PERSONAL) "$personalCount SAVED" else source.status
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .clickable { onSelect(source) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = when (source) {
                            ExerciseSource.PERSONAL -> MaterialTheme.colorScheme.primary
                            ExerciseSource.OFFLINE -> MaterialTheme.colorScheme.tertiary
                            ExerciseSource.LIVE -> MaterialTheme.colorScheme.secondary
                        },
                        maxLines = 1,
                    )
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineExerciseGuide(
    personalExerciseNames: Set<String>,
    onAdd: (CatalogExercise) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var movementArea by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }
    val results = remember(query, movementArea, equipment) {
        OfflineExerciseCatalog.search(
            CatalogQuery(text = query, movementArea = movementArea, equipment = equipment),
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ProvenanceBanner(
            label = "KEEPFIT OFFLINE GUIDE",
            message = "40 original guides. No internet, account, or download required.",
            accent = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search the offline guide") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Movement", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = movementArea == null,
                onClick = { movementArea = null },
                label = { Text("All") },
            )
            OfflineExerciseCatalog.movementAreas.forEach { area ->
                FilterChip(
                    selected = movementArea == area,
                    onClick = { movementArea = area },
                    label = { Text(area) },
                )
            }
        }
        Text("Equipment", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = equipment == null,
                onClick = { equipment = null },
                label = { Text("Any") },
            )
            OfflineExerciseCatalog.equipment.forEach { option ->
                FilterChip(
                    selected = equipment == option,
                    onClick = { equipment = option },
                    label = { Text(option) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${results.size} guides",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (results.isEmpty()) {
            CatalogueEmptyState(
                title = "No offline guide matches",
                message = "Clear a filter or try a broader exercise name.",
            )
        }
        results.forEach { exercise ->
            val isAdded = personalExerciseNames.any { it.equals(exercise.name, ignoreCase = true) }
            CatalogExerciseCard(
                exercise = exercise,
                actionLabel = if (isAdded) "In library" else "Add",
                actionEnabled = !isAdded,
                onOpen = { selectedExercise = exercise },
                onAction = { onAdd(exercise) },
            )
        }
    }

    selectedExercise?.let { exercise ->
        CatalogExerciseDetailDialog(
            exercise = exercise,
            actionLabel = if (personalExerciseNames.any { it.equals(exercise.name, true) }) {
                "Already in library"
            } else {
                "Add to my library"
            },
            actionEnabled = personalExerciseNames.none { it.equals(exercise.name, true) },
            onAction = { onAdd(exercise) },
            onDismiss = { selectedExercise = null },
        )
    }
}

@Composable
fun LiveExerciseCatalogue(
    state: LiveCatalogUiState,
    onSearch: (CatalogQuery) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var bodyArea by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ProvenanceBanner(
            label = "LIVE / VIEW-ONLY PROTOTYPE",
            message = "Search terms go to ExerciseDB. Results are not saved or added to backups.",
            accent = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Exercise name") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
        )
        OutlinedButton(onClick = { showFilters = !showFilters }) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (showFilters) "Hide filters" else "Filter body area, muscle, equipment")
        }
        if (showFilters) {
            OutlinedTextField(
                value = bodyArea,
                onValueChange = { bodyArea = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Body area, e.g. chest") },
                singleLine = true,
            )
            OutlinedTextField(
                value = muscle,
                onValueChange = { muscle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Target muscle, e.g. pectorals") },
                singleLine = true,
            )
            OutlinedTextField(
                value = equipment,
                onValueChange = { equipment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Equipment, e.g. dumbbell") },
                singleLine = true,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSearch(
                    CatalogQuery(
                        text = query,
                        movementArea = bodyArea.ifBlank { null },
                        targetMuscle = muscle.ifBlank { null },
                        equipment = equipment.ifBlank { null },
                    ),
                )
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "Searching live catalogue..." else "Search live demos")
        }
        Spacer(modifier = Modifier.height(14.dp))

        when {
            state.isLoading -> Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Loading fresh ExerciseDB results")
            }

            state.result == null -> CatalogueEmptyState(
                title = "Live demos wait for you",
                message = "Search deliberately when you want current instructions and an animated reference.",
            )

            state.result is CatalogSearchResult.Failure -> {
                val failure = state.result
                CatalogueEmptyState(
                    title = when (failure.kind) {
                        com.keepfit.feature.workouts.catalog.CatalogFailureKind.OFFLINE -> "You are offline"
                        com.keepfit.feature.workouts.catalog.CatalogFailureKind.TIMEOUT -> "Search timed out"
                        com.keepfit.feature.workouts.catalog.CatalogFailureKind.MALFORMED_RESPONSE -> "Results were not safe to show"
                        com.keepfit.feature.workouts.catalog.CatalogFailureKind.PROVIDER_ERROR -> "Provider unavailable"
                    },
                    message = failure.message,
                )
            }

            state.result is CatalogSearchResult.Success -> {
                val success = state.result
                Text(
                    text = "${success.items.size} shown from ${success.total} matches",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (success.items.isEmpty()) {
                    CatalogueEmptyState(
                        title = "No live matches",
                        message = "Try fewer filters or a broader exercise name.",
                    )
                }
                success.items.forEach { exercise ->
                    CatalogExerciseCard(
                        exercise = exercise,
                        actionLabel = if (exercise.demoUrl == null) "Instructions" else "View demo",
                        onOpen = { selectedExercise = exercise },
                        onAction = { selectedExercise = exercise },
                    )
                }
                if (success.hasMore) {
                    Text(
                        "More matches exist. Refine the search to keep this preview focused.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "ExerciseDB by AscendAPI • live data • general form reference",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    selectedExercise?.let { exercise ->
        CatalogExerciseDetailDialog(
            exercise = exercise,
            actionLabel = null,
            onDismiss = { selectedExercise = null },
        )
    }
}

@Composable
private fun ProvenanceBanner(
    label: String,
    message: String,
    accent: Color,
) {
    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).background(accent, CircleShape),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = accent,
                )
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CatalogExerciseCard(
    exercise: CatalogExercise,
    actionLabel: String,
    actionEnabled: Boolean = true,
    onOpen: () -> Unit,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (exercise.demoUrl == null) Icons.Outlined.FitnessCenter else Icons.Outlined.PlayCircleOutline,
                contentDescription = null,
                tint = if (exercise.demoUrl == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(30.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(exercise.movementArea, exercise.equipment.firstOrNull()).filterNotNull().joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    exercise.attribution,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onAction,
                enabled = actionEnabled,
            ) {
                if (actionLabel == "Add") {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun CatalogExerciseDetailDialog(
    exercise: CatalogExercise,
    actionLabel: String?,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val isLiveResult = exercise.origin == CatalogOrigin.EXERCISE_DB_LIVE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (isLiveResult) "LIVE DEMO • VIEW-ONLY" else "OFFLINE GUIDE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLiveResult) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                exercise.demoUrl?.let { demoUrl ->
                    CacheDisabledGif(demoUrl, exercise.name)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    listOf(exercise.movementArea, exercise.equipment.joinToString()).filter(String::isNotBlank).joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (exercise.instructions.isEmpty()) {
                    Text("Instructions are not available for this result.")
                } else {
                    exercise.instructions.forEachIndexed { index, instruction ->
                        Row(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text("${index + 1}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(instruction, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isLiveResult) {
                        "ExerciseDB by AscendAPI. Loaded live without memory or disk caching. General form reference."
                    } else {
                        "Original Keepfit guidance. General fitness reference."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (actionLabel != null) {
                Button(onClick = onAction, enabled = actionEnabled) { Text(actionLabel) }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun CacheDisabledGif(url: String, exerciseName: String) {
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = "$exerciseName animated demonstration",
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "$exerciseName animated demonstration" },
        contentScale = ContentScale.Fit,
        loading = {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        error = {
            Column(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null)
                Text("Animation unavailable. Follow the written steps below.")
            }
        },
    )
}

@Composable
fun PersonalExerciseDetailDialog(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gifImageLoader = rememberGifImageLoader()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "PRIVATE LIBRARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                exercise.demo?.let { demo ->
                    Spacer(modifier = Modifier.height(10.dp))
                    if (demo.mediaType == "VIDEO") {
                        PrivateVideoDemo(uri = demo.uri, exerciseName = exercise.name)
                    } else {
                        SubcomposeAsyncImage(
                            model = demo.uri,
                            imageLoader = gifImageLoader,
                            contentDescription = "${exercise.name} private animated demonstration",
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = { Text("The attached demonstration could not be opened.") },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(exercise.instructions?.ifBlank { "No instructions added." } ?: "No instructions added.")
                exercise.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { Button(onClick = onEdit) { Text("Edit exercise") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    DisposableEffect(imageLoader) {
        onDispose(imageLoader::shutdown)
    }
    return imageLoader
}

@Composable
private fun PrivateVideoDemo(uri: String, exerciseName: String) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose(player::release)
    }
    AndroidView(
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                useController = true
                this.player = player
                contentDescription = "$exerciseName private video demonstration"
            }
        },
        modifier = Modifier.fillMaxWidth().height(220.dp),
    )
}

@Composable
private fun CatalogueEmptyState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
