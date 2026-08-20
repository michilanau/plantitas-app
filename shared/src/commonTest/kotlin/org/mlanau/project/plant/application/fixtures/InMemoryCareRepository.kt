package org.mlanau.project.plant.application.fixtures

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareLogId
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareAnchorKey

/** In-memory [CareRepository] for application-layer tests, so use cases can be exercised without a
 * real database. [seedLog] lets a test set up an already-persisted log directly, the way a
 * previous session would have left one in a real database. */
class InMemoryCareRepository : CareRepository {
    private val rules = MutableStateFlow<List<CareRule>>(emptyList())
    private val logs = MutableStateFlow<List<CareLog>>(emptyList())
    private var nextRuleId = 1
    private var nextLogId = 1

    /** Counts calls to the aggregated anchor queries, so a test can assert a screen loading many
     * rules across many plants queries the anchor once, not once per rule (the N+1 it exists to
     * avoid). */
    var lastCareByPlantAndTypeCallCount = 0
        private set

    override fun getCareRules(plantId: PlantId): Flow<List<CareRule>> =
        rules.map { list -> list.filter { it.plantId == plantId } }

    override fun getAllCareRules(): Flow<List<CareRule>> = rules

    override suspend fun getCareRule(id: CareRuleId): CareRule? = rules.value.find { it.id == id }

    override suspend fun saveCareRule(rule: CareRule): CareRuleId {
        val id = rule.id ?: CareRuleId(nextRuleId++)
        val saved = rule.withId(id)
        rules.update { list -> list.filterNot { it.id == id } + saved }
        return id
    }

    override suspend fun updateDismissedBefore(id: CareRuleId, instant: Instant) {
        rules.update { list -> list.map { if (it.id == id) it.dismissedThrough(instant) else it } }
    }

    override suspend fun deleteCareRule(id: CareRuleId) {
        rules.update { list -> list.filterNot { it.id == id } }
    }

    override fun getCareLogs(plantId: PlantId): Flow<List<CareLog>> =
        logs.map { list -> list.filter { it.plantId == plantId }.sortedByDescending { it.performedAt } }

    override fun getCareLogsInRange(from: Instant, to: Instant): Flow<List<CareLog>> =
        logs.map { list -> list.filter { it.performedAt in from..to } }

    override suspend fun getCareLog(id: CareLogId): CareLog? = logs.value.find { it.id == id }

    override suspend fun saveCareLog(log: CareLog): CareLog {
        val saved = log.id?.let { log } ?: log.withId(CareLogId(nextLogId++))
        logs.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved
    }

    override suspend fun deleteCareLog(id: CareLogId) {
        logs.update { list -> list.filterNot { it.id == id } }
    }

    override fun getLastCareByPlantAndType(): Flow<Map<CareAnchorKey, Instant>> {
        lastCareByPlantAndTypeCallCount++
        return logs.map { list ->
            list.groupBy { CareAnchorKey(it.plantId, it.type) }
                .mapValues { (_, group) -> group.maxOf { it.performedAt } }
        }
    }

    override fun getLastCareByTypeForPlant(plantId: PlantId): Flow<Map<CareType, Instant>> {
        return logs.map { list ->
            list.filter { it.plantId == plantId }
                .groupBy { it.type }
                .mapValues { (_, group) -> group.maxOf { it.performedAt } }
        }
    }

    fun seedLog(log: CareLog) {
        val saved = log.id?.let { log } ?: log.withId(CareLogId(nextLogId++))
        logs.update { it + saved }
    }

    fun persistedLogs(): List<CareLog> = logs.value
}
