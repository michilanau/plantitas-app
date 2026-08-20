package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mlanau.project.plant.application.fixtures.FakeCareNotificationScheduler
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.application.fixtures.InMemoryPlantRepository
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.CareType
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.plant.domain.service.CareOccurrence
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler
import org.mlanau.project.plant.domain.service.OccurrenceStatus

class DismissCareOccurrenceTest {

    private val plantId = PlantId(1)
    private val ruleId = CareRuleId(1)
    private val now = Instant.parse("2026-01-15T09:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }

    private fun dismissCareOccurrence(repository: InMemoryCareRepository): DismissCareOccurrence {
        val rescheduleCareReminder = RescheduleCareReminder(
            repository, InMemoryPlantRepository(), CareOccurrenceScheduler(), FakeCareNotificationScheduler(), fixedClock
        )
        return DismissCareOccurrence(repository, rescheduleCareReminder, fixedClock)
    }

    private suspend fun InMemoryCareRepository.seedRule() {
        saveCareRule(
            CareRule(
                id = ruleId,
                plantId = plantId,
                recurrence = RecurrenceRule.Periodic(everyDays = 3),
                startDate = Instant.parse("2026-01-01T09:00:00Z"),
                details = CareDetails.Water(amountMl = 200)
            )
        )
    }

    private fun occurrence(scheduledAt: Instant, status: OccurrenceStatus) = CareOccurrence(
        careRuleId = ruleId,
        plantId = plantId,
        type = CareType.WATER,
        scheduledAt = scheduledAt,
        details = CareDetails.Water(amountMl = 200),
        status = status,
        lastCareAt = null
    )

    @Test
    fun `dismissing a future scheduled occurrence caps dismissedBefore at its own scheduledAt`() = runBlocking {
        val repository = InMemoryCareRepository()
        repository.seedRule()
        val future = Instant.parse("2026-01-20T09:00:00Z")
        val dismiss = dismissCareOccurrence(repository)

        dismiss(occurrence(future, OccurrenceStatus.SCHEDULED))

        val rule = repository.getCareRule(ruleId)
        assertEquals(future, rule?.dismissedBefore)
    }

    @Test
    fun `dismissing a collapsed overdue occurrence caps dismissedBefore at now, clearing the whole missed range`() = runBlocking {
        val repository = InMemoryCareRepository()
        repository.seedRule()
        // The overdue occurrence's own scheduledAt is far in the past (the oldest missed slot);
        // maxOf(now, scheduledAt) must land on `now`, not that old slot.
        val longOverdue = Instant.parse("2026-01-01T09:00:00Z")
        val dismiss = dismissCareOccurrence(repository)

        dismiss(occurrence(longOverdue, OccurrenceStatus.OVERDUE))

        val rule = repository.getCareRule(ruleId)
        assertEquals(now, rule?.dismissedBefore)
    }

    @Test
    fun `dismissing an older occurrence after a more recent dismissal does not move it back`() = runBlocking {
        val repository = InMemoryCareRepository()
        repository.seedRule()
        val dismiss = dismissCareOccurrence(repository)

        dismiss(occurrence(Instant.parse("2026-01-20T09:00:00Z"), OccurrenceStatus.SCHEDULED))
        dismiss(occurrence(Instant.parse("2026-01-10T09:00:00Z"), OccurrenceStatus.OVERDUE))

        val rule = repository.getCareRule(ruleId)
        assertEquals(Instant.parse("2026-01-20T09:00:00Z"), rule?.dismissedBefore)
    }
}
