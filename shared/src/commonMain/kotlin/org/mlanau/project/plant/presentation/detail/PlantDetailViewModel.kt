package org.mlanau.project.plant.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.GetNextCareEvent
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.CareEvent
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_unknown

data class PlantDetailUiState(
    val plant: Plant? = null,
    val nextEvent: CareEvent? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null
)

class PlantDetailViewModel(
    private val findPlantById: FindPlantById,
    private val getNextCareEvent: GetNextCareEvent
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    fun loadPlant(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val plant = findPlantById(id)
            if (plant != null) {
                _uiState.update { it.copy(plant = plant, isLoading = false) }
                
                // Load next event
                getNextCareEvent(id, Clock.System.now()).collect { event ->
                    _uiState.update { it.copy(nextEvent = event) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = Res.string.error_unknown) }
            }
        }
    }
}
