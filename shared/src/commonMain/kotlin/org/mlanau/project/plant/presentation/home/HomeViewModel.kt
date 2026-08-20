package org.mlanau.project.plant.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.presentation.UiError

data class HomeUiState(
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class HomeViewModel(
    private val findAllPlants: FindAllPlants
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPlants()
    }

    private fun loadPlants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            findAllPlants()
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UiError.LoadPlantsFailed
                    )
                }
                .collect { plants ->
                    _uiState.value = _uiState.value.copy(
                        plants = plants,
                        isLoading = false
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
