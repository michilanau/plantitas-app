package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.repository.PlantRepository

class SavePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(
        id: Int? = null,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null
    ): Result<Unit> {
        return runCatching {
            val plant = Plant(
                id = id,
                name = name,
                description = description?.takeIf { it.isNotBlank() },
                location = location?.takeIf { it.isNotBlank() },
                lightNeed = lightNeed,
                potSize = potSize
            )
            repository.save(plant)
        }
    }
}
