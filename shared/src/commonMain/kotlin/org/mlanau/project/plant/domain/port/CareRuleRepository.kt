package org.mlanau.project.plant.domain.port

import kotlinx.coroutines.flow.Flow
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId

interface CareRuleRepository {
    fun observeByPlant(plantId: PlantId): Flow<List<CareRule>>
    fun observeAll(): Flow<List<CareRule>>
    suspend fun findById(id: CareRuleId): CareRule?

    /** At most one rule exists per (plant, type), so this returns a rule rather than a list. */
    suspend fun findByPlantAndType(plantId: PlantId, type: CareType): CareRule?

    suspend fun save(rule: CareRule): CareRule
    suspend fun delete(id: CareRuleId)
}
