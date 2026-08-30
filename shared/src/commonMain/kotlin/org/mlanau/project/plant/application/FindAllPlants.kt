package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.port.PlantRepository

class FindAllPlants(
    private val repository: PlantRepository
) {
    operator fun invoke(): Flow<List<Plant>> {
        return repository.observeAll()
    }
}
