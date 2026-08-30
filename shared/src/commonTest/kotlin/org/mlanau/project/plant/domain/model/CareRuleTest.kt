package org.mlanau.project.plant.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.mlanau.project.plant.domain.exception.InvalidRecurrenceException

class CareRuleTest {

    private val tz = TimeZone.UTC
    private val plantId = PlantId(1)
    private val start = Instant.parse("2026-01-10T09:00:00Z")
    private val waterDetails = CareDetails.Water.create(amountMl = 200)

    private fun waterRule(
        startDate: Instant = start,
        everyDays: Int = 7,
        notificationTime: LocalTime = LocalTime(9, 0)
    ) = CareRule.create(
        plantId = plantId,
        everyDays = everyDays,
        startDate = startDate,
        notificationTime = notificationTime,
        details = waterDetails
    )

    @Test
    fun `rejects a non-positive recurrence interval`() {
        assertFailsWith<InvalidRecurrenceException> { waterRule(everyDays = 0) }
        assertFailsWith<InvalidRecurrenceException> { waterRule(everyDays = -1) }
    }

    @Test
    fun `anchorFor falls back to startDate when nothing has been logged yet`() {
        assertEquals(start, waterRule().anchorFor(lastCareAt = null, timeZone = tz))
    }

    @Test
    fun `anchorFor uses the logged care's date but the rule's own time of day`() {
        // Logged late at night — the reminder time must not drift to match it.
        val loggedLate = Instant.parse("2026-01-15T23:50:00Z")
        assertEquals(
            Instant.parse("2026-01-15T09:00:00Z"),
            waterRule().anchorFor(lastCareAt = loggedLate, timeZone = tz)
        )
    }

    @Test
    fun `anchorFor ignores a care logged before the rule's own start date`() {
        // The care belongs to a schedule this rule doesn't describe — a rule that was deleted and
        // recreated, or an ad-hoc care from before it existed — so the chosen start date stands.
        val beforeTheRule = Instant.parse("2026-01-05T09:00:00Z")
        assertEquals(start, waterRule().anchorFor(lastCareAt = beforeTheRule, timeZone = tz))
    }

    @Test
    fun `firstOccurrenceAfter returns the anchor itself when there is nothing to exclude`() {
        assertEquals(start, waterRule().firstOccurrenceAfter(start, after = null, timeZone = tz))
    }

    @Test
    fun `firstOccurrenceAfter is always strictly later than the given floor`() {
        val rule = waterRule(everyDays = 3)
        assertEquals(
            Instant.parse("2026-01-13T09:00:00Z"),
            rule.firstOccurrenceAfter(start, after = start, timeZone = tz)
        )
    }

    @Test
    fun `occurrencesBetween skips whole periods to reach the window`() {
        val rule = waterRule(everyDays = 3)
        assertEquals(
            listOf("2026-01-16T09:00:00Z", "2026-01-19T09:00:00Z").map { Instant.parse(it) },
            rule.occurrencesBetween(
                anchor = start,
                from = Instant.parse("2026-01-14T00:00:00Z"),
                until = Instant.parse("2026-01-20T00:00:00Z"),
                timeZone = tz
            )
        )
    }

    @Test
    fun `countOccurrencesIn counts every slot in the backlog`() {
        val rule = waterRule(everyDays = 3) // Jan10, 13, 16, 19
        assertEquals(
            4,
            rule.countOccurrencesIn(
                anchor = start,
                after = null,
                through = Instant.parse("2026-01-19T09:00:00Z"),
                timeZone = tz
            )
        )
    }

    @Test
    fun `reminderSlotAfter picks today's reminder time when it hasn't passed yet`() {
        val rule = waterRule()
        assertEquals(
            Instant.parse("2026-02-01T09:00:00Z"),
            rule.reminderSlotAfter(Instant.parse("2026-02-01T07:00:00Z"), tz)
        )
    }

    @Test
    fun `reminderSlotAfter rolls to tomorrow once today's reminder time has passed`() {
        val rule = waterRule()
        assertEquals(
            Instant.parse("2026-02-02T09:00:00Z"),
            rule.reminderSlotAfter(Instant.parse("2026-02-01T09:00:00Z"), tz)
        )
    }

    @Test
    fun `pausing stops the rule without losing it`() {
        val paused = waterRule().paused()
        assertFalse(paused.active)
        assertEquals(start, paused.startDate)
    }

    @Test
    fun `resuming restarts the schedule from the moment it is resumed`() {
        // Otherwise every occurrence missed while paused would come back at once as a huge backlog.
        val resumedAt = Instant.parse("2026-03-01T18:00:00Z")
        val resumed = waterRule().paused().resumedAt(resumedAt)
        assertTrue(resumed.active)
        assertEquals(resumedAt, resumed.startDate)
    }

    @Test
    fun `create without a plantId leaves the rule unassigned`() {
        val draft = CareRule.create(everyDays = 7, startDate = start, details = waterDetails)
        assertEquals(null, draft.plantId)
    }

    @Test
    fun `assignedTo attaches an unassigned rule to the given plant`() {
        val draft = CareRule.create(everyDays = 7, startDate = start, details = waterDetails)
        assertEquals(plantId, draft.assignedTo(plantId).plantId)
    }

    @Test
    fun `restore falls back to the default reminder time when the stored one is missing or blank`() {
        listOf(null, "  ").forEach { stored ->
            val rule = CareRule.restore(
                id = CareRuleId(1),
                plantId = plantId,
                everyDays = 7,
                startDate = start,
                active = true,
                notificationTime = stored,
                notificationsEnabled = true,
                details = waterDetails
            )
            assertEquals(CareRule.DEFAULT_NOTIFICATION_TIME, rule.notificationTime)
        }
    }
}
