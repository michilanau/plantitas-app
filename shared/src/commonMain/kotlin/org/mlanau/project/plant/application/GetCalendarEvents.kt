package org.mlanau.project.plant.application

import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.mlanau.project.plant.domain.model.CareEvent
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository

data class CareEventWithPlantName(
    val event: CareEvent,
    /**
     * The plant name, or null if the plant was not found.
     * The UI layer is responsible for localizing the missing case.
     */
    val plantName: String?
)

class GetCalendarEvents(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val generateCareEvents: GenerateCareEvents
) {
    operator fun invoke(from: Instant, until: Instant): Flow<List<CareEventWithPlantName>> {
        return combine(
            careRepository.getAllCareRules(),
            careRepository.getEventsInRange(from, until),
            plantRepository.findAll()
        ) { rules, persistedEvents, plants ->
            generateCareEvents.generate(
                rules = rules,
                persistedEvents = persistedEvents,
                from = from,
                until = until
            ).map { event ->
                CareEventWithPlantName(
                    event = event,
                    plantName = plants.find { it.id == event.plantId }?.name
                )
            }
        }
    }
}
