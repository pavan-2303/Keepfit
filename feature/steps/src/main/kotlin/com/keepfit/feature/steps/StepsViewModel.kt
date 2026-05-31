package com.keepfit.feature.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.steps.data.StepsRepository
import com.keepfit.feature.steps.data.StepsStatus
import com.keepfit.feature.steps.data.StepsUiState
import com.keepfit.feature.steps.data.stepsUiStateForError
import com.keepfit.feature.steps.data.stepsUiStateForSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StepsViewModel @Inject constructor(
    private val repository: StepsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(StepsUiState())
    val uiState: StateFlow<StepsUiState> = _uiState.asStateFlow()

    val requiredPermissions: Set<String>
        get() = repository.requiredPermissions

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = StepsStatus.LOADING, message = null)
            runCatching { repository.loadSnapshot() }
                .onSuccess { snapshot -> _uiState.value = stepsUiStateForSnapshot(snapshot) }
                .onFailure { _uiState.value = stepsUiStateForError(it) }
        }
    }

    fun onPermissionsResult(grantedPermissions: Set<String>) {
        if (grantedPermissions.containsAll(requiredPermissions)) {
            refresh()
        } else {
            _uiState.value = StepsUiState(status = StepsStatus.PERMISSION_REQUIRED)
        }
    }
}
