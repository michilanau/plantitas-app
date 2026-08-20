package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository

class GetPlantCareHistory(
    private val repository: CareRepository
) {
    operator fun invoke(plantId: PlantId): Flow<List<CareLog>> = repository.getCareLogs(plantId)
}
