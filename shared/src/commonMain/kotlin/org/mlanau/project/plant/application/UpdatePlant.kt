package org.mlanau.project.plant.application

import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.repository.PlantRepository

class UpdatePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(
        id: Int,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        imageUrl: String? = null,
        createdAt: Instant
    ): Result<Int> {
        return runCatching {
            val plant = Plant(
                id = id,
                name = name,
                description = description?.takeIf { it.isNotBlank() },
                location = location?.takeIf { it.isNotBlank() },
                lightNeed = lightNeed,
                potSize = potSize,
                imageUrl = imageUrl?.takeIf { it.isNotBlank() },
                createdAt = createdAt
            )
            repository.save(plant)
        }
    }
}
