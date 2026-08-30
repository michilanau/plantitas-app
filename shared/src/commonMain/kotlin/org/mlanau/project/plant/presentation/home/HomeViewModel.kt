package org.mlanau.project.plant.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.GetNextPendingCareForPlants
import org.mlanau.project.plant.presentation.UiError

data class HomeUiState(
    val plants: List<Plant> = emptyList(),
    val nextCareByPlant: Map<PlantId, CareTask.Pending> = emptyMap(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class HomeViewModel(
    private val findAllPlants: FindAllPlants,
    private val getNextPendingCareForPlants: GetNextPendingCareForPlants
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            combine(findAllPlants(), getNextPendingCareForPlants()) { plants, nextCare ->
                plants to nextCare
            }
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiError.LoadPlantsFailed
                    )
                }
                .collect { (plants, nextCare) ->
                    _uiState.value = _uiState.value.copy(
                        plants = plants,
                        nextCareByPlant = nextCare,
                        isLoading = false
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
