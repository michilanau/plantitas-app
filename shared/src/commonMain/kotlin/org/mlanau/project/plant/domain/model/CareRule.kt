package org.mlanau.project.plant.domain.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.plant.domain.exceptions.InvalidCareRuleDateRangeException
import org.mlanau.project.plant.domain.exceptions.InvalidRecurrenceException

@JvmInline
value class CareRuleId(val value: Int)

sealed class RecurrenceRule {
    data object Once : RecurrenceRule()

    data class Periodic(val everyDays: Int) : RecurrenceRule() {
        init {
            if (everyDays <= 0) throw InvalidRecurrenceException()
        }
    }

    /**
     * Every instant this recurrence produces starting from [startDate], clamped to `[from, until]`.
     * [until] is expected to already account for the rule's own `endDate` (see
     * [CareRule.occurrencesIn]).
     */
    fun occurrencesBetween(startDate: Instant, from: Instant, until: Instant, timeZone: TimeZone): List<Instant> {
        if (startDate > until) return emptyList()
        return when (this) {
            is Once -> if (startDate in from..until) listOf(startDate) else emptyList()
            is Periodic -> {
                val instants = mutableListOf<Instant>()
                // Skip whole periods to reach `from` efficiently instead of stepping one at a time.
                val daysUntilFrom = startDate.daysUntil(from, timeZone)
                val periodsToSkip = if (daysUntilFrom > 0) daysUntilFrom / everyDays else 0
                var current = startDate.plus(periodsToSkip * everyDays, DateTimeUnit.DAY, timeZone)
                while (current <= until) {
                    if (current >= from) instants.add(current)
                    current = current.plus(everyDays, DateTimeUnit.DAY, timeZone)
                }
                instants
            }
        }
    }

    /**
     * The first instant this recurrence produces, generated from [anchor], that falls strictly
     * after [after]. [after] being `null` means there's nothing to exclude yet, so the result is
     * exactly [anchor] — this lets callers express "no floor" without a sentinel `Instant` value.
     *
     * This is the building block both overdue detection (has anything already passed?) and the
     * anti-loop alarm guard (what's the next strictly-future slot?) are built from — see
     * [org.mlanau.project.plant.domain.service.CareOccurrenceScheduler].
     */
    fun firstOccurrenceAfter(anchor: Instant, after: Instant?, timeZone: TimeZone): Instant? =
        when (this) {
            is Once -> if (after == null || anchor > after) anchor else null
            is Periodic -> {
                if (after == null) {
                    anchor
                } else {
                    val daysUntilAfter = anchor.daysUntil(after, timeZone)
                    val periods = if (daysUntilAfter >= 0) daysUntilAfter / everyDays + 1 else 0
                    var candidate = anchor.plus(periods * everyDays, DateTimeUnit.DAY, timeZone)
                    // The arithmetic above can land exactly on `after` at period/timezone edges
                    // (e.g. a DST transition); step forward until it's strictly later.
                    while (candidate <= after) candidate = candidate.plus(everyDays, DateTimeUnit.DAY, timeZone)
                    candidate
                }
            }
        }

    /**
     * How many occurrences this recurrence produces, generated from [anchor], in `(after, through]`
     * — arithmetic rather than iterative, since this backs the "N days overdue" figure for a
     * periodic rule that may have been neglected for a very long time. `null` [after] again means
     * no floor.
     */
    fun countOccurrencesIn(anchor: Instant, after: Instant?, through: Instant, timeZone: TimeZone): Int {
        val first = firstOccurrenceAfter(anchor, after, timeZone) ?: return 0
        if (first > through) return 0
        return when (this) {
            is Once -> 1
            is Periodic -> first.daysUntil(through, timeZone) / everyDays + 1
        }
    }
}

/**
 * A recurring (or one-off) care task for a plant, e.g. "water 200ml every 3 days".
 * [org.mlanau.project.plant.domain.service.CareOccurrence]s are the individual predicted slots
 * generated from it.
 */
data class CareRule(
    val id: CareRuleId? = null,
    val plantId: PlantId,
    val recurrence: RecurrenceRule,
    /** Since the relative-anchor scheduling model, this is only the floor the rule's occurrences
     * are generated from until the first care is logged — see [effectiveAnchor]. It no longer
     * marks the ongoing rhythm once there's at least one [CareLog] for this (plant, type). */
    val startDate: Instant,
    val endDate: Instant? = null,
    val active: Boolean = true,
    val notificationTime: LocalTime? = null,
    val notificationsEnabled: Boolean = true,
    /** Suppresses every occurrence at or before this instant. Moved forward by dismissing a
     * scheduled or overdue occurrence; independent of the anchor, which tracks care actually
     * performed rather than what was declined. See [dismissedThrough]. */
    val dismissedBefore: Instant? = null,
    val details: CareDetails
) {
    init {
        val end = endDate
        if (end != null && end < startDate) throw InvalidCareRuleDateRangeException()
    }

    val type: CareType get() = details.type

    /** All the occurrences of this rule within `[from, until]`, or nothing if the rule is inactive
     * or its own [endDate] falls before [until]. */
    fun occurrencesIn(from: Instant, until: Instant, timeZone: TimeZone): List<Instant> {
        if (!active) return emptyList()
        val finalUntil = endDate?.let { if (it < until) it else until } ?: until
        return recurrence.occurrencesBetween(startDate, from, finalUntil, timeZone)
    }

    /**
     * Moves [dismissedBefore] forward to at least [instant]. Monotonic by construction — dismissing
     * an old occurrence after a more recent one must not reopen a range that was already cleared,
     * so the marker only ever advances, never retreats.
     */
    fun dismissedThrough(instant: Instant): CareRule =
        copy(dismissedBefore = maxOf(instant, dismissedBefore ?: instant))

    /**
     * The instant the occurrence grid is generated from: the date of the last logged care for
     * this (plant, type), if any, at the rule's own time of day — never the literal timestamp a
     * log happened to be recorded at. Without that normalization, watering at 23:50 one night
     * would drag every future reminder to 23:50 too. Falls back to [startDate] (with its own time
     * of day, unchanged) when there's no care logged yet.
     */
    fun effectiveAnchor(lastCareAt: Instant?, timeZone: TimeZone): Instant {
        val last = lastCareAt ?: return startDate
        val timeOfDay = notificationTime ?: startDate.toLocalDateTime(timeZone).time
        return LocalDateTime(last.toLocalDateTime(timeZone).date, timeOfDay).toInstant(timeZone)
    }

    fun assignedTo(newPlantId: PlantId): CareRule = copy(plantId = newPlantId)

    fun withId(newId: CareRuleId): CareRule = copy(id = newId)
}
