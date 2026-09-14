package com.keepfit.feature.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.feature.workouts.catalog.CatalogAddOutcome
import com.keepfit.feature.workouts.catalog.CatalogLibraryService
import com.keepfit.feature.workouts.catalog.CatalogQuery
import com.keepfit.feature.workouts.catalog.CatalogSearchResult
import com.keepfit.feature.workouts.catalog.ExerciseCatalogProvider
import com.keepfit.feature.workouts.catalog.CatalogExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveCatalogUiState(
    val query: CatalogQuery = CatalogQuery(),
    val isLoading: Boolean = false,
    val result: CatalogSearchResult? = null,
)

@HiltViewModel
class ExerciseCatalogViewModel @Inject constructor(
    private val provider: ExerciseCatalogProvider,
    private val libraryService: CatalogLibraryService,
) : ViewModel() {
    private val _liveState = MutableStateFlow(LiveCatalogUiState())
    val liveState: StateFlow<LiveCatalogUiState> = _liveState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var searchJob: Job? = null

    fun searchLive(query: CatalogQuery) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _liveState.value = LiveCatalogUiState(query = query, isLoading = true)
            val result = provider.search(query)
            _liveState.value = LiveCatalogUiState(query = query, result = result)
        }
    }

    fun addOfflineGuide(guide: CatalogExercise) {
        viewModelScope.launch {
            runCatching { libraryService.addOfflineGuide(guide) }
                .onSuccess { outcome ->
                    _message.value = when (outcome) {
                        CatalogAddOutcome.ADDED -> "${guide.name} added to your library."
                        CatalogAddOutcome.ALREADY_PRESENT -> "${guide.name} is already in your library."
                    }
                }
                .onFailure { error ->
                    _message.value = error.message ?: "The exercise could not be added."
                }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
