package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.service.CareAnchorKey

class SqlDelightCareRepository(database: PlantDb) : CareRepository {
    private val queries = database.plantDbQueries

    override fun getCareRules(plantId: PlantId): Flow<List<CareRule>> {
        return queries.selectCareRulesByPlantId(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllCareRules(): Flow<List<CareRule>> {
        return queries.selectAllCareRules().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCareRule(id: CareRuleId): CareRule? {
        return queries.selectCareRuleById(id.value.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveCareRule(rule: CareRule): CareRuleId {
        val recurrenceType = when (rule.recurrence) {
            is RecurrenceRule.Once -> "ONCE"
            is RecurrenceRule.Periodic -> "PERIODIC"
        }
        val everyDays = (rule.recurrence as? RecurrenceRule.Periodic)?.everyDays?.toLong()
        val ruleId = rule.id

        return if (ruleId != null) {
            queries.updateCareRule(
                plantId = rule.plantId.value.toLong(),
                type = rule.details.typeColumn(),
                recurrenceType = recurrenceType,
                everyDays = everyDays,
                startDate = rule.startDate.toDbString(),
                notificationTime = rule.notificationTime?.toString(),
                notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
                endDate = rule.endDate?.toDbString(),
                active = if (rule.active) 1 else 0,
                dismissedBefore = rule.dismissedBefore?.toDbString(),
                amountMl = rule.details.amountMlColumn(),
                useFilteredWater = rule.details.useFilteredWaterColumn(),
                fertilizerName = rule.details.fertilizerNameColumn(),
                doseMl = rule.details.doseMlColumn(),
                dilutionRatio = rule.details.dilutionRatioColumn(),
                newPotSize = rule.details.newPotSizeColumn(),
                substrateType = rule.details.substrateTypeColumn(),
                id = ruleId.value.toLong()
            )
            ruleId
        } else {
            queries.insertCareRule(
                plantId = rule.plantId.value.toLong(),
                type = rule.details.typeColumn(),
                recurrenceType = recurrenceType,
                everyDays = everyDays,
                startDate = rule.startDate.toDbString(),
                notificationTime = rule.notificationTime?.toString(),
                notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
                endDate = rule.endDate?.toDbString(),
                active = if (rule.active) 1 else 0,
                dismissedBefore = rule.dismissedBefore?.toDbString(),
                amountMl = rule.details.amountMlColumn(),
                useFilteredWater = rule.details.useFilteredWaterColumn(),
                fertilizerName = rule.details.fertilizerNameColumn(),
                doseMl = rule.details.doseMlColumn(),
                dilutionRatio = rule.details.dilutionRatioColumn(),
                newPotSize = rule.details.newPotSizeColumn(),
                substrateType = rule.details.substrateTypeColumn()
            )
            CareRuleId(queries.lastInsertId().executeAsOne().toInt())
        }
    }

    override suspend fun updateDismissedBefore(id: CareRuleId, instant: Instant) {
        queries.updateCareRuleDismissedBefore(dismissedBefore = instant.toDbString(), id = id.value.toLong())
    }

    override suspend fun deleteCareRule(id: CareRuleId) {
        queries.deleteCareRule(id.value.toLong())
    }

    // --- CareLog surface

    override fun getCareLogs(plantId: PlantId): Flow<List<CareLog>> {
        return queries.selectCareLogsByPlantId(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCareLogsInRange(from: Instant, to: Instant): Flow<List<CareLog>> {
        return queries.selectCareLogsInRange(from.toDbString(), to.toDbString()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCareLog(id: CareLogId): CareLog? {
        return queries.selectCareLogById(id.value.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveCareLog(log: CareLog): CareLog {
        // Logs are append-only: there's no update path, since editing "when did I actually water
        // it" is done by undoing (deleting) and logging again, which is also what keeps the anchor
        // recomputation (MAX(performedAt)) simple — there's never a case where an existing row's
        // performedAt needs to be reconciled against a new value.
        queries.insertCareLog(
            plantId = log.plantId.value.toLong(),
            careRuleId = log.careRuleId?.value?.toLong(),
            type = log.details.typeColumn(),
            performedAt = log.performedAt.toDbString(),
            scheduledAt = log.scheduledAt?.toDbString(),
            note = log.note,
            amountMl = log.details.amountMlColumn(),
            useFilteredWater = log.details.useFilteredWaterColumn(),
            fertilizerName = log.details.fertilizerNameColumn(),
            doseMl = log.details.doseMlColumn(),
            dilutionRatio = log.details.dilutionRatioColumn(),
            newPotSize = log.details.newPotSizeColumn(),
            substrateType = log.details.substrateTypeColumn()
        )
        return log.withId(CareLogId(queries.lastInsertId().executeAsOne().toInt()))
    }

    override suspend fun deleteCareLog(id: CareLogId) {
        queries.deleteCareLog(id.value.toLong())
    }

    override fun getLastCareByPlantAndType(): Flow<Map<CareAnchorKey, Instant>> {
        return queries.selectLastCareByPlantAndType().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.mapNotNull { row ->
                val lastPerformedAt = row.lastPerformedAt ?: return@mapNotNull null
                CareAnchorKey(PlantId(row.plantId.toInt()), CareType.valueOf(row.type)) to Instant.parse(lastPerformedAt)
            }.toMap()
        }
    }

    override fun getLastCareByTypeForPlant(plantId: PlantId): Flow<Map<CareType, Instant>> {
        return queries.selectLastCareByTypeForPlant(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.mapNotNull { row ->
                val lastPerformedAt = row.lastPerformedAt ?: return@mapNotNull null
                CareType.valueOf(row.type) to Instant.parse(lastPerformedAt)
            }.toMap()
        }
    }

    private fun CareRuleEntity.toDomain(): CareRule {
        val recurrence = when (recurrenceType) {
            "ONCE" -> RecurrenceRule.Once
            "PERIODIC" -> RecurrenceRule.Periodic(everyDays!!.toInt())
            else -> throw IllegalStateException("Unknown recurrence type: $recurrenceType")
        }
        return CareRule(
            id = CareRuleId(id.toInt()),
            plantId = PlantId(plantId.toInt()),
            recurrence = recurrence,
            startDate = Instant.parse(startDate),
            endDate = endDate?.let { Instant.parse(it) },
            active = active == 1L,
            notificationTime = notificationTime?.let { if (it.isBlank()) null else LocalTime.parse(it) },
            notificationsEnabled = notificationsEnabled == 1L,
            dismissedBefore = dismissedBefore?.let { Instant.parse(it) },
            details = careDetailsFrom(
                type = type,
                amountMl = amountMl,
                useFilteredWater = useFilteredWater,
                fertilizerName = fertilizerName,
                doseMl = doseMl,
                dilutionRatio = dilutionRatio,
                newPotSize = newPotSize,
                substrateType = substrateType
            )
        )
    }

    private fun CareLogEntity.toDomain(): CareLog {
        return CareLog(
            id = CareLogId(id.toInt()),
            plantId = PlantId(plantId.toInt()),
            careRuleId = careRuleId?.let { CareRuleId(it.toInt()) },
            performedAt = Instant.parse(performedAt),
            scheduledAt = scheduledAt?.let { Instant.parse(it) },
            note = note,
            details = careDetailsFrom(
                type = type,
                amountMl = amountMl,
                useFilteredWater = useFilteredWater,
                fertilizerName = fertilizerName,
                doseMl = doseMl,
                dilutionRatio = dilutionRatio,
                newPotSize = newPotSize,
                substrateType = substrateType
            )
        )
    }
}

/**
 * ISO-8601 UTC truncated to whole seconds. Plain [Instant.toString] omits the fractional part
 * entirely when it's zero, which breaks the lexicographic ordering SQLite's `MAX()`/`BETWEEN` rely
 * on for these TEXT columns: `"...T09:00:00Z"` sorts *after* `"...T09:00:00.500Z"` (`Z` > `.`) even
 * though the first instant is later. Harmless as long as every stored instant is formatted the same
 * way, which is why this is applied uniformly rather than only where it currently seems to matter.
 */
internal fun Instant.toDbString(): String = Instant.fromEpochSeconds(epochSeconds).toString()
