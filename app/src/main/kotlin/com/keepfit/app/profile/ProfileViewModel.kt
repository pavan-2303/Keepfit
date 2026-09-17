package com.keepfit.app.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.model.BodyProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object SetupRequired : ProfileUiState
    data class Ready(
        val profile: BodyProfile,
        val profiles: List<BodyProfile>,
        val continueGuidedSetup: Boolean = false,
    ) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {
    private val continueGuidedSetup = MutableStateFlow(false)
    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeActiveProfile(),
        repository.observeProfiles(),
        continueGuidedSetup,
    ) { profile, profiles, shouldContinue ->
            when {
                profile != null -> ProfileUiState.Ready(profile, profiles, shouldContinue)
                profiles.isEmpty() -> ProfileUiState.SetupRequired
                else -> ProfileUiState.Loading
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState.Loading,
        )

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureActiveProfile() }
    }

    fun saveProfile(
        displayName: String,
        heightCm: String,
        birthDate: LocalDate?,
        startingWeightKg: String,
    ) {
        validateAndRun(displayName, heightCm, birthDate, startingWeightKg) { input ->
            continueGuidedSetup.value = true
            repository.saveProfile(input)
        }
    }

    fun addProfile(displayName: String, heightCm: String, birthDate: LocalDate?) {
        validateAndRun(displayName, heightCm, birthDate = birthDate, action = repository::addProfile)
    }

    fun editProfile(profileId: String, displayName: String, heightCm: String, birthDate: LocalDate?) {
        validateAndRun(displayName, heightCm, birthDate = birthDate) { input ->
            repository.editProfile(profileId, input)
        }
    }

    fun selectProfile(profileId: String) {
        continueGuidedSetup.value = false
        viewModelScope.launch {
            runCatching { repository.selectProfile(profileId) }
                .onFailure { _validationMessage.value = it.message ?: "Profile could not be selected." }
        }
    }

    fun archiveProfile(profileId: String) {
        viewModelScope.launch {
            repository.archiveProfile(profileId)
                .onFailure { _validationMessage.value = it.message ?: "Profile could not be archived." }
        }
    }

    fun dismissValidationMessage() {
        _validationMessage.value = null
    }

    private fun validateAndRun(
        displayName: String,
        heightCm: String,
        birthDate: LocalDate? = null,
        startingWeightKg: String = "",
        action: suspend (ProfileInput) -> Unit,
    ) {
        when (
            val result = ProfileInputValidator.validate(
                displayName = displayName,
                heightCm = heightCm,
                birthDate = birthDate,
                startingWeightKg = startingWeightKg,
            )
        ) {
            is ProfileValidationResult.Invalid -> _validationMessage.value = result.message
            is ProfileValidationResult.Valid -> {
                _validationMessage.value = null
                viewModelScope.launch {
                    runCatching { action(result.input) }
                        .onFailure { _validationMessage.value = it.message ?: "Profile could not be saved." }
                }
            }
        }
    }
}
