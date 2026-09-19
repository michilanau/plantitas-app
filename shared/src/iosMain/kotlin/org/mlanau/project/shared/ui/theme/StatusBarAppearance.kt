package org.mlanau.project.shared.ui.theme

/**
 * Bridges the app's effective dark/light theme (LIGHT / DARK / SYSTEM resolved by `App()`, which
 * can differ from the OS setting) out to `ContentView.swift`, which applies it via SwiftUI's
 * `.preferredColorScheme` — the trait then propagates down into the embedded Compose view
 * controller and drives its status bar style the normal UIKit way. There is no Kotlin-bound
 * `UIApplication.setStatusBarStyle:` to call directly (that API was dropped from the SDK), so the
 * status bar can only be styled through this trait, not from Compose alone.
 */
object StatusBarAppearance {
    private var listener: ((Boolean) -> Unit)? = null
    var isDark: Boolean = false
        private set

    fun setListener(block: (Boolean) -> Unit) {
        listener = block
        block(isDark)
    }

    internal fun update(dark: Boolean) {
        isDark = dark
        listener?.invoke(dark)
    }
}
