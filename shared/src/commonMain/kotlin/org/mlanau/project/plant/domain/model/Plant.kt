package org.mlanau.project.plant.domain.model

import org.mlanau.project.plant.domain.error.EmptyPlantNameException

data class Plant(
    val id: Int? = null,
    val name: String,
    val description: String? = null
) {
    init {
        if (name.isBlank()) {
            throw EmptyPlantNameException()
        }
    }
}
