package com.keepfit.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeeklyReviewUiState(
    val isLoading: Boolean = true,
    val snapshot: WeeklyReviewSnapshot? = null,
    val selectedDraft: ReviewDraft? = null,
    val isEditing: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val repository: WeeklyReviewRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeeklyReviewUiState())
    val uiState: StateFlow<WeeklyReviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            runCatching { repository.load() }
                .onSuccess { snapshot ->
                    _uiState.value = WeeklyReviewUiState(
                        isLoading = false,
                        snapshot = snapshot,
                        selectedDraft = (snapshot as? WeeklyReviewSnapshot.Ready)?.result?.drafts?.firstOrNull(),
                    )
                }
                .onFailure { error ->
                    _uiState.value = WeeklyReviewUiState(
                        isLoading = false,
                        message = error.message ?: "The weekly review could not be loaded.",
                    )
                }
        }
    }

    fun selectDraft(draft: ReviewDraft) {
        _uiState.value = _uiState.value.copy(selectedDraft = draft, isEditing = false, message = null)
    }

    fun toggleEditing() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun chooseDate(date: LocalDate) {
        val draft = _uiState.value.selectedDraft ?: return
        val result = (_uiState.value.snapshot as? WeeklyReviewSnapshot.Ready)?.result ?: return
        val updated = if (draft.type == ReviewDraftType.MOVE_SESSION) {
            draft.copy(
                targetDate = date,
                proposed = "Move it to ${date.dayOfWeek.readable()} for this week only",
            )
        } else {
            val selectedWorkout = result.workoutDays.firstOrNull { it.date.dayOfWeek == date.dayOfWeek } ?: return
            val plannedId = selectedWorkout.plannedWorkoutId ?: return
            draft.copy(
                sourcePlannedWorkoutId = plannedId,
                sourceDate = date,
                targetDate = date,
                current = "${selectedWorkout.title} on ${date.dayOfWeek.readable()}",
            )
        }
        _uiState.value = _uiState.value.copy(selectedDraft = updated, isEditing = false)
    }

    fun approve() = runAction { repository.approve(requireNotNull(_uiState.value.selectedDraft)) }

    fun dismiss() = runAction { repository.dismiss() }

    fun setPaused(paused: Boolean) = runAction { repository.setPaused(paused) }

    private fun runAction(action: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true, message = null)
            runCatching { action() }
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        message = error.message ?: "That change could not be saved.",
                    )
                }
        }
    }
}

private fun java.time.DayOfWeek.readable(): String = name.lowercase().replaceFirstChar(Char::uppercase)
