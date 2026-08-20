package org.mlanau.project.plant.domain.service

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

/**
 * Computes the [CareOccurrence]s a set of [CareRule]s produce, given the last logged care for
 * each (plant, type) — this is pure domain logic, so it belongs here rather than to a use case.
 *
 * The scheduling model is relative: occurrences are generated from the last actual care logged for
 * a (plant, type), not from the rule's own `startDate` — see [CareRule.effectiveAnchor]. Overdue
 * occurrences are collapsed into a single one (the oldest unresolved slot, so the "days without
 * care" figure grows monotonically instead of resetting every time the interval elapses again),
 * and while one is unresolved, nothing future is projected for that rule — it would just move the
 * moment the overdue one is dealt with.
 */
class CareOccurrenceScheduler(
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {

    /**
     * Every visible occurrence of [rules] within `[from, until]`, evaluated against [now]: at most
     * one collapsed overdue occurrence per rule (only if its `scheduledAt` itself falls in the
     * window — a rule overdue outside the requested window contributes nothing to it), otherwise
     * whatever scheduled occurrences that rule produces inside the window.
     */
    fun occurrences(
        rules: List<CareRule>,
        anchors: CareAnchors,
        from: Instant,
        until: Instant,
        now: Instant,
        timeZone: TimeZone = timeZoneProvider()
    ): List<CareOccurrence> {
        return rules
            .flatMap { rule -> occurrencesForRuleInWindow(rule, anchors[rule.plantId, rule.type], from, until, now, timeZone) }
            .sortedBy { it.scheduledAt }
    }

    /**
     * What [rule] most urgently needs right now: its collapsed overdue occurrence if it has one
     * (regardless of how long ago it fell — there is no window here), otherwise its next scheduled
     * occurrence within [lookaheadDays]. For display only (e.g. the plant detail screen) — this can
     * return an instant at or before [now], which is exactly what must never back a scheduled
     * alarm; see [nextReminderAt] for that.
     */
    fun nextOccurrence(
        rule: CareRule,
        lastCareAt: Instant?,
        now: Instant,
        timeZone: TimeZone = timeZoneProvider(),
        lookaheadDays: Int = 365
    ): CareOccurrence? {
        if (!rule.active) return null
        val ruleId = rule.id ?: return null
        val state = anchorStateFor(rule, lastCareAt, timeZone)
        val firstCandidate = firstValidCandidate(rule, state, timeZone) ?: return null

        return if (firstCandidate <= now) {
            val (status, count) = OccurrenceStatus.OVERDUE to overdueCount(rule, state, now, timeZone)
            buildOccurrence(rule, ruleId, firstCandidate, status, lastCareAt, count)
        } else {
            val until = now.plus(lookaheadDays, DateTimeUnit.DAY, timeZone)
            if (firstCandidate > until) null
            else buildOccurrence(rule, ruleId, firstCandidate, OccurrenceStatus.SCHEDULED, lastCareAt, collapsedCount = 1)
        }
    }

    /**
     * The next instant strictly after [after] that [rule] would remind about — ignoring overdue
     * collapsing entirely, so the result is always strictly later than [after]. This is the ONLY
     * function that should ever be used to schedule a platform alarm from: [nextOccurrence] and
     * [occurrences] can both legitimately return an instant at or before "now" (the collapsed
     * overdue occurrence), and scheduling an alarm for a past instant fires it immediately — which,
     * the moment that alarm's receiver re-schedules the "next" one the same way, loops forever.
     */
    fun nextReminderAt(
        rule: CareRule,
        lastCareAt: Instant?,
        after: Instant,
        timeZone: TimeZone = timeZoneProvider()
    ): Instant? {
        if (!rule.active) return null
        val state = anchorStateFor(rule, lastCareAt, timeZone)
        val floor = state.resolvedThrough?.let { maxOf(after, it) } ?: after
        return rule.recurrence.firstOccurrenceAfter(state.anchorInstant, floor, timeZone)
            ?.takeIf { rule.endDate == null || it <= rule.endDate }
    }

    private fun occurrencesForRuleInWindow(
        rule: CareRule,
        lastCareAt: Instant?,
        from: Instant,
        until: Instant,
        now: Instant,
        timeZone: TimeZone
    ): List<CareOccurrence> {
        if (!rule.active) return emptyList()
        val ruleId = rule.id ?: return emptyList()
        val state = anchorStateFor(rule, lastCareAt, timeZone)
        val firstCandidate = firstValidCandidate(rule, state, timeZone) ?: return emptyList()

        return if (firstCandidate <= now) {
            if (firstCandidate !in from..until) {
                emptyList()
            } else {
                val count = overdueCount(rule, state, now, timeZone)
                listOf(buildOccurrence(rule, ruleId, firstCandidate, OccurrenceStatus.OVERDUE, lastCareAt, count))
            }
        } else {
            val finalUntil = rule.endDate?.let { minOf(it, until) } ?: until
            val effectiveFrom = maxOf(from, firstCandidate)
            if (effectiveFrom > finalUntil) {
                emptyList()
            } else {
                rule.recurrence.occurrencesBetween(state.anchorInstant, effectiveFrom, finalUntil, timeZone)
                    .map { buildOccurrence(rule, ruleId, it, OccurrenceStatus.SCHEDULED, lastCareAt, collapsedCount = 1) }
            }
        }
    }

    /** The rule's anchor and the floor every occurrence must fall strictly after — see
     * [RuleAnchorState]. Centralized so overdue detection, the collapsed count, and the
     * anti-loop alarm guard can never disagree about what's already resolved. */
    private fun anchorStateFor(rule: CareRule, lastCareAt: Instant?, timeZone: TimeZone): RuleAnchorState {
        val anchorInstant = when (rule.recurrence) {
            is RecurrenceRule.Once -> rule.startDate
            is RecurrenceRule.Periodic -> rule.effectiveAnchor(lastCareAt, timeZone)
        }
        // Both the logged instant itself and its normalized (time-of-day-adjusted) anchor slot
        // have to be excluded: without also taking the raw lastCareAt into account, watering late
        // at night (after the rule's own reminder time) would leave that same day's normalized
        // slot looking un-addressed and it would show up as overdue.
        val resolvedThrough = listOfNotNull(
            rule.dismissedBefore,
            lastCareAt?.let { maxOf(it, anchorInstant) }
        ).maxOrNull()
        return RuleAnchorState(anchorInstant, resolvedThrough)
    }

    private fun firstValidCandidate(rule: CareRule, state: RuleAnchorState, timeZone: TimeZone): Instant? =
        rule.recurrence.firstOccurrenceAfter(state.anchorInstant, state.resolvedThrough, timeZone)
            ?.takeIf { rule.endDate == null || it <= rule.endDate }

    private fun overdueCount(rule: CareRule, state: RuleAnchorState, now: Instant, timeZone: TimeZone): Int {
        val through = rule.endDate?.let { minOf(now, it) } ?: now
        return rule.recurrence.countOccurrencesIn(state.anchorInstant, state.resolvedThrough, through, timeZone)
    }

    private fun buildOccurrence(
        rule: CareRule,
        ruleId: CareRuleId,
        at: Instant,
        status: OccurrenceStatus,
        lastCareAt: Instant?,
        collapsedCount: Int
    ) = CareOccurrence(
        careRuleId = ruleId,
        plantId = rule.plantId,
        type = rule.type,
        scheduledAt = at,
        details = rule.details,
        status = status,
        lastCareAt = lastCareAt,
        collapsedCount = collapsedCount
    )

    private data class RuleAnchorState(val anchorInstant: Instant, val resolvedThrough: Instant?)
}
