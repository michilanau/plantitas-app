package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.repository.PlantRepository

class SavePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(id: Int? = null, name: String, description: String?): Result<Unit> {
        return runCatching {
            val plant = Plant(
                id = id,
                name = name,
                description = description?.takeIf { it.isNotBlank() }
            )
            repository.save(plant)
        }
    }
}
