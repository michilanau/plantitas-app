package org.mlanau.project.plant.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mlanau.project.plant.application.DismissCareOccurrence
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.GetNextCareOccurrence
import org.mlanau.project.plant.application.GetPlantCareHistory
import org.mlanau.project.plant.application.LogCare
import org.mlanau.project.plant.application.UndoCareLog
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.toUiError

data class PlantDetailUiState(
    val plant: Plant? = null,
    val nextOccurrence: CareOccurrence? = null,
    val careRules: List<CareRule> = emptyList(),
    val history: List<CareLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class PlantDetailViewModel(
    private val findPlantById: FindPlantById,
    private val getNextCareOccurrence: GetNextCareOccurrence,
    private val getCareRules: GetCareRules,
    private val getPlantCareHistory: GetPlantCareHistory,
    private val logCare: LogCare,
    private val dismissCareOccurrence: DismissCareOccurrence,
    private val undoCareLog: UndoCareLog,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadPlant(id: Int) {
        loadJob?.cancel()
        // Reset state synchronously to avoid showing old plant data during transition
        _uiState.value = PlantDetailUiState(isLoading = true)

        val plantId = PlantId(id)
        loadJob = viewModelScope.launch {
            val plant = findPlantById(plantId)
            if (plant != null) {
                _uiState.update { it.copy(plant = plant, isLoading = false) }

                launch {
                    getNextCareOccurrence(plantId).collect { occurrence ->
                        _uiState.update { it.copy(nextOccurrence = occurrence) }
                    }
                }

                launch {
                    getCareRules(plantId).collect { rules ->
                        _uiState.update { it.copy(careRules = rules) }
                    }
                }

                launch {
                    getPlantCareHistory(plantId).collect { history ->
                        _uiState.update { it.copy(history = history) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = UiError.PlantNotFound) }
            }
        }
    }

    private fun <T> Result<T>.publishErrorIfAny() {
        onFailure { exception -> _uiState.update { it.copy(error = exception.toUiError()) } }
    }

    fun onMarkDone(occurrence: CareOccurrence) {
        viewModelScope.launch {
            logCare(occurrence).publishErrorIfAny()
        }
    }

    fun onDismissOccurrence(occurrence: CareOccurrence) {
        viewModelScope.launch {
            dismissCareOccurrence(occurrence).publishErrorIfAny()
        }
    }

    fun onLogAdHocCare(details: CareDetails, performedAt: Instant, note: String?) {
        val plantId = _uiState.value.plant?.id ?: return
        viewModelScope.launch {
            logCare(
                plantId = plantId,
                careRuleId = null,
                details = details,
                performedAt = performedAt,
                note = note
            ).publishErrorIfAny()
        }
    }

    fun onUndoLog(log: CareLog) {
        viewModelScope.launch {
            log.id?.let { undoCareLog(it).publishErrorIfAny() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
