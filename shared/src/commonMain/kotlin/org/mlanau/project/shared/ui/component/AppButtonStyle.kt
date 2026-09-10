package org.mlanau.project.shared.ui.component

enum class AppButtonStyle {
    /** The one action a block is for, such as "Mark as done" or "Save". Ink on paper. */
    Primary,

    /** A secondary action that should still stand out, such as "Log care". Lime. */
    Accent,

    /** A quiet action, such as "Add care". Outlined. */
    Outline,

    /** A destructive action, such as "Delete plant". Outlined in the overdue colour. */
    Danger
}
