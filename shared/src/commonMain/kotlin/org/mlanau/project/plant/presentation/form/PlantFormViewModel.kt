package org.mlanau.project.plant.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.domain.exceptions.EmptyPlantNameException
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.application.DeletePlant
import org.mlanau.project.plant.application.GetCareRules
import org.mlanau.project.plant.application.SaveCareRule
import org.mlanau.project.plant.application.DeleteCareRule
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_unknown

data class PlantFormUiState(
    val isSaving: Boolean = false,
    val error: StringResource? = null,
    val isSaveSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val careRules: List<CareRule> = emptyList()
)

class PlantFormViewModel(
    private val createPlant: CreatePlant,
    private val updatePlant: UpdatePlant,
    private val deletePlant: DeletePlant,
    private val getCareRules: GetCareRules,
    private val saveCareRule: SaveCareRule,
    private val deleteCareRule: DeleteCareRule
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantFormUiState())
    val uiState: StateFlow<PlantFormUiState> = _uiState.asStateFlow()

    fun loadCareRules(plantId: Int) {
        viewModelScope.launch {
            getCareRules(plantId).collect { rules ->
                _uiState.update { it.copy(careRules = rules) }
            }
        }
    }

    fun onSavePlant(
        id: Int?,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        createdAt: kotlin.time.Instant? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isSaveSuccess = false) }

            val result = if (id == null) {
                createPlant(name, description, location, lightNeed, potSize)
            } else {
                updatePlant(id, name, description, location, lightNeed, potSize, createdAt!!)
            }

            result.onSuccess { savedPlantId ->
                // Delegate care rule persistence to the SaveCareRule use case,
                // which handles plantId assignment for newly created plants.
                _uiState.value.careRules.forEach { rule ->
                    saveCareRule(rule, savedPlantId)
                }
                _uiState.update { it.copy(isSaving = false, isSaveSuccess = true) }
            }
            result.onFailure { exception ->
                val errorResource = when (exception) {
                    is EmptyPlantNameException -> Res.string.error_empty_name
                    else -> Res.string.error_unknown
                }
                _uiState.update { it.copy(isSaving = false, error = errorResource) }
            }
        }
    }

    fun addCareRule(rule: CareRule) {
        _uiState.update { it.copy(careRules = it.careRules + rule) }
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
            rule.id?.let { deleteCareRule(it) }
        }
    }

    fun onDeletePlant(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = deletePlant(id)
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false, isDeleteSuccess = true) }
            }
            result.onFailure {
                _uiState.update { it.copy(isSaving = false, error = Res.string.error_unknown) }
            }
        }
    }

    fun resetState() {
        _uiState.value = PlantFormUiState()
    }
}
