package org.mlanau.project.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mlanau.project.domain.model.Plant

data class HomeUiState(
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val findAllPlants: FindAllPlants,
    private val savePlant: SavePlant
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPlants()
    }

    fun loadPlants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val plants = findAllPlants()
                _uiState.value = _uiState.value.copy(
                    plants = plants,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onAddPlant(name: String, description: String) {
        viewModelScope.launch {
            savePlant(name, description)
            loadPlants()
        }
    }
}
