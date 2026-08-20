package org.mlanau.project.plant.application

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.repository.CareRepository
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.domain.service.CareAnchors
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler

/**
 * One thing to show on the calendar: either a predicted [CareOccurrence] or an actual [CareLog].
 * Kept as a sealed type (rather than two separate lists) so the UI sorts and lays out a single
 * timeline instead of merging two on its own.
 */
sealed interface CalendarEntry {
    val plantId: PlantId
    val at: Instant
    /** `null` if the owning plant wasn't found — the UI is responsible for localizing that case. */
    val plantName: String?

    data class Scheduled(val occurrence: CareOccurrence, override val plantName: String?) : CalendarEntry {
        override val plantId get() = occurrence.plantId
        override val at get() = occurrence.scheduledAt
    }

    data class Logged(val log: CareLog, override val plantName: String?) : CalendarEntry {
        override val plantId get() = log.plantId
        override val at get() = log.performedAt
    }
}

class GetCalendarEntries(
    private val careRepository: CareRepository,
    private val plantRepository: PlantRepository,
    private val scheduler: CareOccurrenceScheduler,
    private val clock: Clock = Clock.System
) {
    operator fun invoke(from: Instant, until: Instant): Flow<List<CalendarEntry>> {
        return combine(
            careRepository.getAllCareRules(),
            careRepository.getLastCareByPlantAndType(),
            careRepository.getCareLogsInRange(from, until),
            plantRepository.findAll()
        ) { rules, anchorsByKey, logs, plants ->
            val now = clock.now()
            val anchors = CareAnchors.of(anchorsByKey)
            val plantNameById = plants.associate { it.id to it.name }

            val scheduled = scheduler.occurrences(rules, anchors, from, until, now)
                .map { CalendarEntry.Scheduled(it, plantNameById[it.plantId]) }
            val logged = logs.map { CalendarEntry.Logged(it, plantNameById[it.plantId]) }

            (scheduled + logged).sortedBy { it.at }
        }
    }
}
