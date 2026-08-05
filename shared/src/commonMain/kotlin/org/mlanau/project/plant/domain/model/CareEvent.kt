package org.mlanau.project.plant.domain.model

import kotlinx.datetime.LocalDateTime

enum class CareEventStatus { PENDING, DONE, SKIPPED }

sealed class CareEvent {
    abstract val id: Int?
    abstract val careRuleId: Int
    abstract val plantId: Int
    abstract val scheduledAt: LocalDateTime
    abstract val status: CareEventStatus
    abstract val completedAt: LocalDateTime?
    abstract val originalScheduledAt: LocalDateTime?
}

data class WaterCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: LocalDateTime,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: LocalDateTime? = null,
    override val originalScheduledAt: LocalDateTime? = null,
    val amountMl: Int? = null,
    val useFilteredWater: Boolean = false
) : CareEvent()

data class FertilizeCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: LocalDateTime,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: LocalDateTime? = null,
    override val originalScheduledAt: LocalDateTime? = null,
    val fertilizerName: String? = null,
    val doseMl: Int? = null,
    val dilutionRatio: String? = null
) : CareEvent()

data class RepotCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: LocalDateTime,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: LocalDateTime? = null,
    override val originalScheduledAt: LocalDateTime? = null,
    val newPotSize: PotSize? = null,
    val substrateType: String? = null
) : CareEvent()
