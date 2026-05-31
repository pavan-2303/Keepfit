package com.keepfit.feature.steps.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.steps.StepsViewModel
import com.keepfit.feature.steps.data.StepsStatus

@Composable
fun TodayStepsSection(
    modifier: Modifier = Modifier,
    viewModel: StepsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = viewModel::onPermissionsResult,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DAILY STEPS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedContent(
                targetState = uiState.status,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) using
                        SizeTransform(clip = false)
                },
                label = "steps-status",
            ) { status ->
                Column {
                    when (status) {
                        StepsStatus.LOADING -> {
                            Text("Checking Health Connect", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            SupportingText("Looking for availability and permissions.")
                        }

                        StepsStatus.UNAVAILABLE -> {
                            Text("Steps unavailable", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            SupportingText("This device does not currently support Health Connect.")
                        }

                        StepsStatus.UPDATE_REQUIRED -> {
                            Text("Health Connect update required", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            SupportingText("Install or update Health Connect, then return here to read step totals.")
                            Spacer(modifier = Modifier.height(12.dp))
                            FilledTonalButton(onClick = { openHealthConnectStore(context) }) {
                                Text("Open Health Connect")
                            }
                        }

                        StepsStatus.PERMISSION_REQUIRED -> {
                            Text("Connect steps", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            SupportingText("Grant read-only steps permission to show today's total and a seven-day summary.")
                            Spacer(modifier = Modifier.height(14.dp))
                            Row {
                                Button(onClick = { permissionLauncher.launch(viewModel.requiredPermissions) }) {
                                    Text("Grant steps permission")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                FilledTonalButton(onClick = { openHealthConnectPermissions(context) }) {
                                    Text("Manage access")
                                }
                            }
                        }

                        StepsStatus.CONNECTED -> {
                            val summary = requireNotNull(uiState.summary)
                            AssistChip(onClick = {}, label = { Text("Connected") })
                            Spacer(modifier = Modifier.height(10.dp))
                            if (!summary.hasData) {
                                Text("No step data yet", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                SupportingText("Health Connect is connected. Step totals will appear after data is available.")
                            } else {
                                Text("${summary.todaySteps.formatSteps()} steps", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                SupportingText(
                                    "7-day total ${summary.sevenDayTotal.formatSteps()}  •  avg ${summary.sevenDayAverage.formatSteps()} / day",
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                FilledTonalButton(onClick = viewModel::refresh) {
                                    Text("Refresh")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                FilledTonalButton(onClick = { openHealthConnectPermissions(context) }) {
                                    Text("Manage access")
                                }
                            }
                        }

                        StepsStatus.ERROR -> {
                            Text("Steps unavailable", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            SupportingText(uiState.message ?: "Health Connect steps could not be loaded.")
                            Spacer(modifier = Modifier.height(12.dp))
                            Row {
                                FilledTonalButton(onClick = viewModel::refresh) {
                                    Text("Retry")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                FilledTonalButton(onClick = { openHealthConnectPermissions(context) }) {
                                    Text("Open Health Connect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Long.formatSteps(): String = java.text.NumberFormat.getIntegerInstance().format(this)
