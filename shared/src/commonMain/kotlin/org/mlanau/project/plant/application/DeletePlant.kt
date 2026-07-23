package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.repository.PlantRepository

class DeletePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(id: Int): Result<Unit> {
        return runCatching {
            repository.delete(id)
        }
    }
}
