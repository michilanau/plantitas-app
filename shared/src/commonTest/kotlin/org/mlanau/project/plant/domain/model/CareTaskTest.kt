package org.mlanau.project.plant.domain.model

import kotlin.test.Test
import org.mlanau.project.plant.domain.exception.TaskNotDueYetException
import kotlinx.datetime.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exception.BlankFertilizerNameException
import org.mlanau.project.plant.domain.exception.CorruptedRecordException
import org.mlanau.project.plant.domain.exception.FutureCareTaskException
import org.mlanau.project.plant.domain.exception.InvalidMissedCountException
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException

class CareTaskTest {

    private val tz = TimeZone.UTC

    private val plantId = PlantId(1)
    private val ruleId = CareRuleId(1)
    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val yesterday = Instant.parse("2026-01-14T12:00:00Z")
    private val tomorrow = Instant.parse("2026-01-16T12:00:00Z")
    private val waterDetails = CareDetails.Water.create(amountMl = 200)

    @Test
    fun `Done create rejects a performedAt after now`() {
        assertFailsWith<FutureCareTaskException> {
            CareTask.Done.create(
                plantId = plantId,
                careRuleId = ruleId,
                care = waterDetails,
                performedAt = tomorrow,
                now = now
            )
        }
    }

    @Test
    fun `Done create accepts performedAt equal to now`() {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = ruleId,
            care = waterDetails,
            performedAt = now,
            now = now
        )
        assertEquals(now, done.performedAt)
    }

    @Test
    fun `Done create accepts a performedAt in the past`() {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = ruleId,
            care = waterDetails,
            performedAt = yesterday,
            now = now
        )
        assertEquals(yesterday, done.performedAt)
    }

    @Test
    fun `Done create accepts a null careRuleId for an ad hoc task with no rule behind it`() {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = null,
            care = waterDetails,
            performedAt = yesterday,
            now = now
        )
        assertEquals(null, done.careRuleId)
        assertEquals(null, done.scheduledAt)
    }

    @Test
    fun `type always matches the care`() {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = ruleId,
            care = CareDetails.Fertilize.create(fertilizerName = "Compost"),
            performedAt = yesterday,
            now = now
        )
        assertEquals(CareType.FERTILIZE, done.type)
    }

    @Test
    fun `Done create normalizes a blank note to null`() {
        val done = CareTask.Done.create(
            plantId = plantId,
            careRuleId = ruleId,
            care = waterDetails,
            performedAt = yesterday,
            now = now,
            note = "   "
        )
        assertEquals(null, done.note)
    }

    @Test
    fun `Pending complete produces a Done at the performed instant`() {
        val dueAt = Instant.parse("2026-01-14T09:00:00Z")
        val pending = pending(dueAt = dueAt, status = PendingStatus.OVERDUE)
        val done = pending.complete(performedAt = yesterday, now = now, timeZone = tz)
        assertEquals(plantId, done.plantId)
        assertEquals(ruleId, done.careRuleId)
        assertEquals(dueAt, done.scheduledAt)
        assertEquals(yesterday, done.performedAt)
    }

    @Test
    fun `a task due later today can still be completed`() {
        // Completability is a question about the day, not about the reminder's clock time.
        val laterToday = now.plus(6, DateTimeUnit.HOUR, tz)
        val pending = pending(dueAt = laterToday, status = PendingStatus.SCHEDULED)
        assertTrue(pending.isCompletableOn(now, tz))
        pending.complete(performedAt = now, now = now, timeZone = tz)
    }

    @Test
    fun `a task due on a later day cannot be completed`() {
        val tomorrow = now.plus(1, DateTimeUnit.DAY, tz)
        val pending = pending(dueAt = tomorrow, status = PendingStatus.SCHEDULED)
        assertFalse(pending.isCompletableOn(now, tz))
        assertFailsWith<TaskNotDueYetException> {
            pending.complete(performedAt = now, now = now, timeZone = tz)
        }
    }

    @Test
    fun `Pending rejects a missedCount below one`() {
        assertFailsWith<InvalidMissedCountException> {
            pending(dueAt = now, status = PendingStatus.SCHEDULED, missedCount = 0)
        }
    }

    private fun pending(
        dueAt: Instant,
        status: PendingStatus,
        missedCount: Int = 1
    ) = CareTask.Pending(
        careRuleId = ruleId,
        plantId = plantId,
        care = waterDetails,
        dueAt = dueAt,
        at = if (status == PendingStatus.OVERDUE) now else dueAt,
        status = status,
        lastPerformedAt = null,
        missedCount = missedCount
    )

    @Test
    fun `Water rejects a non-positive amountMl`() {
        assertFailsWith<NonPositiveAmountException> {
            CareDetails.Water.create(amountMl = 0)
        }
    }

    @Test
    fun `Fertilize rejects a blank fertilizerName`() {
        assertFailsWith<BlankFertilizerNameException> {
            CareDetails.Fertilize.create(fertilizerName = "   ")
        }
    }

    @Test
    fun `Fertilize rejects a non-positive doseMl`() {
        assertFailsWith<NonPositiveAmountException> {
            CareDetails.Fertilize.create(fertilizerName = "Compost", doseMl = -1)
        }
    }

    @Test
    fun `Fertilize restore rejects a null fertilizerName as a corrupted record instead of crashing`() {
        assertFailsWith<CorruptedRecordException> {
            CareDetails.Fertilize.restore(recordId = 42, fertilizerName = null, doseMl = null, dilutionRatio = null)
        }
    }

    @Test
    fun `Repot restore rejects a missing newPotSize as a corrupted record instead of crashing`() {
        assertFailsWith<CorruptedRecordException> {
            CareDetails.Repot.restore(recordId = 42, newPotSize = null, substrateType = null)
        }
    }
}
