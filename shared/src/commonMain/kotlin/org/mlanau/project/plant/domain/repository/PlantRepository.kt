package org.mlanau.project.plant.domain.repository

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.Plant

interface PlantRepository {
    fun findAll(): Flow<List<Plant>>
    suspend fun findById(id: Int): Plant?
    suspend fun save(plant: Plant): Int
    suspend fun delete(id: Int)
}
