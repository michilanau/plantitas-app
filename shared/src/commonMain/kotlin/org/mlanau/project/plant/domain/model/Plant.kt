package org.mlanau.project.plant.domain.model

import kotlin.time.Instant
import org.mlanau.project.plant.domain.exception.EmptyPlantNameException

@ConsistentCopyVisibility
data class Plant private constructor(
    val id: PlantId?,
    val name: String,
    val description: String?,
    val location: String?,
    val lightNeed: LightNeed?,
    val potSize: PotSize?,
    val imageUrl: String?,
    val createdAt: Instant
) {
    init {
        if (name.isBlank()) throw EmptyPlantNameException()
    }

    fun withId(newId: PlantId): Plant = copy(id = newId)

    fun withDetails(
        name: String,
        description: String?,
        location: String? = this.location,
        lightNeed: LightNeed? = this.lightNeed,
        potSize: PotSize? = this.potSize,
        imageUrl: String? = this.imageUrl
    ): Plant = copy(
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotBlank() },
        location = location?.trim()?.takeIf { it.isNotBlank() },
        lightNeed = lightNeed,
        potSize = potSize,
        imageUrl = imageUrl?.takeIf { it.isNotBlank() }
    )

    companion object {
        /** Builds a new, not-yet-persisted plant: [id] is always null. */
        fun create(
            name: String,
            description: String?,
            location: String? = null,
            lightNeed: LightNeed? = null,
            potSize: PotSize? = null,
            imageUrl: String? = null,
            createdAt: Instant
        ): Plant = Plant(
            id = null,
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            location = location?.trim()?.takeIf { it.isNotBlank() },
            lightNeed = lightNeed,
            potSize = potSize,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
            createdAt = createdAt
        )

        /**
         * Rehydrates a plant from persistence. Applies the same normalization as [create] so a
         * legacy row storing `''` instead of `NULL` in a nullable column doesn't enter the domain
         * as a blank string.
         */
        fun restore(
            id: PlantId,
            name: String,
            description: String?,
            location: String?,
            lightNeed: LightNeed?,
            potSize: PotSize?,
            imageUrl: String?,
            createdAt: Instant
        ): Plant = Plant(
            id = id,
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            location = location?.trim()?.takeIf { it.isNotBlank() },
            lightNeed = lightNeed,
            potSize = potSize,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
            createdAt = createdAt
        )
    }
}
