package org.mlanau.project.plant.application

import kotlin.time.Clock
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.port.PlantRepository

class CreatePlant(
    private val repository: PlantRepository,
    private val clock: Clock = Clock.System
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        imageUrl: String? = null
    ): Result<Plant> {
        return runCatchingDomainErrors {
            val plant = Plant.create(
                name = name,
                description = description,
                location = location,
                lightNeed = lightNeed,
                potSize = potSize,
                imageUrl = imageUrl,
                createdAt = clock.now()
            )
            repository.save(plant)
        }
    }
}
