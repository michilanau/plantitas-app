package org.mlanau.project.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
actual fun ApplySystemBarAppearance(darkTheme: Boolean) {
    // ContentView.swift observes StatusBarAppearance and applies it via .preferredColorScheme —
    // see that object's doc for why the status bar can't be styled from Compose directly.
    SideEffect { StatusBarAppearance.update(darkTheme) }
}
