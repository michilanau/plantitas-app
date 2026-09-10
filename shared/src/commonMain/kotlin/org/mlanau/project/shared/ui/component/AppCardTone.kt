package org.mlanau.project.shared.ui.component

/**
 * What a card is saying, which is what decides how it is painted. Keeping this an enum rather than
 * loose colour arguments is what stops the app growing a third and fourth card recipe.
 */
enum class AppCardTone {
    /** A plant, a care rule, a task or a log entry: a plain card on the paper. */
    Default,

    /** An overdue care. A soft raspberry ground so it reads before anything else on screen. */
    Alert
}
