package org.mlanau.project.plant.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.mlanau.project.plant.domain.exceptions.InvalidCareRuleDateRangeException
import org.mlanau.project.plant.domain.exceptions.InvalidRecurrenceException

class CareRuleTest {

    private val plantId = PlantId(1)
    private val start = Instant.parse("2026-01-10T00:00:00Z")
    private val beforeStart = Instant.parse("2026-01-05T00:00:00Z")
    private val afterStart = Instant.parse("2026-01-20T00:00:00Z")
    private val waterDetails = CareDetails.Water(amountMl = 200)

    private fun waterRule(startDate: Instant = start, endDate: Instant? = null) = CareRule(
        plantId = plantId,
        recurrence = RecurrenceRule.Once,
        startDate = startDate,
        endDate = endDate,
        details = waterDetails
    )

    @Test
    fun `rejects an end date before the start date`() {
        assertFailsWith<InvalidCareRuleDateRangeException> {
            waterRule(endDate = beforeStart)
        }
    }

    @Test
    fun `accepts an end date on or after the start date`() {
        waterRule(endDate = afterStart)
        waterRule(endDate = start)
    }

    @Test
    fun `accepts a null end date`() {
        waterRule(endDate = null)
    }

    @Test
    fun `validates the date range regardless of care details`() {
        assertFailsWith<InvalidCareRuleDateRangeException> {
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = start,
                endDate = beforeStart,
                details = CareDetails.Fertilize(fertilizerName = "Compost")
            )
        }
        assertFailsWith<InvalidCareRuleDateRangeException> {
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = start,
                endDate = beforeStart,
                details = CareDetails.Repot(newPotSize = PotSize.MEDIUM)
            )
        }
    }

    @Test
    fun `rejects a non-positive recurrence interval`() {
        assertFailsWith<InvalidRecurrenceException> {
            RecurrenceRule.Periodic(everyDays = 0)
        }
        assertFailsWith<InvalidRecurrenceException> {
            RecurrenceRule.Periodic(everyDays = -1)
        }
    }

    @Test
    fun `dismissedThrough advances to the given instant when nothing was dismissed yet`() {
        val rule = waterRule()
        assertEquals(afterStart, rule.dismissedThrough(afterStart).dismissedBefore)
    }

    @Test
    fun `dismissedThrough is monotonic — dismissing an older occurrence after a newer one does not move it back`() {
        val rule = waterRule().dismissedThrough(afterStart)
        val stillAfterStart = rule.dismissedThrough(start)
        assertEquals(afterStart, stillAfterStart.dismissedBefore)
    }

    @Test
    fun `dismissedThrough still advances past the current mark when moving forward`() {
        val rule = waterRule().dismissedThrough(start)
        val later = Instant.parse("2026-02-01T00:00:00Z")
        assertEquals(later, rule.dismissedThrough(later).dismissedBefore)
    }

    @Test
    fun `effectiveAnchor falls back to startDate when nothing has been logged yet`() {
        val rule = waterRule()
        assertEquals(start, rule.effectiveAnchor(lastCareAt = null, timeZone = TimeZone.UTC))
    }

    @Test
    fun `effectiveAnchor uses the logged care's date but the rule's own time of day`() {
        val ruleWithNotificationTime = waterRule().copy(notificationTime = kotlinx.datetime.LocalTime(9, 0))
        // Logged late at night — the reminder time must not drift to match it.
        val loggedLate = Instant.parse("2026-01-15T23:50:00Z")
        val anchor = ruleWithNotificationTime.effectiveAnchor(lastCareAt = loggedLate, timeZone = TimeZone.UTC)
        assertEquals(Instant.parse("2026-01-15T09:00:00Z"), anchor)
    }

    @Test
    fun `effectiveAnchor falls back to startDate's time of day when the rule has no explicit notification time`() {
        // startDate itself carries 00:00:00Z here.
        val rule = waterRule()
        val loggedLate = Instant.parse("2026-01-15T23:50:00Z")
        val anchor = rule.effectiveAnchor(lastCareAt = loggedLate, timeZone = TimeZone.UTC)
        assertEquals(Instant.parse("2026-01-15T00:00:00Z"), anchor)
    }
}
