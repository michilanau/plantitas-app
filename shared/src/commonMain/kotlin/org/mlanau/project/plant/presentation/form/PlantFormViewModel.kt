package org.mlanau.project.plant.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.mlanau.project.plant.domain.exceptions.EmptyPlantNameException
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.application.CreatePlant
import org.mlanau.project.plant.application.UpdatePlant
import org.mlanau.project.plant.application.DeletePlant
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.error_empty_name
import plantitas_app.shared.generated.resources.error_unknown

data class PlantFormUiState(
    val isSaving: Boolean = false,
    val error: StringResource? = null,
    val isSaveSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false
)

class PlantFormViewModel(
    private val createPlant: CreatePlant,
    private val updatePlant: UpdatePlant,
    private val deletePlant: DeletePlant
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlantFormUiState())
    val uiState: StateFlow<PlantFormUiState> = _uiState.asStateFlow()

    fun onSavePlant(
        id: Int?,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        createdAt: LocalDateTime? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, isSaveSuccess = false)
            
            val result = if (id == null) {
                createPlant(name, description, location, lightNeed, potSize)
            } else {
                updatePlant(id, name, description, location, lightNeed, potSize, createdAt!!)
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSaveSuccess = true)
            }
            result.onFailure { exception ->
                val errorResource = when (exception) {
                    is EmptyPlantNameException -> Res.string.error_empty_name
                    else -> Res.string.error_unknown
                }
                _uiState.value = _uiState.value.copy(isSaving = false, error = errorResource)
            }
        }
    }

    fun onDeletePlant(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = deletePlant(id)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isDeleteSuccess = true)
            }
            result.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = Res.string.error_unknown)
            }
        }
    }

    fun resetState() {
        _uiState.value = PlantFormUiState()
    }
}
