package org.mlanau.project.plant.application

import org.mlanau.project.plant.domain.exception.PlantNotFoundException
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.port.PlantRepository

class UpdatePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(
        id: PlantId,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        imageUrl: String? = null
    ): Result<Plant> {
        return runCatchingDomainErrors {
            val existing = repository.findById(id) ?: throw PlantNotFoundException()
            val plant = existing.withDetails(
                name = name,
                description = description,
                location = location,
                lightNeed = lightNeed,
                potSize = potSize,
                imageUrl = imageUrl
            )
            // A rename reaches every pending reminder's text through CareReminderSync's own
            // combine, which observes plants — no need to re-sync this plant's rules by hand.
            repository.save(plant)
        }
    }
}
