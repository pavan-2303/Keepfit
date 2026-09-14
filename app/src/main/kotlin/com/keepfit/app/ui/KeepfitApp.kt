package com.keepfit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.app.profile.ProfileUiState
import com.keepfit.app.profile.ProfileViewModel
import com.keepfit.app.ui.home.HomeShell
import com.keepfit.app.ui.onboarding.ProfileSetupScreen
import com.keepfit.core.designsystem.KeepfitTheme

@Composable
fun KeepfitApp(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val validationMessage by viewModel.validationMessage.collectAsStateWithLifecycle()

    KeepfitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val state = uiState) {
                ProfileUiState.Loading -> LoadingScreen()
                ProfileUiState.SetupRequired -> ProfileSetupScreen(
                    validationMessage = validationMessage,
                    onSave = viewModel::saveProfile,
                )
                is ProfileUiState.Ready -> HomeShell(
                    profile = state.profile,
                    startInGuidedSetup = state.continueGuidedSetup,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
