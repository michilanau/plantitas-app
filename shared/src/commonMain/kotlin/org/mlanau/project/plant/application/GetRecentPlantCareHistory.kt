package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareTaskRepository

/** A plant's [limit] most recent logged cares, newest first. */
class GetRecentPlantCareHistory(
    private val repository: CareTaskRepository
) {
    operator fun invoke(plantId: PlantId, limit: Int): Flow<List<CareTask.Done>> =
        repository.observeRecentByPlant(plantId, limit)
}
