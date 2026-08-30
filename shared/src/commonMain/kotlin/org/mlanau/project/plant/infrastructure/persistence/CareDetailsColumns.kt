package org.mlanau.project.plant.infrastructure.persistence

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.plant.domain.exception.CorruptedRecordException
import org.mlanau.project.plant.domain.model.CareDetails

/**
 * ISO-8601 UTC, fixed width, truncated to whole seconds.
 *
 * These instants live in TEXT columns, so SQLite's `MAX()` and `BETWEEN` compare them
 * lexicographically — which only agrees with chronological order if every stored instant has
 * exactly the same shape. [Instant.toString] does not guarantee that: it drops the fractional
 * part when it is zero, and drops the seconds field too when both are zero, so `"...T09:00Z"`
 * would sort *after* `"...T09:00:30Z"` (`Z` > `:`). Formatting explicitly removes the guesswork
 * rather than relying on which fields the default rendering happens to keep.
 */
private val DB_INSTANT_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char('T')
    hour(); char(':'); minute(); char(':'); second()
    char('Z')
}

internal fun Instant.toDbString(): String = DB_INSTANT_FORMAT.format(toLocalDateTime(TimeZone.UTC))

internal fun careDetailsFrom(
    recordId: Int,
    type: String,
    amountMl: Long?,
    useFilteredWater: Long?,
    fertilizerName: String?,
    doseMl: Long?,
    dilutionRatio: String?,
    newPotSize: String?,
    substrateType: String?
): CareDetails = when (type) {
    "WATER" -> CareDetails.Water.restore(amountMl = amountMl, useFilteredWater = useFilteredWater)
    "FERTILIZE" -> CareDetails.Fertilize.restore(recordId, fertilizerName, doseMl, dilutionRatio)
    "REPOT" -> CareDetails.Repot.restore(recordId, newPotSize, substrateType)
    else -> throw CorruptedRecordException(recordId, "unknown care type '$type'")
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
