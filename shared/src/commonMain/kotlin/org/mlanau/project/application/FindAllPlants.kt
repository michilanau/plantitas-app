package org.mlanau.project.application

import org.mlanau.project.domain.model.Plant
import org.mlanau.project.domain.repository.PlantRepository

class FindAllPlants(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(): List<Plant> {
        return repository.findAll()
    }
}
