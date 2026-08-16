package org.mlanau.project.plant.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.application.*
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.model.CareRule
import kotlinx.datetime.*
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_unknown

data class PlantDetailUiState(
    val plant: Plant? = null,
    val nextEvent: CareEvent? = null,
    val careRules: List<CareRule> = emptyList(),
    val isLoading: Boolean = false,
    val error: StringResource? = null
)

class PlantDetailViewModel(
    private val findPlantById: FindPlantById,
    private val getNextCareEvent: GetNextCareEvent,
    private val getCareRules: GetCareRules,
    private val toggleCareEventStatus: ToggleCareEventStatus,
    private val skipCareEvent: SkipCareEvent,
    private val rescheduleCareEvent: RescheduleCareEvent,
    private val deleteCareEvent: DeleteCareEvent,
    private val resetCareEventStatus: ResetCareEventStatus
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
                launch {
                    getNextCareEvent(id, Clock.System.now()).collect { event ->
                        _uiState.update { it.copy(nextEvent = event) }
                    }
                }

                // Load care rules
                launch {
                    getCareRules(id).collect { rules ->
                        _uiState.update { it.copy(careRules = rules) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = Res.string.error_unknown) }
            }
        }
    }

    fun toggleEventStatus(event: CareEvent) {
        viewModelScope.launch {
            toggleCareEventStatus(event)
        }
    }

    fun skipEvent(event: CareEvent) {
        viewModelScope.launch {
            skipCareEvent(event)
        }
    }

    fun rescheduleEvent(event: CareEvent, newDate: LocalDate) {
        viewModelScope.launch {
            val timeZone = TimeZone.currentSystemDefault()
            val currentDateTime = event.scheduledAt.toLocalDateTime(timeZone)
            val newInstant = LocalDateTime(newDate, currentDateTime.time).toInstant(timeZone)
            rescheduleCareEvent(event, newInstant)
        }
    }

    fun deleteEvent(event: CareEvent) {
        viewModelScope.launch {
            event.id?.let { deleteCareEvent(it) }
        }
    }

    fun resetEventStatus(event: CareEvent) {
        viewModelScope.launch {
            resetCareEventStatus(event)
        }
    }
}
