package org.mlanau.project.plant.domain.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.PendingStatus
import org.mlanau.project.plant.domain.model.PlantId

class CareSchedulerTest {

    private val tz = TimeZone.UTC
    private val plantId = PlantId(1)
    private val otherPlantId = PlantId(2)
    private val ruleId = CareRuleId(1)

    // Every 3 days from Jan 1st, 09:00 — chosen so the by-hand arithmetic in these tests (days
    // between tasks, missed counts) stays easy to verify.
    private val start = Instant.parse("2026-01-01T09:00:00Z")
    private val waterDetails = CareDetails.Water.create(amountMl = 200)

    private fun periodicRule(
        startDate: Instant = start,
        everyDays: Int = 3,
        notificationTime: LocalTime = LocalTime(9, 0)
    ) = CareRule.create(
        plantId = plantId,
        everyDays = everyDays,
        startDate = startDate,
        notificationTime = notificationTime,
        details = waterDetails
    ).withId(ruleId)

    // Fixed to UTC — the default TimeZoneProvider reads the system zone, which would make these
    // date-boundary-sensitive tests depend on wherever they happen to run.
    private val scheduler = CareScheduler(timeZoneProvider = { tz })

    @Test
    fun `with no completed tasks, the first pending task is the rule's own startDate`() {
        val rule = periodicRule()
        val next = scheduler.nextPending(rule, lastPerformedAt = null, now = start.minus1Day())
        assertEquals(start, next?.dueAt)
        assertEquals(PendingStatus.SCHEDULED, next?.status)
    }

    @Test
    fun `a completed task resets the rhythm from its own date instead of continuing the old startDate cadence`() {
        val rule = periodicRule() // Jan1, 4, 7, 10, 13... if never completed
        val performedAt = Instant.parse("2026-01-11T14:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = performedAt, now = Instant.parse("2026-01-12T00:00:00Z"))
        // Jan11 + 3 days, not the next multiple of the original Jan1 cadence (which would be Jan13).
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.dueAt)
    }

    @Test
    fun `a care logged before the rule started never overrides the chosen start date`() {
        // This is what makes a deleted-and-recreated rule work: the old rule's care history stays
        // in the plant's record, but a rule that says "starting today" starts today.
        val rule = periodicRule(startDate = Instant.parse("2026-02-01T09:00:00Z"), everyDays = 30)
        val oldCare = Instant.parse("2026-01-20T10:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = oldCare, now = Instant.parse("2026-02-01T08:00:00Z"))
        assertEquals(Instant.parse("2026-02-01T09:00:00Z"), next?.dueAt)
        assertEquals(PendingStatus.SCHEDULED, next?.status)
    }

    @Test
    fun `a care logged after the rule started does re-anchor it`() {
        val rule = periodicRule(startDate = Instant.parse("2026-02-01T09:00:00Z"), everyDays = 30)
        val care = Instant.parse("2026-02-03T10:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = care, now = Instant.parse("2026-02-04T00:00:00Z"))
        assertEquals(Instant.parse("2026-03-05T09:00:00Z"), next?.dueAt)
    }

    @Test
    fun `a completed task from a different plant does not move this rule's anchor`() {
        val rule = periodicRule()
        val lastCareDates = mapOf(otherPlantId to mapOf(waterDetails.type to Instant.parse("2026-01-02T09:00:00Z")))
        val tasks = scheduler.pendingIn(
            listOf(rule), lastCareDates, from = start.minus1Day(), until = start.plus1Day(), now = start.minus1Day()
        )
        assertEquals(start, tasks.single().dueAt)
    }

    @Test
    fun `anchorFor's time-of-day normalization carries through to the next pending task`() {
        val rule = periodicRule()
        // Watered late at night — the reminder must not drift to 23:50.
        val performedLate = Instant.parse("2026-01-11T23:50:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = performedLate, now = Instant.parse("2026-01-12T00:00:00Z"))
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.dueAt)
    }

    @Test
    fun `completing exactly at the anchor's normalized slot does not leave that same slot overdue`() {
        // Watered a bit before the normalized 09:00 slot, same day.
        val rule = periodicRule()
        val performedAt = Instant.parse("2026-01-11T08:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = performedAt, now = Instant.parse("2026-01-11T10:00:00Z"))
        // Should already be the next cycle, not an overdue Jan11 09:00.
        assertEquals(PendingStatus.SCHEDULED, next?.status)
        assertEquals(Instant.parse("2026-01-14T09:00:00Z"), next?.dueAt)
    }

    @Test
    fun `four missed waterings collapse into a single overdue task at the oldest one`() {
        val rule = periodicRule() // Jan1, 4, 7, 10
        val now = Instant.parse("2026-01-10T09:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = null, now = now)
        assertEquals(PendingStatus.OVERDUE, next?.status)
        assertEquals(start, next?.dueAt)
        assertEquals(4, next?.missedCount)
    }

    @Test
    fun `the collapsed task's due date never changes as more time passes, only its missedCount grows`() {
        val rule = periodicRule()
        val atJan10 = scheduler.nextPending(rule, lastPerformedAt = null, now = Instant.parse("2026-01-10T09:00:00Z"))
        val atJan13 = scheduler.nextPending(rule, lastPerformedAt = null, now = Instant.parse("2026-01-13T09:00:00Z"))
        assertEquals(start, atJan10?.dueAt)
        assertEquals(start, atJan13?.dueAt)
        assertEquals(4, atJan10?.missedCount)
        assertEquals(5, atJan13?.missedCount)
    }

    @Test
    fun `an overdue task is placed on today, not on the day it was missed`() {
        val rule = periodicRule()
        val now = Instant.parse("2026-01-10T09:00:00Z")
        val next = scheduler.nextPending(rule, lastPerformedAt = null, now = now)
        assertEquals(start, next?.dueAt)
        assertEquals(now, next?.at)
    }

    @Test
    fun `while a rule has an overdue task, the future grid is still projected within the window`() {
        val rule = periodicRule()
        val now = Instant.parse("2026-01-10T09:00:00Z")
        val tasks = scheduler.pendingIn(
            listOf(rule), emptyMap(),
            from = start, until = Instant.parse("2026-02-01T00:00:00Z"),
            now = now
        )
        assertEquals(
            listOf(now) + listOf(
                "2026-01-13T09:00:00Z", "2026-01-16T09:00:00Z", "2026-01-19T09:00:00Z",
                "2026-01-22T09:00:00Z", "2026-01-25T09:00:00Z", "2026-01-28T09:00:00Z", "2026-01-31T09:00:00Z"
            ).map { Instant.parse(it) },
            tasks.map { it.at }
        )
        assertEquals(PendingStatus.OVERDUE, tasks.first().status)
        assertEquals(4, tasks.first().missedCount)
        assertTrue(tasks.drop(1).all { it.status == PendingStatus.SCHEDULED })
    }

    @Test
    fun `an overdue task enters any window containing today, however long ago it was missed`() {
        // The whole point of pinning it to today: a plant forgotten in January must still show up
        // when the user is looking at February.
        val rule = periodicRule()
        val now = Instant.parse("2026-02-10T12:00:00Z")
        val tasks = scheduler.pendingIn(
            listOf(rule), emptyMap(),
            from = Instant.parse("2026-02-01T00:00:00Z"),
            until = Instant.parse("2026-03-01T00:00:00Z"),
            now = now
        )
        val overdue = tasks.single { it.status == PendingStatus.OVERDUE }
        assertEquals(now, overdue.at)
        assertEquals(start, overdue.dueAt)
    }

    @Test
    fun `an overdue task stays out of a window that does not contain today`() {
        val rule = periodicRule()
        val tasks = scheduler.pendingIn(
            listOf(rule), emptyMap(),
            from = Instant.parse("2026-03-01T00:00:00Z"),
            until = Instant.parse("2026-04-01T00:00:00Z"),
            now = Instant.parse("2026-02-10T12:00:00Z")
        )
        assertTrue(tasks.none { it.status == PendingStatus.OVERDUE })
        assertTrue(tasks.isNotEmpty())
    }

    @Test
    fun `a rule not yet assigned to a plant never produces a pending task, overdue or scheduled`() {
        val rule = CareRule.create(everyDays = 3, startDate = start, details = waterDetails).withId(ruleId)
        assertNull(scheduler.nextPending(rule, lastPerformedAt = null, now = Instant.parse("2026-02-01T00:00:00Z")))
        assertEquals(
            emptyList(),
            scheduler.pendingIn(
                listOf(rule), emptyMap(),
                from = start, until = Instant.parse("2026-02-01T00:00:00Z"), now = start
            )
        )
    }

    @Test
    fun `nextReminderAt is always strictly later than the given instant, even with an overdue task pending`() {
        val rule = periodicRule()
        val now = Instant.parse("2026-01-10T12:00:00Z")
        val overdue = scheduler.nextPending(rule, lastPerformedAt = null, now = now)
        assertEquals(PendingStatus.OVERDUE, overdue?.status)
        // An overdue task nags again at the rule's own reminder time on the following day.
        val reminder = scheduler.nextReminderAt(rule, lastPerformedAt = null, after = now)
        assertEquals(Instant.parse("2026-01-11T09:00:00Z"), reminder)
        assertTrue(reminder!! > now)
    }

    @Test
    fun `an overdue task re-reminds once a day, not once per missed occurrence`() {
        val rule = periodicRule(everyDays = 30)
        val now = Instant.parse("2026-03-10T12:00:00Z")
        assertEquals(
            Instant.parse("2026-03-11T09:00:00Z"),
            scheduler.nextReminderAt(rule, lastPerformedAt = null, after = now)
        )
    }

    @Test
    fun `nextReminderAt uses the scheduled slot itself while the task is not due yet`() {
        val rule = periodicRule()
        assertEquals(
            start,
            scheduler.nextReminderAt(rule, lastPerformedAt = null, after = start.minus1Day())
        )
    }

    @Test
    fun `pendingIn projects every future slot within the window when nothing is overdue`() {
        val rule = periodicRule()
        val tasks = scheduler.pendingIn(
            listOf(rule), emptyMap(),
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
            tasks.map { it.dueAt }
        )
    }

    @Test
    fun `a reminder series opens on the due slot and then nags once a day`() {
        val rule = periodicRule() // Jan1, 4, 7... at 09:00
        val series = scheduler.reminderSeries(rule, lastPerformedAt = null, from = start.minus1Day(), count = 4)

        assertEquals(
            listOf(
                start,
                Instant.parse("2026-01-02T09:00:00Z"),
                Instant.parse("2026-01-03T09:00:00Z"),
                Instant.parse("2026-01-04T09:00:00Z")
            ),
            series.map { it.at }
        )
        assertEquals(PendingStatus.SCHEDULED, series.first().task.status)
        assertTrue(series.drop(1).all { it.task.status == PendingStatus.OVERDUE })
    }

    @Test
    fun `each reminder in the series carries the lateness it will have when it fires`() {
        val rule = periodicRule()
        val series = scheduler.reminderSeries(rule, lastPerformedAt = null, from = start.minus1Day(), count = 4)

        // dueAt stays pinned to the slot first owed, so the "N days late" each reminder renders
        // (dueAt until its own instant) grows 0, 1, 2, 3 across the run.
        assertTrue(series.drop(1).all { it.task.dueAt == start })
        assertEquals(listOf(0, 1, 2, 3), series.map { start.daysUntil(it.at, tz) })
    }

    @Test
    fun `a series for an already overdue rule starts with the daily nag, not the missed slot`() {
        val rule = periodicRule()
        val now = Instant.parse("2026-01-05T12:00:00Z") // Jan1 and Jan4 both missed
        val series = scheduler.reminderSeries(rule, lastPerformedAt = null, from = now, count = 2)

        assertEquals(
            listOf(Instant.parse("2026-01-06T09:00:00Z"), Instant.parse("2026-01-07T09:00:00Z")),
            series.map { it.at }
        )
        assertTrue(series.all { it.task.status == PendingStatus.OVERDUE })
        assertEquals(start, series.first().task.dueAt)
        assertEquals(2, series.first().task.missedCount)
    }

    @Test
    fun `a series never runs longer than asked, and asking for none yields none`() {
        val rule = periodicRule()
        assertEquals(1, scheduler.reminderSeries(rule, null, start.minus1Day(), count = 1).size)
        assertEquals(6, scheduler.reminderSeries(rule, null, start.minus1Day(), count = 6).size)
        assertTrue(scheduler.reminderSeries(rule, null, start.minus1Day(), count = 0).isEmpty())
    }

    @Test
    fun `a series is empty when the rule owes nothing at all`() {
        // Due beyond nextPending's lookahead, so there is no task to remind about.
        val rule = periodicRule(startDate = Instant.parse("2030-01-01T09:00:00Z"), everyDays = 3)
        assertTrue(scheduler.reminderSeries(rule, null, from = start, count = 4).isEmpty())
    }

    @Test
    fun `the head of a series is exactly what nextPending and nextReminderAt give on their own`() {
        // The platforms that chain reminders at fire time only ever register this first element,
        // so it has to stay identical to the single-reminder calculation.
        val rule = periodicRule()
        val cases = listOf(start.minus1Day(), Instant.parse("2026-01-05T12:00:00Z"), start)
        for (now in cases) {
            val head = scheduler.reminderSeries(rule, null, from = now, count = 3).first()
            assertEquals(scheduler.nextReminderAt(rule, null, now), head.at)
            assertEquals(scheduler.nextPending(rule, null, now), head.task)
        }
    }

    private fun Instant.minus1Day(): Instant = this.plus(-1, DateTimeUnit.DAY, tz)
    private fun Instant.plus1Day(): Instant = this.plus(1, DateTimeUnit.DAY, tz)
}
