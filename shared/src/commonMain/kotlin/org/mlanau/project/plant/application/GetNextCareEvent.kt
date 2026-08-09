package org.mlanau.project.plant.application

import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.model.CareEventStatus
import org.mlanau.project.plant.domain.repository.CareRepository

class GetNextCareEvent(
    private val repository: CareRepository,
    private val generateCareEvents: GenerateCareEvents
) {
    operator fun invoke(plantId: Int, from: Instant): Flow<CareEvent?> {
        val rulesFlow = repository.getCareRules(plantId)
        
        // We look ahead a reasonable amount of time, e.g., 3 months, to find a virtual event
        val timeZone = TimeZone.currentSystemDefault()
        val until = from.plus(90, DateTimeUnit.DAY, timeZone)
        
        val persistedEventsFlow = repository.getEventsByPlantIdInRange(plantId, from, until)

        return combine(rulesFlow, persistedEventsFlow) { rules, persistedEvents ->
            val allEvents = generateCareEvents.generate(rules, persistedEvents, from, until)
            
            // Find the first event that is PENDING or scheduled for the future
            allEvents.find { it.status == CareEventStatus.PENDING && it.scheduledAt >= from }
        }
    }
}
