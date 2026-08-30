package org.mlanau.project.plant.domain.port

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId

interface CareTaskRepository {
    fun observeByPlant(plantId: PlantId): Flow<List<CareTask.Done>>
    fun observeInRange(from: Instant, until: Instant): Flow<List<CareTask.Done>>
    suspend fun findById(id: CareTaskId): CareTask.Done?
    suspend fun save(task: CareTask.Done): CareTask.Done
    suspend fun delete(id: CareTaskId)

    fun observeLastCareDates(plantId: PlantId): Flow<Map<CareType, Instant>>
    fun observeLastCareDates(): Flow<Map<PlantId, Map<CareType, Instant>>>
    suspend fun lastCareDate(plantId: PlantId, type: CareType): Instant?
}
