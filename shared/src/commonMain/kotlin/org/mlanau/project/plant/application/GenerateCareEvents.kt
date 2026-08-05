package org.mlanau.project.plant.application

import kotlinx.datetime.*
import org.mlanau.project.plant.domain.model.*

class GenerateCareEvents {

    fun generate(
        rules: List<CareRule>,
        persistedEvents: List<CareEvent>,
        until: LocalDate
    ): List<CareEvent> {
        val allEvents = mutableListOf<CareEvent>()

        for (rule in rules) {
            if (!rule.active) continue
            
            val virtualDates = computeDates(rule.recurrence, rule.startDate, rule.endDate, until)
            
            for (date in virtualDates) {
                val scheduledAt = LocalDateTime(date, rule.notificationTime)
                
                // Check if there's a persisted event for this rule and this slot
                val existingEvent = persistedEvents.find { 
                    it.careRuleId == rule.id && it.originalScheduledAt == scheduledAt 
                }
                
                if (existingEvent != null) {
                    allEvents.add(existingEvent)
                } else {
                    allEvents.add(createVirtualEvent(rule, scheduledAt))
                }
            }
        }
        
        // Also add persisted events that are NOT linked to a rule's slot (e.g. historical or manually added)
        // or events that were moved to a date outside the current recurrence calculation but still in range
        val unlinkedPersistedEvents = persistedEvents.filter { persisted ->
            val isLinked = rules.any { rule ->
                // Check if this persisted event was originally from one of the rules in the range
                // Note: This logic might need refinement depending on how we handle "moved" events
                persisted.careRuleId == rule.id && persisted.originalScheduledAt != null
            }
            !isLinked
        }
        
        allEvents.addAll(unlinkedPersistedEvents)

        return allEvents.sortedBy { it.scheduledAt }
    }

    private fun computeDates(
        recurrence: RecurrenceRule,
        startDate: LocalDate,
        endDate: LocalDate?,
        until: LocalDate
    ): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val finalUntil = if (endDate != null && endDate < until) endDate else until
        
        when (recurrence) {
            is RecurrenceRule.Once -> {
                if (startDate <= finalUntil) {
                    dates.add(startDate)
                }
            }
            is RecurrenceRule.Periodic -> {
                var current = startDate
                while (current <= finalUntil) {
                    dates.add(current)
                    current = current.plus(recurrence.everyDays, DateTimeUnit.DAY)
                }
            }
        }
        return dates
    }

    private fun createVirtualEvent(rule: CareRule, scheduledAt: LocalDateTime): CareEvent {
        return when (rule) {
            is WaterCareRule -> WaterCareEvent(
                careRuleId = rule.id!!,
                plantId = rule.plantId,
                scheduledAt = scheduledAt,
                originalScheduledAt = scheduledAt,
                amountMl = rule.amountMl,
                useFilteredWater = rule.useFilteredWater
            )
            is FertilizeCareRule -> FertilizeCareEvent(
                careRuleId = rule.id!!,
                plantId = rule.plantId,
                scheduledAt = scheduledAt,
                originalScheduledAt = scheduledAt,
                fertilizerName = rule.fertilizerName,
                doseMl = rule.doseMl,
                dilutionRatio = rule.dilutionRatio
            )
            is RepotCareRule -> RepotCareEvent(
                careRuleId = rule.id!!,
                plantId = rule.plantId,
                scheduledAt = scheduledAt,
                originalScheduledAt = scheduledAt,
                newPotSize = rule.newPotSize,
                substrateType = rule.substrateType
            )
        }
    }
}
