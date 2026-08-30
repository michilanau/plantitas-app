package org.mlanau.project.di

import org.koin.mp.KoinPlatform

/**
 * iOS entry point for dependency injection. The Android app starts Koin from its `Application`;
 * iOS has no equivalent, so `iOSApp.swift` must call this before the first Compose screen is
 * shown — otherwise every `koinInject` / `koinViewModel` in `App()` fails. Guarded so a hot
 * reload or a second call is harmless.
 */
fun startKoinIfNeeded() {
    if (KoinPlatform.getKoinOrNull() == null) {
        initKoin()
    }
}
