package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.mlanau.project.plant.application.fixtures.FakeCareNotificationScheduler
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.application.fixtures.InMemoryPlantRepository
import org.mlanau.project.plant.domain.exceptions.FutureCareLogException
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.domain.service.OccurrenceStatus

class LogCareTest {

    private val plantId = PlantId(1)
    private val ruleId = CareRuleId(1)
    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }

    private fun logCare(repository: InMemoryCareRepository): LogCare {
        val rescheduleCareReminder = RescheduleCareReminder(
            repository, InMemoryPlantRepository(), CareOccurrenceScheduler(), FakeCareNotificationScheduler(), fixedClock
        )
        return LogCare(repository, rescheduleCareReminder, fixedClock)
    }

    @Test
    fun `rejects logging a care in the future`() {
        runBlocking {
            val repository = InMemoryCareRepository()

            val result = logCare(repository)(
                plantId = plantId,
                careRuleId = ruleId,
                details = CareDetails.Water(amountMl = 200),
                performedAt = Instant.parse("2026-01-16T00:00:00Z")
            )

            assertIs<FutureCareLogException>(result.exceptionOrNull())
        }
    }

    @Test
    fun `logs an ad hoc care with no rule and no scheduled occurrence behind it`() = runBlocking {
        val repository = InMemoryCareRepository()

        val yesterday = Instant.parse("2026-01-14T09:00:00Z")
        val result = logCare(repository)(
            plantId = plantId,
            careRuleId = null,
            details = CareDetails.Water(amountMl = 150),
            performedAt = yesterday
        )

        val saved = repository.persistedLogs().single()
        assertEquals(result.getOrNull(), saved.id)
        assertNull(saved.careRuleId)
        assertNull(saved.scheduledAt)
        assertEquals(yesterday, saved.performedAt)
    }

    @Test
    fun `logging from an occurrence carries its rule, details and scheduled slot`() = runBlocking {
        val repository = InMemoryCareRepository()

        val occurrence = CareOccurrence(
            careRuleId = ruleId,
            plantId = plantId,
            type = CareType.WATER,
            scheduledAt = Instant.parse("2026-01-15T09:00:00Z"),
            details = CareDetails.Water(amountMl = 250),
            status = OccurrenceStatus.SCHEDULED,
            lastCareAt = null
        )

        logCare(repository)(occurrence, performedAt = now)

        val saved = repository.persistedLogs().single()
        assertEquals(ruleId, saved.careRuleId)
        assertEquals(occurrence.scheduledAt, saved.scheduledAt)
        assertEquals(occurrence.details, saved.details)
    }
}
