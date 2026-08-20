package org.mlanau.project.plant.application

import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import org.mlanau.project.plant.domain.exceptions.PlantNotFoundException
import org.mlanau.project.plant.domain.model.LightNeed
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository

class UpdatePlant(
    private val repository: PlantRepository,
    private val careRepository: CareRepository,
    private val rescheduleCareReminder: RescheduleCareReminder
) {
    suspend operator fun invoke(
        id: PlantId,
        name: String,
        description: String?,
        location: String? = null,
        lightNeed: LightNeed? = null,
        potSize: PotSize? = null,
        imageUrl: String? = null,
        createdAt: Instant
    ): Result<PlantId> {
        return runCatchingDomainErrors {
            if (repository.findById(id) == null) throw PlantNotFoundException()
            val plant = Plant.create(
                id = id,
                name = name,
                description = description,
                location = location,
                lightNeed = lightNeed,
                potSize = potSize,
                imageUrl = imageUrl,
                createdAt = createdAt
            )
            val savedId = repository.save(plant)

            // The notification text (which includes the plant's name) is resolved once, when the
            // alarm is scheduled, not when it's shown — so a rename has to re-schedule every rule of
            // this plant, otherwise its pending reminder keeps saying the old name.
            val rules = careRepository.getCareRules(savedId).first()
            rules.forEach { rule ->
                rule.id?.let { rescheduleCareReminder(it) }
            }

            savedId
        }
    }
}
