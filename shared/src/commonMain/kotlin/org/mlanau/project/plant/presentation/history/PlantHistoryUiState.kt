package org.mlanau.project.plant.presentation.history

import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.presentation.UiError

data class PlantHistoryUiState(
    val plantName: String? = null,
    val history: List<CareTask.Done> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)
