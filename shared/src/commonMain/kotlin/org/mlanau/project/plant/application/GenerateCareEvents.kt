package org.mlanau.project.plant.application

import kotlinx.datetime.*
import org.mlanau.project.plant.domain.model.*

class GenerateCareEvents {

    fun generate(
        rules: List<CareRule>,
        persistedEvents: List<CareEvent>,
        from: Instant,
        until: Instant
    ): List<CareEvent> {
        val allEvents = persistedEvents.toMutableList()

        for (rule in rules) {
            if (!rule.active) continue
            
            val virtualInstants = computeInstantsInRange(rule, from, until)
            
            for (instant in virtualInstants) {
                // Check if there's already a persisted event for this specific rule and slot.
                // We use a date-based check to avoid duplicates when the rule's time changes.
                val alreadyPersisted = persistedEvents.any { 
                    it.careRuleId == rule.id && isSameSlot(it.originalScheduledAt ?: it.scheduledAt, instant)
                }
                
                if (!alreadyPersisted) {
                    allEvents.add(createVirtualEvent(rule, instant))
                }
            }
        }

        return allEvents.sortedBy { it.scheduledAt }
    }

    private fun isSameSlot(persistedInstant: Instant, virtualInstant: Instant): Boolean {
        if (persistedInstant == virtualInstant) return true
        
        val timeZone = TimeZone.currentSystemDefault()
        return persistedInstant.toLocalDateTime(timeZone).date == virtualInstant.toLocalDateTime(timeZone).date
    }

    private fun computeInstantsInRange(
        rule: CareRule,
        from: Instant,
        until: Instant
    ): List<Instant> {
        val instants = mutableListOf<Instant>()
        val recurrence = rule.recurrence
        val startDate = rule.startDate
        val endDate = rule.endDate
        
        val finalUntil = if (endDate != null && endDate < until) endDate else until
        if (startDate > finalUntil) return emptyList()

        val timeZone = TimeZone.currentSystemDefault()

        when (recurrence) {
            is RecurrenceRule.Once -> {
                if (startDate in from..finalUntil) {
                    instants.add(startDate)
                }
            }
            is RecurrenceRule.Periodic -> {
                val everyDays = recurrence.everyDays
                
                // Calculate days until 'from' to skip periods efficiently
                val daysUntilFrom = startDate.daysUntil(from, timeZone)
                val periodsToSkip = if (daysUntilFrom > 0) daysUntilFrom / everyDays else 0
                
                var current = startDate.plus(periodsToSkip * everyDays, DateTimeUnit.DAY, timeZone)
                
                while (current <= finalUntil) {
                    if (current >= from) {
                        instants.add(current)
                    }
                    current = current.plus(everyDays, DateTimeUnit.DAY, timeZone)
                }
            }
        }
        return instants
    }

    private fun createVirtualEvent(rule: CareRule, scheduledAt: Instant): CareEvent {
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
