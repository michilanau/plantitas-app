package org.mlanau.project.plant.domain.service

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId

data class CareAnchorKey(val plantId: PlantId, val type: CareType)

/**
 * The most recent logged care for every (plant, [CareType]) that has at least one — the anchor
 * every rule's occurrences are generated from. A value class over a plain [Map] so it has
 * structural `equals`, letting a `Flow<CareAnchors>` skip re-emitting when the aggregate hasn't
 * actually changed even if the underlying log rows have (e.g. an unrelated column update).
 *
 * Built from a single aggregated repository query (or, in tests, from a full log list) rather than
 * looked up per rule — the reason this exists as its own type instead of every caller just walking
 * [CareLog]s directly is to make that "one query for everything" shape impossible to accidentally
 * turn into an N+1 per rule.
 */
@JvmInline
value class CareAnchors(private val byKey: Map<CareAnchorKey, Instant>) {
    operator fun get(plantId: PlantId, type: CareType): Instant? = byKey[CareAnchorKey(plantId, type)]

    companion object {
        val EMPTY = CareAnchors(emptyMap())

        fun of(entries: Map<CareAnchorKey, Instant>): CareAnchors = CareAnchors(entries)

        /** For tests: derives the anchor map directly from a list of logs, the way the real
         * `MAX(performedAt) GROUP BY (plantId, type)` query would. */
        fun from(logs: List<CareLog>): CareAnchors = CareAnchors(
            logs.groupBy { CareAnchorKey(it.plantId, it.type) }
                .mapValues { (_, group) -> group.maxOf { it.performedAt } }
        )
    }
}
