package org.mlanau.project.plant.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exceptions.FutureCareLogException

@JvmInline
value class CareLogId(val value: Int)

/**
 * A care that actually happened, as opposed to a [org.mlanau.project.plant.domain.service.CareOccurrence]
 * which is only ever a prediction. A log is a fact: it survives deleting the rule that (maybe)
 * produced it ([careRuleId] goes to `null` rather than the row disappearing), and it doesn't need
 * one — [scheduledAt] is `null` when the user records a care for a day that never had a planned
 * occurrence. This is what the scheduling anchor for a (plant, [CareType]) is derived from
 * ([performedAt], grouped by plant and type), so "marking a task done" always means creating one
 * of these rather than mutating anything.
 */
data class CareLog(
    val id: CareLogId? = null,
    val plantId: PlantId,
    val careRuleId: CareRuleId?,
    val performedAt: Instant,
    val scheduledAt: Instant? = null,
    val details: CareDetails,
    val note: String? = null
) {
    val type: CareType get() = details.type

    fun withId(newId: CareLogId): CareLog = copy(id = newId)

    companion object {
        /**
         * The only way to construct a log that hasn't come back from persistence yet — a
         * previously saved log is always in the past by construction, but one the user is about
         * to record needs the invariant checked against the clock: logging care that hasn't
         * happened yet would let a future occurrence be marked done, which the UI is meant to
         * prevent from the other direction (disabling the action) but the domain has to guarantee.
         */
        fun create(
            plantId: PlantId,
            careRuleId: CareRuleId?,
            details: CareDetails,
            performedAt: Instant,
            now: Instant,
            scheduledAt: Instant? = null,
            note: String? = null
        ): CareLog {
            if (performedAt > now) throw FutureCareLogException()
            return CareLog(
                plantId = plantId,
                careRuleId = careRuleId,
                performedAt = performedAt,
                scheduledAt = scheduledAt,
                details = details,
                note = note
            )
        }
    }
}
