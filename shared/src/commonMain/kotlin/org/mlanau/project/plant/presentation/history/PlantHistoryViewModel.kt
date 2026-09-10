package org.mlanau.project.plant.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mlanau.project.plant.application.DeleteCareTask
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.GetPlantCareHistory
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.toUiError

class PlantHistoryViewModel(
    private val findPlantById: FindPlantById,
    private val getPlantCareHistory: GetPlantCareHistory,
    private val deleteCareTask: DeleteCareTask
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantHistoryUiState())
    val uiState: StateFlow<PlantHistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadHistory(id: Int) {
        loadJob?.cancel()
        _uiState.value = PlantHistoryUiState(isLoading = true)

        val plantId = PlantId(id)
        loadJob = viewModelScope.launch {
            val plant = findPlantById(plantId)
            if (plant == null) {
                _uiState.update { it.copy(isLoading = false, error = UiError.PlantNotFound) }
                return@launch
            }
            _uiState.update { it.copy(plantName = plant.name) }
            getPlantCareHistory(plantId).collect { history ->
                _uiState.update { it.copy(history = history, isLoading = false) }
            }
        }
    }

    fun onUndoTask(done: CareTask.Done) {
        val id = done.id ?: return
        viewModelScope.launch {
            deleteCareTask(id)
                .onFailure { exception -> _uiState.update { it.copy(error = exception.toUiError()) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
