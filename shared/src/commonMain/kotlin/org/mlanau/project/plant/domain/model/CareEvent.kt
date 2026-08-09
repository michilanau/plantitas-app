package org.mlanau.project.plant.domain.model

import kotlin.time.Instant

enum class CareEventStatus { PENDING, DONE, SKIPPED }

sealed class CareEvent {
    abstract val id: Int?
    abstract val careRuleId: Int
    abstract val plantId: Int
    abstract val scheduledAt: Instant
    abstract val status: CareEventStatus
    abstract val completedAt: Instant?
    abstract val originalScheduledAt: Instant?
}

data class WaterCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: Instant,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: Instant? = null,
    override val originalScheduledAt: Instant? = null,
    val amountMl: Int? = null,
    val useFilteredWater: Boolean = false
) : CareEvent()

data class FertilizeCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: Instant,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: Instant? = null,
    override val originalScheduledAt: Instant? = null,
    val fertilizerName: String? = null,
    val doseMl: Int? = null,
    val dilutionRatio: String? = null
) : CareEvent()

data class RepotCareEvent(
    override val id: Int? = null,
    override val careRuleId: Int,
    override val plantId: Int,
    override val scheduledAt: Instant,
    override val status: CareEventStatus = CareEventStatus.PENDING,
    override val completedAt: Instant? = null,
    override val originalScheduledAt: Instant? = null,
    val newPotSize: PotSize? = null,
    val substrateType: String? = null
) : CareEvent()
