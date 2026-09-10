package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareTaskId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.port.CareTaskRepository

class SqlDelightCareTaskRepository(database: PlantDb) : CareTaskRepository {
    private val queries = database.careTaskQueries

    override fun observeByPlant(plantId: PlantId): Flow<List<CareTask.Done>> {
        return queries.selectCareTasksByPlantId(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeRecentByPlant(plantId: PlantId, limit: Int): Flow<List<CareTask.Done>> {
        return queries.selectRecentCareTasksByPlantId(plantId.value.toLong(), limit.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeInRange(from: Instant, until: Instant): Flow<List<CareTask.Done>> {
        return queries.selectCareTasksInRange(from.toDbString(), until.toDbString()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun findById(id: CareTaskId): CareTask.Done? {
        return queries.selectCareTaskById(id.value.toLong()).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun save(task: CareTask.Done): CareTask.Done {
        // Append-only: there's no update path, since editing "when did I actually water it" is
        // done by undoing (deleting) and completing it again, which is also what keeps the anchor
        // recomputation (MAX(performedAt)) simple.
        queries.insertCareTask(
            plantId = task.plantId.value.toLong(),
            careRuleId = task.careRuleId?.value?.toLong(),
            type = task.care.typeColumn(),
            performedAt = task.performedAt.toDbString(),
            scheduledAt = task.scheduledAt?.toDbString(),
            note = task.note,
            amountMl = task.care.amountMlColumn(),
            useFilteredWater = task.care.useFilteredWaterColumn(),
            fertilizerName = task.care.fertilizerNameColumn(),
            doseMl = task.care.doseMlColumn(),
            dilutionRatio = task.care.dilutionRatioColumn(),
            newPotSize = task.care.newPotSizeColumn(),
            substrateType = task.care.substrateTypeColumn()
        )
        return task.withId(CareTaskId(queries.lastInsertId().executeAsOne().toInt()))
    }

    override suspend fun delete(id: CareTaskId) {
        queries.deleteCareTask(id.value.toLong())
    }

    override fun observeLastCareDates(plantId: PlantId): Flow<Map<CareType, Instant>> {
        return queries.selectLastCareDatesForPlant(plantId.value.toLong()).asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.mapNotNull { row ->
                val lastPerformedAt = row.lastPerformedAt ?: return@mapNotNull null
                CareType.valueOf(row.type) to Instant.parse(lastPerformedAt)
            }.toMap()
        }
    }

    override fun observeLastCareDates(): Flow<Map<PlantId, Map<CareType, Instant>>> {
        return queries.selectLastCareDatesByPlantAndType().asFlow().mapToList(Dispatchers.IO).map { rows ->
            rows.mapNotNull { row ->
                val lastPerformedAt = row.lastPerformedAt ?: return@mapNotNull null
                Triple(PlantId(row.plantId.toInt()), CareType.valueOf(row.type), Instant.parse(lastPerformedAt))
            }.groupBy({ it.first }, { it.second to it.third }).mapValues { (_, entries) -> entries.toMap() }
        }
    }

    override suspend fun lastCareDate(plantId: PlantId, type: CareType): Instant? {
        val lastPerformedAt = queries.selectLastCareDateForPlantAndType(plantId.value.toLong(), type.name).executeAsOneOrNull()?.lastPerformedAt
        return lastPerformedAt?.let { Instant.parse(it) }
    }

    private fun CareTaskEntity.toDomain(): CareTask.Done {
        val recordId = id.toInt()
        return CareTask.Done.restore(
            id = CareTaskId(recordId),
            plantId = PlantId(plantId.toInt()),
            careRuleId = careRuleId?.let { CareRuleId(it.toInt()) },
            performedAt = Instant.parse(performedAt),
            scheduledAt = scheduledAt?.let { Instant.parse(it) },
            note = note,
            care = careDetailsFrom(
                recordId = recordId,
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
