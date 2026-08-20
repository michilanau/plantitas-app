package org.mlanau.project.plant.infrastructure.persistence

import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.PotSize
import org.mlanau.project.plant.domain.model.type

/**
 * [CareRuleEntity], [CareEventEntity] and [CareLogEntity] all share the exact same task-detail
 * columns, so both directions of the mapping (domain -> columns below, columns -> domain here) are
 * written once and reused across the three of them.
 */
internal fun careDetailsFrom(
    type: String,
    amountMl: Long?,
    useFilteredWater: Long?,
    fertilizerName: String?,
    doseMl: Long?,
    dilutionRatio: String?,
    newPotSize: String?,
    substrateType: String?
): CareDetails = when (type) {
    "WATER" -> CareDetails.Water(
        amountMl = amountMl?.toInt(),
        useFilteredWater = useFilteredWater == 1L
    )
    "FERTILIZE" -> CareDetails.Fertilize(
        fertilizerName = fertilizerName!!,
        doseMl = doseMl?.toInt(),
        dilutionRatio = dilutionRatio
    )
    "REPOT" -> CareDetails.Repot(
        newPotSize = PotSize.valueOf(newPotSize!!),
        substrateType = substrateType
    )
    else -> throw IllegalStateException("Unknown care type: $type")
}

internal fun CareDetails.typeColumn(): String = type.name

// amountMl is water-specific; a fertilizer's dose has its own doseMl column below (the previous
// implementation wrote it into both, which was redundant since only the WATER row is ever read
// back through this column).
internal fun CareDetails.amountMlColumn(): Long? = (this as? CareDetails.Water)?.amountMl?.toLong()

internal fun CareDetails.useFilteredWaterColumn(): Long? =
    (this as? CareDetails.Water)?.let { if (it.useFilteredWater) 1L else 0L }

internal fun CareDetails.fertilizerNameColumn(): String? = (this as? CareDetails.Fertilize)?.fertilizerName
internal fun CareDetails.doseMlColumn(): Long? = (this as? CareDetails.Fertilize)?.doseMl?.toLong()
internal fun CareDetails.dilutionRatioColumn(): String? = (this as? CareDetails.Fertilize)?.dilutionRatio
internal fun CareDetails.newPotSizeColumn(): String? = (this as? CareDetails.Repot)?.newPotSize?.name
internal fun CareDetails.substrateTypeColumn(): String? = (this as? CareDetails.Repot)?.substrateType
