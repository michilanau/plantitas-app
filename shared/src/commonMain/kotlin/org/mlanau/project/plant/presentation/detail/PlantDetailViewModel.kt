package org.mlanau.project.plant.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mlanau.project.plant.application.CompleteCareTask
import org.mlanau.project.plant.application.DeleteCareTask
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.GetNextPendingCare
import org.mlanau.project.plant.application.GetPlantCareHistory
import org.mlanau.project.plant.application.LogAdHocCare
import org.mlanau.project.plant.application.PauseCareRule
import org.mlanau.project.plant.application.ResumeCareRule
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.presentation.CareToast
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.toUiError

data class PlantDetailUiState(
    val plant: Plant? = null,
    val nextPending: CareTask.Pending? = null,
    val careRules: List<CareRule> = emptyList(),
    val history: List<CareTask.Done> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class PlantDetailViewModel(
    private val findPlantById: FindPlantById,
    private val getNextPendingCare: GetNextPendingCare,
    private val getCareRules: GetCareRules,
    private val getPlantCareHistory: GetPlantCareHistory,
    private val completeCareTask: CompleteCareTask,
    private val logAdHocCare: LogAdHocCare,
    private val deleteCareTask: DeleteCareTask,
    private val saveCareRule: SaveCareRule,
    private val pauseCareRule: PauseCareRule,
    private val resumeCareRule: ResumeCareRule,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantDetailUiState())
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    private val _toasts = Channel<CareToast>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

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
                    getNextPendingCare(plantId).collect { pending ->
                        _uiState.update { it.copy(nextPending = pending) }
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

    fun onMarkDone(pending: CareTask.Pending) {
        viewModelScope.launch {
            completeCareTask(pending)
                .onSuccess { _toasts.send(CareToast.Logged(it)) }
                .publishErrorIfAny()
        }
    }

    fun onLogAdHocCare(details: CareDetails, performedAt: Instant, note: String?) {
        val plantId = _uiState.value.plant?.id ?: return
        viewModelScope.launch {
            logAdHocCare(
                plantId = plantId,
                care = details,
                performedAt = performedAt,
                note = note
            )
                .onSuccess { _toasts.send(CareToast.Logged(it)) }
                .publishErrorIfAny()
        }
    }

    fun onUndoTask(done: CareTask.Done) {
        viewModelScope.launch {
            done.id?.let { id ->
                deleteCareTask(id)
                    .onSuccess { _toasts.send(CareToast.Undone) }
                    .publishErrorIfAny()
            }
        }
    }

    /** UNDO of the snackbar shown after [onMarkDone] / [onLogAdHocCare]. */
    fun onUndoLoggedCare(taskId: CareTaskId) {
        viewModelScope.launch {
            deleteCareTask(taskId).publishErrorIfAny()
        }
    }

    fun onToggleRulePaused(rule: CareRule) {
        val id = rule.id ?: return
        viewModelScope.launch {
            val result = if (rule.active) pauseCareRule(id) else resumeCareRule(id)
            result.publishErrorIfAny()
        }
    }

    fun onSaveRule(rule: CareRule) {
        val plantId = _uiState.value.plant?.id ?: return
        viewModelScope.launch {
            saveCareRule(rule, plantId).publishErrorIfAny()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
