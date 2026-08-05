package org.mlanau.project.plant.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.model.CareRule

interface CareRepository {
    fun getCareRules(plantId: Int): Flow<List<CareRule>>
    suspend fun saveCareRule(rule: CareRule)
    suspend fun deleteCareRule(id: Int)
    
    fun getEventsInRange(from: LocalDate, to: LocalDate): Flow<List<CareEvent>>
    fun getEventsByRuleId(ruleId: Int): Flow<List<CareEvent>>
    suspend fun saveCareEvent(event: CareEvent)
    suspend fun updateEventStatus(eventId: Int, status: org.mlanau.project.plant.domain.model.CareEventStatus, completedAt: kotlinx.datetime.LocalDateTime?)
    suspend fun deleteCareEvent(id: Int)
}
