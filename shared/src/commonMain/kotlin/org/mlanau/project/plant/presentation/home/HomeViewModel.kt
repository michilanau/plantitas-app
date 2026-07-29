package org.mlanau.project.plant.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.domain.error.EmptyPlantNameException
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.application.FindAllPlants
import org.mlanau.project.plant.application.SavePlant
import org.mlanau.project.plant.application.DeletePlant
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_load_plants
import plantitas_app.shared.generated.resources.error_unknown

data class HomeUiState(
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val isSaving: Boolean = false,
    val saveError: StringResource? = null,
    val isSaveSuccess: Boolean = false
)

class HomeViewModel(
    private val findAllPlants: FindAllPlants,
    private val savePlant: SavePlant,
    private val deletePlant: DeletePlant
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

    fun onSavePlant(
        id: Int?,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, isSaveSuccess = false)
            val result = savePlant(id, name, description, location, lightNeed, potSize)
            result.onSuccess {
                loadPlants()
                _uiState.value = _uiState.value.copy(isSaving = false, isSaveSuccess = true)
            }
            result.onFailure { exception ->
                val errorResource = when (exception) {
                    is EmptyPlantNameException -> Res.string.error_empty_name
                    else -> Res.string.error_unknown
                }
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = errorResource)
            }
        }
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveError = null, isSaveSuccess = false, isSaving = false)
    }

    fun onDeletePlant(id: Int) {
        viewModelScope.launch {
            val result = deletePlant(id)
            result.onSuccess {
                loadPlants()
            }
            result.onFailure {
                _uiState.value = _uiState.value.copy(error = Res.string.error_unknown)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
