package org.mlanau.project.plant.infrastructure.persistence

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import org.mlanau.project.plant.domain.model.*
import org.mlanau.project.plant.domain.repository.CareRepository

class SqlDelightCareRepository(database: PlantDb) : CareRepository {
    private val queries = database.plantDbQueries

    override fun getCareRules(plantId: Int): Flow<List<CareRule>> {
        return queries.selectCareRulesByPlantId(plantId.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllCareRules(): Flow<List<CareRule>> {
        return queries.selectAllCareRules().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveCareRule(rule: CareRule): Int {
        val type = when (rule) {
            is WaterCareRule -> "WATER"
            is FertilizeCareRule -> "FERTILIZE"
            is RepotCareRule -> "REPOT"
        }
        val recurrenceType = when (rule.recurrence) {
            is RecurrenceRule.Once -> "ONCE"
            is RecurrenceRule.Periodic -> "PERIODIC"
        }
        val everyDays = (rule.recurrence as? RecurrenceRule.Periodic)?.everyDays?.toLong()
        val ruleId = rule.id

        return if (ruleId != null) {
            queries.updateCareRule(
                plantId = rule.plantId.toLong(),
                type = type,
                recurrenceType = recurrenceType,
                everyDays = everyDays,
                startDate = rule.startDate.toString(),
                notificationTime = rule.notificationTime?.toString(),
                notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
                endDate = rule.endDate?.toString(),
                active = if (rule.active) 1 else 0,
                amountMl = when (rule) {
                    is WaterCareRule -> rule.amountMl?.toLong()
                    is FertilizeCareRule -> rule.doseMl?.toLong()
                    else -> null
                },
                useFilteredWater = if (rule is WaterCareRule) (if (rule.useFilteredWater) 1L else 0L) else null,
                fertilizerName = (rule as? FertilizeCareRule)?.fertilizerName,
                doseMl = (rule as? FertilizeCareRule)?.doseMl?.toLong(),
                dilutionRatio = (rule as? FertilizeCareRule)?.dilutionRatio,
                newPotSize = (rule as? RepotCareRule)?.newPotSize?.name,
                substrateType = (rule as? RepotCareRule)?.substrateType,
                id = ruleId.toLong()
            )
            ruleId
        } else {
            queries.insertCareRule(
                plantId = rule.plantId.toLong(),
                type = type,
                recurrenceType = recurrenceType,
                everyDays = everyDays,
                startDate = rule.startDate.toString(),
                notificationTime = rule.notificationTime?.toString(),
                notificationsEnabled = if (rule.notificationsEnabled) 1L else 0L,
                endDate = rule.endDate?.toString(),
                active = if (rule.active) 1 else 0,
                amountMl = when (rule) {
                    is WaterCareRule -> rule.amountMl?.toLong()
                    is FertilizeCareRule -> rule.doseMl?.toLong()
                    else -> null
                },
                useFilteredWater = if (rule is WaterCareRule) (if (rule.useFilteredWater) 1L else 0L) else null,
                fertilizerName = (rule as? FertilizeCareRule)?.fertilizerName,
                doseMl = (rule as? FertilizeCareRule)?.doseMl?.toLong(),
                dilutionRatio = (rule as? FertilizeCareRule)?.dilutionRatio,
                newPotSize = (rule as? RepotCareRule)?.newPotSize?.name,
                substrateType = (rule as? RepotCareRule)?.substrateType
            )
            queries.lastInsertId().executeAsOne().toInt()
        }
    }

    override suspend fun deleteCareRule(id: Int) {
        queries.deleteCareRule(id.toLong())
    }

    override fun getEventsInRange(from: Instant, to: Instant): Flow<List<CareEvent>> {
        return queries.selectCareEventsInRange(from.toString(), to.toString()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEventsByPlantIdInRange(plantId: Int, from: Instant, to: Instant): Flow<List<CareEvent>> {
        return queries.selectCareEventsByPlantIdInRange(plantId.toLong(), from.toString(), to.toString()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEventsByRuleId(ruleId: Int): Flow<List<CareEvent>> {
        return queries.selectCareEventsByRuleId(ruleId.toLong()).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun findNextEventByPlantId(plantId: Int, from: Instant): Flow<CareEvent?> {
        return queries.selectNextCareEventByPlantId(plantId.toLong(), from.toString()).asFlow().mapToOneOrNull(Dispatchers.IO).map { 
            it?.toDomain()
        }
    }

    override suspend fun saveCareEvent(event: CareEvent) {
        val type = when (event) {
            is WaterCareEvent -> "WATER"
            is FertilizeCareEvent -> "FERTILIZE"
            is RepotCareEvent -> "REPOT"
        }
        val eventId = event.id
        if (eventId != null) {
            queries.updateCareEvent(
                scheduledAt = event.scheduledAt.toString(),
                status = event.status.name,
                completedAt = event.completedAt?.toString(),
                amountMl = when (event) {
                    is WaterCareEvent -> event.amountMl?.toLong()
                    is FertilizeCareEvent -> event.doseMl?.toLong()
                    else -> null
                },
                useFilteredWater = if (event is WaterCareEvent) (if (event.useFilteredWater) 1L else 0L) else null,
                fertilizerName = (event as? FertilizeCareEvent)?.fertilizerName,
                doseMl = (event as? FertilizeCareEvent)?.doseMl?.toLong(),
                dilutionRatio = (event as? FertilizeCareEvent)?.dilutionRatio,
                newPotSize = (event as? RepotCareEvent)?.newPotSize?.name,
                substrateType = (event as? RepotCareEvent)?.substrateType,
                id = eventId.toLong()
            )
        } else {
            queries.insertCareEvent(
                careRuleId = event.careRuleId.toLong(),
                plantId = event.plantId.toLong(),
                type = type,
                scheduledAt = event.scheduledAt.toString(),
                status = event.status.name,
                completedAt = event.completedAt?.toString(),
                originalScheduledAt = event.originalScheduledAt?.toString(),
                amountMl = when (event) {
                    is WaterCareEvent -> event.amountMl?.toLong()
                    is FertilizeCareEvent -> event.doseMl?.toLong()
                    else -> null
                },
                useFilteredWater = if (event is WaterCareEvent) (if (event.useFilteredWater) 1L else 0L) else null,
                fertilizerName = (event as? FertilizeCareEvent)?.fertilizerName,
                doseMl = (event as? FertilizeCareEvent)?.doseMl?.toLong(),
                dilutionRatio = (event as? FertilizeCareEvent)?.dilutionRatio,
                newPotSize = (event as? RepotCareEvent)?.newPotSize?.name,
                substrateType = (event as? RepotCareEvent)?.substrateType
            )
        }
    }

    override suspend fun updateEventStatus(eventId: Int, status: CareEventStatus, completedAt: Instant?) {
        queries.updateCareEventStatus(status.name, completedAt?.toString(), eventId.toLong())
    }

    override suspend fun deleteCareEvent(id: Int) {
        queries.deleteCareEvent(id.toLong())
    }

    override suspend fun deletePendingEventsByRuleId(ruleId: Int) {
        queries.deletePendingCareEventsByRuleId(ruleId.toLong())
    }

    private fun CareRuleEntity.toDomain(): CareRule {
        val recurrence = when (recurrenceType) {
            "ONCE" -> RecurrenceRule.Once
            "PERIODIC" -> RecurrenceRule.Periodic(everyDays!!.toInt())
            else -> throw IllegalStateException("Unknown recurrence type: $recurrenceType")
        }
        val startDate = Instant.parse(startDate)
        val endDate = endDate?.let { Instant.parse(it) }
        val active = active == 1L
        val notificationTime = notificationTime?.let { if (it.isBlank()) null else LocalTime.parse(it) }
        val notificationsEnabled = notificationsEnabled == 1L

        return when (type) {
            "WATER" -> WaterCareRule(
                id = id.toInt(),
                plantId = plantId.toInt(),
                recurrence = recurrence,
                startDate = startDate,
                endDate = endDate,
                active = active,
                notificationTime = notificationTime,
                notificationsEnabled = notificationsEnabled,
                amountMl = amountMl?.toInt(),
                useFilteredWater = useFilteredWater == 1L
            )
            "FERTILIZE" -> FertilizeCareRule(
                id = id.toInt(),
                plantId = plantId.toInt(),
                recurrence = recurrence,
                startDate = startDate,
                endDate = endDate,
                active = active,
                notificationTime = notificationTime,
                notificationsEnabled = notificationsEnabled,
                fertilizerName = fertilizerName!!,
                doseMl = doseMl?.toInt(),
                dilutionRatio = dilutionRatio
            )
            "REPOT" -> RepotCareRule(
                id = id.toInt(),
                plantId = plantId.toInt(),
                recurrence = recurrence,
                startDate = startDate,
                endDate = endDate,
                active = active,
                notificationTime = notificationTime,
                notificationsEnabled = notificationsEnabled,
                newPotSize = PotSize.valueOf(newPotSize!!),
                substrateType = substrateType
            )
            else -> throw IllegalStateException("Unknown care rule type: $type")
        }
    }

    private fun CareEventEntity.toDomain(): CareEvent {
        val scheduledAt = Instant.parse(scheduledAt)
        val status = CareEventStatus.valueOf(status)
        val completedAt = completedAt?.let { Instant.parse(it) }
        val originalScheduledAt = originalScheduledAt?.let { Instant.parse(it) }

        return when (type) {
            "WATER" -> WaterCareEvent(
                id = id.toInt(),
                careRuleId = careRuleId.toInt(),
                plantId = plantId.toInt(),
                scheduledAt = scheduledAt,
                status = status,
                completedAt = completedAt,
                originalScheduledAt = originalScheduledAt,
                amountMl = amountMl?.toInt(),
                useFilteredWater = useFilteredWater == 1L
            )
            "FERTILIZE" -> FertilizeCareEvent(
                id = id.toInt(),
                careRuleId = careRuleId.toInt(),
                plantId = plantId.toInt(),
                scheduledAt = scheduledAt,
                status = status,
                completedAt = completedAt,
                originalScheduledAt = originalScheduledAt,
                fertilizerName = fertilizerName,
                doseMl = doseMl?.toInt(),
                dilutionRatio = dilutionRatio
            )
            "REPOT" -> RepotCareEvent(
                id = id.toInt(),
                careRuleId = careRuleId.toInt(),
                plantId = plantId.toInt(),
                scheduledAt = scheduledAt,
                status = status,
                completedAt = completedAt,
                originalScheduledAt = originalScheduledAt,
                newPotSize = newPotSize?.let { PotSize.valueOf(it) },
                substrateType = substrateType
            )
            else -> throw IllegalStateException("Unknown care event type: $type")
        }
    }
}
