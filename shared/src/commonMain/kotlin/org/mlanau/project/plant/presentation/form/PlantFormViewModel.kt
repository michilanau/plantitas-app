package org.mlanau.project.plant.presentation.form

import kotlin.time.Clock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.port.ImageStorage
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.FindPlantById
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.application.DeleteCareRule
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.toUiError

data class PlantFormUiState(
    val plant: Plant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiError? = null,
    val isSaveSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val careRules: List<CareRule> = emptyList(),
    // The editable form fields live here (not as `remember` state in the Composable) so they
    // survive process death / configuration changes across the destination's ViewModel scope.
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val lightNeed: LightNeed? = null,
    val potSize: PotSize? = null,
    val imageUrl: String? = null,
    val imageBytes: ByteArray? = null
) {
    val hasChanges: Boolean
        get() = name != (plant?.name ?: "") ||
            description != (plant?.description ?: "") ||
            location != (plant?.location ?: "") ||
            lightNeed != plant?.lightNeed ||
            potSize != plant?.potSize ||
            imageBytes != null
}

class PlantFormViewModel(
    private val findPlantById: FindPlantById,
    private val createPlant: CreatePlant,
    private val updatePlant: UpdatePlant,
    private val deletePlant: DeletePlant,
    private val getCareRules: GetCareRules,
    private val saveCareRule: SaveCareRule,
    private val deleteCareRule: DeleteCareRule,
    private val imageStorage: ImageStorage,
    private val clock: Clock = Clock.System
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantFormUiState())
    val uiState: StateFlow<PlantFormUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadRulesJob: Job? = null

    private var originalCareRules: List<CareRule>? = null

    fun loadPlant(id: Int?) {
        loadJob?.cancel()
        loadRulesJob?.cancel()
        originalCareRules = null

        if (id == null) {
            _uiState.value = PlantFormUiState()
            return
        }

        _uiState.value = PlantFormUiState(isLoading = true)
        val plantId = PlantId(id)

        loadJob = viewModelScope.launch {
            val plant = findPlantById(plantId)
            _uiState.update {
                it.copy(
                    plant = plant,
                    isLoading = false,
                    name = plant?.name ?: "",
                    description = plant?.description ?: "",
                    location = plant?.location ?: "",
                    lightNeed = plant?.lightNeed,
                    potSize = plant?.potSize,
                    imageUrl = plant?.imageUrl
                )
            }
        }
        loadRulesJob = viewModelScope.launch {
            getCareRules(plantId).collect { rules ->
                if (originalCareRules == null) {
                    originalCareRules = rules
                }
                _uiState.update { it.copy(careRules = rules) }
            }
        }
    }

    fun onNameChanged(value: String) = _uiState.update { it.copy(name = value) }
    fun onDescriptionChanged(value: String) = _uiState.update { it.copy(description = value) }
    fun onLocationChanged(value: String) = _uiState.update { it.copy(location = value) }
    fun onLightNeedSelected(value: LightNeed?) = _uiState.update { it.copy(lightNeed = value) }
    fun onPotSizeSelected(value: PotSize?) = _uiState.update { it.copy(potSize = value) }

    fun onImagePicked(bytes: ByteArray) = _uiState.update { it.copy(imageBytes = bytes, imageUrl = null) }

    fun onSavePlant() {
        val state = _uiState.value
        val previousImageUrl = state.plant?.imageUrl

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isSaveSuccess = false) }

            // A freshly picked photo is only written to storage once the user actually saves —
            // not on every pick — so an abandoned edit never leaves an orphaned file behind.
            val finalImageUrl = state.imageBytes?.let { imageStorage.save(it) } ?: state.imageUrl

            val existingPlant = state.plant
            val result = if (existingPlant?.id == null) {
                createPlant(state.name, state.description, state.location, state.lightNeed, state.potSize, finalImageUrl)
            } else {
                updatePlant(
                    existingPlant.id,
                    state.name,
                    state.description,
                    state.location,
                    state.lightNeed,
                    state.potSize,
                    finalImageUrl
                )
            }

            result.onSuccess { savedPlant ->
                val savedPlantId = requireNotNull(savedPlant.id)

                // The new photo (if any) already replaced the old one in finalImageUrl above; the
                // old file is now orphaned.
                if (state.imageBytes != null && previousImageUrl != null) {
                    imageStorage.delete(previousImageUrl)
                }

                // Delegate care rule persistence to the SaveCareRule use case, which handles
                // plantId assignment for newly created plants. Only rules that are new or that the
                // user actually edited are sent: resaving an untouched rule would otherwise wipe
                // its already-generated pending events for no reason.
                val original = originalCareRules ?: emptyList()
                val changedRules = _uiState.value.careRules.filterNot { rule -> original.any { it == rule } }
                changedRules.forEach { rule ->
                    saveCareRule(rule, savedPlantId)
                }
                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
            }
            result.onFailure { exception ->
                _uiState.update { it.copy(isSaving = false, error = exception.toUiError()) }
            }
        }
    }

    fun addCareRule(rule: CareRule) {
        _uiState.update { it.copy(careRules = it.careRules + rule) }
    }

    /**
     * Pauses or resumes a rule in the draft list. Resuming moves the start date to now, so the
     * time spent paused never comes back as a pile of missed occurrences.
     */
    fun toggleCareRulePaused(rule: CareRule) {
        val toggled = if (rule.active) rule.paused() else rule.resumedAt(clock.now())
        updateCareRuleInList(rule, toggled)
    }

    fun updateCareRuleInList(oldRule: CareRule, newRule: CareRule) {
        _uiState.update { state ->
            val newList = state.careRules.map { if (it == oldRule) newRule else it }
            state.copy(careRules = newList)
        }
    }

    fun removeCareRule(rule: CareRule) {
        _uiState.update { it.copy(careRules = it.careRules - rule) }
        viewModelScope.launch {
            rule.id?.let { id ->
                deleteCareRule(id).onFailure { exception ->
                    _uiState.update { it.copy(error = exception.toUiError()) }
                }
            }
        }
    }

    fun onDeletePlant() {
        val id = _uiState.value.plant?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = deletePlant(id)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, isDeleteSuccess = true) }
            }
            result.onFailure { exception ->
                _uiState.update { it.copy(isSaving = false, error = exception.toUiError()) }
            }
        }
    }

    fun resetState() {
        loadJob?.cancel()
        loadRulesJob?.cancel()
        originalCareRules = null
        _uiState.value = PlantFormUiState()
    }
}
