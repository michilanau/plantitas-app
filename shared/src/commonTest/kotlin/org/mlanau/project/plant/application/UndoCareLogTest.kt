package org.mlanau.project.plant.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.mlanau.project.plant.application.fixtures.FakeCareNotificationScheduler
import org.mlanau.project.plant.application.fixtures.InMemoryCareRepository
import org.mlanau.project.plant.application.fixtures.InMemoryPlantRepository
import org.mlanau.project.plant.domain.model.CareDetails
import org.mlanau.project.plant.domain.model.CareLog
import org.mlanau.project.plant.domain.model.CareRuleId
import org.mlanau.project.plant.domain.model.PlantId
import org.mlanau.project.plant.domain.service.CareOccurrenceScheduler

class UndoCareLogTest {

    private val plantId = PlantId(1)
    private val ruleId = CareRuleId(1)

    @Test
    fun `deletes the log — its anchor contribution disappears with it`() = runBlocking {
        val repository = InMemoryCareRepository()
        val log = CareLog(
            plantId = plantId,
            careRuleId = ruleId,
            performedAt = Instant.parse("2026-01-15T09:00:00Z"),
            details = CareDetails.Water(amountMl = 200)
        )
        repository.seedLog(log)
        val savedId = repository.persistedLogs().single().id!!

        val rescheduleCareReminder = RescheduleCareReminder(
            repository, InMemoryPlantRepository(), CareOccurrenceScheduler(), FakeCareNotificationScheduler()
        )
        val undoCareLog = UndoCareLog(repository, rescheduleCareReminder)
        val result = undoCareLog(savedId)

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), repository.persistedLogs())
    }
}
