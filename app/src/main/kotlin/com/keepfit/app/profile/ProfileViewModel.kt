package com.keepfit.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.model.BodyProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object SetupRequired : ProfileUiState
    data class Ready(val profile: BodyProfile) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = repository.observeLocalProfile()
        .map { profile ->
            profile?.let(ProfileUiState::Ready) ?: ProfileUiState.SetupRequired
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState.Loading,
        )

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()

    fun saveProfile(displayName: String, heightCm: String) {
        when (val result = ProfileInputValidator.validate(displayName, heightCm)) {
            is ProfileValidationResult.Invalid -> _validationMessage.value = result.message
            is ProfileValidationResult.Valid -> {
                _validationMessage.value = null
                viewModelScope.launch {
                    repository.saveProfile(result.input)
                }
            }
        }
    }
}

