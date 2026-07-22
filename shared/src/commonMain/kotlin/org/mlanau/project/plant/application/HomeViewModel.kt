package org.mlanau.project.plant.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.domain.error.EmptyPlantNameException
import org.mlanau.project.plant.domain.model.Plant
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_load_plants
import plantitas_app.shared.generated.resources.error_unknown

data class HomeUiState(
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = false,
    val error: StringResource? = null
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = Res.string.error_load_plants
                )
            }
        }
    }

    fun onAddPlant(name: String, description: String) {
        viewModelScope.launch {
            val result = savePlant(name, description)
            result.onSuccess {
                loadPlants()
            }
            result.onFailure { exception ->
                val errorResource = when (exception) {
                    is EmptyPlantNameException -> Res.string.error_empty_name
                    else -> Res.string.error_unknown
                }
                _uiState.value = _uiState.value.copy(error = errorResource)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
