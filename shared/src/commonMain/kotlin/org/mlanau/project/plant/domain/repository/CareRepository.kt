package org.mlanau.project.plant.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareEventStatus

interface CareRepository {
    fun getCareRules(plantId: Int): Flow<List<CareRule>>
    fun getAllCareRules(): Flow<List<CareRule>>
    suspend fun saveCareRule(rule: CareRule): Int
    suspend fun deleteCareRule(id: Int)
    
    fun getEventsInRange(from: Instant, to: Instant): Flow<List<CareEvent>>
    fun getEventsByPlantIdInRange(plantId: Int, from: Instant, to: Instant): Flow<List<CareEvent>>
    fun getEventsByRuleId(ruleId: Int): Flow<List<CareEvent>>
    fun findNextEventByPlantId(plantId: Int, from: Instant): Flow<CareEvent?>
    suspend fun saveCareEvent(event: CareEvent)
    suspend fun updateEventStatus(eventId: Int, status: CareEventStatus, completedAt: Instant?)
    suspend fun deleteCareEvent(id: Int)
    suspend fun deletePendingEventsByRuleId(ruleId: Int)
}
