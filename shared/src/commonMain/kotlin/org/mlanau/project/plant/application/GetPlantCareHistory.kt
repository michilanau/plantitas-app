package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareTaskRepository

class GetPlantCareHistory(
    private val repository: CareTaskRepository
) {
    operator fun invoke(plantId: PlantId): Flow<List<CareTask.Done>> = repository.observeByPlant(plantId)
}
