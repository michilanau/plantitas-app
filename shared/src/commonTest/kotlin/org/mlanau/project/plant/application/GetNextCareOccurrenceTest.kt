package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.domain.service.OccurrenceStatus

class GetNextCareOccurrenceTest {

    private val plantId = PlantId(1)
    private val fixedScheduler = CareOccurrenceScheduler(timeZoneProvider = { TimeZone.UTC })
    private val now = Instant.parse("2026-01-10T09:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun `returns null when the plant has no care rules`() = runBlocking {
        val repository = InMemoryCareRepository()
        val getNextCareOccurrence = GetNextCareOccurrence(repository, fixedScheduler, fixedClock)

        assertNull(getNextCareOccurrence(plantId).first())
    }

    @Test
    fun `an overdue occurrence wins over a chronologically earlier scheduled one`() {
        runBlocking {
        val repository = InMemoryCareRepository()
        val getNextCareOccurrence = GetNextCareOccurrence(repository, fixedScheduler, fixedClock)

        // Fertilizing is scheduled for tomorrow (earlier in absolute time)...
        repository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = Instant.parse("2026-01-11T09:00:00Z"),
                details = CareDetails.Fertilize(fertilizerName = "Compost")
            )
        )
        // ...but watering has been overdue since a week ago.
        repository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Periodic(everyDays = 3),
                startDate = Instant.parse("2026-01-03T09:00:00Z"),
                details = CareDetails.Water(amountMl = 200)
            )
        )

        val next = getNextCareOccurrence(plantId).first()

        assertEquals(OccurrenceStatus.OVERDUE, next?.status)
        assertIs<CareDetails.Water>(next?.details)
        }
    }

    @Test
    fun `with nothing overdue, the chronologically earliest scheduled occurrence wins`() = runBlocking {
        val repository = InMemoryCareRepository()
        val getNextCareOccurrence = GetNextCareOccurrence(repository, fixedScheduler, fixedClock)

        repository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = Instant.parse("2026-01-20T09:00:00Z"),
                details = CareDetails.Fertilize(fertilizerName = "Compost")
            )
        )
        repository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = Instant.parse("2026-01-12T09:00:00Z"),
                details = CareDetails.Water(amountMl = 200)
            )
        )

        val next = getNextCareOccurrence(plantId).first()

        assertEquals(Instant.parse("2026-01-12T09:00:00Z"), next?.scheduledAt)
    }
}
