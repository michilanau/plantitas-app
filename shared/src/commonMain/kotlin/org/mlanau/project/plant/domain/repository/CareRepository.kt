package org.mlanau.project.plant.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareLogId
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.service.CareAnchorKey

interface CareRepository {
    fun getCareRules(plantId: PlantId): Flow<List<CareRule>>
    fun getAllCareRules(): Flow<List<CareRule>>
    suspend fun getCareRule(id: CareRuleId): CareRule?
    suspend fun saveCareRule(rule: CareRule): CareRuleId
    suspend fun updateDismissedBefore(id: CareRuleId, instant: Instant)
    suspend fun deleteCareRule(id: CareRuleId)

    // Care logs — the factual record a CareOccurrence becomes once "done", see CareLog's doc.
    fun getCareLogs(plantId: PlantId): Flow<List<CareLog>>
    fun getCareLogsInRange(from: Instant, to: Instant): Flow<List<CareLog>>
    suspend fun getCareLog(id: CareLogId): CareLog?
    suspend fun saveCareLog(log: CareLog): CareLog
    suspend fun deleteCareLog(id: CareLogId)

    /**
     * The most recent [CareLog.performedAt] for every (plant, type) that has at least one log, in
     * a single aggregated query — the anchor every rule's occurrences are generated from. Exposed
     * this way (rather than one lookup per rule) so a screen loading every rule for every plant,
     * like the calendar, doesn't pay for it with an N+1 query per rule.
     */
    fun getLastCareByPlantAndType(): Flow<Map<CareAnchorKey, Instant>>

    /** Same as [getLastCareByPlantAndType], scoped to one plant — for screens (plant detail) that
     * only ever need that plant's own anchors. */
    fun getLastCareByTypeForPlant(plantId: PlantId): Flow<Map<CareType, Instant>>
}
