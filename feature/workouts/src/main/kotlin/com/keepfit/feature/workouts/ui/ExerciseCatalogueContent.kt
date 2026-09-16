package com.keepfit.feature.workouts.ui

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.keepfit.core.media.CoreExerciseGuidanceCatalog
import com.keepfit.feature.workouts.data.Exercise

@Composable
fun PersonalExerciseDetailDialog(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gifImageLoader = rememberGifImageLoader()
    val isBundled = exercise.source == DATASET_SOURCE
    val guidance = remember(exercise.id) { CoreExerciseGuidanceCatalog.find(exercise.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (isBundled) "BUNDLED CATALOGUE" else "CUSTOM EXERCISE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                guidance?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExerciseMovementGuide(it)
                }
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
                Text("Body area: ${exercise.muscleGroup.catalogueLabel()}")
                exercise.equipment?.takeIf(String::isNotBlank)?.let {
                    Text("Equipment: ${it.catalogueLabel()}")
                }
                exercise.targetMuscle?.takeIf(String::isNotBlank)?.let {
                    Text("Target: ${it.catalogueLabel()}")
                }
                exercise.secondaryMuscles?.takeIf(String::isNotBlank)?.let {
                    Text("Also works: ${it.catalogueLabel()}")
                }
                Spacer(modifier = Modifier.height(12.dp))
                val steps = exercise.instructions
                    ?.lineSequence()
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    ?.toList()
                    .orEmpty()
                if (steps.isEmpty()) {
                    Text("No instructions added.")
                } else {
                    Text("Instructions", style = MaterialTheme.typography.titleSmall)
                    steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step")
                        if (index != steps.lastIndex) Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                exercise.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isBundled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Exercises Dataset • MIT metadata and instructions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

private const val DATASET_SOURCE = "hasaneyldrm/exercises-dataset"
