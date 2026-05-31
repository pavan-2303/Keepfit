package com.keepfit.feature.steps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.steps.data.StepsRepository
import com.keepfit.feature.steps.data.StepsSnapshot
import com.keepfit.feature.steps.data.StepsStatus
import com.keepfit.feature.steps.data.StepsUiState
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
                .onSuccess { snapshot ->
                    _uiState.value = when (snapshot) {
                        StepsSnapshot.PermissionRequired -> StepsUiState(status = StepsStatus.PERMISSION_REQUIRED)
                        StepsSnapshot.Unavailable -> StepsUiState(status = StepsStatus.UNAVAILABLE)
                        StepsSnapshot.UpdateRequired -> StepsUiState(status = StepsStatus.UPDATE_REQUIRED)
                        is StepsSnapshot.Connected -> StepsUiState(
                            status = StepsStatus.CONNECTED,
                            summary = snapshot.summary,
                        )
                    }
                }
                .onFailure {
                    _uiState.value = StepsUiState(
                        status = StepsStatus.ERROR,
                        message = it.message ?: "Health Connect steps could not be loaded.",
                    )
                }
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
