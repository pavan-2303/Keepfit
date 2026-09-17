package com.keepfit.feature.steps.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                SupportingText(
                    when (uiState.status) {
                        StepsStatus.LOADING -> "Checking Health Connect"
                        StepsStatus.UNAVAILABLE -> "Health Connect unavailable"
                        StepsStatus.UPDATE_REQUIRED -> "Health Connect needs an update"
                        StepsStatus.PERMISSION_REQUIRED -> "Optional read-only daily total"
                        StepsStatus.CONNECTED -> {
                            val summary = uiState.summary
                            if (summary == null || !summary.hasData) {
                                "Connected · no data yet"
                            } else {
                                "${summary.todaySteps.formatSteps()} today · " +
                                    "${summary.sevenDayAverage.formatSteps()} daily avg"
                            }
                        }
                        StepsStatus.ERROR -> uiState.message ?: "Could not load steps"
                    },
                )
            }
            when (uiState.status) {
                StepsStatus.PERMISSION_REQUIRED -> TextButton(
                    onClick = { permissionLauncher.launch(viewModel.requiredPermissions) },
                ) { Text("Connect") }
                StepsStatus.UPDATE_REQUIRED -> TextButton(
                    onClick = { openHealthConnectStore(context) },
                ) { Text("Update") }
                StepsStatus.CONNECTED, StepsStatus.ERROR -> TextButton(onClick = viewModel::refresh) {
                    Text("Refresh")
                }
                StepsStatus.LOADING, StepsStatus.UNAVAILABLE -> Unit
            }
        }
    }
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun Long.formatSteps(): String = java.text.NumberFormat.getIntegerInstance().format(this)
