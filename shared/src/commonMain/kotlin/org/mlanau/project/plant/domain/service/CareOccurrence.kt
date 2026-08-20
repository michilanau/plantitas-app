package org.mlanau.project.plant.domain.service

import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId

enum class OccurrenceStatus { SCHEDULED, OVERDUE }

/**
 * A predicted occurrence of a [org.mlanau.project.plant.domain.model.CareRule] — always virtual,
 * never persisted, never "done": marking one done means creating a
 * [org.mlanau.project.plant.domain.model.CareLog], not mutating this. Computed by
 * [CareOccurrenceScheduler] from the rule and the last logged care for its (plant, type); it has
 * no id of its own because it isn't a row anywhere.
 */
data class CareOccurrence(
    val careRuleId: CareRuleId,
    val plantId: PlantId,
    val type: CareType,
    val scheduledAt: Instant,
    val details: CareDetails,
    val status: OccurrenceStatus,
    /** The last logged care for this (plant, type), or `null` if there's never been one. Kept as
     * the raw instant rather than a precomputed "N days" so the UI derives that against its own
     * `now` and doesn't go stale sitting in a [kotlinx.coroutines.flow.StateFlow]. */
    val lastCareAt: Instant?,
    /** How many overdue slots this occurrence stands in for — 1 for a scheduled (non-overdue)
     * occurrence, or the count of missed slots collapsed into this one when [status] is
     * [OccurrenceStatus.OVERDUE]. This is what lets the "days without care" figure grow instead of
     * resetting every time a periodic rule's interval elapses again. */
    val collapsedCount: Int = 1
)
