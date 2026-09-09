package org.mlanau.project.shared.ui.theme

import androidx.compose.runtime.Composable

@Composable
actual fun ApplySystemBarAppearance(darkTheme: Boolean) {
    // The iOS status bar style is driven from ContentView.swift via .preferredColorScheme(...);
    // there is nothing to set from Compose here yet.
}
