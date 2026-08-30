package org.mlanau.project.plant.domain.port

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.PlantId

interface PlantRepository {
    fun observeAll(): Flow<List<Plant>>
    suspend fun findById(id: PlantId): Plant?
    suspend fun save(plant: Plant): Plant
    suspend fun delete(id: PlantId)
}
