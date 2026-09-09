package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mlanau.project.plant.domain.exception.MissingPersistedIdException
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareRuleRepository

class SqlDelightCareRuleRepository(database: PlantDb) : CareRuleRepository {
    private val queries = database.careRuleQueries

    override fun observeByPlant(plantId: PlantId): Flow<List<CareRule>> {
        return queries.selectCareRulesByPlantId(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAll(): Flow<List<CareRule>> {
        return queries.selectAllCareRules().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findById(id: CareRuleId): CareRule? {
        return queries.selectCareRuleById(id.value.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun findByPlantAndType(plantId: PlantId, type: CareType): CareRule? {
        return queries.selectCareRuleByPlantIdAndType(plantId.value.toLong(), type.name)
            .executeAsOneOrNull()
            ?.toDomain()
    }

    override suspend fun save(rule: CareRule): CareRule {
        val ruleId = rule.id ?: return insert(rule)
        queries.updateCareRule(
            everyDays = rule.everyDays.toLong(),
            startDate = rule.startDate.toDbString(),
            notificationTime = rule.notificationTime.toString(),
            notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
            amountMl = rule.details.amountMlColumn(),
            useFilteredWater = rule.details.useFilteredWaterColumn(),
            fertilizerName = rule.details.fertilizerNameColumn(),
            doseMl = rule.details.doseMlColumn(),
            dilutionRatio = rule.details.dilutionRatioColumn(),
            newPotSize = rule.details.newPotSizeColumn(),
            substrateType = rule.details.substrateTypeColumn(),
            id = ruleId.value.toLong()
        )
        return rule
    }

    private fun insert(rule: CareRule): CareRule {
        val plantId = rule.plantId ?: throw MissingPersistedIdException()
        queries.insertCareRule(
            plantId = plantId.value.toLong(),
            type = rule.details.typeColumn(),
            everyDays = rule.everyDays.toLong(),
            startDate = rule.startDate.toDbString(),
            notificationTime = rule.notificationTime.toString(),
            notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
            amountMl = rule.details.amountMlColumn(),
            useFilteredWater = rule.details.useFilteredWaterColumn(),
            fertilizerName = rule.details.fertilizerNameColumn(),
            doseMl = rule.details.doseMlColumn(),
            dilutionRatio = rule.details.dilutionRatioColumn(),
            newPotSize = rule.details.newPotSizeColumn(),
            substrateType = rule.details.substrateTypeColumn()
        )
        return rule.withId(CareRuleId(queries.lastInsertId().executeAsOne().toInt()))
    }

    override suspend fun delete(id: CareRuleId) {
        queries.deleteCareRule(id.value.toLong())
    }

    private fun CareRuleEntity.toDomain(): CareRule = CareRule.restore(
        id = CareRuleId(id.toInt()),
        plantId = PlantId(plantId.toInt()),
        everyDays = everyDays,
        startDate = Instant.parse(startDate),
        notificationTime = notificationTime,
        notificationsEnabled = notificationsEnabled == 1L,
        details = careDetailsFrom(
            recordId = id.toInt(),
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
