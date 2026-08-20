package org.mlanau.project.plant.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant
import org.mlanau.project.plant.domain.exceptions.FutureCareLogException

class CareLogTest {

    private val plantId = PlantId(1)
    private val ruleId = CareRuleId(1)
    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val yesterday = Instant.parse("2026-01-14T12:00:00Z")
    private val tomorrow = Instant.parse("2026-01-16T12:00:00Z")
    private val waterDetails = CareDetails.Water(amountMl = 200)

    @Test
    fun `create rejects a performedAt after now`() {
        assertFailsWith<FutureCareLogException> {
            CareLog.create(
                plantId = plantId,
                careRuleId = ruleId,
                details = waterDetails,
                performedAt = tomorrow,
                now = now
            )
        }
    }

    @Test
    fun `create accepts performedAt equal to now`() {
        val log = CareLog.create(
            plantId = plantId,
            careRuleId = ruleId,
            details = waterDetails,
            performedAt = now,
            now = now
        )
        assertEquals(now, log.performedAt)
    }

    @Test
    fun `create accepts a performedAt in the past`() {
        val log = CareLog.create(
            plantId = plantId,
            careRuleId = ruleId,
            details = waterDetails,
            performedAt = yesterday,
            now = now
        )
        assertEquals(yesterday, log.performedAt)
    }

    @Test
    fun `create accepts a null careRuleId for an ad hoc log with no occurrence behind it`() {
        val log = CareLog.create(
            plantId = plantId,
            careRuleId = null,
            details = waterDetails,
            performedAt = yesterday,
            now = now
        )
        assertEquals(null, log.careRuleId)
        assertEquals(null, log.scheduledAt)
    }

    @Test
    fun `type always matches the details`() {
        val log = CareLog.create(
            plantId = plantId,
            careRuleId = ruleId,
            details = CareDetails.Fertilize(fertilizerName = "Compost"),
            performedAt = yesterday,
            now = now
        )
        assertEquals(CareType.FERTILIZE, log.type)
    }
}
