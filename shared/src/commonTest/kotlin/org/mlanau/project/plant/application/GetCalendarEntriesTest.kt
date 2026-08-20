package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.application.fixtures.InMemoryPlantRepository
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler

class GetCalendarEntriesTest {

    private val fixedScheduler = CareOccurrenceScheduler(timeZoneProvider = { TimeZone.UTC })
    private val now = Instant.parse("2026-01-10T00:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun `attaches the owning plant's name to each scheduled entry`() = runBlocking {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val getCalendarEntries = GetCalendarEntries(careRepository, plantRepository, fixedScheduler, fixedClock)

        val start = Instant.parse("2026-01-01T09:00:00Z")
        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = start))
        careRepository.saveCareRule(
            CareRule(plantId = plantId, recurrence = RecurrenceRule.Once, startDate = start, details = CareDetails.Water(amountMl = 100))
        )

        val entries = getCalendarEntries(
            from = Instant.parse("2026-01-01T00:00:00Z"),
            until = Instant.parse("2026-01-02T00:00:00Z")
        ).first()

        assertEquals(listOf("Monstera"), entries.map { it.plantName })
    }

    @Test
    fun `reports a null plant name when the owning plant can't be found`() = runBlocking {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val getCalendarEntries = GetCalendarEntries(careRepository, plantRepository, fixedScheduler, fixedClock)

        val start = Instant.parse("2026-01-01T09:00:00Z")
        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = start))
        careRepository.saveCareRule(
            CareRule(plantId = plantId, recurrence = RecurrenceRule.Once, startDate = start, details = CareDetails.Water(amountMl = 100))
        )
        plantRepository.delete(plantId)

        val entries = getCalendarEntries(
            from = Instant.parse("2026-01-01T00:00:00Z"),
            until = Instant.parse("2026-01-02T00:00:00Z")
        ).first()

        assertEquals(listOf(null), entries.map { it.plantName })
    }

    @Test
    fun `mixes scheduled occurrences and logged care, sorted by instant`() {
        runBlocking {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val getCalendarEntries = GetCalendarEntries(careRepository, plantRepository, fixedScheduler, fixedClock)

        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = now))
        // A future scheduled occurrence (no logs, no dismissal — start on Jan20 is untouched).
        careRepository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Once,
                startDate = Instant.parse("2026-01-20T09:00:00Z"),
                details = CareDetails.Water(amountMl = 100)
            )
        )
        // A care logged earlier in the window, from an unrelated rule.
        careRepository.seedLog(
            CareLog(
                plantId = plantId,
                careRuleId = CareRuleId(999),
                performedAt = Instant.parse("2026-01-05T09:00:00Z"),
                details = CareDetails.Fertilize(fertilizerName = "Compost")
            )
        )

        val entries = getCalendarEntries(
            from = Instant.parse("2026-01-01T00:00:00Z"),
            until = Instant.parse("2026-02-01T00:00:00Z")
        ).first()

        assertEquals(2, entries.size)
        assertEquals(
            listOf(Instant.parse("2026-01-05T09:00:00Z"), Instant.parse("2026-01-20T09:00:00Z")),
            entries.map { it.at }
        )
        assertIs<CalendarEntry.Logged>(entries[0])
        assertIs<CalendarEntry.Scheduled>(entries[1])
        }
    }

    @Test
    fun `a log whose rule was deleted still shows up`() = runBlocking {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val getCalendarEntries = GetCalendarEntries(careRepository, plantRepository, fixedScheduler, fixedClock)

        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = now))
        careRepository.seedLog(
            CareLog(
                plantId = plantId,
                careRuleId = null, // the rule that produced this was deleted
                performedAt = Instant.parse("2026-01-05T09:00:00Z"),
                details = CareDetails.Water(amountMl = 100)
            )
        )

        val entries = getCalendarEntries(
            from = Instant.parse("2026-01-01T00:00:00Z"),
            until = Instant.parse("2026-01-10T00:00:00Z")
        ).first()

        assertEquals(1, entries.size)
    }

    @Test
    fun `queries the anchor once regardless of how many rules and plants there are`() = runBlocking {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val getCalendarEntries = GetCalendarEntries(careRepository, plantRepository, fixedScheduler, fixedClock)

        repeat(3) { i ->
            val plantId = plantRepository.save(Plant.create(name = "Plant $i", description = null, createdAt = now))
            careRepository.saveCareRule(
                CareRule(
                    plantId = plantId,
                    recurrence = RecurrenceRule.Periodic(everyDays = 3),
                    startDate = Instant.parse("2026-01-01T09:00:00Z"),
                    details = CareDetails.Water(amountMl = 100)
                )
            )
        }

        getCalendarEntries(
            from = Instant.parse("2026-01-01T00:00:00Z"),
            until = Instant.parse("2026-02-01T00:00:00Z")
        ).first()

        assertEquals(1, careRepository.lastCareByPlantAndTypeCallCount)
    }
}
