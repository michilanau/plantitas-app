package org.mlanau.project.application

import org.mlanau.project.domain.model.Plant
import org.mlanau.project.domain.repository.PlantRepository

class SavePlant(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(name: String, description: String) {
        if (name.isBlank()) return
        
        val plant = Plant(
            id = 0,
            name = name,
            description = description
        )
        repository.save(plant)
    }
}
