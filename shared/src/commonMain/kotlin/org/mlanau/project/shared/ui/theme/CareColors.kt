package org.mlanau.project.shared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The accent colours for each kind of care, kept out of the Material [androidx.compose.material3.ColorScheme]
 * because they carry their own meaning (which task, and whether it is overdue or done) rather than a
 * surface/role. Provided through [LocalCareColors] so a screen just reads the tone it needs.
 */
data class CareColors(
    val water: Color,
    val fertilize: Color,
    val repot: Color,
    val overdue: Color,
    val done: Color
)
