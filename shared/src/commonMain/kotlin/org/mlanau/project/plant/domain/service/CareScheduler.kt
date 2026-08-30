package org.mlanau.project.plant.domain.service

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareTask
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PendingStatus
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.shared.time.SystemTimeZoneProvider
import org.mlanau.project.shared.time.TimeZoneProvider

/**
 * Turns rules plus care history into the tasks a plant currently owes.
 *
 * Two invariants shape everything here:
 *
 * - **A rule owes at most one unfinished task at a time.** Missed occurrences never accumulate as
 *   separate tasks; they collapse into a single overdue one whose [CareTask.Pending.missedCount]
 *   says how many slots it stands for. You water a plant once, however long you forgot it.
 * - **An overdue task shows up today, not on the day it was missed.** Its
 *   [CareTask.Pending.dueAt] still points at the oldest missed slot (that's what "N days late"
 *   counts from), but its [CareTask.Pending.at] follows the present, so it cannot fall off the
 *   end of the calendar month the user happens to be looking at.
 */
class CareScheduler(
    private val timeZoneProvider: TimeZoneProvider = SystemTimeZoneProvider
) {

    fun pendingIn(
        rules: List<CareRule>,
        lastCareDates: Map<PlantId, Map<CareType, Instant>>,
        from: Instant,
        until: Instant,
        now: Instant,
        timeZone: TimeZone = timeZoneProvider()
    ): List<CareTask.Pending> {
        return rules
            .flatMap { rule -> pendingForRuleInWindow(rule, rule.plantId?.let { lastCareDates[it] }?.get(rule.type), from, until, now, timeZone) }
            .sortedBy { it.at }
    }

    fun nextPending(
        rule: CareRule,
        lastPerformedAt: Instant?,
        now: Instant,
        timeZone: TimeZone = timeZoneProvider(),
        lookaheadDays: Int = 365
    ): CareTask.Pending? {
        if (!rule.active) return null
        val ruleId = rule.id ?: return null
        val plantId = rule.plantId ?: return null
        val state = anchorStateFor(rule, lastPerformedAt, timeZone)
        val candidate = rule.firstOccurrenceAfter(state.anchor, state.resolvedThrough, timeZone)

        return if (candidate <= now) {
            overdue(rule, ruleId, plantId, candidate, now, state, lastPerformedAt, timeZone)
        } else {
            if (candidate > now.plus(lookaheadDays, DateTimeUnit.DAY, timeZone)) null
            else scheduled(rule, ruleId, plantId, candidate, lastPerformedAt)
        }
    }

    /**
     * When the platform alarm for [rule] should next fire — always strictly after [after], since
     * an alarm set in the past fires immediately and, once its receiver re-schedules "the next
     * one" the same way, loops forever.
     *
     * For a task that isn't due yet this is simply its slot. For an overdue one it is the next
     * daily nag at the rule's own reminder time: an overdue task keeps asking once a day, in a
     * single notification per rule, until it's done.
     */
    fun nextReminderAt(
        rule: CareRule,
        lastPerformedAt: Instant?,
        after: Instant,
        timeZone: TimeZone = timeZoneProvider()
    ): Instant? {
        if (!rule.active) return null
        val state = anchorStateFor(rule, lastPerformedAt, timeZone)
        val candidate = rule.firstOccurrenceAfter(state.anchor, state.resolvedThrough, timeZone)
        return if (candidate > after) candidate else rule.reminderSlotAfter(after, timeZone)
    }

    private fun pendingForRuleInWindow(
        rule: CareRule,
        lastPerformedAt: Instant?,
        from: Instant,
        until: Instant,
        now: Instant,
        timeZone: TimeZone
    ): List<CareTask.Pending> {
        if (!rule.active) return emptyList()
        val ruleId = rule.id ?: return emptyList()
        val plantId = rule.plantId ?: return emptyList()
        val state = anchorStateFor(rule, lastPerformedAt, timeZone)
        val candidate = rule.firstOccurrenceAfter(state.anchor, state.resolvedThrough, timeZone)

        if (candidate > now) {
            return projectFrom(rule, ruleId, plantId, state.anchor, candidate, from, until, lastPerformedAt, timeZone)
        }

        // The overdue task is placed on `now`, so it enters any window containing today rather
        // than the (possibly long past) window its missed slot fell in.
        val overdue = if (now in from..until) {
            listOf(overdue(rule, ruleId, plantId, candidate, now, state, lastPerformedAt, timeZone))
        } else {
            emptyList()
        }

        // The floor for the future grid is `now`, not `resolvedThrough`: the overdue task above
        // already stands for everything up to now, so re-deriving from `resolvedThrough` here
        // would repeat slots it has already absorbed.
        val futureFloor = rule.firstOccurrenceAfter(state.anchor, now, timeZone)
        return overdue + projectFrom(rule, ruleId, plantId, state.anchor, futureFloor, from, until, lastPerformedAt, timeZone)
    }

    private fun projectFrom(
        rule: CareRule,
        ruleId: CareRuleId,
        plantId: PlantId,
        anchor: Instant,
        firstCandidate: Instant,
        from: Instant,
        until: Instant,
        lastPerformedAt: Instant?,
        timeZone: TimeZone
    ): List<CareTask.Pending> {
        val effectiveFrom = maxOf(from, firstCandidate)
        if (effectiveFrom > until) return emptyList()
        return rule.occurrencesBetween(anchor, effectiveFrom, until, timeZone)
            .map { scheduled(rule, ruleId, plantId, it, lastPerformedAt) }
    }

    /**
     * The anchor the occurrence grid is generated from, plus the instant everything up to which
     * is already answered for.
     *
     * A care only resolves slots if it happened at or after the rule's own [CareRule.startDate];
     * an older one belongs to a schedule this rule doesn't describe, so it leaves `startDate`
     * standing as a real, due occurrence. Both the raw care instant and its normalized anchor
     * slot are excluded: without the raw one, watering late at night — after the rule's reminder
     * time — would leave that same day's slot looking un-addressed and immediately overdue.
     */
    private fun anchorStateFor(rule: CareRule, lastPerformedAt: Instant?, timeZone: TimeZone): RuleAnchorState {
        val anchor = rule.anchorFor(lastPerformedAt, timeZone)
        val resolvedThrough = lastPerformedAt
            ?.takeIf { it >= rule.startDate }
            ?.let { maxOf(it, anchor) }
        return RuleAnchorState(anchor, resolvedThrough)
    }

    private fun overdue(
        rule: CareRule,
        ruleId: CareRuleId,
        plantId: PlantId,
        dueAt: Instant,
        now: Instant,
        state: RuleAnchorState,
        lastPerformedAt: Instant?,
        timeZone: TimeZone
    ) = CareTask.Pending(
        careRuleId = ruleId,
        plantId = plantId,
        care = rule.details,
        dueAt = dueAt,
        at = now,
        status = PendingStatus.OVERDUE,
        lastPerformedAt = lastPerformedAt,
        missedCount = rule.countOccurrencesIn(state.anchor, state.resolvedThrough, now, timeZone)
    )

    private fun scheduled(
        rule: CareRule,
        ruleId: CareRuleId,
        plantId: PlantId,
        dueAt: Instant,
        lastPerformedAt: Instant?
    ) = CareTask.Pending(
        careRuleId = ruleId,
        plantId = plantId,
        care = rule.details,
        dueAt = dueAt,
        at = dueAt,
        status = PendingStatus.SCHEDULED,
        lastPerformedAt = lastPerformedAt,
        missedCount = 1
    )

    private data class RuleAnchorState(val anchor: Instant, val resolvedThrough: Instant?)
}
