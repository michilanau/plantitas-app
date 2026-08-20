package org.mlanau.project.plant.domain.model

/**
 * The task-specific data for a care task: how much to water, what to fertilize with, or what pot
 * to repot into. Shared by [CareRule], [org.mlanau.project.plant.domain.service.CareOccurrence]
 * and [CareLog]: a rule's [CareDetails] is the recurring recipe (e.g. "200ml of filtered water
 * every 3 days"), an occurrence's is a snapshot of that recipe for one predicted slot, and a log's
 * is what actually happened — which can differ from the recipe (e.g. logging a different amount
 * for a single watering without changing the recurring schedule).
 */
sealed interface CareDetails {
    data class Water(
        val amountMl: Int? = null,
        val useFilteredWater: Boolean = false
    ) : CareDetails

    data class Fertilize(
        val fertilizerName: String,
        val doseMl: Int? = null,
        val dilutionRatio: String? = null
    ) : CareDetails

    data class Repot(
        val newPotSize: PotSize,
        val substrateType: String? = null
    ) : CareDetails
}
