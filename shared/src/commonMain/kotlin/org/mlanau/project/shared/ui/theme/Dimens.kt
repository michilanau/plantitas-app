package org.mlanau.project.shared.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The measurements every screen shares, so the app reads as one thing rather than as seven screens
 * that each picked their own numbers.
 *
 * Corner radii are NOT here — they live in the theme's `Shapes`: `shapes.large` for cards,
 * `shapes.medium` for rows, `shapes.small` for fields and `shapes.extraLarge` for the plant hero and
 * sheets. Buttons, chips and the tab bar are fully rounded.
 */

/** The single vertical line every screen's content aligns to. */
val ScreenGutter = 20.dp

/** Content stops widening here so a tablet gets a readable column instead of a stretched phone. */
val ContentMaxWidth = 640.dp

/** Full-width primary/secondary actions. Comfortably above the 44 dp touch-target floor. */
val ActionButtonHeight = 54.dp

/** Bottom room a tab screen leaves so its last item and its snackbar clear the floating tab bar. */
val TabBarClearance = 112.dp

/** Icon ladder: metadata next to small text, icons inside buttons and inline runs, list row leading. */
val MetaIconSize = 16.dp
val InlineIconSize = 20.dp
val RowIconSize = 20.dp
