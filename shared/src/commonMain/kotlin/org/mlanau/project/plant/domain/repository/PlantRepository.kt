package org.mlanau.project.plant.domain.repository

import org.mlanau.project.plant.domain.model.Plant

interface PlantRepository {
    suspend fun findAll(): List<Plant>
    suspend fun save(plant: Plant)
    suspend fun delete(id: Int)
}
