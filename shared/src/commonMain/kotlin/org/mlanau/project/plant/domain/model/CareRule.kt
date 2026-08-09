package org.mlanau.project.plant.domain.model

import kotlin.time.Instant
import org.mlanau.project.plant.domain.exceptions.InvalidCareRuleDateRangeException
import org.mlanau.project.plant.domain.exceptions.InvalidRecurrenceException

sealed class RecurrenceRule {
    data object Once : RecurrenceRule()

    data class Periodic(val everyDays: Int) : RecurrenceRule() {
        init {
            if (everyDays <= 0) throw InvalidRecurrenceException()
        }
    }
}

sealed class CareRule {
    abstract val id: Int?
    abstract val plantId: Int
    abstract val recurrence: RecurrenceRule
    abstract val startDate: Instant
    abstract val endDate: Instant?
    abstract val active: Boolean

    init {
        if (endDate != null && endDate!! < startDate) throw InvalidCareRuleDateRangeException()
    }
}

data class WaterCareRule(
    override val id: Int? = null,
    override val plantId: Int,
    override val recurrence: RecurrenceRule,
    override val startDate: Instant,
    override val endDate: Instant? = null,
    override val active: Boolean = true,
    val amountMl: Int? = null,
    val useFilteredWater: Boolean = false
) : CareRule()

data class FertilizeCareRule(
    override val id: Int? = null,
    override val plantId: Int,
    override val recurrence: RecurrenceRule,
    override val startDate: Instant,
    override val endDate: Instant? = null,
    override val active: Boolean = true,
    val fertilizerName: String,
    val doseMl: Int? = null,
    val dilutionRatio: String? = null
) : CareRule()

data class RepotCareRule(
    override val id: Int? = null,
    override val plantId: Int,
    override val recurrence: RecurrenceRule,
    override val startDate: Instant,
    override val endDate: Instant? = null,
    override val active: Boolean = true,
    val newPotSize: PotSize,
    val substrateType: String? = null
) : CareRule()
