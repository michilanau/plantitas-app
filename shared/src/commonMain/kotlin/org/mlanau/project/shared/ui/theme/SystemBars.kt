package org.mlanau.project.shared.ui.theme

import androidx.compose.runtime.Composable

/**
 * Aligns the platform status/navigation bar icon contrast with the app's *in-app* theme, which can
 * differ from the OS setting (the user picks LIGHT / DARK / SYSTEM in Settings). Called from
 * [PlantitasTheme].
 */
@Composable
expect fun ApplySystemBarAppearance(darkTheme: Boolean)
