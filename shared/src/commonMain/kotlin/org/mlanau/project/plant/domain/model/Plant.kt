package org.mlanau.project.plant.domain.model

import org.mlanau.project.plant.domain.error.EmptyPlantNameException

data class Plant(
    val id: Int? = null,
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val lightNeed: LightNeed? = null,
    val potSize: PotSize? = null
) {
    init {
        if (name.isBlank()) {
            throw EmptyPlantNameException()
        }
    }
}
