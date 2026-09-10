package org.mlanau.project.shared.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The accent colours for each kind of care, kept out of the Material [androidx.compose.material3.ColorScheme]
 * because they carry their own meaning (which task, and whether it is overdue or done) rather than a
 * surface/role. Each accent has a soft `Container` ground for tinted badges and pills, and
 * [onAccent] is the content colour on a solid accent. Provided through [LocalCareColors].
 */
data class CareColors(
    val water: Color,
    val waterContainer: Color,
    val fertilize: Color,
    val fertilizeContainer: Color,
    val repot: Color,
    val repotContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val done: Color,
    val onAccent: Color
)
