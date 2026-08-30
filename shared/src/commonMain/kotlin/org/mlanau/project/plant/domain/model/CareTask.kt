package org.mlanau.project.plant.domain.model

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.mlanau.project.plant.domain.exception.FutureCareTaskException
import org.mlanau.project.plant.domain.exception.InvalidMissedCountException
import org.mlanau.project.plant.domain.exception.TaskNotDueYetException

sealed interface CareTask {
    val plantId: PlantId
    val care: CareDetails
    val at: Instant
    val type: CareType get() = care.type

    /**
     * A care a rule says is owed. It is a prediction, never a record: nothing is persisted until
     * it is [complete]d into a [Done].
     *
     * [dueAt] and [at] come apart once a task is overdue. [dueAt] stays pinned to the oldest slot
     * still unanswered — it is what "N days late" is counted from, and it never moves as more
     * time passes — while [at] is where the task belongs on the calendar, which for an overdue
     * task is the present day, so a forgotten plant can never be scrolled out of sight.
     */
    data class Pending(
        val careRuleId: CareRuleId,
        override val plantId: PlantId,
        override val care: CareDetails,
        val dueAt: Instant,
        override val at: Instant,
        val status: PendingStatus,
        val lastPerformedAt: Instant?,
        val missedCount: Int = 1
    ) : CareTask {
        init {
            if (missedCount < 1) throw InvalidMissedCountException()
        }

        val isOverdue: Boolean get() = status == PendingStatus.OVERDUE

        /**
         * Whether this task can be ticked off yet: only today's and overdue ones can. The
         * comparison is by calendar day rather than by instant, so a task due at 21:00 is
         * completable from the moment the day starts — "I've already watered it today" is a
         * statement about the day, not about the reminder's clock time.
         */
        fun isCompletableOn(now: Instant, timeZone: TimeZone): Boolean =
            dueAt.toLocalDateTime(timeZone).date <= now.toLocalDateTime(timeZone).date

        fun complete(
            performedAt: Instant,
            now: Instant,
            timeZone: TimeZone,
            care: CareDetails = this.care,
            note: String? = null
        ): Done {
            // A task the user hasn't reached yet isn't done early, it just isn't this task: caring
            // ahead of schedule is an ad-hoc care, which re-anchors the rhythm on its own.
            if (!isCompletableOn(now, timeZone)) throw TaskNotDueYetException()
            return Done.create(
                plantId = plantId,
                careRuleId = careRuleId,
                care = care,
                performedAt = performedAt,
                now = now,
                scheduledAt = dueAt,
                note = note
            )
        }
    }

    @ConsistentCopyVisibility
    data class Done private constructor(
        val id: CareTaskId?,
        override val plantId: PlantId,
        val careRuleId: CareRuleId?,
        override val care: CareDetails,
        val performedAt: Instant,
        val scheduledAt: Instant?,
        val note: String?
    ) : CareTask {
        override val at: Instant get() = performedAt

        fun withId(newId: CareTaskId): Done = copy(id = newId)

        companion object {
            /** Builds a new, not-yet-persisted completed care: [id] is always null. */
            fun create(
                plantId: PlantId,
                careRuleId: CareRuleId?,
                care: CareDetails,
                performedAt: Instant,
                now: Instant,
                scheduledAt: Instant? = null,
                note: String? = null
            ): Done {
                if (performedAt > now) throw FutureCareTaskException()
                return Done(
                    id = null,
                    plantId = plantId,
                    careRuleId = careRuleId,
                    care = care,
                    performedAt = performedAt,
                    scheduledAt = scheduledAt,
                    note = note?.takeIf { it.isNotBlank() }
                )
            }

            /**
             * Rehydrates a completed care from persistence. Applies the same normalization as
             * [create] — except the future-date check, which only makes sense against the clock a
             * care was originally logged with — so a legacy row storing `''` instead of `NULL`
             * doesn't enter the domain as a blank note.
             */
            fun restore(
                id: CareTaskId,
                plantId: PlantId,
                careRuleId: CareRuleId?,
                care: CareDetails,
                performedAt: Instant,
                scheduledAt: Instant?,
                note: String?
            ): Done = Done(
                id = id,
                plantId = plantId,
                careRuleId = careRuleId,
                care = care,
                performedAt = performedAt,
                scheduledAt = scheduledAt,
                note = note?.takeIf { it.isNotBlank() }
            )
        }
    }
}
