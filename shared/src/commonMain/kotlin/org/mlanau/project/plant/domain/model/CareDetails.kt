package org.mlanau.project.plant.domain.model

import org.mlanau.project.plant.domain.exception.BlankFertilizerNameException
import org.mlanau.project.plant.domain.exception.CorruptedRecordException
import org.mlanau.project.plant.domain.exception.NonPositiveAmountException

sealed interface CareDetails {
    val type: CareType

    @ConsistentCopyVisibility
    data class Water private constructor(
        val amountMl: Int?,
        val useFilteredWater: Boolean
    ) : CareDetails {
        override val type: CareType get() = CareType.WATER

        init {
            if (amountMl != null && amountMl <= 0) throw NonPositiveAmountException()
        }

        companion object {
            fun create(amountMl: Int? = null, useFilteredWater: Boolean = false): Water =
                Water(amountMl, useFilteredWater)

            /** Rehydrates from persistence; a `NULL` `useFilteredWater` column means "unset", i.e. `false`. */
            fun restore(amountMl: Long?, useFilteredWater: Long?): Water =
                Water(amountMl?.toInt(), useFilteredWater == 1L)
        }
    }

    @ConsistentCopyVisibility
    data class Fertilize private constructor(
        val fertilizerName: String,
        val doseMl: Int?,
        val dilutionRatio: String?
    ) : CareDetails {
        override val type: CareType get() = CareType.FERTILIZE

        init {
            if (fertilizerName.isBlank()) throw BlankFertilizerNameException()
            if (doseMl != null && doseMl <= 0) throw NonPositiveAmountException()
        }

        companion object {
            fun create(fertilizerName: String, doseMl: Int? = null, dilutionRatio: String? = null): Fertilize =
                Fertilize(fertilizerName.trim(), doseMl, dilutionRatio?.trim()?.takeIf { it.isNotBlank() })

            /**
             * Rehydrates from persistence. `fertilizerName` is a nullable `TEXT` column at the
             * schema level even though every row written through [create] always has one; a `NULL`
             * there means the record is corrupt, not that the name is optional.
             */
            fun restore(recordId: Int, fertilizerName: String?, doseMl: Long?, dilutionRatio: String?): Fertilize {
                if (fertilizerName.isNullOrBlank()) {
                    throw CorruptedRecordException(recordId, "fertilize care missing fertilizerName")
                }
                return Fertilize(fertilizerName.trim(), doseMl?.toInt(), dilutionRatio?.trim()?.takeIf { it.isNotBlank() })
            }
        }
    }

    @ConsistentCopyVisibility
    data class Repot private constructor(
        val newPotSize: PotSize,
        val substrateType: String?
    ) : CareDetails {
        override val type: CareType get() = CareType.REPOT

        companion object {
            fun create(newPotSize: PotSize, substrateType: String? = null): Repot =
                Repot(newPotSize, substrateType?.trim()?.takeIf { it.isNotBlank() })

            /** Rehydrates from persistence; `newPotSize` missing or unrecognized means the record is corrupt. */
            fun restore(recordId: Int, newPotSize: String?, substrateType: String?): Repot {
                val potSize = newPotSize?.let { runCatching { PotSize.valueOf(it) }.getOrNull() }
                    ?: throw CorruptedRecordException(recordId, "repot care missing or invalid newPotSize")
                return Repot(potSize, substrateType?.trim()?.takeIf { it.isNotBlank() })
            }
        }
    }
}
