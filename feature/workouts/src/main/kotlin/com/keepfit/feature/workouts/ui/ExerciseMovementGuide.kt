package com.keepfit.feature.workouts.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepfit.core.media.ExerciseGuidance
import com.keepfit.core.media.GuidanceEquipment
import com.keepfit.core.media.GuidanceMotion
import com.keepfit.core.media.GuidancePoint
import com.keepfit.core.media.GuidancePose
import com.keepfit.core.media.GuidanceTarget
import kotlinx.coroutines.launch

@Composable
fun ExerciseMovementGuide(
    guidance: ExerciseGuidance,
    modifier: Modifier = Modifier,
) {
    val motion = remember(guidance.exerciseId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ink = MaterialTheme.colorScheme.onPrimaryContainer
    val ghost = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val accent = MaterialTheme.colorScheme.secondary

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Movement guide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Start in ink · finish in outline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    scope.launch {
                        motion.stop()
                        motion.snapTo(0f)
                        motion.animateTo(1f, tween(900))
                        motion.animateTo(0f, tween(650))
                    }
                },
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text("Play movement")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .semantics {
                            contentDescription = "${guidance.exerciseName} movement diagram"
                        },
                ) {
                    val finish = guidance.finishPose
                    val current = guidance.startPose.interpolateTo(finish, motion.value)
                    drawGround(ink.copy(alpha = 0.18f))
                    drawTargetGlow(current, guidance.target, accent.copy(alpha = 0.18f))
                    drawEquipment(finish, guidance.equipment, guidance.motion, ghost)
                    drawBody(finish, ghost)
                    drawEquipment(current, guidance.equipment, guidance.motion, accent)
                    drawBody(current, ink)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(guidance.primaryCue, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                guidance.safetyCue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Target: ${guidance.target.label()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Original Keepfit movement figure",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ExerciseGuidanceDialog(
    guidance: ExerciseGuidance,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${guidance.exerciseName} guide") },
        text = { ExerciseMovementGuide(guidance) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close guide") }
        },
    )
}

private fun DrawScope.drawBody(pose: GuidancePose, color: Color) {
    val stroke = size.minDimension * 0.032f
    val shoulder = toOffset(pose.shoulder)
    val hip = toOffset(pose.hip)
    drawLine(color, shoulder, hip, stroke, StrokeCap.Round)
    drawLine(color, shoulder, toOffset(pose.leftElbow), stroke, StrokeCap.Round)
    drawLine(color, toOffset(pose.leftElbow), toOffset(pose.leftHand), stroke, StrokeCap.Round)
    drawLine(color, shoulder, toOffset(pose.rightElbow), stroke, StrokeCap.Round)
    drawLine(color, toOffset(pose.rightElbow), toOffset(pose.rightHand), stroke, StrokeCap.Round)
    drawLine(color, hip, toOffset(pose.leftKnee), stroke, StrokeCap.Round)
    drawLine(color, toOffset(pose.leftKnee), toOffset(pose.leftAnkle), stroke, StrokeCap.Round)
    drawLine(color, hip, toOffset(pose.rightKnee), stroke, StrokeCap.Round)
    drawLine(color, toOffset(pose.rightKnee), toOffset(pose.rightAnkle), stroke, StrokeCap.Round)
    drawLine(color, shoulder, toOffset(pose.head), stroke * 0.7f, StrokeCap.Round)
    drawCircle(color, radius = size.minDimension * 0.052f, center = toOffset(pose.head))
}

private fun DrawScope.drawEquipment(
    pose: GuidancePose,
    equipment: GuidanceEquipment,
    motion: GuidanceMotion,
    color: Color,
) {
    val leftHand = toOffset(pose.leftHand)
    val rightHand = toOffset(pose.rightHand)
    val stroke = size.minDimension * 0.018f
    if (motion == GuidanceMotion.BENCH_PRESS) {
        drawLine(
            color.copy(alpha = color.alpha * .65f),
            Offset(size.width * .14f, size.height * .66f),
            Offset(size.width * .67f, size.height * .66f),
            stroke * 1.5f,
            StrokeCap.Round,
        )
    }
    when (equipment) {
        GuidanceEquipment.NONE -> Unit
        GuidanceEquipment.BARBELL -> {
            val left = minOf(leftHand.x, rightHand.x) - size.width * .12f
            val right = maxOf(leftHand.x, rightHand.x) + size.width * .12f
            val y = (leftHand.y + rightHand.y) / 2f
            drawLine(color, Offset(left, y), Offset(right, y), stroke, StrokeCap.Round)
            drawLine(color, Offset(left, y - 12f), Offset(left, y + 12f), stroke * 1.6f)
            drawLine(color, Offset(right, y - 12f), Offset(right, y + 12f), stroke * 1.6f)
        }
        GuidanceEquipment.DUMBBELLS -> {
            drawCircle(color, radius = stroke * 1.4f, center = leftHand)
            drawCircle(color, radius = stroke * 1.4f, center = rightHand)
        }
        GuidanceEquipment.BENCH -> {
            drawLine(
                color,
                Offset(size.width * .15f, size.height * .74f),
                Offset(size.width * .42f, size.height * .62f),
                stroke * 1.4f,
                StrokeCap.Round,
            )
        }
        GuidanceEquipment.CABLE -> {
            val anchor = Offset(size.width * .88f, size.height * .08f)
            drawLine(color.copy(alpha = color.alpha * .75f), anchor, rightHand, stroke)
            drawCircle(color, radius = stroke, center = anchor)
        }
        GuidanceEquipment.PULL_UP_BAR -> {
            drawLine(
                color,
                Offset(size.width * .20f, size.height * .07f),
                Offset(size.width * .80f, size.height * .07f),
                stroke * 1.3f,
            )
        }
        GuidanceEquipment.STEP -> {
            drawRect(
                color.copy(alpha = color.alpha * .6f),
                topLeft = Offset(size.width * .17f, size.height * .73f),
                size = Size(size.width * .28f, size.height * .08f),
            )
        }
        GuidanceEquipment.LEG_PRESS -> {
            drawLine(
                color.copy(alpha = color.alpha * .55f),
                Offset(size.width * .18f, size.height * .78f),
                Offset(size.width * .82f, size.height * .18f),
                stroke * 1.5f,
            )
            drawLine(
                color,
                Offset(size.width * .75f, size.height * .19f),
                Offset(size.width * .88f, size.height * .34f),
                stroke * 2f,
            )
        }
        GuidanceEquipment.WEIGHT_PLATE -> {
            drawCircle(
                color,
                radius = size.minDimension * .065f,
                center = toOffset(pose.hip),
                style = Stroke(stroke),
            )
        }
    }
}

private fun DrawScope.drawTargetGlow(
    pose: GuidancePose,
    target: GuidanceTarget,
    color: Color,
) {
    val center = when (target) {
        GuidanceTarget.CHEST, GuidanceTarget.SHOULDERS,
        GuidanceTarget.UPPER_BACK, GuidanceTarget.LATS -> toOffset(pose.shoulder)
        GuidanceTarget.GLUTES, GuidanceTarget.QUADS,
        GuidanceTarget.POSTERIOR_CHAIN, GuidanceTarget.CORE,
        GuidanceTarget.FULL_BODY -> toOffset(pose.hip)
        GuidanceTarget.CALVES -> toOffset(pose.leftAnkle)
        GuidanceTarget.BICEPS, GuidanceTarget.TRICEPS -> toOffset(pose.leftElbow)
    }
    drawCircle(color, radius = size.minDimension * .12f, center = center)
}

private fun DrawScope.drawGround(color: Color) {
    drawLine(
        color,
        Offset(size.width * .08f, size.height * .94f),
        Offset(size.width * .92f, size.height * .94f),
        size.minDimension * .008f,
    )
}

private fun DrawScope.toOffset(point: GuidancePoint): Offset = Offset(
    x = point.x * size.width,
    y = point.y * size.height,
)

private fun GuidanceTarget.label(): String = name
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar(Char::titlecase)
