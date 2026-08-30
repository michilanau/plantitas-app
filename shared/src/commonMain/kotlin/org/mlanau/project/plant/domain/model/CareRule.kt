package org.mlanau.project.plant.domain.model

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.plant.domain.exception.CorruptedRecordException
import org.mlanau.project.plant.domain.exception.InvalidRecurrenceException

/**
 * A plant's standing instruction for one kind of care: do this every [everyDays] days, starting
 * on [startDate], reminding at [notificationTime].
 *
 * A plant has at most one rule per [CareType] — there is no such thing as two watering schedules
 * for the same plant — which is why the schedule can be anchored on the plant's care history for
 * that type without ever having to ask which rule a past care belonged to.
 */
@ConsistentCopyVisibility
data class CareRule private constructor(
    val id: CareRuleId?,
    val plantId: PlantId?,
    val everyDays: Int,
    val startDate: Instant,
    val active: Boolean,
    val notificationTime: LocalTime,
    val notificationsEnabled: Boolean,
    val details: CareDetails
) {
    init {
        if (everyDays <= 0) throw InvalidRecurrenceException()
    }

    val type: CareType get() = details.type

    /**
     * The instant the occurrence grid is generated from: the later of [startDate] and the date of
     * the last logged care for this (plant, type), taken at the rule's own [notificationTime]
     * rather than at the literal timestamp the care was recorded at — otherwise watering at 23:50
     * one night would drag every future reminder to 23:50 too.
     *
     * A care logged *before* [startDate] is deliberately ignored: it happened under a schedule
     * this rule doesn't describe (a rule that was deleted and recreated, or a care logged ad hoc
     * before the rule existed), so it must not silently override the start date the user chose.
     * This is what makes creating — or resuming — a rule "starting today" actually start today.
     */
    fun anchorFor(lastCareAt: Instant?, timeZone: TimeZone): Instant {
        val last = lastCareAt ?: return startDate
        val normalized = LocalDateTime(last.toLocalDateTime(timeZone).date, notificationTime).toInstant(timeZone)
        return maxOf(normalized, startDate)
    }

    /**
     * Every occurrence this rule produces in `[from, until]`, generated from [anchor].
     *
     * Whole periods are skipped arithmetically to reach [from] instead of stepping one at a time,
     * so a daily rule with an anchor years back still costs one division.
     */
    fun occurrencesBetween(anchor: Instant, from: Instant, until: Instant, timeZone: TimeZone): List<Instant> {
        if (anchor > until) return emptyList()
        val occurrences = mutableListOf<Instant>()
        val daysUntilFrom = anchor.daysUntil(from, timeZone)
        val periodsToSkip = if (daysUntilFrom > 0) daysUntilFrom / everyDays else 0
        var current = anchor.plus(periodsToSkip * everyDays, DateTimeUnit.DAY, timeZone)
        while (current <= until) {
            if (current >= from) occurrences.add(current)
            current = current.plus(everyDays, DateTimeUnit.DAY, timeZone)
        }
        return occurrences
    }

    /**
     * The first occurrence generated from [anchor] that falls strictly after [after]. [after]
     * being `null` means there is nothing to exclude yet, so the result is exactly [anchor] —
     * which is what makes [startDate] itself a real, due occurrence for a rule whose care has
     * never been logged.
     */
    fun firstOccurrenceAfter(anchor: Instant, after: Instant?, timeZone: TimeZone): Instant {
        if (after == null) return anchor
        val daysUntilAfter = anchor.daysUntil(after, timeZone)
        val periods = if (daysUntilAfter >= 0) daysUntilAfter / everyDays + 1 else 0
        var candidate = anchor.plus(periods * everyDays, DateTimeUnit.DAY, timeZone)
        // The arithmetic above can land exactly on `after` at period/timezone edges (e.g. a DST
        // transition); step forward until it's strictly later.
        while (candidate <= after) candidate = candidate.plus(everyDays, DateTimeUnit.DAY, timeZone)
        return candidate
    }

    /** How many occurrences fall in `(after, through]` — the size of the backlog a single overdue task stands for. */
    fun countOccurrencesIn(anchor: Instant, after: Instant?, through: Instant, timeZone: TimeZone): Int {
        val first = firstOccurrenceAfter(anchor, after, timeZone)
        if (first > through) return 0
        return first.daysUntil(through, timeZone) / everyDays + 1
    }

    /** The instant on or after [after] at which this rule nags, i.e. [notificationTime] on [after]'s day or the next. */
    fun reminderSlotAfter(after: Instant, timeZone: TimeZone): Instant {
        val todaySlot = LocalDateTime(after.toLocalDateTime(timeZone).date, notificationTime).toInstant(timeZone)
        return if (todaySlot > after) todaySlot else todaySlot.plus(1, DateTimeUnit.DAY, timeZone)
    }

    fun assignedTo(newPlantId: PlantId): CareRule = copy(plantId = newPlantId)

    fun withId(newId: CareRuleId): CareRule = copy(id = newId)

    /**
     * Edits the schedule of an existing rule, preserving its id, its plant and its paused state.
     * [details] carries the care type, which callers must not change: a rule's type identifies it
     * within its plant, so switching it is deleting one rule and creating another.
     */
    fun withSchedule(
        everyDays: Int,
        startDate: Instant,
        notificationTime: LocalTime,
        notificationsEnabled: Boolean,
        details: CareDetails
    ): CareRule = copy(
        everyDays = everyDays,
        startDate = startDate,
        notificationTime = notificationTime,
        notificationsEnabled = notificationsEnabled,
        details = details
    )

    /** Stops producing tasks and reminders without losing the rule or the plant's care history. */
    fun paused(): CareRule = copy(active = false)

    /**
     * Resumes a paused rule as if it had just been created: [startDate] moves to [now], so the
     * time spent paused never comes back as a pile of missed occurrences, and the first task is
     * due straight away — unless the care was already logged after [now], which re-anchors it to
     * the next full cycle like any other completed care.
     */
    fun resumedAt(now: Instant): CareRule = copy(active = true, startDate = now)

    companion object {
        /**
         * The time of day a reminder fires when the rule doesn't carry one of its own — every
         * rule created through the app picks a time explicitly, so this only covers rows written
         * before the column became mandatory.
         */
        val DEFAULT_NOTIFICATION_TIME = LocalTime(9, 0)

        /** Builds a new, not-yet-persisted rule: [id] is always null. */
        fun create(
            plantId: PlantId? = null,
            everyDays: Int,
            startDate: Instant,
            active: Boolean = true,
            notificationTime: LocalTime = DEFAULT_NOTIFICATION_TIME,
            notificationsEnabled: Boolean = true,
            details: CareDetails
        ): CareRule = CareRule(
            id = null,
            plantId = plantId,
            everyDays = everyDays,
            startDate = startDate,
            active = active,
            notificationTime = notificationTime,
            notificationsEnabled = notificationsEnabled,
            details = details
        )

        /**
         * Rehydrates a rule from persistence. [notificationTime] takes the raw `TEXT` column
         * value and falls back to [DEFAULT_NOTIFICATION_TIME] when it is absent or blank, so a
         * legacy row can't enter the domain without a reminder time.
         */
        fun restore(
            id: CareRuleId,
            plantId: PlantId,
            everyDays: Long?,
            startDate: Instant,
            active: Boolean,
            notificationTime: String?,
            notificationsEnabled: Boolean,
            details: CareDetails
        ): CareRule = CareRule(
            id = id,
            plantId = plantId,
            everyDays = everyDays?.toInt()
                ?: throw CorruptedRecordException(id.value, "care rule missing everyDays"),
            startDate = startDate,
            active = active,
            notificationTime = notificationTime?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }
                ?: DEFAULT_NOTIFICATION_TIME,
            notificationsEnabled = notificationsEnabled,
            details = details
        )
    }
}
