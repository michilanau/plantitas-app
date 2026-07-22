package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.repository.PlantRepository

class FindAllPlants(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(): List<Plant> {
        return repository.findAll()
    }
}
