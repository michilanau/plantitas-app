package org.mlanau.project.plant.domain.model

/**
 * The kind of care task, independent of any one occurrence's or rule's specific details. Exists
 * as its own domain concept (rather than switching on [CareDetails] everywhere) because the
 * relative-anchor scheduling model keys the "last time this was done" lookup by
 * (plant, [CareType]) — a care log's type has to be a value you can group and compare, not
 * something re-derived ad hoc at every call site.
 */
enum class CareType { WATER, FERTILIZE, REPOT }

val CareDetails.type: CareType
    get() = when (this) {
        is CareDetails.Water -> CareType.WATER
        is CareDetails.Fertilize -> CareType.FERTILIZE
        is CareDetails.Repot -> CareType.REPOT
    }
