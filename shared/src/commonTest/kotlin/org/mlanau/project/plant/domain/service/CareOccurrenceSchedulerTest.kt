package org.mlanau.project.plant.domain.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.plant.domain.model.type

class CareOccurrenceSchedulerTest {

    private val tz = TimeZone.UTC
    private val plantId = PlantId(1)
    private val otherPlantId = PlantId(2)
    private val ruleId = CareRuleId(1)

    // Every 3 days from Jan 1st, 09:00 — chosen so the by-hand arithmetic in these tests (days
    // between occurrences, collapsed counts) stays easy to verify.
    private val start = Instant.parse("2026-01-01T09:00:00Z")
    private val waterDetails = CareDetails.Water(amountMl = 200)

    private fun periodicRule(
        startDate: Instant = start,
        everyDays: Int = 3,
        endDate: Instant? = null,
        active: Boolean = true,
        dismissedBefore: Instant? = null,
        notificationTime: LocalTime? = null
    ) = CareRule(
        id = ruleId,
        plantId = plantId,
        recurrence = RecurrenceRule.Periodic(everyDays),
        startDate = startDate,
        endDate = endDate,
        active = active,
        dismissedBefore = dismissedBefore,
        notificationTime = notificationTime,
        details = waterDetails
    )

    private fun onceRule(
        startDate: Instant = start,
        dismissedBefore: Instant? = null
    ) = CareRule(
        id = ruleId,
        plantId = plantId,
        recurrence = RecurrenceRule.Once,
        startDate = startDate,
        dismissedBefore = dismissedBefore,
        details = waterDetails
    )

    // Fixed to UTC — the default TimeZoneProvider reads the system zone, which would make these
    // date-boundary-sensitive tests depend on wherever they happen to run.
    private val scheduler = CareOccurrenceScheduler(timeZoneProvider = { tz })

    // --- Derived anchor

    @Test
    fun `with no logs, the first occurrence is the rule's own startDate`() {
        val rule = periodicRule()
        val next = scheduler.nextOccurrence(rule, lastCareAt = null, now = start.minus1Day())
        assertEquals(start, next?.scheduledAt)
        assertEquals(OccurrenceStatus.SCHEDULED, next?.status)
    }

    @Test
    fun `a log resets the rhythm from its own date instead of continuing the old startDate cadence`() {
        val rule = periodicRule() // Jan1, 4, 7, 10, 13... if never logged
        val loggedAt = Instant.parse("2026-01-11T14:00:00Z")
        val next = scheduler.nextOccurrence(rule, lastCareAt = loggedAt, now = Instant.parse("2026-01-12T00:00:00Z"))
        // Jan11 + 3 days, not the next multiple of the original Jan1 cadence (which would be Jan13).
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.scheduledAt)
    }

    @Test
    fun `a log from a different plant does not move this rule's anchor`() {
        val rule = periodicRule()
        val anchors = CareAnchors.from(
            listOf(logAt(Instant.parse("2026-01-02T09:00:00Z"), plant = otherPlantId))
        )
        val occurrences = scheduler.occurrences(
            listOf(rule), anchors, from = start.minus1Day(), until = start.plus1Day(), now = start.minus1Day()
        )
        assertEquals(start, occurrences.single().scheduledAt)
    }

    @Test
    fun `effectiveAnchor's time-of-day normalization carries through to the next occurrence`() {
        val rule = periodicRule(notificationTime = LocalTime(9, 0))
        // Watered late at night — the reminder must not drift to 23:50.
        val loggedLate = Instant.parse("2026-01-11T23:50:00Z")
        val next = scheduler.nextOccurrence(rule, lastCareAt = loggedLate, now = Instant.parse("2026-01-12T00:00:00Z"))
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.scheduledAt)
    }

    @Test
    fun `logging exactly at the anchor's normalized slot does not leave that same slot overdue`() {
        // Watered a bit before the normalized 09:00 slot, same day.
        val rule = periodicRule(notificationTime = LocalTime(9, 0))
        val loggedAt = Instant.parse("2026-01-11T08:00:00Z")
        val next = scheduler.nextOccurrence(rule, lastCareAt = loggedAt, now = Instant.parse("2026-01-11T10:00:00Z"))
        // Should already be the next cycle, not an overdue Jan11 09:00.
        assertEquals(OccurrenceStatus.SCHEDULED, next?.status)
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.scheduledAt)
    }

    // --- Overdue collapsing

    @Test
    fun `four missed waterings collapse into a single overdue occurrence at the oldest one`() {
        val rule = periodicRule() // Jan1, 4, 7, 10
        val now = Instant.parse("2026-01-10T09:00:00Z")
        val next = scheduler.nextOccurrence(rule, lastCareAt = null, now = now)
        assertEquals(OccurrenceStatus.OVERDUE, next?.status)
        assertEquals(start, next?.scheduledAt)
        assertEquals(4, next?.collapsedCount)
    }

    @Test
    fun `the collapsed occurrence's date never changes as more time passes, only its count grows`() {
        val rule = periodicRule()
        val atJan10 = scheduler.nextOccurrence(rule, lastCareAt = null, now = Instant.parse("2026-01-10T09:00:00Z"))
        val atJan13 = scheduler.nextOccurrence(rule, lastCareAt = null, now = Instant.parse("2026-01-13T09:00:00Z"))
        assertEquals(start, atJan10?.scheduledAt)
        assertEquals(start, atJan13?.scheduledAt)
        assertTrue((atJan13?.collapsedCount ?: 0) > (atJan10?.collapsedCount ?: 0))
        assertEquals(4, atJan10?.collapsedCount)
        assertEquals(5, atJan13?.collapsedCount)
    }

    @Test
    fun `while a rule has an overdue occurrence, nothing future is projected for it in a window`() {
        val rule = periodicRule()
        val anchors = CareAnchors.EMPTY
        val occurrences = scheduler.occurrences(
            listOf(rule), anchors,
            from = start, until = Instant.parse("2026-02-01T00:00:00Z"),
            now = Instant.parse("2026-01-10T09:00:00Z")
        )
        assertEquals(1, occurrences.size)
        assertEquals(OccurrenceStatus.OVERDUE, occurrences.single().status)
    }

    @Test
    fun `an overdue occurrence outside the requested window contributes nothing to that window`() {
        val rule = periodicRule()
        val occurrences = scheduler.occurrences(
            listOf(rule), CareAnchors.EMPTY,
            from = Instant.parse("2026-02-01T00:00:00Z"),
            until = Instant.parse("2026-03-01T00:00:00Z"),
            now = Instant.parse("2026-01-10T09:00:00Z")
        )
        assertEquals(emptyList(), occurrences)
    }

    // --- Dismissal

    @Test
    fun `dismissing a collapsed overdue occurrence clears the whole range in one action`() {
        val overdueRule = periodicRule() // Jan1..10 collapsed, count 4, at now = Jan10 09:00
        val now = Instant.parse("2026-01-10T09:00:00Z")
        // DismissCareOccurrence would persist maxOf(now, occurrence.scheduledAt) as dismissedBefore.
        val dismissed = overdueRule.copy(dismissedBefore = now)
        val next = scheduler.nextOccurrence(dismissed, lastCareAt = null, now = now)
        // Not Jan4 or Jan7 resurfacing — straight to the next future slot.
        assertEquals(OccurrenceStatus.SCHEDULED, next?.status)
        assertEquals(Instant.parse("2026-01-13T09:00:00Z"), next?.scheduledAt)
    }

    @Test
    fun `dismissing does not move the anchor — the rhythm is unaffected once care is logged again`() {
        val now = Instant.parse("2026-01-10T09:00:00Z")
        val dismissed = periodicRule(dismissedBefore = now)
        val loggedAt = Instant.parse("2026-01-20T09:00:00Z")
        val next = scheduler.nextOccurrence(dismissed, lastCareAt = loggedAt, now = loggedAt)
        assertEquals(Instant.parse("2026-01-23T09:00:00Z"), next?.scheduledAt)
    }

    // --- Bounds

    @Test
    fun `an inactive rule never produces an occurrence, overdue or scheduled`() {
        val rule = periodicRule(active = false)
        assertNull(scheduler.nextOccurrence(rule, lastCareAt = null, now = Instant.parse("2026-02-01T00:00:00Z")))
        assertEquals(
            emptyList(),
            scheduler.occurrences(
                listOf(rule), CareAnchors.EMPTY,
                from = start, until = Instant.parse("2026-02-01T00:00:00Z"), now = start
            )
        )
    }

    @Test
    fun `endDate caps the collapsed overdue count`() {
        val rule = periodicRule(endDate = Instant.parse("2026-01-05T09:00:00Z")) // only Jan1, Jan4 qualify
        val next = scheduler.nextOccurrence(rule, lastCareAt = null, now = Instant.parse("2026-02-01T00:00:00Z"))
        assertEquals(OccurrenceStatus.OVERDUE, next?.status)
        assertEquals(start, next?.scheduledAt)
        assertEquals(2, next?.collapsedCount)
    }

    @Test
    fun `a rule exhausted by its own endDate before the resolved floor produces nothing`() {
        val now = Instant.parse("2026-01-20T00:00:00Z")
        val dismissed = periodicRule(
            endDate = Instant.parse("2026-01-05T00:00:00Z"),
            dismissedBefore = Instant.parse("2026-01-10T00:00:00Z")
        )
        assertNull(scheduler.nextOccurrence(dismissed, lastCareAt = null, now = now))
    }

    @Test
    fun `Once keeps producing its single occurrence, overdue, until it's logged`() {
        val rule = onceRule()
        val next = scheduler.nextOccurrence(rule, lastCareAt = null, now = Instant.parse("2026-01-05T00:00:00Z"))
        assertEquals(OccurrenceStatus.OVERDUE, next?.status)
        assertEquals(start, next?.scheduledAt)
        assertEquals(1, next?.collapsedCount)
    }

    @Test
    fun `Once never re-anchors — once logged, it never produces another occurrence`() {
        val rule = onceRule()
        val loggedAt = Instant.parse("2026-01-05T10:00:00Z")
        assertNull(scheduler.nextOccurrence(rule, lastCareAt = loggedAt, now = Instant.parse("2026-02-01T00:00:00Z")))
    }

    // --- The anti-alarm-loop guard

    @Test
    fun `nextReminderAt is always strictly later than the given instant, even with an overdue occurrence pending`() {
        val rule = periodicRule()
        val now = Instant.parse("2026-01-10T09:00:00Z")
        // nextOccurrence would return the Jan1 overdue slot here...
        val overdue = scheduler.nextOccurrence(rule, lastCareAt = null, now = now)
        assertEquals(OccurrenceStatus.OVERDUE, overdue?.status)
        // ...but nextReminderAt must never hand back an instant that isn't strictly after `now`.
        val reminder = scheduler.nextReminderAt(rule, lastCareAt = null, after = now)
        assertEquals(Instant.parse("2026-01-13T09:00:00Z"), reminder)
        assertTrue(reminder!! > now)
    }

    @Test
    fun `nextReminderAt respects endDate and returns null once exhausted`() {
        val rule = periodicRule(endDate = Instant.parse("2026-01-05T09:00:00Z"))
        val reminder = scheduler.nextReminderAt(rule, lastCareAt = null, after = Instant.parse("2026-01-04T09:00:00Z"))
        assertNull(reminder)
    }

    // --- Window projection (multiple future occurrences, no overdue)

    @Test
    fun `occurrences projects every future slot within the window when nothing is overdue`() {
        val rule = periodicRule()
        val occurrences = scheduler.occurrences(
            listOf(rule), CareAnchors.EMPTY,
            from = start, until = Instant.parse("2026-01-10T09:00:00Z"),
            now = start.minus1Day()
        )
        assertEquals(
            listOf(
                start,
                Instant.parse("2026-01-04T09:00:00Z"),
                Instant.parse("2026-01-07T09:00:00Z"),
                Instant.parse("2026-01-10T09:00:00Z")
            ),
            occurrences.map { it.scheduledAt }
        )
    }

    @Test
    fun `Once produces nothing outside its own instant`() {
        val rule = onceRule()
        val occurrences = scheduler.occurrences(
            listOf(rule), CareAnchors.EMPTY,
            from = Instant.parse("2026-01-02T00:00:00Z"),
            until = Instant.parse("2026-01-10T00:00:00Z"),
            now = start.minus1Day()
        )
        assertEquals(emptyList(), occurrences)
    }

    // --- CareAnchors

    @Test
    fun `CareAnchors from logs takes the max performedAt per (plant, type)`() {
        val logs = listOf(
            logAt(Instant.parse("2026-01-01T09:00:00Z")),
            logAt(Instant.parse("2026-01-05T09:00:00Z")),
            logAt(Instant.parse("2026-01-03T09:00:00Z"))
        )
        val anchors = CareAnchors.from(logs)
        assertEquals(Instant.parse("2026-01-05T09:00:00Z"), anchors[plantId, waterDetails.type])
    }

    @Test
    fun `CareAnchors from logs keeps different plants and types apart`() {
        val logs = listOf(
            logAt(Instant.parse("2026-01-01T09:00:00Z"), plant = plantId, details = waterDetails),
            logAt(Instant.parse("2026-01-05T09:00:00Z"), plant = plantId, details = CareDetails.Fertilize("Compost")),
            logAt(Instant.parse("2026-01-07T09:00:00Z"), plant = otherPlantId, details = waterDetails)
        )
        val anchors = CareAnchors.from(logs)
        assertEquals(Instant.parse("2026-01-01T09:00:00Z"), anchors[plantId, waterDetails.type])
        assertEquals(Instant.parse("2026-01-07T09:00:00Z"), anchors[otherPlantId, waterDetails.type])
        assertNull(anchors[otherPlantId, CareDetails.Fertilize("x").type])
    }

    private fun logAt(
        performedAt: Instant,
        plant: PlantId = plantId,
        details: CareDetails = waterDetails
    ) = CareLog(plantId = plant, careRuleId = ruleId, performedAt = performedAt, details = details)

    private fun Instant.minus1Day(): Instant = this.plus(-1, DateTimeUnit.DAY, tz)
    private fun Instant.plus1Day(): Instant = this.plus(1, DateTimeUnit.DAY, tz)
}
