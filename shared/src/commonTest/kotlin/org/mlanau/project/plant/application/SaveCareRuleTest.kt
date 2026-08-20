package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.mlanau.project.plant.application.fixtures.FakeCareNotificationScheduler
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.application.fixtures.InMemoryPlantRepository
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareRule
import org.mlanau.project.plant.domain.model.Plant
import org.mlanau.project.plant.domain.model.RecurrenceRule
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler

/**
 * With occurrences 100% derived from the rule and the logged-care anchor, there's nothing
 * materialized for a save to accidentally discard — the "silently wipes reprogrammed events" class
 * of bug this test class used to guard against, back when occurrences were persisted CareEvent
 * rows, no longer has a way to exist. What's left to verify is the plain persistence contract.
 */
class SaveCareRuleTest {

    private val start = Instant.parse("2026-01-01T09:00:00Z")

    private fun useCase(): Triple<SaveCareRule, InMemoryCareRepository, InMemoryPlantRepository> {
        val careRepository = InMemoryCareRepository()
        val plantRepository = InMemoryPlantRepository()
        val rescheduleCareReminder = RescheduleCareReminder(
            careRepository, plantRepository, CareOccurrenceScheduler(), FakeCareNotificationScheduler()
        )
        return Triple(SaveCareRule(careRepository, rescheduleCareReminder), careRepository, plantRepository)
    }

    @Test
    fun `saves a brand new rule`() = runBlocking {
        val (saveCareRule, careRepository, plantRepository) = useCase()
        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = start))

        val newRule = CareRule(
            plantId = plantId,
            recurrence = RecurrenceRule.Once,
            startDate = start,
            details = CareDetails.Fertilize(fertilizerName = "Compost")
        )

        val result = saveCareRule(newRule)

        assertEquals(true, result.isSuccess)
        assertEquals(1, careRepository.getAllCareRules().first().size)
    }

    @Test
    fun `reassigns the rule to the given plantId, overriding the rule's own`() = runBlocking {
        val (saveCareRule, careRepository, plantRepository) = useCase()
        val originalPlantId = plantRepository.save(Plant.create(name = "Placeholder", description = null, createdAt = start))
        val newPlantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = start))

        val rule = CareRule(
            plantId = originalPlantId,
            recurrence = RecurrenceRule.Once,
            startDate = start,
            details = CareDetails.Water(amountMl = 200)
        )

        saveCareRule(rule, plantId = newPlantId)

        assertEquals(newPlantId, careRepository.getAllCareRules().first().single().plantId)
    }

    @Test
    fun `edited rules keep their id instead of creating a duplicate`() = runBlocking {
        val (saveCareRule, careRepository, plantRepository) = useCase()
        val plantId = plantRepository.save(Plant.create(name = "Monstera", description = null, createdAt = start))
        val ruleId = careRepository.saveCareRule(
            CareRule(
                plantId = plantId,
                recurrence = RecurrenceRule.Periodic(everyDays = 3),
                startDate = start,
                details = CareDetails.Water(amountMl = 100)
            )
        )

        val existingRule = careRepository.getCareRules(plantId).first().single()
        saveCareRule(existingRule.copy(details = CareDetails.Water(amountMl = 250)))

        val rules = careRepository.getCareRules(plantId).first()
        assertEquals(1, rules.size)
        assertEquals(ruleId, rules.single().id)
    }
}
