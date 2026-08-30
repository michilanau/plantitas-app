package org.mlanau.project.plant.presentation

import org.mlanau.project.plant.domain.model.CareTaskId

/**
 * One-shot feedback for a care action, delivered over a channel rather than held in UI state so it
 * fires exactly once and never re-shows on recomposition.
 */
sealed interface CareToast {
    /** A care was just logged (scheduled task completed, or ad-hoc); [taskId] backs the UNDO. */
    data class Logged(val taskId: CareTaskId) : CareToast

    /** A previously logged care was removed. */
    data object Undone : CareToast
}
