package org.mlanau.project.domain.repository

import org.mlanau.project.domain.model.Plant

interface PlantRepository {
    suspend fun findAll(): List<Plant>
    suspend fun save(plant: Plant)
}
