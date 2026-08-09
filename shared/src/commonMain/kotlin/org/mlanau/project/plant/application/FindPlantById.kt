package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.repository.PlantRepository

class FindPlantById(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(id: Int): Plant? {
        return repository.findById(id)
    }
}
