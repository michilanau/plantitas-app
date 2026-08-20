package org.mlanau.project.plant.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exceptions.EmptyPlantNameException

@JvmInline
value class PlantId(val value: Int)

data class Plant(
    val id: PlantId? = null,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val lightNeed: LightNeed? = null,
    val potSize: PotSize? = null,
    val imageUrl: String? = null,
    val createdAt: Instant
) {
    init {
        if (name.isBlank()) {
            throw EmptyPlantNameException()
        }
    }

    companion object {
        /**
         * Builds a [Plant], normalizing blank optional fields to null: a field the user left empty
         * is the same as a field that was never set. [CreatePlant][org.mlanau.project.plant.application.CreatePlant]
         * and [UpdatePlant][org.mlanau.project.plant.application.UpdatePlant] used to duplicate this
         * normalization by hand.
         */
        fun create(
            id: PlantId? = null,
            name: String,
            description: String?,
            location: String? = null,
            lightNeed: LightNeed? = null,
            potSize: PotSize? = null,
            imageUrl: String? = null,
            createdAt: Instant
        ): Plant = Plant(
            id = id,
            name = name,
            description = description?.takeIf { it.isNotBlank() },
            location = location?.takeIf { it.isNotBlank() },
            lightNeed = lightNeed,
            potSize = potSize,
            imageUrl = imageUrl?.takeIf { it.isNotBlank() },
            createdAt = createdAt
        )
    }
}
